package com.mygdx.td.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.mygdx.td.render.OrderedOrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;
import com.mygdx.td.animations.EnemyVisual;
import com.mygdx.td.animations.TowerVisual;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.ui.UIHud;
import com.mygdx.td.world.TowerSpot;
import com.mygdx.td.world.World;

import java.util.HashSet;
import java.util.Set;

public class GameScreen extends InputAdapter implements Screen {

    private final TDGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final World world;

    private TiledMap tiledMap;
    private OrderedOrthogonalTiledMapRenderer mapRenderer;
    private final Array<MapLayer> flatLayers = new Array<>();

    private static final String PROPS_BELOW = "PropsBelow";
    private static final String PROPS_ABOVE = "PropsAbove";
    private static final String ANIMATIONS  = "Animations";

    private final Array<Vector2> loadedWaypoints = new Array<>();
    private static final String MAP_PATH = "maps/level1.tmx";

    // Enemy strips
    private static final String ENEMY_BASE = "enemies/wizard";
    private static final String E_WALK_SIDE = "S_Run.png";
    private static final String E_WALK_DOWN = "D_Run.png";
    private static final String E_WALK_UP   = "U_Run.png";
    private static final String E_DEATH_SIDE   = "S_Death.png";
    private static final String E_DEATH_DOWN   = "D_Death.png";
    private static final String E_DEATH_UP     = "U_Death.png";
    private static final String E_DEATH_GENERIC= null;

    private static final int E_FRAME_W = 96;
    private static final int E_FRAME_H = 96;
    private static final int E_SPACING_X = 0, E_MARGIN_X = 0, E_MARGIN_Y = 0;
    private static final int E_FRAMES_WALK  = -1;
    private static final float E_WALK_SEC   = 0.12f;
    private static final int E_FRAMES_DEATH = -1;
    private static final float E_DEATH_SEC  = 0.10f;
    private static final float ENEMY_TILE_SIZE  = 64f;
    private static final boolean ENEMY_BASE_FACES_RIGHT = false;

    private static final float HP_BAR_WIDTH_FIXED = 40f;
    private static final float HP_BAR_HEIGHT      = 6f;
    private static final float HP_BAR_Y_OFFSET    = 38f;

    private static final boolean ENABLE_WAVE_HP_SCALING = true;
    private static final float   HP_MULTIPLIER_PER_WAVE = 1.12f;
    private final Set<Enemy> hpScaledEnemies = new HashSet<>();

    private static final int GOLD_PER_KILL = 5;
    private final Set<Enemy> rewardedEnemies = new HashSet<>();

    // Tower assets
    private static final String TOWER_BASE_FOLDER = "towers/wood";
    private static final String T_BASE_IDLE   = "B_Idle.png";
    private static final String T_BASE_UPG_1  = "B_Upgrade1.png";
    private static final String T_BASE_UPG_2  = "B_Upgrade2.png";
    private static final int    T_BASE_IDLE_FR = 4; private static final float  T_BASE_IDLE_SEC = 0.12f;
    private static final int    T_UPG1_FR = 4;      private static final float  T_UPG1_SEC = 0.10f;
    private static final int    T_UPG2_FR = 4;      private static final float  T_UPG2_SEC = 0.10f;
    private static final int   T_BASE_FRAME_W = 70, T_BASE_FRAME_H = 130;
    private static final int   T_BASE_SPACING_X = 0, T_BASE_MARGIN_X = 0, T_BASE_MARGIN_Y = 0;
    private static final int   T_DRAW_W = 70, T_DRAW_H = 130;
    private static final int   T_ANCHOR_BOTTOM_TO_POSY = 32;

    private final ObjectMap<Enemy, EnemyVisual> enemyVisuals = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyVisual> dyingEnemyVisuals = new ObjectMap<>();
    private final ObjectMap<Tower, TowerVisual> towerVisuals = new ObjectMap<>();

    private final Vector2 mouseWorld = new Vector2();
    private TowerSpot hoverSpot = null;

    private Tower selectedTower = null;
    private Enemy selectedEnemy = null;

    private final UIHud hud;

    private boolean debugPathLines = false;
    private boolean debugRanges = true;
    private boolean debugSpots = false;
    private boolean onlySelectedTowerRange = false;

    public GameScreen(TDGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new StretchViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT, camera);

        // Đặt camera về gốc dưới-trái của map, đảm bảo map gốc (0,0) trùng với world gốc
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
        camera.update();

