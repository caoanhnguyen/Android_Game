package com.mygdx.td.entities;

/**
 * Định nghĩa loại trụ & chuỗi nâng cấp.
 * upgradeLevel:
 * 0: WOOD
 * 1: STONE
 * 2: IRON
 * 3: STONE2
 * 4: IRON2
 *
 * Mới:
 *  - Tách bảng chi phí nâng cấp riêng (UPGRADE_COST_*).
 *  - incrementalUpgradeCost() nay dùng bảng này thay vì (next.cost - current.cost).
 */
public class TowerType {

    public final String name;
    public final int cost;       // Giá mua thẳng tower cấp này
    public final float range;
    public final float damage;

    public final String assetFolder;
    public final String idleFile;
    public final String upgradeFile;
    public final int upgradeLevel;
    public final String iconRegion;

    public TowerType(
        String name,
        int cost,
        float range,
        float damage,
        String assetFolder,
        String idleFile,
        String upgradeFile,
        int upgradeLevel,
        String iconRegion
    ) {
        this.name = name;
        this.cost = cost;
        this.range = range;
        this.damage = damage;
        this.assetFolder = assetFolder;
        this.idleFile = idleFile;
        this.upgradeFile = upgradeFile;
        this.upgradeLevel = upgradeLevel;
        this.iconRegion = iconRegion;
    }

    public static final TowerType WOOD   = new TowerType("WOOD",   50, 120f, 10f,  "wood",   "B_Idle.png", "B_Upgrade1.png", 0, "tower1_icon");
    public static final TowerType STONE  = new TowerType("STONE",  75, 160f, 16f,  "stone",  "B_Idle.png", "B_Upgrade3.png", 1, "tower2_icon");
    public static final TowerType IRON   = new TowerType("IRON",  100, 200f, 22f,  "iron",   "B_Idle.png", "B_Upgrade4.png", 2, "tower3_icon");
    public static final TowerType STONE2 = new TowerType("STONE2",140, 240f, 30f,  "stone2", "B_Idle.png", "B_Upgrade5.png", 3, "tower4_icon");
    public static final TowerType IRON2  = new TowerType("IRON2", 190, 300f, 40f,  "iron2",  "B_Idle.png", "B_Upgrade6.png", 4, "tower5_icon");

    public static final TowerType[] ALL = new TowerType[]{ WOOD, STONE, IRON, STONE2, IRON2 };

    public TowerType nextLevel() {
        int idx = upgradeLevel + 1;
        if (idx >= ALL.length) return null;
        return ALL[idx];
    }

    public static TowerType[] forLevel(int level) {
        if (level <= 1) return new TowerType[]{ WOOD, STONE, IRON };
        if (level == 2) return new TowerType[]{ WOOD, STONE, IRON, STONE2 };
        return ALL;
    }

    public static int maxAllowedUpgradeLevelForGameLevel(int gameLevel) {
        if (gameLevel <= 1) return 2;
        if (gameLevel == 2) return 3;
        return 4;
    }

    /* ================== BẢNG CHI PHÍ NÂNG CẤP ==================
       Điều chỉnh nếu muốn cân bằng khác:
       - WOOD -> STONE: 40 (mua mới STONE là 75)
       - STONE -> IRON: 50
       - IRON -> STONE2: 65
       - STONE2 -> IRON2: 85
       (Nếu bạn muốn nhẹ hơn hay nặng hơn chỉ cần sửa các hằng dưới.)
     */
    private static final int UPGRADE_COST_0_1 = 40;
    private static final int UPGRADE_COST_1_2 = 50;
    private static final int UPGRADE_COST_2_3 = 65;
    private static final int UPGRADE_COST_3_4 = 85;

    public static int incrementalUpgradeCost(TowerType current, TowerType next) {
        if (current == null || next == null) return 0;
        int from = current.upgradeLevel;
        int to = next.upgradeLevel;
        if (to - from != 1) return 0; // chỉ cho phép lên 1 cấp
        switch (from) {
            case 0: return UPGRADE_COST_0_1;
            case 1: return UPGRADE_COST_1_2;
            case 2: return UPGRADE_COST_2_3;
            case 3: return UPGRADE_COST_3_4;
            default: return 0;
        }
    }
}
