package com.mygdx.td.managers;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.Constants;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.World;

/**
 * Quản lý wave: 20 wave hữu hạn, có boss ở 5/10/15/20.
 * Mỗi loại enemy spawn đúng asset (qua trường type).
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
        boolean isBossWave() {
            for (WaveEntry e: entries) {
                if (e.type == EnemyType.MINI_BOSS || e.type == EnemyType.MID_BOSS
                    || e.type == EnemyType.MINI_BOSS_2 || e.type == EnemyType.FINAL_BOSS) return true;
            }
            return false;
        }
    }

    private final World world;

    private int currentWave = 0;
    private boolean inWave = false;

    private final int totalWaves = 20;
    private final Array<WaveDef> waves = new Array<>();

    private int entryIndex = 0;
    private int remainInEntry = 0;
    private float entryTimer = 0f;

    private boolean allWavesCompleted = false;

    public WaveManager(World world) {
        this.world = world;
        buildDefaultWaves();
    }

    public void update(float dt) {
        if (!inWave) return;

        if (currentWave < 1 || currentWave > totalWaves) {
            inWave = false;
            return;
        }

        WaveDef def = waves.get(currentWave - 1);
        if (entryIndex >= def.entries.size) {
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
        if (currentWave >= totalWaves) {
            return;
        }
        currentWave++;
        inWave = true;
        entryIndex = 0;
        remainInEntry = 0;
        entryTimer = 0f;
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

    public void resumeAtWave(int nextWave) {
        if (nextWave < 1) nextWave = 1;
        if (nextWave > totalWaves) nextWave = totalWaves;
        this.currentWave = nextWave - 1;
        this.inWave = false;
        this.entryIndex = 0;
        this.remainInEntry = 0;
        this.entryTimer = 0f;
        this.allWavesCompleted = (nextWave - 1) >= totalWaves;
    }

    // ===================== Wave definitions & spawning =====================

    private void buildDefaultWaves() {
        waves.clear();
        for (int w = 1; w <= 4; w++) {
            WaveDef d = new WaveDef();
            int count = 8 + (w - 1) * 2;
            float iv = Math.max(0.7f, 1.2f - (w - 1) * 0.04f);
            d.add(EnemyType.GRUNT, count, iv);
            waves.add(d);
        }
        {
            WaveDef d = new WaveDef();
            d.add(EnemyType.MINI_BOSS, 1, 1.6f);
            waves.add(d);
        }
        for (int w = 6; w <= 9; w++) {
            WaveDef d = new WaveDef();
            boolean runnerPhase = (w % 2 == 0);
            int gruntCount = 10 + (w - 6) * 2;
            int otherCount = 8 + (w - 6) * 2;
            float ivGrunt = Math.max(0.6f, 1.0f - (w - 6) * 0.05f);
            float ivOther = Math.max(0.6f, 1.1f - (w - 6) * 0.05f);
            d.add(EnemyType.GRUNT, gruntCount, ivGrunt);
            d.add(runnerPhase ? EnemyType.RUNNER : EnemyType.TANK, otherCount, ivOther);
            waves.add(d);
        }
        {
            WaveDef d = new WaveDef();
            d.add(EnemyType.MID_BOSS, 1, 1.7f);
            waves.add(d);
        }
        for (int w = 11; w <= 14; w++) {
            WaveDef d = new WaveDef();
            int each = 8 + (w - 11) * 2;
            float iv = Math.max(0.55f, 0.9f - (w - 11) * 0.05f);
            d.add(EnemyType.GRUNT, each, iv);
            d.add(EnemyType.RUNNER, each, iv);
            d.add(EnemyType.TANK, each - 2, iv + 0.05f);
            waves.add(d);
        }
        {
            WaveDef d = new WaveDef();
            d.add(EnemyType.MINI_BOSS_2, 2, 1.8f);
            waves.add(d);
        }
        for (int w = 16; w <= 19; w++) {
            WaveDef d = new WaveDef();
            int g = 10 + (w - 16) * 2;
            int r = 8 + (w - 16) * 2;
            int t = 8 + (w - 16) * 2;
            float iv = Math.max(0.5f, 0.85f - (w - 16) * 0.05f);
            d.add(EnemyType.ELITE_GRUNT, g, iv);
            if (w % 2 == 0) {
                d.add(EnemyType.ELITE_RUNNER, r, iv);
                d.add(EnemyType.ELITE_TANK, t - 2, iv + 0.05f);
            } else {
                d.add(EnemyType.ELITE_TANK, t, iv + 0.05f);
                d.add(EnemyType.ELITE_RUNNER, r - 2, iv);
            }
            waves.add(d);
        }
        {
            WaveDef d = new WaveDef();
            d.add(EnemyType.FINAL_BOSS, 1, 1.8f);
            waves.add(d);
        }
        while (waves.size < totalWaves) {
            WaveDef d = new WaveDef();
            d.add(EnemyType.GRUNT, 12, 0.9f);
            waves.add(d);
        }
    }

    // CHỈNH: truyền đúng type khi tạo Enemy
    private void spawnEnemyOfType(EnemyType type, int wave) {
        Enemy e = new Enemy(world.path, type);

        float H = Constants.ENEMY_HP;
        float hpMult = hpMultForWave(wave);

        float speedMul = 1.0f;
        int reward = 5;

        switch (type) {
            case GRUNT:
                reward = 5;
                break;
            case RUNNER:
                hpMult *= 0.65f;
                speedMul = 1.25f;
                reward = 4;
                break;
            case TANK:
                hpMult *= 2.0f;
                speedMul = 0.80f;
                reward = 7;
                break;
            case ELITE_GRUNT:
                hpMult *= 1.25f;
                speedMul = 1.10f;
                reward = Math.round(5 * 1.2f);
                break;
            case ELITE_RUNNER:
                hpMult *= 0.65f * 1.25f;
                speedMul = 1.30f;
                reward = Math.round(4 * 1.2f);
                break;
            case ELITE_TANK:
                hpMult *= 2.0f * 1.25f;
                speedMul = 0.80f;
                reward = Math.round(7 * 1.2f);
                break;
            case MINI_BOSS:
                hpMult = 18f;
                speedMul = 0.90f;
                e.damageTakenMultiplier = 0.9f;
                reward = 30;
                break;
            case MID_BOSS:
                hpMult = 40f;
                speedMul = 0.90f;
                e.shieldPulsing = true;
                e.shieldPeriod = 6f;
                e.shieldDuration = 2f;
                e.shieldDmgMultiplier = 0.5f;
                reward = 45;
                break;
            case MINI_BOSS_2:
                hpMult = 28f;
                speedMul = 0.90f;
                e.damageTakenMultiplier = 0.9f;
                reward = 35;
                break;
            case FINAL_BOSS:
                hpMult = 85f;
                speedMul = 0.85f;
                e.enrageEnabled = true;
                e.enrageThreshold = 0.5f;
                e.enrageSpeedMultiplier = 1.2f;
                reward = 80;
                break;
        }

        float hp = H * hpMult;
        e.maxHp = e.hp = hp;
        e.setBaseSpeed(Constants.ENEMY_BASE_SPEED * speedMul);
        e.goldReward = reward;

        world.enemies.add(e);
    }

    private float hpMultForWave(int w) {
        if (w <= 5) return 1.0f + 0.08f * (w - 1);
        if (w <= 10) return 1.32f + 0.10f * (w - 6);
        if (w <= 15) return 1.82f + 0.12f * (w - 11);
        if (w <= 19) return 2.42f + 0.14f * (w - 16);
        return 3.00f;
    }
}
