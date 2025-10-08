package com.mygdx.td.managers;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.World;

/**
 * WaveManager – phiên bản “Early Game siêu dễ”:
 *
 *  EARLY (wave 1..5):
 *      Dùng bảng giảm HP cực mạnh (EARLY_WEAK_FACTORS).
 *      Không difficulty multiplier, không tăng speed.
 *
 *  MID (bắt đầu scale từ START_SCALE_WAVE = 6):
 *      HP tăng rất nhẹ ban đầu, rồi mạnh dần về cuối game.
 *
 *  DIFFICULTY (map level) chỉ bắt đầu ảnh hưởng từ DIFF_RAMP_START_WAVE (12) và ramp tới 22.
 *      => Trong 20 wave đầu chưa bao giờ đạt full multiplier, giữ game nhẹ.
 *
 *  SPEED: chỉ tăng từ SPEED_START_WAVE (14) và rất nhẹ.
 *
 *  Tất cả hằng số gom nhóm dễ chỉnh ở đầu file.
 */
public class WaveManager {

    /* ================= Enemy Types ================= */
    public enum EnemyType {
        GRUNT, RUNNER, TANK,
        ELITE_GRUNT, ELITE_RUNNER, ELITE_TANK,
        MINI_BOSS, MID_BOSS, MINI_BOSS_2, FINAL_BOSS
    }

    /* ================= Internal Data ================= */
    private static class WaveEntry {
        EnemyType type; int count; float interval;
        WaveEntry(EnemyType type, int count, float interval) {
            this.type = type; this.count = count; this.interval = interval;
        }
    }
    private static class WaveDef {
        Array<WaveEntry> entries = new Array<>();
        void add(EnemyType t, int count, float interval) { entries.add(new WaveEntry(t, count, interval)); }
    }

    /* ================= Tuning Constants ================= */

    // Tổng số wave
    private static final int TOTAL_WAVES = 20;

    // 5 wave đầu yếu hẳn (HP multiplier trực tiếp)
    private static final float[] EARLY_WEAK_FACTORS = {
        0.50f, // wave 1
        0.55f, // wave 2
        0.60f, // wave 3
        0.65f, // wave 4
        0.70f  // wave 5
    };
    private static final int EARLY_WEAK_COUNT = EARLY_WEAK_FACTORS.length;

    // Bắt đầu wave nào thì áp cơ chế scale bình thường (sau khi kết thúc giai đoạn cực dễ)
    private static final int START_SCALE_WAVE = 6; // wave 6 trở đi mới scale

    // Phân đoạn tăng HP sau giai đoạn
    // Wave 6..11: +3% mỗi wave (mild)
    // Wave 12..16: +5% mỗi wave (medium)
    // Wave 17..20: +7% mỗi wave (late)
    private static final int SEG_MILD_END   = 11; // 6..11
    private static final int SEG_MED_END    = 16; // 12..16
    // 17..20 late
    private static final float INC_MILD     = 0.03f;
    private static final float INC_MED      = 0.05f;
    private static final float INC_LATE     = 0.07f;

    // Map difficulty multiplier ramp (bắt đầu trễ)
    private static final int DIFF_RAMP_START_WAVE = 12;
    private static final int DIFF_RAMP_END_WAVE   = 22; // chưa full trong 20 wave

    // Tốc độ – chỉ tăng muộn và nhẹ
    private static final int SPEED_START_WAVE = 14;
    private static final float SPEED_DIFFICULTY_PORTION = 0.15f;  // 15% phần diff
    private static final float EXTRA_SPEED_PER_WAVE = 0.004f;     // +0.4% mỗi wave sau SPEED_START_WAVE

    // Giảm thêm toàn cục nếu vẫn khó
    private static final float EASY_GLOBAL_HP_SCALE = 1.00f;

    // Trần an toàn cuối game
    private static final float FINAL_HP_CLAMP = 1.45f;

    /* ================= State Fields ================= */
    private final World world;
    private final int difficultyLevel;
    private final float baseDifficultyMultiplier;

    private int currentWave = 0;
    private boolean inWave = false;
    private final Array<WaveDef> waves = new Array<>();
    private int entryIndex = 0;
    private int remainInEntry = 0;
    private float entryTimer = 0f;
    private boolean allWavesCompleted = false;

