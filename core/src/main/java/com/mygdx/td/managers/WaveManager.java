package com.mygdx.td.managers;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.World;

/**
 * WaveManager hỗ trợ:
 *  - startNextWave()
 *  - resumeAtWave(nextWave) để khôi phục từ save (nextWave là wave CHƯA bắt đầu; currentWave = nextWave - 1)
 *  - Scaling theo difficultyLevel (map level) + theo wave.
 */
public class WaveManager {

    public enum EnemyType {
        GRUNT, RUNNER, TANK,
        ELITE_GRUNT, ELITE_RUNNER, ELITE_TANK,
        MINI_BOSS, MID_BOSS, MINI_BOSS_2, FINAL_BOSS
    }

    private static class WaveEntry {
        EnemyType type;
        int count;
        float interval;
        WaveEntry(EnemyType type, int count, float interval) {
            this.type = type; this.count = count; this.interval = interval;
        }
    }

    private static class WaveDef {
        Array<WaveEntry> entries = new Array<>();
        void add(EnemyType t, int count, float interval) { entries.add(new WaveEntry(t, count, interval)); }
    }

    private final World world;
    private final int difficultyLevel;
    private final float baseDifficultyMultiplier;

    private int currentWave = 0;          // Wave đã hoàn tất hoặc đang ở trước lúc start (1-based khi inWave true)
    private boolean inWave = false;

    private final int totalWaves = 20;
    private final Array<WaveDef> waves = new Array<>();

    private int entryIndex = 0;
    private int remainInEntry = 0;
    private float entryTimer = 0f;

    private boolean allWavesCompleted = false;

    public WaveManager(World world, int difficultyLevel) {
        this.world = world;
        this.difficultyLevel = difficultyLevel;
        this.baseDifficultyMultiplier = computeDifficultyMultiplier(difficultyLevel);
        buildDefaultWaves();
    }

    private float computeDifficultyMultiplier(int lvl) {
        if (lvl <= 1) return 1.0f;
        if (lvl == 2) return 1.25f;
        if (lvl == 3) return 1.5f;
        return 1.5f + 0.15f * (lvl - 3);
    }

    public void update(float dt) {
        if (!inWave) return;

        if (currentWave < 1 || currentWave > totalWaves) {
            inWave = false;
            return;
        }

        WaveDef def = waves.get(currentWave - 1);
        if (entryIndex >= def.entries.size) {
            // Wave xong
            inWave = false;
            if (currentWave >= totalWaves) allWavesCompleted = true;
            return;
        }

        if (remainInEntry <= 0) {
            WaveEntry e = def.entries.get(entryIndex);
            remainInEntry = e.count;
            entryTimer = 0f;
        }

        entryTimer -= dt;
        if (entryTimer <= 0f && remainInEntry > 0) {
            WaveEntry e = def.entries.get(entryIndex);
            spawnEnemyOfType(e.type, currentWave);
            remainInEntry--;
            entryTimer = e.interval;
            if (remainInEntry <= 0) {
                entryIndex++;
            }
        }
    }

    public void startNextWave() {
        if (inWave) return;
        if (currentWave >= totalWaves) return;
        currentWave++;          // chuyển sang wave mới
        inWave = true;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
    }

    /**
     * Khôi phục trạng thái wave sau load:
     * nextWave là wave chuẩn bị chạy (CHƯA start), ví dụ nếu nextWave = 5 thì currentWave = 4, inWave = false.
     */
    public void resumeAtWave(int nextWave) {
        if (nextWave < 1) nextWave = 1;
        if (nextWave > totalWaves) nextWave = totalWaves;
        currentWave = nextWave - 1;
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
        allWavesCompleted = currentWave >= totalWaves;
    }

    public void forceStop() {
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
    }

    public boolean isInWave() { return inWave; }
    public int getCurrentWave() { return currentWave; }
    public int getTotalWaves() { return totalWaves; }
    public boolean isAllWavesCompleted() { return allWavesCompleted; }

    public void reset() {
        currentWave = 0;
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
        allWavesCompleted = false;
    }

