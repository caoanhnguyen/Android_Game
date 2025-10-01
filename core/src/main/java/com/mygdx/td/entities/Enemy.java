package com.mygdx.td.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.mygdx.td.world.Path;
import com.mygdx.td.Constants;

/**
 * Enemy di chuyển dọc theo các waypoint trong Path.
 * Hỗ trợ:
 *  - HP / getHpPercent()
 *  - reachedEnd (đến cuối path)
 *  - dead (bị tiêu diệt, có deathTimer để chơi hoạt ảnh chết)
 *  - slow effect (applySlow)
 *
 * Bổ sung:
 *  - dir (hướng di chuyển) để renderer chọn animation/hướng mặt.
 *  - deathTimer: thời gian còn lại trước khi cho phép remove enemy khi đã chết.
 *  - goldReward: vàng rơi theo từng enemy (để WaveManager gán theo loại).
 *  - damageTakenMultiplier: hệ số sát thương nhận (boss có thể giảm).
 *  - shield pulse (mid-boss): định kỳ giảm sát thương trong shieldDuration.
 *  - enrage (final boss): khi HP <= threshold, tăng speed.
 */
public class Enemy {

    public final Vector2 pos = new Vector2();
    private final Vector2 dir = new Vector2(1, 0); // hướng chuẩn hoá

    public float hp;
    public float maxHp;

    public boolean dead = false;
    public boolean reachedEnd = false;

    // Đếm ngược cho hoạt ảnh chết (để World không remove ngay)
    public float deathTimer = 0f; // giây

    private final Array<Vector2> waypoints;
    private int currentIndex = 0;
    private float baseSpeed;
    private float slowFactor = 1f;
    private float slowTimer = 0f;

    private static final float WAYPOINT_EPSILON = 2f;

    // Thưởng vàng khi hạ gục
    public int goldReward = 5;

    // Hệ số sát thương nhận (1f = bình thường, 0.9f = nhận 90% dmg)
    public float damageTakenMultiplier = 1f;

    // Shield pulse cho mid-boss
    public boolean shieldPulsing = false;
    public float shieldPeriod = 6f;        // mỗi 6s
    public float shieldDuration = 2f;      // bật khiên 2s
    public float shieldDmgMultiplier = 0.5f; // nhận 50% dmg khi có khiên
    private float shieldPhase = 0f;        // timer chu kỳ

    // Enrage cho final boss
    public boolean enrageEnabled = false;
    public float enrageThreshold = 0.5f; // 50% HP
    public float enrageSpeedMultiplier = 1.2f;
    private boolean enraged = false;

    public Enemy(Path path) {
        this.waypoints = path.getWaypoints();
        if (waypoints.size == 0) throw new IllegalStateException("Path has no waypoints");

        pos.set(waypoints.first());
        currentIndex = 1;

        baseSpeed = Constants.ENEMY_BASE_SPEED;
        maxHp = hp = Constants.ENEMY_HP;
    }

    public void update(float dt) {
        if (reachedEnd) return;

        // Nếu đã chết: chỉ đếm deathTimer
        if (dead) {
            if (deathTimer > 0f) deathTimer -= dt;
            return;
        }

        // Shield pulse (mid-boss)
        if (shieldPulsing) {
            float cycle = shieldPeriod + shieldDuration;
            shieldPhase += dt;
            while (shieldPhase >= cycle) shieldPhase -= cycle;
        }

        // Enrage (final boss)
        if (enrageEnabled && !enraged && hp <= maxHp * enrageThreshold) {
            setBaseSpeed(baseSpeed * enrageSpeedMultiplier);
            enraged = true;
        }

        // Cập nhật slow (nếu có)
        if (slowTimer > 0f) {
            slowTimer -= dt;
            if (slowTimer <= 0f) { slowTimer = 0f; slowFactor = 1f; }
        }

        // Nếu đã hết waypoint -> reachedEnd
        if (currentIndex >= waypoints.size) {
            reachedEnd = true;
            return;
        }

        Vector2 target = waypoints.get(currentIndex);
        float effectiveSpeed = baseSpeed * slowFactor;
        moveTowards(target, effectiveSpeed * dt);

        // Kiểm tra tới gần waypoint
        if (pos.dst2(target) <= WAYPOINT_EPSILON * WAYPOINT_EPSILON) {
            currentIndex++;
            if (currentIndex >= waypoints.size) reachedEnd = true;
        }
    }

    private void moveTowards(Vector2 target, float maxStep) {
        float dx = target.x - pos.x;
        float dy = target.y - pos.y;
        float dist2 = dx * dx + dy * dy;
        if (dist2 == 0) return;
        float dist = (float) Math.sqrt(dist2);
        if (dist <= maxStep) {
            pos.set(target);
        } else {
            float inv = 1f / dist;
            pos.x += dx * inv * maxStep;
            pos.y += dy * inv * maxStep;
            dir.set(dx * inv, dy * inv); // cập nhật hướng
        }
    }

    public void damage(float dmg) {
        if (dead || reachedEnd) return;

        float mult = damageTakenMultiplier;
        if (shieldPulsing) {
            // Trong khoảng shieldDuration ở đầu chu kỳ -> bật khiên
            if (shieldPhase >= 0f && shieldPhase <= shieldDuration) {
                mult *= shieldDmgMultiplier;
            }
        }

        hp -= (dmg * mult);
        if (hp <= 0f) {
            hp = 0f;
            dead = true;
            // Thời lượng mặc định cho hoạt ảnh chết (có thể tinh chỉnh theo strip của bạn)
            deathTimer = 0.9f; // ~ 6 frame * 0.10s + chút trễ
        }
    }

    public float getHpPercent() { return maxHp <= 0f ? 0f : hp / maxHp; }

    public void applySlow(float percent, float duration) {
        if (percent <= 0f || duration <= 0f) return;
        float newFactor = 1f - percent;
        if (newFactor < 0.1f) newFactor = 0.1f;

        if (newFactor < slowFactor) { slowFactor = newFactor; slowTimer = duration; }
        else if (newFactor == slowFactor) { if (duration > slowTimer) slowTimer = duration; }
        else { if (slowTimer < duration * 0.5f) slowTimer = duration * 0.5f; }
    }

    public void setBaseSpeed(float speed) { this.baseSpeed = speed; }

    // Cho World biết khi nào remove được (sau khi chết và hết timer, hoặc tới đích)
    public boolean isRemovable() {
        return reachedEnd || (dead && deathTimer <= 0f);
    }

    // Getters cho renderer/tower
    public Vector2 getPos() { return pos; }
    public Vector2 getDir() { return dir; }
    public boolean isDead() { return dead; }
    public boolean hasReachedEnd() { return reachedEnd; }

    public int getGoldReward() { return goldReward; }
}
