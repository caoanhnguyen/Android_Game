package com.mygdx.td.world;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.managers.WaveManager;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.td.Constants;

public class World {

    public final Array<Tower> towers = new Array<>();
    public final Array<Enemy> enemies = new Array<>();
    public final Array<Bullet> bullets = new Array<>();

    public final Path path = new Path();
    public final WaveManager waveManager = new WaveManager(this);

    public final Array<TowerSpot> towerSpots = new Array<>();

    public int gold = 150;
    public int lives = 20;

    private boolean lifeLostFlag = false;
    public boolean gameOver = false;

    private float bulletCleanupTimer = 0f;

    // Pending shots (đợi tới fire moment)
    private static class PendingShot {
        Tower tower; Enemy target; float t;
        PendingShot(Tower tower, Enemy target, float t) { this.tower = tower; this.target = target; this.t = t; }
    }
    private final Array<PendingShot> pendingShots = new Array<>();

    public World() {}

    public void update(float dt) {
        if (gameOver) return;

        waveManager.update(dt);

        // Enemies
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt);
            if (e.reachedEnd && !e.dead) {
                lives--;
                lifeLostFlag = true;
                e.dead = true;
                if (lives <= 0) { lives = 0; triggerGameOver(); }
            }
            if (e.dead) {
                // remove luôn – nếu bạn dùng deathTimer trong Enemy, hãy đổi logic theo file trước đó
                enemies.removeIndex(i);
            }
        }

        // Towers
        for (Tower t : towers) {
            t.update(dt);
            Enemy target = findTarget(t.pos.x, t.pos.y, t.getRange());
            // Cập nhật aim khi không bắn
            if (target != null && t.getState() == Tower.State.IDLE) {
                // set hướng nhìn mượt – dùng beginAttack để chốt
                Vector2 dir = new Vector2(target.pos).sub(t.pos);
                if (!dir.isZero()) dir.nor();
                t.getAimDir().set(dir);
            }

            if (target != null && t.canFire()) {
                // bắt đầu chu kỳ tấn công
                float delay = t.beginAttackTowards(target.pos.x, target.pos.y);
                pendingShots.add(new PendingShot(t, target, delay));
            }
        }

        // Pending shots – tới thời điểm bắn mới spawn đạn
        for (int i = pendingShots.size - 1; i >= 0; i--) {
            PendingShot p = pendingShots.get(i);
            p.t -= dt;
            if (p.t <= 0f) {
                if (p.target != null && !p.target.dead) {
                    spawnBullet(p.tower, p.target);
                }
                pendingShots.removeIndex(i);
            }
        }

        // Bullets
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(dt);
            if (b.dead) {
                bullets.removeIndex(i);
                continue;
            }
            Enemy hit = bulletHit(b);
            if (hit != null) {
                hit.damage(b.damage);
                b.dead = true;
                bullets.removeIndex(i);
            }
        }

        // Cleanup bullets ra ngoài màn
        bulletCleanupTimer += dt;
        if (bulletCleanupTimer >= 3f) {
            bulletCleanupTimer = 0f;
            for (int i = bullets.size - 1; i >= 0; i--) {
                Bullet b = bullets.get(i);
                if (b.pos.x < -64 || b.pos.x > Constants.VIRTUAL_WIDTH + 64
                    || b.pos.y < -64 || b.pos.y > Constants.VIRTUAL_HEIGHT + 64) {
                    bullets.removeIndex(i);
                }
            }
        }
    }

    private void triggerGameOver() {
        gameOver = true;
        waveManager.forceStop();
        pendingShots.clear();
    }

    public boolean canAffordTower() {
        return gold >= 50;
    }

    public boolean placeTowerFree(float x, float y) {
        if (!canAffordTower()) return false;
        Tower t = new Tower(x, y);
        towers.add(t);
        gold -= 50;
        return true;
    }

    public boolean placeTowerOnSpot(TowerSpot spot) {
        if (!canAffordTower() || spot.used) return false;
        Tower t = new Tower(spot.rect.x + spot.rect.width / 2f, spot.rect.y + spot.rect.height / 2f, spot.rect);
        towers.add(t);
        spot.used = true;
        gold -= 50;
        return true;
    }

    private Enemy findTarget(float x, float y, float range) {
        float r2 = range * range;
        Enemy best = null;
        float bestDist2 = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.dead || e.reachedEnd) continue;
            float d2 = e.pos.dst2(x, y);
            if (d2 <= r2 && d2 < bestDist2) { bestDist2 = d2; best = e; }
        }
        return best;
    }

    private void spawnBullet(Tower t, Enemy target) {
        Bullet b = new Bullet();
        b.pos.set(t.pos);
        b.target = target;
        b.speed = 300f;
        b.damage = t.getDamage();
        bullets.add(b);
    }

    private Enemy bulletHit(Bullet b) {
        if (b.target == null || b.target.dead) return null;
        if (b.pos.dst2(b.target.pos) <= 16f) return b.target;
        return null;
    }

    public boolean consumeLifeLostFlag() {
        if (lifeLostFlag) { lifeLostFlag = false; return true; }
        return false;
    }

    public void reset() {
        towers.clear();
        enemies.clear();
        bullets.clear();
        pendingShots.clear();
        waveManager.reset();
        gold = 150;
        lives = 20;
        lifeLostFlag = false;
        gameOver = false;
        for (TowerSpot s : towerSpots) s.used = false;
    }
}
