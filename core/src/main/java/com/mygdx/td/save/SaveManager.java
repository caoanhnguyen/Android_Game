package com.mygdx.td.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.Array;
import com.mygdx.td.world.World;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;

public class SaveManager {
    private static final String PREFS = "td_checkpoint";
    private static final String KEY_STATE = "state";

    public static void saveCheckpoint(int level, World world, int nextWave) {
        GameState s = new GameState();
        s.level = level;
        s.nextWave = Math.max(1, nextWave);
        s.gold = world.gold;
        s.lives = world.lives;

        Array<Tower> tws = world.towers;
        for (int i = 0; i < tws.size; i++) {
            Tower t = tws.get(i);
            GameState.TowerSave ts = new GameState.TowerSave();
            ts.x = t.pos.x;
            ts.y = t.pos.y;
            ts.typeLevel = t.getUpgradeLevel();
            Rectangle r = t.placeRect;
            if (r != null) {
                ts.hasRect = true;
                ts.rx = r.x; ts.ry = r.y; ts.rw = r.width; ts.rh = r.height;
            } else {
                ts.hasRect = false;
            }
            s.towers.add(ts);
        }

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        String payload = json.toJson(s);

        Preferences p = Gdx.app.getPreferences(PREFS);
        p.putString(KEY_STATE, payload);
        p.flush();
        Gdx.app.log("SaveManager", "Checkpoint saved: lvl=" + s.level + " nextWave=" + s.nextWave + " towers=" + s.towers.size);
    }

    public static boolean hasCheckpoint() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        String payload = p.getString(KEY_STATE, "");
        return payload != null && payload.length() > 0;
    }

    public static GameState loadCheckpoint() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        String payload = p.getString(KEY_STATE, "");
        if (payload == null || payload.isEmpty()) return null;
        Json json = new Json();
        try {
            return json.fromJson(GameState.class, payload);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to parse checkpoint: " + e.getMessage());
            return null;
        }
    }

    public static void clearCheckpoint() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        p.remove(KEY_STATE);
        p.flush();
    }
}