    /* ================= Constructor ================= */
    public WaveManager(World world, int difficultyLevel) {
        this.world = world;
        this.difficultyLevel = difficultyLevel;
        this.baseDifficultyMultiplier = computeBaseDifficultyMultiplier(difficultyLevel);
        buildDefaultWaves();
    }

    private float computeBaseDifficultyMultiplier(int lvl) {
        if (lvl <= 1) return 1.00f;
        if (lvl == 2) return 1.10f;
        if (lvl == 3) return 1.18f;
        return 1.18f + 0.08f * (lvl - 3);
    }

    /* ================= Core Loop ================= */
    public void update(float dt) {
        if (!inWave) return;

        if (currentWave < 1 || currentWave > TOTAL_WAVES) {
            inWave = false;
            return;
        }

        WaveDef def = waves.get(currentWave - 1);
        if (entryIndex >= def.entries.size) {
            inWave = false;
            if (currentWave >= TOTAL_WAVES) allWavesCompleted = true;
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
            if (remainInEntry <= 0) entryIndex++;
        }
    }

    public void startNextWave() {
        if (inWave) return;
        if (currentWave >= TOTAL_WAVES) return;
        currentWave++;
        inWave = true;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
    }

    /**
     * nextWave là wave CHƯA bắt đầu (currentWave = nextWave - 1).
     */
    public void resumeAtWave(int nextWave) {
        if (nextWave < 1) nextWave = 1;
        if (nextWave > TOTAL_WAVES) nextWave = TOTAL_WAVES;
        currentWave = nextWave - 1;
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
        allWavesCompleted = currentWave >= TOTAL_WAVES;
    }

    public void forceStop() {
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
    }

    public boolean isInWave() { return inWave; }
    public int getCurrentWave() { return currentWave; }
    public int getTotalWaves() { return TOTAL_WAVES; }
    public boolean isAllWavesCompleted() { return allWavesCompleted; }

    public void reset() {
        currentWave = 0;
        inWave = false;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
        allWavesCompleted = false;
    }

    /* ================= Wave Definitions ================= */
    private void buildDefaultWaves() {
        // Giữ kiểu cấu trúc cũ – vẫn có mini boss ở wave 5/10/15/20 nếu bạn muốn khác thì sửa ở đây.
        for (int i = 1; i <= TOTAL_WAVES; i++) {
            WaveDef w = new WaveDef();

            // Để early đã dễ nên mini boss đầu dời về wave 6/10/15/20 (ví dụ) – bạn tùy chỉnh.
            if (i == 10 || i == 15 || i == 20) {
                w.add(EnemyType.MINI_BOSS, 1, 1.8f);
                if (i >= 15) w.add(EnemyType.ELITE_RUNNER, 4 + i / 5, 0.9f);
            } else {
                // Base grunt line
                w.add(EnemyType.GRUNT, 6 + i, 0.75f);

                if (i >= 3)  w.add(EnemyType.RUNNER, 3 + i / 2, 0.65f);
                if (i >= 6)  w.add(EnemyType.TANK, 2 + i / 3, 1.15f);
                if (i >= 8)  w.add(EnemyType.ELITE_GRUNT, 2 + i / 4, 0.9f);
                if (i >= 12) w.add(EnemyType.ELITE_TANK, 1 + i / 6, 1.4f);
                if (i >= 16) w.add(EnemyType.ELITE_RUNNER, 3 + i / 4, 0.65f);
            }
            waves.add(w);
        }
    }

    /* ================= Enemy Spawn & Scaling ================= */
    private void spawnEnemyOfType(EnemyType type, int waveNumber) {
        Enemy e = new Enemy(world.path, type);
        applyBaseStats(e, type);
        applyScaling(e, waveNumber);
        world.enemies.add(e);
    }

