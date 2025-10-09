package com.mygdx.td.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;
import com.mygdx.td.ally.AllyUnit;
import com.mygdx.td.abilities.AbilityManager;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.managers.WaveManager;

/**
 * World – quản lý state game & logic cơ bản.
 */
public class World {

    public final Array<Tower> towers = new Array<>();
    public final Array<Enemy> enemies = new Array<>();
    public final Array<Bullet> bullets = new Array<>();
    public final Array<AllyUnit> allies = new Array<>();

    private final ObjectMap<Tower, Array<AllyUnit>> towerAlliesMap = new ObjectMap<>();

    public final Path path = new Path();
    public final WaveManager waveManager;
    public final Array<TowerSpot> towerSpots = new Array<>();

    public int gold = 150;
    public int lives = 20;

    private boolean lifeLostFlag = false;
    public boolean gameOver = false;
    private boolean victory = false;

    private float bulletCleanupTimer = 0f;
    private TDGame game;

    private final int difficultyLevel;

    // Abilities
    public final AbilityManager abilityManager;

    private static class PendingShot {
        Tower tower; Enemy target; float t;
        PendingShot(Tower tower, Enemy target, float t) { this.tower = tower; this.target = target; this.t = t; }
    }
    private final Array<PendingShot> pendingShots = new Array<>();

    private static final float ALLY_Y_OFFSET = 28f;
    private static final float ALLY_PREATTACK_TIME = 0.10f;
    private static final float ALLY_ATTACK_TIME   = 0.38f;

    public World(int difficultyLevel) {
        this.difficultyLevel = Math.max(1, difficultyLevel);
        this.waveManager = new WaveManager(this, this.difficultyLevel);
        this.abilityManager = new AbilityManager(this);
    }

    public void setGame(TDGame game) { this.game = game; }
    public TDGame getGame() { return game; } // NEW: cho AbilityManager truy cập playSound/assets
    public int getDifficultyLevel() { return difficultyLevel; }

    public void update(float dt) {
        if (gameOver) return;

        waveManager.update(dt);
        abilityManager.update(dt);

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

        // ALLIES
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
                Array<AllyUnit> arr = towerAlliesMap.get(t);
                if (arr != null)
                    for (AllyUnit al : arr) if (al != null && !al.isDead()) al.setState(AllyUnit.State.PREATTACK);
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
                Array<AllyUnit> arr = towerAlliesMap.get(p.tower);
                if (arr != null)
                    for (AllyUnit al : arr) if (al != null && !al.isDead()) al.setState(AllyUnit.State.ATTACK);
                pendingShots.removeIndex(i);
            }
        }

        // BULLETS
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(dt);
            if (b.dead) { bullets.removeIndex(i); continue; }
            Enemy hit = bulletHit(b);
            if (hit != null) {
                hit.damage(b.damage);
                if (hit.dead) gold += Math.max(0, hit.getGoldReward());
                b.dead = true;
                bullets.removeIndex(i);
            }
        }

        // Cleanup bullets ra ngoài
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

    private void updateAllyUnit(AllyUnit ally, float dt) {
        if (ally.state == AllyUnit.State.DEAD) return;
        ally.stateTime += dt;
        ally.auxTimer  += dt;

        Tower owner = ally.owner;
        if (owner == null) return;

        ally.pos.set(owner.pos.x + ally.offsetX, owner.pos.y + ALLY_Y_OFFSET);
        Vector2 aim = owner.getAimDir();
        if (!aim.isZero()) ally.dir.set(aim).nor();

        if (Math.abs(ally.dir.y) > Math.abs(ally.dir.x)) {
            ally.facing = (ally.dir.y >= 0) ? AllyUnit.Facing.UP : AllyUnit.Facing.DOWN;
            ally.facingRight = true;
        } else {
            ally.facing = AllyUnit.Facing.SIDE;
            ally.facingRight = ally.dir.x >= 0f;
        }

        switch (ally.state) {
            case PREATTACK:
                if (ally.auxTimer >= ALLY_PREATTACK_TIME) ally.setState(AllyUnit.State.ATTACK);
                break;
            case ATTACK:
                if (ally.auxTimer >= ALLY_ATTACK_TIME) ally.setState(AllyUnit.State.IDLE);
                break;
            default: break;
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
        ensureAlliesForTower(t);
        return true;
    }

    public boolean upgradeTower(Tower t) {
        TowerType next = t.type.nextLevel();
        if (next == null) return false;
        int allowedMax = TowerType.maxAllowedUpgradeLevelForGameLevel(difficultyLevel);
        if (next.upgradeLevel > allowedMax) return false;

        int incCost = TowerType.incrementalUpgradeCost(t.type, next);
        if (gold < incCost) return false;

        gold -= incCost;
        t.upgrade();
        ensureAlliesForTower(t);
        if (game != null) game.playSound(game.assets.upgradeTowerSound);
        return true;
    }

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

    private void spawnBullet(Tower t, Enemy target) {
        Bullet b = new Bullet();
        Array<AllyUnit> arr = towerAlliesMap.get(t);
        AllyUnit shooter = (arr != null && arr.size > 0) ? arr.get(0) : null;

        float startX = shooter != null ? shooter.pos.x : t.pos.x;
        float startY = shooter != null ? shooter.pos.y + 6f : t.pos.y;
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
        b.damage = t.getDamage();
        bullets.add(b);

        if (game != null) game.playSound(game.assets.arrowShootSound);
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
        allies.clear();
        towerAlliesMap.clear();
        pendingShots.clear();
        waveManager.reset();
        abilityManager.reset();
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
            if (t.type.nextLevel() != null) t.upgrade();
            else break;
        }
        towers.add(t);
        if (rect != null) {
            TowerSpot spot = findSpotByRectApprox(rect);
            if (spot != null) spot.used = true;
        }
        t.forceFinishPlacementAndUpgrades();
        t.skipPlaceAnimation = true;
        ensureAlliesForTower(t);
    }

    private void ensureAlliesForTower(Tower tower) {
        int required = (tower.type.upgradeLevel >= 3) ? 2 : 1;
        Array<AllyUnit> arr = towerAlliesMap.get(tower);
        if (arr == null) {
            arr = new Array<>();
            towerAlliesMap.put(tower, arr);
        }
        for (int i = arr.size - 1; i >= 0; i--) {
            if (arr.get(i) == null || arr.get(i).isDead()) arr.removeIndex(i);
        }
        while (arr.size > required) arr.removeIndex(arr.size - 1);
        while (arr.size < required) {
            AllyUnit u = new AllyUnit();
            u.owner = tower;
            u.offsetX = required == 1 ? 0f : (arr.size == 0 ? -10f : 10f);
            u.pos.set(tower.pos.x + u.offsetX, tower.pos.y + ALLY_Y_OFFSET);
            u.setState(AllyUnit.State.IDLE);
            allies.add(u);
            arr.add(u);
        }
    }

    private TowerSpot findSpotByRectApprox(Rectangle r) {
        for (TowerSpot s : towerSpots) {
            if (approxEq(s.rect.x, r.x)
                && approxEq(s.rect.y, r.y)
                && approxEq(s.rect.width, r.width)
                && approxEq(s.rect.height, r.height)) return s;
        }
        return null;
    }

    private boolean approxEq(float a, float b) { return Math.abs(a - b) <= 0.5f; }

    public boolean isVictory() { return victory; }
    public void clearVictory() { victory = false; }
}
