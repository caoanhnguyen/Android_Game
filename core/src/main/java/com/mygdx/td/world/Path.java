package com.mygdx.td.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Lưu danh sách waypoint kẻ địch đi qua.
 * Có thể build mặc định hoặc load từ Tiled.
 */
public class Path {
    private final Array<Vector2> waypoints = new Array<>();

    public Path() {
        buildDefault();
    }

    public void buildDefault() {
        waypoints.clear();
        // Mặc định cũ (fallback khi không có map)
        waypoints.add(new Vector2(60,  400));
        waypoints.add(new Vector2(180, 400));
        waypoints.add(new Vector2(180, 300));
        waypoints.add(new Vector2(360, 300));
        waypoints.add(new Vector2(360, 180));
        waypoints.add(new Vector2(600, 180));
        waypoints.add(new Vector2(600, 120));
        waypoints.add(new Vector2(840, 120));
    }

    public Array<Vector2> getWaypoints() {
        return waypoints;
    }

    /**
     * Ghi đè toàn bộ waypoint bằng dữ liệu load từ map.
     */
    public void loadFrom(Array<Vector2> src) {
        waypoints.clear();
        for (Vector2 v : src) {
            // copy để tránh sửa nhầm đối tượng nguồn
            waypoints.add(new Vector2(v));
        }
    }

    public boolean isEmpty() {
        return waypoints.size == 0;
    }
}
