package com.mygdx.td.entities;

import com.badlogic.gdx.math.Vector2;

/**
 * Bullet (arrow) – được spawn với dx, dy & angleDeg.
 * Mặc định: homing nhẹ (cập nhật hướng mỗi frame về target). Muốn bay thẳng:
 *  - Set STRAIGHT_SHOT = true.
 */
public class Bullet {
    public final Vector2 pos = new Vector2();
    public Enemy target;
    public float speed = 300f;
    public float damage = 10f;
    public boolean dead = false;

    // Hướng bay (đơn vị)
    public float dx = 1f;
    public float dy = 0f;
    // Góc vẽ (0° sang phải, tăng CCW)
    public float angleDeg = 0f;
    // Cấu hình
    private static final boolean STRAIGHT_SHOT = false; // đổi true nếu muốn không homing sau spawn

    // (Optional slow info)
    public float slowPercent = 0f;
    public float slowDuration = 0f;

    public void update(float dt) {
        if (dead) return;

        if (!STRAIGHT_SHOT) {
            // Homing: cập nhật hướng tới target (nếu còn sống)
            if (target == null || target.dead || target.reachedEnd) {
                dead = true;
                return;
            }
            float vx = target.pos.x - pos.x;
            float vy = target.pos.y - pos.y;
            float len = (float)Math.sqrt(vx * vx + vy * vy);
            if (len > 0.0001f) {
                dx = vx / len;
                dy = vy / len;
                angleDeg = (float)Math.toDegrees(Math.atan2(dy, dx));
            }
        } else {
            // Straight shot: nếu target chết vẫn tiếp tục bay (hoặc đánh dấu dead tuỳ ý)
            if (target != null && (target.dead || target.reachedEnd)) {
                // có thể cho biến mất sớm:
                // dead = true;
            }
        }

        pos.x += dx * speed * dt;
        pos.y += dy * speed * dt;
    }
}
