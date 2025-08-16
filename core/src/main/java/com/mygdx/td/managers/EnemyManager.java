package com.mygdx.td.managers;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Array;
import com.mygdx.td.animations.EnemyVisual;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.Path;

/**
 * EnemyManager quản lý Enemy + EnemyVisual theo cặp, cập nhật và vẽ.
 * - Cắt animation theo frame cố định (frameW/frameH), hỗ trợ spacing/margin.
 * - Death theo hướng (SIDE/UP/DOWN) + death generic (fallback).
 * - Snap-to-pixel và Nearest filter được cấu hình trong EnemyVisual.
 *
 * Lưu ý tích hợp:
 * - Nếu bạn đang dùng GameScreen.syncEnemyVisuals() để tự tạo visual cho world.enemies,
 *   KHÔNG dùng EnemyManager.draw() nữa để tránh vẽ trùng. Khi dùng EnemyManager, bạn có thể:
 *     + Bỏ syncEnemyVisuals() và chỉ gọi manager.update()/manager.draw().
 *     + Hoặc dùng manager.spawn(...) để tạo Enemy và tự thêm visual vào map của bạn (tùy biến thêm nếu cần).
 */
public class EnemyManager implements AutoCloseable {

    public static class Config {
        // Thư mục chứa strip
        public String baseFolder = "enemies/house";

        // Tên file walk (có thể null nếu không có hướng đó)
        public String walkSide = "S_Run.png";
        public String walkDown = "D_Run.png";
        public String walkUp   = "U_Run.png";

        // Tên file death theo hướng (có thể null)
        public String deathSide = "S_Death.png";
        public String deathDown = "D_Death.png";
        public String deathUp   = "U_Death.png";
        // Fallback death chung (nếu 3 cái trên null)
        public String deathGeneric = null;

        // Kích thước 1 khung (pixel) trong strip
        public int frameW = 96;
        public int frameH = 96;

        // Nếu strip có khoảng trắng giữa frame hoặc lề trái/trên
        public int spacingX = 0;
        public int marginX  = 0;
        public int marginY  = 0;

        // Số khung và thời lượng (sec). frames <= 0 -> auto đếm theo width/(frameW+spacingX)
        public int   framesWalk  = -1;
        public float walkFrameSec = 0.12f;

        public int   framesDeath  = -1;
        public float deathFrameSec = 0.10f;

        // Chiều cao hiển thị mong muốn (pixel). Ví dụ 64 để khớp tile.
        public float tileSize = 64f;

        // Strip SIDE gốc nhìn sang phải? Nếu strip gốc nhìn sang trái -> để false
        public boolean baseFacesRight = false;
    }

    private static class Unit {
        Enemy e;
        EnemyVisual v;
        Unit(Enemy e, EnemyVisual v) { this.e = e; this.v = v; }
    }

    private final Array<Unit> units = new Array<>();
    private final Config cfg;

    public EnemyManager() {
        this(new Config());
    }

    public EnemyManager(Config config) {
        this.cfg = config;
    }

    /**
     * Tạo một Enemy theo path, gán speed/hp, đồng thời tạo EnemyVisual từ config và quản lý bên trong.
     */
    public Enemy spawn(Path path, float speed, float hp) {
        Enemy e = new Enemy(path);
        e.setBaseSpeed(speed);
        e.maxHp = e.hp = hp;

        EnemyVisual v = EnemyVisual.fromStripsFixed(
            cfg.baseFolder,
            cfg.walkSide, cfg.walkDown, cfg.walkUp,
            cfg.deathSide, cfg.deathDown, cfg.deathUp, cfg.deathGeneric,
            cfg.frameW, cfg.frameH,
            cfg.framesWalk, cfg.walkFrameSec,
            cfg.framesDeath, cfg.deathFrameSec,
            cfg.spacingX, cfg.marginX, cfg.marginY,
            cfg.tileSize, cfg.baseFacesRight
        );

        units.add(new Unit(e, v));
        return e;
    }

    /**
     * Cập nhật Enemy và Visual. Kẻ địch tới đích hoặc chết xong anim thì tự loại khỏi danh sách.
     */
    public void update(float dt) {
        for (int i = units.size - 1; i >= 0; i--) {
            Unit u = units.get(i);
            u.e.update(dt);
            u.v.update(u.e, dt);

            // Loại bỏ khi:
            // - Enemy tới đích (tùy game logic, nếu muốn về World trừ máu thì nên làm ở nơi khác)
            // - Enemy chết xong anim (isReadyToRemove)
            if (u.e.hasReachedEnd() || u.v.isReadyToRemove(u.e)) {
                u.v.dispose();
                units.removeIndex(i);
            }
        }
    }

    /**
     * Vẽ toàn bộ enemy đã spawn bởi manager này.
     */
    public void draw(Batch batch) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            u.v.draw(batch, u.e);
        }
    }

    /**
     * Trả ra danh sách kẻ địch còn sống (không tính đã tới đích).
     */
    public Array<Enemy> getAliveEnemies(Array<Enemy> out) {
        out.clear();
        for (int i = 0; i < units.size; i++) {
            Enemy e = units.get(i).e;
            if (!e.isDead() && !e.hasReachedEnd()) out.add(e);
        }
        return out;
    }

    /**
     * Lấy trực tiếp danh sách Enemy (kể cả đã chết nhưng chưa remove vì còn đang chạy anim death).
     */
    public Array<Enemy> getAllEnemies(Array<Enemy> out) {
        out.clear();
        for (int i = 0; i < units.size; i++) out.add(units.get(i).e);
        return out;
    }

    /**
     * Hủy toàn bộ visual (gọi khi quit/reset scene).
     */
    @Override
    public void close() {
        for (int i = 0; i < units.size; i++) {
            if (units.get(i).v != null) units.get(i).v.dispose();
        }
        units.clear();
    }
}
