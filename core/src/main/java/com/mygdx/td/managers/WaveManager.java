package com.mygdx.td.managers;

import com.mygdx.td.world.World;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.Constants;
import com.badlogic.gdx.math.Vector2;

/**
 * Quản lý wave: spawn enemy theo thời gian.
 */
public class WaveManager {

    private final World world;

    private int currentWave = 0;
    private boolean inWave = false;

    private int remainingThisWave = 0;
    private float spawnInterval = 1.2f;
    private float spawnTimer = 0f;

    public WaveManager(World world) {
        this.world = world;
    }

    public void update(float dt) {
        if (!inWave) return;
        spawnTimer -= dt;
        if (spawnTimer <= 0f && remainingThisWave > 0) {
            spawnTimer = spawnInterval;
            spawnEnemy();
            remainingThisWave--;
            if (remainingThisWave <= 0) {
                // Wave kết thúc khi không còn enemy alive -> kiểm tra ở World?
                inWave = false;
            }
        }
    }

    private void spawnEnemy() {
        Enemy e = new Enemy(world.path);
        // Có thể scale HP theo wave:
        e.hp = Constants.ENEMY_HP * (1f + currentWave * 0.15f);
        world.enemies.add(e);
    }

    public void startNextWave() {
        if (inWave) return;
        currentWave++;
        // số lượng enemy mỗi wave
        remainingThisWave = 6 + currentWave * 2;
        // giảm spawnInterval dần (tối thiểu 0.4)
        spawnInterval = Math.max(0.4f, 1.2f - currentWave * 0.05f);
        spawnTimer = 0f;
        inWave = true;
    }

    public void forceStop() {
        inWave = false;
        remainingThisWave = 0;
        spawnTimer = 0f;
    }

    public boolean isInWave() {
        return inWave;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void reset() {
        currentWave = 0;
        inWave = false;
        remainingThisWave = 0;
        spawnTimer = 0f;
        spawnInterval = 1.2f;
    }
}
