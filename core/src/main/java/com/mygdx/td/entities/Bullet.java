package com.mygdx.td.entities;

import com.badlogic.gdx.math.Vector2;

public class Bullet {
    public final Vector2 pos = new Vector2();
    public Enemy target;
    public float speed = 300f;
    public float damage = 10f;
    public boolean dead = false;

    // (Sau này slow)
    public float slowPercent = 0f;     // ví dụ 0.4f = giảm 40%
    public float slowDuration = 0f;

    public void update(float dt) {
        if (dead) return;
        if (target == null || target.dead || target.reachedEnd) {
            dead = true;
            return;
        }
        // bay hướng target
        float dx = target.pos.x - pos.x;
        float dy = target.pos.y - pos.y;
        float dist2 = dx*dx + dy*dy;
        if (dist2 == 0f) return;
        float dist = (float)Math.sqrt(dist2);
        float step = speed * dt;
        if (dist <= step) {
            // sẽ được xử lý hit trong World.bulletHit()
            pos.set(target.pos);
        } else {
            float inv = 1f / dist;
            pos.x += dx * inv * step;
            pos.y += dy * inv * step;
        }
    }
}
