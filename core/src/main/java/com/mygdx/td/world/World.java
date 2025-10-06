package com.mygdx.td.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;
import com.mygdx.td.ally.AllyUnit;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.managers.WaveManager;

/**
 * World (bản giữ nguyên logic gốc tower/enemy/bullet, chỉ thêm ally hiển thị và
 * điều chỉnh spawn bullet để bắn ra từ ally bằng mũi tên).
 */
public class World {

    /* ================== Collections & Core State ================== */
    public final Array<Tower> towers = new Array<>();
    public final Array<Enemy> enemies = new Array<>();
    public final Array<Bullet> bullets = new Array<>();
    public final Array<AllyUnit> allies = new Array<>();

    // map tower -> ally (1:1)
    private final ObjectMap<Tower, AllyUnit> towerAllyMap = new ObjectMap<>();

    public final Path path = new Path();
    public final WaveManager waveManager = new WaveManager(this);
    public final Array<TowerSpot> towerSpots = new Array<>();

    public int gold = 150;
    public int lives = 20;

    private boolean lifeLostFlag = false;
    public boolean gameOver = false;
    private boolean victory = false;

    private float bulletCleanupTimer = 0f;

    private TDGame game;

    /* ================== Pending Shot ================== */
    private static class PendingShot {
        Tower tower; Enemy target; float t;
        PendingShot(Tower tower, Enemy target, float t) { this.tower = tower; this.target = target; this.t = t; }
    }
    private final Array<PendingShot> pendingShots = new Array<>();

    /* ================== Ally Visual Timing ================== */
    private static final float ALLY_Y_OFFSET = 28f;
    private static final float ALLY_PREATTACK_TIME = 0.10f;
    private static final float ALLY_ATTACK_TIME   = 0.38f;

    public World() {}

    public void setGame(TDGame game) { this.game = game; }