        world = new World();
        loadMap();
        applyLoadedPath();

        hud = new UIHud(game, world);
        hud.onStartWave = () -> {
            if (!world.waveManager.isInWave() && !world.gameOver) world.waveManager.startNextWave();
        };

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(hud.getStage());
        mux.addProcessor(this);
        Gdx.input.setInputProcessor(mux);

        hud.updateHudValues(world.waveManager.isInWave());
    }

    private void loadMap() {
        try {
            tiledMap = new TmxMapLoader().load(MAP_PATH);
            mapRenderer = new OrderedOrthogonalTiledMapRenderer(tiledMap, 1f);
            flattenLayers();
            extractPath();
            extractTowerSpots();
        } catch (Exception e) {
            Gdx.app.error("MAP", "Load map fail: " + e.getMessage());
        }
    }

    private void flattenLayers() {
        flatLayers.clear();
        MapLayers layers = tiledMap.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayer l = layers.get(i);
            flatLayers.add(l);
        }
    }

    private void renderAllLayersWithActors() {
        mapRenderer.setView(camera);

        int idxBelow = findLayerIndex(PROPS_BELOW);
        int idxAbove = findLayerIndex(PROPS_ABOVE);
        int idxAnim  = findLayerIndex(ANIMATIONS);

        if (idxBelow < 0) idxBelow = 0;
        if (idxAbove < 0) idxAbove = idxBelow;
        if (idxAnim  < 0) idxAnim = flatLayers.size - 1;

        mapRenderer.beginCustom();
        for (int i = 0; i <= idxBelow; i++)
            mapRenderer.renderLayer(flatLayers.get(i));
        mapRenderer.endCustom();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Enemy e : world.enemies) {
            EnemyVisual v = enemyVisuals.get(e);
            if (v != null) v.draw(game.batch, e);
        }
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : dyingEnemyVisuals.entries()) {
            entry.value.draw(game.batch, entry.key);
        }
        for (Bullet b : world.bullets) {
            game.batch.draw(game.assets.bulletTex, b.pos.x - 8, b.pos.y - 8, 16, 16);
        }
        game.batch.end();

        mapRenderer.beginCustom();
        for (int i = idxBelow + 1; i <= idxAbove; i++)
            mapRenderer.renderLayer(flatLayers.get(i));
        mapRenderer.endCustom();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Tower t : world.towers) {
            TowerVisual v = towerVisuals.get(t);
            if (v != null) v.draw(game.batch, t);
        }
        game.batch.end();

        mapRenderer.beginCustom();
        for (int i = idxAbove + 1; i <= idxAnim; i++)
            mapRenderer.renderLayer(flatLayers.get(i));
        mapRenderer.endCustom();

        mapRenderer.beginCustom();
        for (int i = idxAnim + 1; i < flatLayers.size; i++)
            mapRenderer.renderLayer(flatLayers.get(i));
        mapRenderer.endCustom();
    }

    private int findLayerIndex(String name) {
        for (int i = 0; i < flatLayers.size; i++) {
            if (flatLayers.get(i).getName() != null &&
                flatLayers.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private void extractPath() {
        loadedWaypoints.clear();
        if (tiledMap == null) return;
        MapLayer layer = tiledMap.getLayers().get("Path");
        if (layer == null) return;
        for (MapObject o : layer.getObjects()) {
            if (o instanceof PolylineMapObject) {
                float[] v = ((PolylineMapObject) o).getPolyline().getTransformedVertices();
                for (int i = 0; i < v.length; i += 2) loadedWaypoints.add(new Vector2(v[i], v[i + 1]));
                break;
            }
        }
    }

    private void extractTowerSpots() {
        world.towerSpots.clear();
        if (tiledMap == null) return;
        MapLayer spots = tiledMap.getLayers().get("TowerSpots");
        if (spots == null) return;
        for (MapObject o : spots.getObjects()) {
            if (o instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) o).getRectangle();
                world.towerSpots.add(new TowerSpot(new Rectangle(r)));
            }
        }
    }

    private void applyLoadedPath() {
        if (loadedWaypoints.size >= 2) world.path.loadFrom(loadedWaypoints);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // xóa màn hình bằng màu xanh đậm

        camera.update();
        AnimatedTiledMapTile.updateAnimationBaseTime();

        renderAllLayersWithActors();

        // Draw HP bars (separate batch)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Enemy e : world.enemies) {
            if (e.dead) continue;
            float pctRaw = e.getHpPercent();
            float pct = Math.max(0f, Math.min(1f, pctRaw));
            float bw = HP_BAR_WIDTH_FIXED, bh = HP_BAR_HEIGHT;
            float bx = e.pos.x - bw / 2f;
            float by = e.pos.y + HP_BAR_Y_OFFSET;

            game.batch.setColor(0, 0, 0, 0.6f);
            game.batch.draw(game.assets.whitePixel, bx, by, bw, bh);
            game.batch.setColor(0, 1, 0, 1);
            game.batch.draw(game.assets.whitePixel, bx, by, bw * pct, bh);
            game.batch.setColor(1, 1, 1, 1);
        }
        game.batch.end();

        // HUD
        hud.act(delta);
        hud.updateHudValues(world.waveManager.isInWave());
        hud.draw();
    }

    private void update(float dt) {
        if (!world.gameOver) {
            world.update(dt);
            applyKillRewards();
            applyWaveHpScaling();
            syncEnemyVisuals();
            syncTowerVisuals();
            updateEnemyVisuals(dt);
            updateTowerVisuals(dt);
        } else {
            hoverSpot = null;
        }
    }

    private void applyKillRewards() {
        for (Enemy e : world.enemies) {
            if (e.isDead() && !rewardedEnemies.contains(e)) {
                world.gold += GOLD_PER_KILL;
                rewardedEnemies.add(e);
            }
        }
        rewardedEnemies.removeIf(e -> !world.enemies.contains(e, true));
    }

    private void applyWaveHpScaling() {
        if (!ENABLE_WAVE_HP_SCALING) return;
        int waveIndex = 1;
        try {
            waveIndex = Math.max(1, world.waveManager.getCurrentWave());
        } catch (Throwable ignored) { waveIndex = 1; }
        if (waveIndex <= 1) return;
        float mul = (float) Math.pow(HP_MULTIPLIER_PER_WAVE, waveIndex - 1);
        for (Enemy e : world.enemies) {
            if (hpScaledEnemies.contains(e)) continue;
            e.maxHp *= mul;
            e.hp    *= mul;
            hpScaledEnemies.add(e);
        }
        hpScaledEnemies.removeIf(e -> !world.enemies.contains(e, true));
    }

    private void updateTowerVisuals(float dt) {
        for (ObjectMap.Entry<Tower, TowerVisual> entry : towerVisuals.entries()) {
            entry.value.update(entry.key, dt);
        }
    }

    private void updateEnemyVisuals(float dt) {
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : enemyVisuals.entries()) {
            entry.value.update(entry.key, dt);
        }
        Set<Enemy> toRemove = new HashSet<>();
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : dyingEnemyVisuals.entries()) {
            entry.value.update(entry.key, dt);
            if (entry.value.isReadyToRemove(entry.key)) {
                entry.value.dispose();
                toRemove.add(entry.key);
            }
        }
        for (Enemy e : toRemove) dyingEnemyVisuals.remove(e);
    }

    private void syncEnemyVisuals() {
        for (Enemy e : world.enemies) {
            if (!enemyVisuals.containsKey(e)) {
                EnemyVisual v = EnemyVisual.fromStripsFixed(
                    ENEMY_BASE, E_WALK_SIDE, E_WALK_DOWN, E_WALK_UP,
                    E_DEATH_SIDE, E_DEATH_DOWN, E_DEATH_UP, E_DEATH_GENERIC,
                    E_FRAME_W, E_FRAME_H, E_FRAMES_WALK, E_WALK_SEC,
                    E_FRAMES_DEATH, E_DEATH_SEC, E_SPACING_X, E_MARGIN_X, E_MARGIN_Y,
                    ENEMY_TILE_SIZE, ENEMY_BASE_FACES_RIGHT
                );
                enemyVisuals.put(e, v);
            }
        }
        Set<Enemy> toMove = new HashSet<>();
        for (Enemy e : enemyVisuals.keys()) if (!world.enemies.contains(e, true)) toMove.add(e);
        for (Enemy e : toMove) {
            EnemyVisual v = enemyVisuals.remove(e);
            if (v != null) {
                if (v.isReadyToRemove(e)) v.dispose();
                else dyingEnemyVisuals.put(e, v);
            }
        }
    }

    private void syncTowerVisuals() {
        TowerVisual.Config cfg = new TowerVisual.Config();
        cfg.baseFolder = TOWER_BASE_FOLDER;
        cfg.baseFrameW = T_BASE_FRAME_W; cfg.baseFrameH = T_BASE_FRAME_H;
        cfg.baseSpacingX = T_BASE_SPACING_X; cfg.baseMarginX = T_BASE_MARGIN_X; cfg.baseMarginY = T_BASE_MARGIN_Y;
        cfg.drawW = T_DRAW_W; cfg.drawH = T_DRAW_H; cfg.anchorBottomToPosY = T_ANCHOR_BOTTOM_TO_POSY;
        cfg.enableUnit = false;
        cfg.baseIdleFile = T_BASE_IDLE; cfg.baseIdleFrames = T_BASE_IDLE_FR; cfg.baseIdleFPSec = T_BASE_IDLE_SEC;
        cfg.baseUpgrade1File = T_BASE_UPG_1; cfg.baseUpgrade1Frames = T_UPG1_FR; cfg.baseUpgrade1FPSec = T_UPG1_SEC;
        cfg.baseUpgrade2File = T_BASE_UPG_2; cfg.baseUpgrade2Frames = T_UPG2_FR; cfg.baseUpgrade2FPSec = T_UPG2_SEC;

        for (Tower t : world.towers) {
            if (!towerVisuals.containsKey(t)) {
                TowerVisual v = TowerVisual.fromConfig(cfg);
                towerVisuals.put(t, v);
                v.triggerPlaceUpgrade();
            }
        }
        Set<Tower> tr = new HashSet<>();
        for (Tower t : towerVisuals.keys()) if (!world.towers.contains(t, true)) tr.add(t);
        for (Tower t : tr) { var v = towerVisuals.remove(t); if (v != null) v.dispose(); }
    }

    private TowerSpot findHoverSpot(float x, float y) {
        for (TowerSpot s : world.towerSpots) {
            if (s.contains(x, y) && !s.used) return s;
        }
        return null;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F1) debugRanges = !debugRanges;
        else if (keycode == Input.Keys.F2) debugSpots = !debugSpots;
        else if (keycode == Input.Keys.F3) debugPathLines = !debugPathLines;
        else if (keycode == Input.Keys.F4) onlySelectedTowerRange = !onlySelectedTowerRange;
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        camera.update();

        Vector3 touch = new Vector3(screenX, screenY, 0);
        viewport.unproject(touch);

        float worldX = touch.x;
        float worldY = touch.y;

        Gdx.app.log("TOUCH", "screen: " + screenX + "," + screenY + " => world: " + worldX + "," + worldY);
        Gdx.app.log("CAMERA", "pos: " + camera.position.x + "," + camera.position.y);
        Gdx.app.log("VIEWPORT", "viewW: " + viewport.getWorldWidth() + ", viewH: " + viewport.getWorldHeight());

        // Chỉ nhận touch trong vùng world thực sự
        if (worldX < 0 || worldX > viewport.getWorldWidth() || worldY < 0 || worldY > viewport.getWorldHeight()) {
            Gdx.app.log("TOUCH", "Bấm ngoài vùng world, không xử lý.");
            return false;
        }

        // ... các logic còn lại giữ nguyên như cũ
        if (!world.waveManager.isInWave()) {
            TowerSpot spot = findHoverSpot(worldX, worldY);
            if (spot != null && world.placeTowerOnSpot(spot)) {
                Gdx.app.log("TOWER", "Placed tower at: " + spot.rect);
            }
        }
        return true;
    }

    private Tower findTowerAt(float x, float y, float r) {
        float r2 = r*r;
        for (Tower t : world.towers) if (t.pos.dst2(x,y) <= r2) return t;
        return null;
    }
    private Enemy findEnemyAt(float x, float y, float r) {
        float r2 = r*r;
        for (Enemy e : world.enemies) if (!e.dead && e.pos.dst2(x,y) <= r2) return e;
        return null;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.getStage().getViewport().update(width, height, true);
    }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();
        for (ObjectMap.Entry<Enemy, EnemyVisual> e : enemyVisuals.entries()) e.value.dispose();
        enemyVisuals.clear();
        for (ObjectMap.Entry<Enemy, EnemyVisual> e : dyingEnemyVisuals.entries()) e.value.dispose();
        dyingEnemyVisuals.clear();
        for (ObjectMap.Entry<Tower, TowerVisual> t : towerVisuals.entries()) t.value.dispose();
        towerVisuals.clear();
        hud.dispose();
    }
}
