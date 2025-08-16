package com.mygdx.td.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;

/**
 * Gói thông tin 1 spot xây tower lấy từ map.
 */
public class TowerSpot {
    public Rectangle rect;
    public boolean used = false;

    public TowerSpot(Rectangle r) {
        this.rect = new Rectangle(r);
    }

    public boolean contains(float x, float y) {
        float pad = 20f;
        Rectangle r = new Rectangle(rect.x - pad, rect.y - pad, rect.width + pad * 2, rect.height + pad * 2);
        boolean ok = r.contains(x, y);
        if (ok) {
            Gdx.app.log("SPOT", "Touch " + x + "," + y + " in spot " + r.toString());
        }
        return ok;
    }
}
