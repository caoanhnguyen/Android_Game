package com.mygdx.td;

public final class Constants {
    private Constants() {}

    // Virtual resolution
    public static final int VIRTUAL_WIDTH = 960;
    public static final int VIRTUAL_HEIGHT = 544;

    // Enemy
    public static final float ENEMY_BASE_SPEED = 80f;
    public static final float ENEMY_HP = 50f;

    // Tower (basic)
    public static final float TOWER_RANGE = 140f;
    public static final float TOWER_FIRE_INTERVAL = 0.6f;
    public static final float BULLET_SPEED = 300f;
    public static final float BULLET_DAMAGE = 25f;

    // Economy
    public static final int GOLD_START = 150;
    public static final int TOWER_COST = 50;
    public static final int GOLD_PER_ENEMY = 10;

    // Placement constraints
    public static final float MIN_TOWER_SPACING = 55f;      // khoảng cách tối thiểu giữa 2 tower
    public static final float MIN_DISTANCE_FROM_PATH = 28f; // tránh sát đường

    // Waves
    public static final float TIME_BETWEEN_WAVES = 4f;      // thời gian nghỉ mặc định
    public static final int   BASE_LIVES = 20;              // lives khởi đầu

    // (Chuẩn bị cho đa loại tower / upgrade sau này)
    public static final float FAST_TOWER_FIRE_INTERVAL = 0.25f;
    public static final float FAST_TOWER_RANGE = 110f;
    public static final float FAST_TOWER_DAMAGE = 12f;

    public static final float HEAVY_TOWER_FIRE_INTERVAL = 1.2f;
    public static final float HEAVY_TOWER_RANGE = 150f;
    public static final float HEAVY_TOWER_DAMAGE = 60f;

    public static final float SLOW_TOWER_FIRE_INTERVAL = 0.8f;
    public static final float SLOW_TOWER_RANGE = 130f;
    public static final float SLOW_TOWER_DAMAGE = 18f;
    public static final float SLOW_EFFECT_PERCENT = 0.40f;      // giảm tốc 40%
    public static final float SLOW_EFFECT_DURATION = 2.5f;      // giây

    // Upgrade multipliers (sẽ dùng nếu bạn làm nâng cấp)
    public static final float UPGRADE_DAMAGE_MULT = 1.25f;
    public static final float UPGRADE_RANGE_ADD = 12f;
    public static final float UPGRADE_FIRE_RATE_MULT = 0.90f;
}
