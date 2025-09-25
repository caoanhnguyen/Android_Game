package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.td.TDGame;
import com.mygdx.td.animations.EnemyVisual;
import com.mygdx.td.animations.TowerVisual;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.render.OrderedOrthogonalTiledMapRenderer;
import com.mygdx.td.ui.UIHud;
import com.mygdx.td.world.TowerSpot;
import com.mygdx.td.world.World;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

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

    private final Array<Vector2> loadedWaypoints = new Array<>();
    private String mapPath = "maps/level1.tmx";

    private final ObjectMap<Tower, TowerVisual> towerVisuals = new ObjectMap<>();
    private TowerSpot selectedSpot;
    private final UIHud hud;
    private boolean gamePaused = false;
    private Tower selectedTower = null;

    // Enemy visuals (khôi phục theo repo cũ)
    private final ObjectMap<Enemy, EnemyVisual> enemyVisuals = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyVisual> dyingEnemyVisuals = new ObjectMap<>();
    private final Set<Enemy> rewardedEnemies = new HashSet<>();

    // Cấu hình strips enemy (giống repo cũ)
    private static final String ENEMY_BASE_FOLDER = "enemies/wizard";
    private static final String E_WALK_SIDE = "S_Run.png";
    private static final String E_WALK_DOWN = "D_Run.png";
    private static final String E_WALK_UP   = "U_Run.png";
    private static final String E_DEATH_SIDE    = "S_Death.png";
    private static final String E_DEATH_DOWN    = "D_Death.png";
    private static final String E_DEATH_UP      = "U_Death.png";
    private static final String E_DEATH_GENERIC = null;
    private static final int    E_FRAME_W = 96;
    private static final int    E_FRAME_H = 96;
    private static final int    E_SPACING_X = 0, E_MARGIN_X = 0, E_MARGIN_Y = 0;
    private static final int    E_FRAMES_WALK  = -1;        // auto detect
    private static final float  E_WALK_FRAME_SEC = 0.12f;
    private static final int    E_FRAMES_DEATH = -1;        // auto detect
    private static final float  E_DEATH_FRAME_SEC = 0.10f;
    private static final float  ENEMY_TILE_SIZE = 64f;
    private static final boolean ENEMY_STRIP_FACES_RIGHT = false;

    // HP bar
    private static final float HP_BAR_WIDTH = 40f;
    private static final float HP_BAR_HEIGHT = 6f;
    private static final float HP_BAR_Y_OFFSET = 38f;
    private static final int GOLD_PER_KILL = 5;

    public GameScreen(TDGame game) { this(game, 1); }

    public GameScreen(TDGame game, int level) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();

        mapPath = getMapPathForLevel(level);

        world = new World();
        world.setGame(game);
        loadMap();
        applyLoadedPath();

        hud = new UIHud(game, world);
        hud.setTowerTypes(TowerType.ALL);
        hud.setTowerSelectListener(selectedType -> {
            if (selectedSpot != null) {
                if (world.placeTowerOnSpot(selectedSpot, selectedType)) {
                    // visual Tower sẽ tạo ở render
                }
                selectedSpot = null;
            }
        });

        // WIRE HUD CALLBACKS: BẮT ĐẦU WAVE + ĐỒNG BỘ PAUSE
        hud.onStartWave = () -> {
            if (world.waveManager != null && !world.waveManager.isInWave() && !world.gameOver) {
                world.waveManager.startNextWave();
                Gdx.app.log("GAME", "Start wave " + world.waveManager.getCurrentWave());
            }
        };
        hud.onPauseToggle = () -> {
            gamePaused = hud.isPaused();
            Gdx.app.log("GAME", gamePaused ? "Paused" : "Playing");
            // Nếu vừa chuyển sang Playing mà chưa có wave thì tự bắt đầu
            if (!gamePaused && world.waveManager != null && !world.waveManager.isInWave() && !world.gameOver) {
                world.waveManager.startNextWave();
                Gdx.app.log("GAME", "Auto start wave " + world.waveManager.getCurrentWave());
            }
        };
        // Đồng bộ trạng thái ban đầu với HUD (HUD mặc định paused=true)
        gamePaused = hud.isPaused();

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(hud.getStage());
        mux.addProcessor(this);
        Gdx.input.setInputProcessor(mux);
    }

    private String getMapPathForLevel(int level) {
        String cand = "maps/level" + level + ".tmx";
        if (Gdx.files.internal(cand).exists()) return cand;
        return "maps/level1.tmx";
    }

    private void loadMap() {
        try {
            tiledMap = new TmxMapLoader().load(mapPath);
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
        for (int i = 0; i < layers.size(); i++) flatLayers.add(layers.get(i));
    }

    private void extractPath() {
        loadedWaypoints.clear();
        if (tiledMap == null) return;
        MapLayer layer = tiledMap.getLayers().get("Path");
        if (layer == null) return;
        for (com.badlogic.gdx.maps.MapObject o : layer.getObjects()) {
            if (o instanceof com.badlogic.gdx.maps.objects.PolylineMapObject) {
                float[] v = ((com.badlogic.gdx.maps.objects.PolylineMapObject) o).getPolyline().getTransformedVertices();
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
        for (com.badlogic.gdx.maps.MapObject o : spots.getObjects()) {
            if (o instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                Rectangle r = ((com.badlogic.gdx.maps.objects.RectangleMapObject) o).getRectangle();
                world.towerSpots.add(new TowerSpot(new Rectangle(r)));
            }
        }
    }

    private void applyLoadedPath() {
        if (loadedWaypoints.size >= 2) world.path.loadFrom(loadedWaypoints);
    }

    // Enemy visuals

    private void syncEnemyVisuals() {
        // Add visuals cho enemy mới xuất hiện
        for (Enemy e : world.enemies) {
            if (!enemyVisuals.containsKey(e)) {
                EnemyVisual v = EnemyVisual.fromStripsFixed(
                    ENEMY_BASE_FOLDER,
                    E_WALK_SIDE, E_WALK_DOWN, E_WALK_UP,
                    E_DEATH_SIDE, E_DEATH_DOWN, E_DEATH_UP, E_DEATH_GENERIC,
                    E_FRAME_W, E_FRAME_H,
                    E_FRAMES_WALK, E_WALK_FRAME_SEC,
                    E_FRAMES_DEATH, E_DEATH_FRAME_SEC,
                    E_SPACING_X, E_MARGIN_X, E_MARGIN_Y,
                    ENEMY_TILE_SIZE, ENEMY_STRIP_FACES_RIGHT
                );
                enemyVisuals.put(e, v);
            }
        }
        // Chuyển visual của enemy rời world sang dying map (để chạy death anim)
        Set<Enemy> toMove = new HashSet<>();
        for (Enemy e : enemyVisuals.keys()) {
            if (!world.enemies.contains(e, true)) toMove.add(e);
        }
        for (Enemy e : toMove) {
            EnemyVisual v = enemyVisuals.remove(e);
            if (v != null) {
                // Thưởng vàng nếu là bị tiêu diệt (không phải lọt end)
                if (!rewardedEnemies.contains(e) && e.isDead() && !e.reachedEnd) {
                    world.gold += GOLD_PER_KILL;
                    rewardedEnemies.add(e);
                }
                if (v.isReadyToRemove(e)) {
                    v.dispose();
                } else {
                    dyingEnemyVisuals.put(e, v);
                }
            }
        }
        // Gỡ dấu đã thưởng cho các enemy không còn trong dying map
        rewardedEnemies.removeIf(e -> !dyingEnemyVisuals.containsKey(e));
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

    private void applyKillRewards() {
        // Giữ lại để an toàn (nhưng hiện tại thưởng vàng chủ yếu trong syncEnemyVisuals khi rời world)
        for (Enemy e : world.enemies) {
            if (e.isDead() && !rewardedEnemies.contains(e)) {
                world.gold += GOLD_PER_KILL;
                rewardedEnemies.add(e);
            }
        }
        rewardedEnemies.removeIf(e -> !world.enemies.contains(e, true) && !dyingEnemyVisuals.containsKey(e));
    }

    @Override
    public void render(float delta) {
        if (!gamePaused) {
            world.update(delta);
            applyKillRewards();
            syncEnemyVisuals();
            updateEnemyVisuals(delta);
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        AnimatedTiledMapTile.updateAnimationBaseTime();

        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Draw enemies (alive)
        for (Enemy e : world.enemies) {
            EnemyVisual v = enemyVisuals.get(e);
            if (v != null) v.draw(game.batch, e);
        }
        // Draw enemies still playing death animation
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : dyingEnemyVisuals.entries()) {
            entry.value.draw(game.batch, entry.key);
        }
        // Draw HP bars
        for (Enemy e : world.enemies) {
            if (e.isDead()) continue;
            float pct = Math.max(0f, Math.min(1f, e.getHpPercent()));
            float bw = HP_BAR_WIDTH, bh = HP_BAR_HEIGHT;
            float bx = e.pos.x - bw / 2f;
            float by = e.pos.y + HP_BAR_Y_OFFSET;

            game.batch.setColor(0, 0, 0, 0.6f);
            game.batch.draw(game.assets.whitePixel, bx, by, bw, bh);
            game.batch.setColor(0, 1, 0, 1);
            game.batch.draw(game.assets.whitePixel, bx, by, bw * pct, bh);
            game.batch.setColor(1, 1, 1, 1);
        }

        // Draw towers
        for (Tower t : world.towers) {
            TowerVisual v = towerVisuals.get(t);
            if (v == null) {
                v = new TowerVisual(t.type);
                v.triggerPlace();
                towerVisuals.put(t, v);
            }
            v.update(t, delta);
            v.draw(game.batch, t);
        }
        // Draw bullets
        for (Bullet b : world.bullets) {
            game.batch.draw(game.assets.bulletTex, b.pos.x - 8, b.pos.y - 8, 16, 16);
        }

        game.batch.end();

        hud.act(delta);
        hud.updateHudValues(world.waveManager.isInWave());
        hud.draw();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        camera.update();
        Vector3 touch = new Vector3(screenX, screenY, 0);
        viewport.unproject(touch);

        float worldX = touch.x;
        float worldY = touch.y;
        if (worldX < 0 || worldX > viewport.getWorldWidth() || worldY < 0 || worldY > viewport.getWorldHeight())
            return false;

        // Ưu tiên chọn trụ nếu bấm vào trụ đã đặt, nếu không thì chọn spot
        Tower tower = findTowerAt(worldX, worldY, 40);
        if (tower != null) {
            selectedTower = tower;
            hud.showUpgradePopupHUD(tower, () -> {
                boolean ok = world.upgradeTower(tower);
                if (ok) {
                    TowerVisual v = towerVisuals.get(tower);
                    if (v != null) v.triggerUpgrade();
                }
                hud.hideUpgradePopupHUD();
            });
            return true;
        }

        TowerSpot spot = findHoverSpot(worldX, worldY);
        if (spot != null && !spot.used) {
            selectedSpot = spot;
            hud.showTowerPopupHUD();
            return true;
        }
        return false;
    }

    private Tower findTowerAt(float x, float y, float r) {
        float r2 = r * r;
        for (Tower t : world.towers)
            if (t.pos.dst2(x, y) <= r2) return t;
        return null;
    }

    private TowerSpot findHoverSpot(float x, float y) {
        for (TowerSpot s : world.towerSpots)
            if (s.contains(x, y) && !s.used) return s;
        return null;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0); camera.update(); if (hud != null) hud.getStage().getViewport().update(w, h, true); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() { gamePaused = true; }
    @Override public void resume() { gamePaused = false; }
    @Override public void dispose() {
        hud.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();
        for (TowerVisual v : towerVisuals.values()) v.dispose();
        towerVisuals.clear();
        for (EnemyVisual v : enemyVisuals.values()) v.dispose();
        enemyVisuals.clear();
        for (EnemyVisual v : dyingEnemyVisuals.values()) v.dispose();
        dyingEnemyVisuals.clear();
    }
}