    private void applyBaseStats(Enemy e, EnemyType type) {
        // Giảm nhẹ base HP tổng thể so với bản trước
        float baseHp, speedFactor;
        int gold;
        boolean shield = false, enrage = false;
        float dmgTakenMul = 1f;

        switch (type) {
            case GRUNT:         baseHp = 85f;   speedFactor = 1.00f; gold = 5;  break;
            case RUNNER:        baseHp = 65f;   speedFactor = 1.35f; gold = 6;  break;
            case TANK:          baseHp = 190f;  speedFactor = 0.75f; gold = 9;  dmgTakenMul = 0.95f; break;
            case ELITE_GRUNT:   baseHp = 150f;  speedFactor = 1.05f; gold = 11; dmgTakenMul = 0.90f; break;
            case ELITE_RUNNER:  baseHp = 115f;  speedFactor = 1.55f; gold = 13; break;
            case ELITE_TANK:    baseHp = 340f;  speedFactor = 0.70f; gold = 18; dmgTakenMul = 0.85f; break;
            case MINI_BOSS:     baseHp = 730f;  speedFactor = 0.85f; gold = 55; shield = true; dmgTakenMul = 0.90f; break;
            case MID_BOSS:      baseHp = 1300f; speedFactor = 0.80f; gold = 110; shield = true; dmgTakenMul = 0.85f; break;
            case MINI_BOSS_2:   baseHp = 1600f; speedFactor = 0.85f; gold = 140; shield = true; dmgTakenMul = 0.80f; break;
            case FINAL_BOSS:    baseHp = 3000f; speedFactor = 0.90f; gold = 260; shield = true; enrage = true; dmgTakenMul = 0.75f; break;
            default:            baseHp = 85f;   speedFactor = 1.00f; gold = 5;
        }

        e.maxHp = baseHp;
        e.hp    = baseHp;
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

        // 1. EARLY SUPER WEAK
        if (waveNumber <= EARLY_WEAK_COUNT) {
            float mul = EARLY_WEAK_FACTORS[waveNumber - 1] * EASY_GLOBAL_HP_SCALE;
            e.maxHp *= mul;
            e.hp = e.maxHp;
            // Không tăng speed, không diff multiplier
            return;
        }

        // 2. Wave HP scaling sau early
        float hpFactor;
        if (waveNumber < START_SCALE_WAVE) {
            hpFactor = 1f; // (chỉ xảy ra nếu START_SCALE_WAVE > EARLY_WEAK_COUNT)
        } else if (waveNumber <= SEG_MILD_END) {
            // Mild segment: wave 6..11
            int n = waveNumber - (START_SCALE_WAVE - 1); // wave6 ->1
            hpFactor = 1f + INC_MILD * n;
        } else if (waveNumber <= SEG_MED_END) {
            // Medium: wave 12..16
            int mildLen = SEG_MILD_END - (START_SCALE_WAVE - 1); // số wave mild
            float baseAtMedStart = 1f + INC_MILD * mildLen;
            int n = waveNumber - SEG_MILD_END; // 1..5
            hpFactor = baseAtMedStart + INC_MED * n;
        } else {
            // Late: wave 17..20
            int mildLen = SEG_MILD_END - (START_SCALE_WAVE - 1);
            int medLen  = SEG_MED_END - SEG_MILD_END;
            float baseAtLateStart = (1f + INC_MILD * mildLen) + INC_MED * medLen;
            int n = waveNumber - SEG_MED_END; // 1..4
            hpFactor = baseAtLateStart + INC_LATE * n;
        }

        // 3. Difficulty ramp (bắt đầu rất muộn)
        float diffRampProgress;
        if (waveNumber < DIFF_RAMP_START_WAVE) diffRampProgress = 0f;
        else if (waveNumber >= DIFF_RAMP_END_WAVE) diffRampProgress = 1f;
        else diffRampProgress = (waveNumber - DIFF_RAMP_START_WAVE) /
                (float)(DIFF_RAMP_END_WAVE - DIFF_RAMP_START_WAVE);

        float difficultyEffective = 1f + (baseDifficultyMultiplier - 1f) * diffRampProgress;

        float finalHpMul = hpFactor * difficultyEffective * EASY_GLOBAL_HP_SCALE;
        if (finalHpMul > FINAL_HP_CLAMP) finalHpMul = FINAL_HP_CLAMP;

        e.maxHp *= finalHpMul;
        e.hp = e.maxHp;

        // 4. Speed scaling (bỏ qua trước SPEED_START_WAVE)
        if (waveNumber >= SPEED_START_WAVE) {
            float speedMul = 1f
                + (difficultyEffective - 1f) * SPEED_DIFFICULTY_PORTION
                + EXTRA_SPEED_PER_WAVE * (waveNumber - (SPEED_START_WAVE - 1));
            try {
                var f = Enemy.class.getDeclaredField("baseSpeed");
                f.setAccessible(true);
                float cur = f.getFloat(e);
                f.setFloat(e, cur * speedMul);
            } catch (Throwable ignored) {}
        }
    }
}