    /* ===================================================================== */
    /* UPDATE LOOP                                                           */
    /* ===================================================================== */
    public void update(float dt) {
        if (gameOver) return;

        waveManager.update(dt);

        // ENEMIES
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt);
            if (e.reachedEnd && !e.dead) {
                lives--;
                lifeLostFlag = true;
                e.dead = true;
                if (lives <= 0) { lives = 0; triggerGameOver(); }
            }
            if (e.isRemovable()) enemies.removeIndex(i);
        }

        // ALLIES (visual only)
        for (int i = allies.size - 1; i >= 0; i--) {
            AllyUnit a = allies.get(i);
            updateAllyUnit(a, dt);
            if (a.isDead()) allies.removeIndex(i);
        }

        // TOWERS
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
                // Ally vào PREATTACK
                AllyUnit ally = towerAllyMap.get(t);
                if (ally != null) {
                    ally.setState(AllyUnit.State.PREATTACK);
                    ally.auxTimer = 0f;
                }
            }
        }

        // Pending shots
        for (int i = pendingShots.size - 1; i >= 0; i--) {
            PendingShot p = pendingShots.get(i);
            p.t -= dt;
            if (p.t <= 0f) {
                if (p.target != null && !p.target.dead) {
                    spawnBullet(p.tower, p.target);
                }
                AllyUnit ally = towerAllyMap.get(p.tower);
                if (ally != null && ally.state != AllyUnit.State.DEAD) {
                    ally.setState(AllyUnit.State.ATTACK);
                    ally.auxTimer = 0f;
                }
                pendingShots.removeIndex(i);
            }
        }

        // BULLETS
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
                    gold += Math.max(0, hit.getGoldReward());
                }
                b.dead = true;
                bullets.removeIndex(i);
            }
        }

        // Cleanup out-of-bounds
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

        // Victory
        if (!victory && waveManager.isAllWavesCompleted() && !waveManager.isInWave() && enemies.size == 0) {
            victory = true;
        }
    }

    /* ===================================================================== */
    /* ALLY UPDATE                                                           */
    /* ===================================================================== */
    private void updateAllyUnit(AllyUnit ally, float dt) {
        if (ally.state == AllyUnit.State.DEAD) return;

        ally.stateTime += dt;
        ally.auxTimer   += dt;

        Tower owner = ally.owner;
        if (owner == null) return;

        ally.pos.set(owner.pos.x, owner.pos.y + ALLY_Y_OFFSET);

        Vector2 aim = owner.getAimDir();
        if (!aim.isZero()) {
            ally.dir.set(aim).nor();
        }

        // Determine facing
        if (Math.abs(ally.dir.y) > Math.abs(ally.dir.x)) {
            ally.facing = (ally.dir.y >= 0) ? AllyUnit.Facing.UP : AllyUnit.Facing.DOWN;
            ally.facingRight = true;
        } else {
            ally.facing = AllyUnit.Facing.SIDE;
            ally.facingRight = ally.dir.x >= 0f;
        }

        switch (ally.state) {
            case PREATTACK:
                if (ally.auxTimer >= ALLY_PREATTACK_TIME) {
                    // Nếu pendingShot chưa bắn vẫn chuyển sang ATTACK để không bị đứng cứng.
                    ally.setState(AllyUnit.State.ATTACK);
                }
                break;
            case ATTACK:
                if (ally.auxTimer >= ALLY_ATTACK_TIME) {
                    ally.setState(AllyUnit.State.IDLE);
                }
                break;
            case IDLE:
            default:
                break;
        }
    }

    /* ===================================================================== */
    /* GAME OVER / HELPERS                                                   */
    /* ===================================================================== */
    private void triggerGameOver() {
        gameOver = true;
        waveManager.forceStop();
        pendingShots.clear();
    }

    public boolean canAffordTower(int cost) { return gold >= cost; }

    public boolean placeTowerOnSpot(TowerSpot spot, TowerType type) {
        if (!canAffordTower(type.cost) || spot.used) return false;
        Tower t = new Tower(
            spot.rect.x + spot.rect.width / 2f,
            spot.rect.y + spot.rect.height / 2f,
            spot.rect,
            type
        );
        towers.add(t);
        spot.used = true;
        gold -= type.cost;
        if (game != null) game.playSound(game.assets.upgradeTowerSound);
        spawnAllyForTower(t);
        return true;
    }

    public boolean upgradeTower(Tower t) {
        TowerType next = t.type.nextLevel();
        if (next == null) return false;
        if (gold < next.cost) return false;
        gold -= next.cost;
        t.upgrade();
        if (game != null) game.playSound(game.assets.upgradeTowerSound);
        return true;
    }

    public boolean placeTowerFree(float x, float y) {
        if (!canAffordTower(50)) return false;
        Tower t = new Tower(x, y, null, TowerType.WOOD);
        towers.add(t);
        gold -= 50;
        if (game != null) game.playSound(game.assets.upgradeTowerSound);
        spawnAllyForTower(t);
        return true;
    }

    public boolean placeTowerOnSpot(TowerSpot spot) {
        if (!canAffordTower(50) || spot.used) return false;
        Tower t = new Tower(
            spot.rect.x + spot.rect.width / 2f,
            spot.rect.y + spot.rect.height / 2f,
            spot.rect,
            TowerType.WOOD
        );
        towers.add(t);
        spot.used = true;
        gold -= 50;
        spawnAllyForTower(t);
        return true;
    }

    private void spawnAllyForTower(Tower tower) {
        if (towerAllyMap.containsKey(tower)) return;
        AllyUnit ally = new AllyUnit();
        ally.owner = tower;
        ally.pos.set(tower.pos.x, tower.pos.y + ALLY_Y_OFFSET);
        ally.facing = AllyUnit.Facing.SIDE;
        ally.setState(AllyUnit.State.IDLE);
        ally.auxTimer = 0f;
        allies.add(ally);
        towerAllyMap.put(tower, ally);
    }

    /* ===================================================================== */
    /* TARGETING & BULLETS                                                   */
    /* ===================================================================== */
    private Enemy findTarget(float x, float y, float range) {
        float r2 = range * range;
        Enemy best = null;
        float bestDist2 = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.dead || e.reachedEnd) continue;
            float d2 = e.pos.dst2(x, y);
            if (d2 <= r2 && d2 < bestDist2) {
                bestDist2 = d2;
                best = e;
            }
        }
        return best;
    }

    /**
     * Spawn bullet (arrow) từ vị trí ally thay vì tâm tower.
     */
    private void spawnBullet(Tower t, Enemy target) {
        Bullet b = new Bullet();

        // Lấy ally tương ứng
        AllyUnit ally = towerAllyMap.get(t);

        float startX;
        float startY;
        if (ally != null) {
            startX = ally.pos.x;
            startY = ally.pos.y + 6f; // nâng nhẹ để trông từ cung
        } else {
            startX = t.pos.x;
            startY = t.pos.y;
        }
        b.pos.set(startX, startY);

        if (target != null) {
            float vx = target.pos.x - startX;
            float vy = target.pos.y - startY;
            float len = (float)Math.sqrt(vx*vx + vy*vy);
            if (len > 0.0001f) {
                b.dx = vx / len;
                b.dy = vy / len;
            } else {
                b.dx = 1f; b.dy = 0f;
            }
        } else {
            b.dx = 1f; b.dy = 0f;
        }
        b.angleDeg = (float)Math.toDegrees(Math.atan2(b.dy, b.dx));

        b.target = target;
        b.speed  = 300f;
        b.damage = t.getDamage(); // không thay đổi sát thương
        bullets.add(b);

        if (game != null) game.playSound(game.assets.arrowShootSound);
    }

    private Enemy bulletHit(Bullet b) {
        if (b.target == null || b.target.dead) return null;
        if (b.pos.dst2(b.target.pos) <= 16f) return b.target;
        return null;
    }

    /* ===================================================================== */
    /* MISC                                                                  */
    /* ===================================================================== */
    public boolean consumeLifeLostFlag() {
        if (lifeLostFlag) { lifeLostFlag = false; return true; }
        return false;
    }

    public void reset() {
        towers.clear();
        enemies.clear();
        bullets.clear();
        allies.clear();
        towerAllyMap.clear();
        pendingShots.clear();
        waveManager.reset();
        gold = 150;
        lives = 20;
        lifeLostFlag = false;
        gameOver = false;
        victory = false;
        for (TowerSpot s : towerSpots) s.used = false;
    }

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
        t.forceFinishPlacementAndUpgrades(); // đảm bảo state IDLE
        t.skipPlaceAnimation = true;
        spawnAllyForTower(t);
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

    private boolean approxEq(float a, float b) { return Math.abs(a - b) <= 0.5f; }

    public boolean isVictory() { return victory; }
    public void clearVictory() { victory = false; }
}
