package com.mygdx.td.entities;

/**
 * Định nghĩa loại trụ & chuỗi nâng cấp.
 * Cấp:
 *  0: WOOD
 *  1: STONE
 *  2: IRON
 *  3: STONE2
 *  4: IRON2
 *
 * Chuỗi upgrade: WOOD -> STONE -> IRON -> STONE2 -> IRON2
 *
 * Ghi chú asset:
 *  - assetFolder: (wood / stone / iron / stone2 / iron2)
 *  - idleFile: luôn "B_Idle.png"
 *  - upgradeFile: bạn đã chuẩn bị tương ứng (ví dụ B_Upgrade1.png .. B_Upgrade6.png). Điều chỉnh tên nếu khác.
 *  - iconRegion: region trong atlas towers/tower_icons.atlas (tower1_icon ... tower5_icon)
 */
public class TowerType {

    public final String name;
    public final int cost;
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

    /* Các mốc cân đối (có thể chỉnh lại nếu bạn muốn):
       - Tăng range nhẹ mỗi cấp
       - Damage tăng rõ rệt
       - Cost tăng dần để khuyến khích đầu tư
     */
    public static final TowerType WOOD   = new TowerType("WOOD",   50, 120f, 10f,  "wood",   "B_Idle.png", "B_Upgrade1.png", 0, "tower1_icon");
    public static final TowerType STONE  = new TowerType("STONE",  75, 160f, 16f,  "stone",  "B_Idle.png", "B_Upgrade3.png", 1, "tower2_icon");
    public static final TowerType IRON   = new TowerType("IRON",  100, 200f, 22f,  "iron",   "B_Idle.png", "B_Upgrade4.png", 2, "tower3_icon");
    public static final TowerType STONE2 = new TowerType("STONE2",140, 240f, 30f,  "stone2", "B_Idle.png", "B_Upgrade5.png", 3, "tower4_icon");
    public static final TowerType IRON2  = new TowerType("IRON2", 190, 300f, 40f,  "iron2",  "B_Idle.png", "B_Upgrade6.png", 4, "tower5_icon");

    // ALL phải theo đúng thứ tự upgradeLevel tăng dần
    public static final TowerType[] ALL = new TowerType[]{
        WOOD, STONE, IRON, STONE2, IRON2
    };

    public TowerType nextLevel() {
        int idx = upgradeLevel + 1;
        if (idx >= ALL.length) return null;
        return ALL[idx];
    }

    /**
     * Trả về danh sách tower có thể đặt tùy theo level / map.
     *  level 1: 3 loại đầu
     *  level 2: 4 loại đầu
     *  level >=3: full 5 loại
     */
    public static TowerType[] forLevel(int level) {
        if (level <= 1) {
            return new TowerType[]{ WOOD, STONE, IRON };
        } else if (level == 2) {
            return new TowerType[]{ WOOD, STONE, IRON, STONE2 };
        } else {
            return ALL;
        }
    }
}
