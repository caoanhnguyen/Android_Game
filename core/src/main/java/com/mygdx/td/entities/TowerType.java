package com.mygdx.td.entities;

public class TowerType {
    public final String name;         // Tên hiển thị
    public final int cost;            // Giá tiền để đặt/nâng lên cấp này
    public final float range;
    public final float damage;

    // Assets hiển thị trong game
    public final String assetFolder;  // Thư mục frames của tower
    public final String idleFile;     // File idle (strip/sheet)
    public final String upgradeFile;  // File hiệu ứng upgrade (strip/sheet)

    // Icon HUD (region trong towers/tower_icons.atlas)
    public final String iconRegion;

    // Thứ tự cấp
    public final int upgradeLevel;    // 0: wood, 1: stone, 2: iron

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

    // Khớp đúng atlas: tower1_icon, tower2_icon, tower3_icon
    public static final TowerType WOOD  = new TowerType("WOOD",  50, 120, 10, "wood",  "B_Idle.png", "B_Upgrade1.png", 0, "tower1_icon");
    public static final TowerType STONE = new TowerType("STONE", 75, 160, 16, "stone", "B_Idle.png", "B_Upgrade3.png", 1, "tower2_icon");
    public static final TowerType IRON  = new TowerType("IRON", 100, 200, 22, "iron",  "B_Idle.png", "B_Upgrade4.png", 2, "tower3_icon");

    public static final TowerType[] ALL = new TowerType[]{ WOOD, STONE, IRON };

    public TowerType nextLevel() {
        int idx = upgradeLevel + 1;
        if (idx >= ALL.length) return null;
        return ALL[idx];
    }
}