    private void buildDefaultWaves() {
        // Bạn thay bằng cấu hình wave trước đó nếu đã có list hoàn chỉnh.
        for (int i = 1; i <= totalWaves; i++) {
            WaveDef w = new WaveDef();
            if (i % 5 == 0) {
                w.add(EnemyType.MINI_BOSS, 1, 1.6f);
                w.add(EnemyType.ELITE_RUNNER, 6 + i / 5, 0.9f);
            } else {
                w.add(EnemyType.GRUNT, 8 + i, 0.7f);
                if (i >= 3)  w.add(EnemyType.RUNNER, 4 + i / 2, 0.55f);
                if (i >= 6)  w.add(EnemyType.TANK, 2 + i / 3, 1.1f);
                if (i >= 9)  w.add(EnemyType.ELITE_GRUNT, 3 + i / 3, 0.85f);
                if (i >= 12) w.add(EnemyType.ELITE_TANK, 1 + i / 6, 1.3f);
                if (i >= 17) w.add(EnemyType.ELITE_RUNNER, 5 + i / 4, 0.6f);
            }
            waves.add(w);
        }
    }

    private void spawnEnemyOfType(EnemyType type, int waveNumber) {
        Enemy e = new Enemy(world.path, type); // Enemy tự thiết lập waypoint đầu
        applyBaseStats(e, type);
        applyScaling(e, waveNumber);
        world.enemies.add(e);
    }

    private void applyBaseStats(Enemy e, EnemyType type) {
        float baseHp;
        float speedFactor;
        int gold;
        boolean shield = false;
        boolean enrage = false;
        float dmgTakenMul = 1f;

        switch (type) {
            case GRUNT:         baseHp = 100f;  speedFactor = 1.00f; gold = 5;  break;
            case RUNNER:        baseHp = 80f;   speedFactor = 1.35f; gold = 6;  break;
            case TANK:          baseHp = 240f;  speedFactor = 0.75f; gold = 10; dmgTakenMul = 0.95f; break;
            case ELITE_GRUNT:   baseHp = 180f;  speedFactor = 1.05f; gold = 12; dmgTakenMul = 0.90f; break;
            case ELITE_RUNNER:  baseHp = 140f;  speedFactor = 1.55f; gold = 14; break;
            case ELITE_TANK:    baseHp = 420f;  speedFactor = 0.70f; gold = 20; dmgTakenMul = 0.85f; break;
            case MINI_BOSS:     baseHp = 900f;  speedFactor = 0.85f; gold = 60;  shield = true; dmgTakenMul = 0.90f; break;
            case MID_BOSS:      baseHp = 1600f; speedFactor = 0.80f; gold = 120; shield = true; dmgTakenMul = 0.85f; break;
            case MINI_BOSS_2:   baseHp = 2000f; speedFactor = 0.85f; gold = 150; shield = true; dmgTakenMul = 0.80f; break;
            case FINAL_BOSS:    baseHp = 4000f; speedFactor = 0.90f; gold = 300; shield = true; enrage = true; dmgTakenMul = 0.75f; break;
            default:            baseHp = 100f;  speedFactor = 1.00f; gold = 5;
        }

        e.maxHp = baseHp;
        e.hp = baseHp;
        e.goldReward = gold;
        e.damageTakenMultiplier = dmgTakenMul;

        if (shield) {
            e.shieldPulsing = true;
            e.shieldPeriod = 6f;
            e.shieldDuration = 2f;
            e.shieldDmgMultiplier = 0.5f;
        }
        if (enrage) {
            e.enrageEnabled = true;
            e.enrageThreshold = 0.5f;
            e.enrageSpeedMultiplier = 1.25f;
        }

        e.setBaseSpeed(com.mygdx.td.Constants.ENEMY_BASE_SPEED * speedFactor);
    }

    private void applyScaling(Enemy e, int waveNumber) {
        float waveScale = 1f + 0.04f * (waveNumber - 1);      // +4% mỗi wave
        float hpMul     = baseDifficultyMultiplier * waveScale;
        float speedMul  = 1f + (baseDifficultyMultiplier - 1f) * 0.30f;

        e.maxHp *= hpMul;
        e.hp    = e.maxHp;

        // Nếu cần chính xác hơn hãy thêm getter baseSpeed trong Enemy; tạm bỏ qua vì không có.
        try {
            // reflect baseSpeed private để nhân (optional)
            var f = Enemy.class.getDeclaredField("baseSpeed");
            f.setAccessible(true);
            float current = f.getFloat(e);
            f.setFloat(e, current * speedMul);
        } catch (Throwable ignored) {}
    }
}
