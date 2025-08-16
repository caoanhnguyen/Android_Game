package com.mygdx.td.systems;

import com.badlogic.gdx.utils.Array;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Cộng vàng 1 lần duy nhất cho mỗi enemy khi nó chuyển sang trạng thái dead.
 * Mặc định: +1 vàng/kill (có thể truyền số khác qua constructor).
 */
public class KillRewardSystem {
    private final Set<Enemy> rewarded = new HashSet<>();
    private final int rewardPerKill;

    public KillRewardSystem() { this(1); }
    public KillRewardSystem(int rewardPerKill) { this.rewardPerKill = rewardPerKill; }

    public void update(World world, Array<Enemy> enemies) {
        if (world == null || enemies == null) return;

        for (Enemy e : enemies) {
            if (e == null) continue;
            // Dùng hàm isDead() để tránh phụ thuộc field public
            if (e.isDead() && !rewarded.contains(e)) {
                // Thưởng vàng ngay khi xác nhận kẻ địch đã chết
                world.gold += rewardPerKill;
                rewarded.add(e);
            }
        }

        // Dọn set để tránh giữ tham chiếu quá lâu (khi enemy đã bị remove)
        rewarded.removeIf(e -> !enemies.contains(e, true));
    }

    public void reset() { rewarded.clear(); }
}
