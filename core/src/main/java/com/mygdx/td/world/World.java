package com.mygdx.td.world;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.managers.WaveManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;

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

    private static class PendingShot {
        Tower tower; Enemy target; float t;
        PendingShot(Tower tower, Enemy target, float t) { this.tower = tower; this.target = target; this.t = t; }
    }
    private final Array<PendingShot> pendingShots = new Array<>();

    // Tham chiếu tới game hoặc Assets để phát âm thanh
    private TDGame game;

    // Victory flag
    private boolean victory = false;

    public World() {}

    public void setGame(TDGame game) { this.game = game; }

    public void update(float dt) {
        if (gameOver) return;

        waveManager.update(dt);

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt);
            if (e.reachedEnd && !e.dead) {
                lives--;
                lifeLostFlag = true;
                e.dead = true;
                if (lives <= 0) { lives = 0; triggerGameOver(); }
            }
            if (e.isRemovable()) {
                enemies.removeIndex(i);
            }
        }

        for (Tower t : towers) {
            t.update(dt);
            Enemy target = findTarget(t.pos.x, t.pos.y, t.getRange());
            if (target != null && t.getState() == Tower.State.IDLE) {
                Vector2 dir = new Vector2(target.pos).sub(t.pos);
                if (!dir.isZero()) dir.nor();
                t.getAimDir().set(dir);
            }

            if (target != null && t.canFire()) {
                float delay = t.beginAttackTowards(target.pos.x, target.pos.y);
                pendingShots.add(new PendingShot(t, target, delay));
            }
        }

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
                if (hit.dead) {
                    // Thưởng vàng theo enemy
                    gold += Math.max(0, hit.getGoldReward());
                }
                b.dead = true;
                bullets.removeIndex(i);
            }
        }

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

        // Kiểm tra Victory: đã hoàn tất tất cả wave, không còn enemy alive
        if (!victory && waveManager.isAllWavesCompleted() && !waveManager.isInWave() && enemies.size == 0) {
            victory = true;
        }
    }

    private void triggerGameOver() {
        gameOver = true;
        waveManager.forceStop();
        pendingShots.clear();
    }

    public boolean canAffordTower(int cost) { return gold >= cost; }

    public boolean placeTowerOnSpot(TowerSpot spot, TowerType type) {
        if (!canAffordTower(type.cost) || spot.used) return false;
        Tower t = new Tower(spot.rect.x + spot.rect.width / 2f, spot.rect.y + spot.rect.height / 2f, spot.rect, type);
        towers.add(t);
        spot.used = true;
        gold -= type.cost;
        game.playSound(game.assets.upgradeTowerSound);
        return true;
    }

    public boolean upgradeTower(Tower t) {
        TowerType next = t.type.nextLevel();
        if (next == null) return false;
        if (gold < next.cost) return false;
        gold -= next.cost;
        t.upgrade();
        game.playSound(game.assets.upgradeTowerSound);
        return true;
    }

    public boolean placeTowerFree(float x, float y) {
        if (!canAffordTower(50)) return false;
        Tower t = new Tower(x, y, null, TowerType.WOOD);
        towers.add(t);
        gold -= 50;
        game.playSound(game.assets.upgradeTowerSound);
        return true;
    }

    public boolean placeTowerOnSpot(TowerSpot spot) {
        if (!canAffordTower(50) || spot.used) return false;
        Tower t = new Tower(spot.rect.x + spot.rect.width / 2f, spot.rect.y + spot.rect.height / 2f, spot.rect, TowerType.WOOD);
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

        // Phát âm thanh bắn đạn nếu có assets
        game.playSound(game.assets.laserGunSound);
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
        victory = false;
        for (TowerSpot s : towerSpots) s.used = false;
    }

    // ============ Restore support ============
    // Dùng khi khôi phục checkpoint: thêm trụ không trừ vàng, đánh dấu spot nếu có.
    public void addTowerRestored(float x, float y, Rectangle rect, int upgradeLevel) {
        Tower t = new Tower(x, y, rect, TowerType.WOOD);
        for (int i = 0; i < upgradeLevel; i++) {
            if (t.type.nextLevel() != null) t.upgrade(); else break;
        }
        towers.add(t);

        if (rect != null) {
            TowerSpot spot = findSpotByRectApprox(rect);
            if (spot != null) spot.used = true;
        }
    }

    private TowerSpot findSpotByRectApprox(Rectangle r) {
        for (TowerSpot s : towerSpots) {
            if (approxEq(s.rect.x, r.x)
                && approxEq(s.rect.y, r.y)
                && approxEq(s.rect.width, r.width)
                && approxEq(s.rect.height, r.height)) {
                return s;
            }
        }
        return null;
    }

    private boolean approxEq(float a, float b) { return Math.abs(a - b) <= (float) 0.5; }

    // Victory accessors
    public boolean isVictory() { return victory; }
    public void clearVictory() { victory = false; }
}
