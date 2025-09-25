package com.mygdx.td.save;

import com.badlogic.gdx.utils.Array;

public class GameState {
    public int level;       // level index (1-based)
    public int nextWave;    // wave sẽ bắt đầu khi resume (1-based: 1,2,3,...)
    public int gold;
    public int lives;

    public Array<TowerSave> towers = new Array<>();

    public static class TowerSave {
        public float x, y;        // vị trí tâm
        public int typeLevel;     // TowerType.upgradeLevel (0..)
        public boolean hasRect;   // có thuộc 1 TowerSpot không
        public float rx, ry, rw, rh; // rect spot (nếu có)
    }
}
