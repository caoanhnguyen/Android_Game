package com.mygdx.td.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Tower {

    public enum State { IDLE, PREATTACK, ATTACK, RECOVER }

    public final Vector2 pos = new Vector2();
    public Rectangle placeRect = null; // nếu đặt theo spot

    // Combat stats
    private float range = 150f;
    private int damage = 15;

    // Attack timings (giây) – chỉnh theo cảm giác/asset
    public float preattackSec = 0.18f;
    public float fireOffsetInAttackSec = 0.05f; // đạn bay ra ngay đầu ATTACK một chút
    public float attackSec = 0.20f;
    public float recoverSec = 0.12f;

    // Cooldown điều khiển nhịp bắn
    private float cooldown = 0f;

    // State machine cho anim unit
    private State state = State.IDLE;
    private float stateTime = 0f;
    private final Vector2 aimDir = new Vector2(1, 0); // hướng nhìn của unit khi idle/attack

    public Tower(float x, float y) { this.pos.set(x, y); }
    public Tower(float x, float y, Rectangle r) { this(x, y); this.placeRect = r; }

    public void update(float dt) {
        if (cooldown > 0f) cooldown -= dt;

        stateTime += dt;
        switch (state) {
            case PREATTACK:
                if (stateTime >= preattackSec) {
                    // chuyển sang ATTACK
                    state = State.ATTACK;
                    stateTime = 0f;
                }
                break;
            case ATTACK:
                if (stateTime >= attackSec) {
                    state = State.RECOVER;
                    stateTime = 0f;
                }
                break;
            case RECOVER:
                if (stateTime >= recoverSec) {
                    state = State.IDLE;
                    stateTime = 0f;
                }
                break;
            default:
                break;
        }
    }

    // World sẽ gọi khi đã chọn mục tiêu và được phép bắn (cooldown <= 0)
    // Trả về độ trễ tính từ bây giờ đến lúc bắn (giây)
    public float beginAttackTowards(float targetX, float targetY) {
        // đặt hướng nhìn chốt cho cả vòng tấn công
        aimDir.set(targetX - pos.x, targetY - pos.y);
        if (aimDir.isZero()) aimDir.set(1, 0); else aimDir.nor();

        state = State.PREATTACK;
        stateTime = 0f;

        // reset cooldown theo tổng chu kỳ
        cooldown = getAttackCycleSec();
        // Fire moment: cuối preattack + offset đầu ATTACK
        return preattackSec + fireOffsetInAttackSec;
    }

    public boolean canFire() {
        return cooldown <= 0f && state == State.IDLE;
    }

    public void resetFireCooldown() {
        cooldown = getAttackCycleSec();
    }

    public float getAttackCycleSec() {
        return preattackSec + attackSec + recoverSec;
    }

    public Vector2 getAimDir() { return aimDir; }
    public State getState() { return state; }
    public float getStateTime() { return stateTime; }

    public float getRange() { return range; }
    public int getDamage() { return damage; }

    public void setRange(float r) { this.range = r; }
    public void setDamage(int d) { this.damage = d; }
}
