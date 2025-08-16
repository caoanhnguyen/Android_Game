package com.mygdx.td.world;

import com.badlogic.gdx.math.Rectangle;

/**
 * Gói thông tin 1 spot xây tower lấy từ map.
 */
public class TowerSpot {
    public final Rectangle rect;
    public boolean used = false;

    public TowerSpot(Rectangle r) {
        // copy để tránh reference trực tiếp map object (an toàn khi dispose map)
        this.rect = new Rectangle(r);
    }

    public boolean contains(float x, float y) {
        return rect.contains(x, y);
    }
}
