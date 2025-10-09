package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.td.TDGame;
import com.mygdx.td.animations.EnemyVisual;
import com.mygdx.td.animations.TowerVisual;
import com.mygdx.td.ally.AllyUnit;
import com.mygdx.td.ally.AllyUnitVisual;
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.managers.WaveManager.EnemyType;
import com.mygdx.td.render.OrderedOrthogonalTiledMapRenderer;
import com.mygdx.td.save.GameState;
import com.mygdx.td.save.SaveManager;
import com.mygdx.td.ui.UIHud;
import com.mygdx.td.ui.AbilityHud;
import com.mygdx.td.world.TowerSpot;
import com.mygdx.td.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * GameScreen – thêm: hỗ trợ dùng ability theo kiểu CHỌN → CHẠM.
 */
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
    private final ObjectMap<Enemy, EnemyVisual> enemyVisuals = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyVisual> dyingEnemyVisuals = new ObjectMap<>();
    private final ObjectMap<AllyUnit, AllyUnitVisual> allyVisuals = new ObjectMap<>();

    private TowerSpot selectedSpot;
    private Tower selectedTower;

    final int MAP_WIDTH_TILES = 30;
    final int MAP_HEIGHT_TILES = 17;
    final int TILE_SIZE = 32;

    final int MAP_WIDTH_PX = MAP_WIDTH_TILES * TILE_SIZE;
    final int MAP_HEIGHT_PX = MAP_HEIGHT_TILES * TILE_SIZE;

    private final UIHud hud;
    private AbilityHud abilityHud;

    private boolean gamePaused = false;
    private final int level;
    private boolean victoryShown = false;
    private boolean gameOverShown = false;

    private static final int SMALL_FRAME_W = 48;
    private static final int SMALL_FRAME_H = 48;
    private static final int SMALL_FRAME_COUNT = 6;
    private static final float SMALL_TILE_SIZE = 64f;

    private static final int LARGE_FRAME_W = 96;
    private static final int LARGE_FRAME_H = 96;
    private static final int LARGE_FRAME_COUNT = 6;
    private static final float LARGE_TILE_SIZE = 96f;

    private static final float WALK_FRAME_SEC = 0.12f;
    private static final float DEATH_FRAME_SEC = 0.10f;
    private static final boolean BASE_FACES_RIGHT = false;

    private static final float ALLY_FRAME_W = 48f;
    private static final float ALLY_FRAME_H = 48f;
    private static final float ALLY_TILE_SIZE = 48f;
    private static final String ALLY_ASSET_FOLDER = "allies/archer";

    private static final float HP_BAR_WIDTH = 40f;
    private static final float HP_BAR_HEIGHT = 6f;
    private static final float HP_BAR_Y_OFFSET = 38f;

    private Stage overlayStage;
    private boolean settingsShown = false;
    private Table overlayRoot;
    private Image dimBg;
    private Stack frameStack;
    private Table settingsContent;
    private Slider musicSlider, soundSlider;
    private ImageButton musicBtn, soundBtn;
    private Skin buttonSkin, iconSkinActive, iconSkinInactive, sliderSkin;
    private NinePatchDrawable frameDrawable, sliderBgDrawable;
    private TextButton saveBtn, backBtn;
    private BitmapFont titleFont;
    private Preferences prefs;
    private boolean prevMusicEnabled, prevSoundEnabled;
    private float prevMusicVolume, prevSoundVolume;

    private ShapeRenderer shapeRenderer;

    public GameScreen(TDGame game) { this(game, 1); }

    public GameScreen(TDGame game, int level) {
        this.game = game;
        this.level = level;

        camera = new OrthographicCamera();
        viewport = new FitViewport(MAP_WIDTH_PX, MAP_HEIGHT_PX, camera);
        camera.position.set(MAP_WIDTH_PX / 2f, MAP_HEIGHT_PX / 2f, 0f);
        camera.update();

        shapeRenderer = new ShapeRenderer();

        mapPath = getMapPathForLevel(level);

        world = new World(level);
        world.setGame(game);
        loadMap();
        applyLoadedPath();

        hud = new UIHud(game, world);
        hud.setTowerTypes(TowerType.forLevel(level));
        hud.setTowerSelectListener(type -> {
            if (selectedSpot != null) {
                world.placeTowerOnSpot(selectedSpot, type);
                selectedSpot = null;
            }
        });

        hud.onStartWave = () -> {
            if (!world.gameOver && !world.waveManager.isInWave())
                world.waveManager.startNextWave();
        };
        hud.onPauseToggle = () -> {
            gamePaused = hud.isPaused();
            if (!gamePaused && !world.waveManager.isInWave() && !world.gameOver)
                world.waveManager.startNextWave();
        };
        hud.onOpenSettings = this::toggleSettingsOverlay;
        hud.onExitRequested = this::showExitToSelectConfirm;

        abilityHud = new AbilityHud(game, world, world.abilityManager, viewport, hud.getStage());

        gamePaused = hud.isPaused();

        overlayStage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT), game.batch);
        buildSettingsOverlay();

        attemptResumeFromCheckpoint();

        InputMultiplexer mux = new InputMultiplexer(overlayStage, hud.getStage(), this);
        Gdx.input.setInputProcessor(mux);
    }

    private String getMapPathForLevel(int level) {
        String cand = "maps/level" + level + ".tmx";
        return Gdx.files.internal(cand).exists() ? cand : "maps/level1.tmx";
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

    private void attemptResumeFromCheckpoint() {
        GameState s = SaveManager.loadCheckpoint();
        if (s == null || s.level != this.level) return;
        applyGameState(s);
    }

    private void applyGameState(GameState s) {
        world.reset();
        for (GameState.TowerSave ts : s.towers) {
            Rectangle rect = ts.hasRect ? new Rectangle(ts.rx, ts.ry, ts.rw, ts.rh) : null;
            world.addTowerRestored(ts.x, ts.y, rect, ts.typeLevel);
        }
        world.gold = s.gold;
        world.lives = s.lives;
        world.waveManager.resumeAtWave(s.nextWave);
    }

    private int computeNextWaveForSave() {
        int cur = world.waveManager.getCurrentWave();
        boolean in = world.waveManager.isInWave();
        return Math.max(1, cur + (in ? 0 : 1));
    }

    private static boolean isLarge(EnemyType type) {
        switch (type) {
            case ELITE_TANK:
            case ELITE_GRUNT:
            case RUNNER:
            case MINI_BOSS:
            case MINI_BOSS_2:
            case MID_BOSS:
            case FINAL_BOSS:
                return true;
            default:
                return false;
        }
    }

    private static String folderFor(EnemyType type) {
        switch (type) {
            case GRUNT:         return "enemies/grunt";
            case RUNNER:        return "enemies/runner";
            case TANK:          return "enemies/tank";
            case ELITE_GRUNT:   return "enemies/elite_grunt";
            case ELITE_RUNNER:  return "enemies/elite_runner";
            case ELITE_TANK:    return "enemies/elite_tank";
            case MINI_BOSS:
            case MINI_BOSS_2:   return "enemies/mini_boss";
            case MID_BOSS:      return "enemies/mid_boss";
            case FINAL_BOSS:    return "enemies/final_boss";
            default:            return "enemies/grunt";
        }
    }

    private void syncEnemyVisuals() {
        for (Enemy e : world.enemies) {
            if (!enemyVisuals.containsKey(e)) {
                boolean large = isLarge(e.type);
                int fW = large ? LARGE_FRAME_W : SMALL_FRAME_W;
                int fH = large ? LARGE_FRAME_H : SMALL_FRAME_H;
                int fCount = large ? LARGE_FRAME_COUNT : SMALL_FRAME_COUNT;
                float tSize = large ? LARGE_TILE_SIZE : SMALL_TILE_SIZE;

                EnemyVisual vis;
                try {
                    vis = EnemyVisual.create(folderFor(e.type),
                        fW, fH, fCount, WALK_FRAME_SEC, DEATH_FRAME_SEC,
                        tSize, BASE_FACES_RIGHT);
                } catch (Exception ex) {
                    Gdx.app.error("EnemyVisual", "Load fail type=" + e.type + ": " + ex.getMessage());
                    continue;
                }
                enemyVisuals.put(e, vis);
            }
        }

        Set<Enemy> toMove = new HashSet<>();
        for (Enemy e : enemyVisuals.keys()) {
            if (!world.enemies.contains(e, true)) toMove.add(e);
        }
        for (Enemy e : toMove) {
            EnemyVisual v = enemyVisuals.remove(e);
            if (v != null) {
                if (v.isReadyToRemove(e)) v.dispose();
                else dyingEnemyVisuals.put(e, v);
            }
        }
    }

    private void syncAllyVisuals() {
        for (AllyUnit ally : world.allies) {
            if (!allyVisuals.containsKey(ally)) {
                allyVisuals.put(ally, new AllyUnitVisual(
                    ALLY_ASSET_FOLDER,
                    (int) ALLY_FRAME_W,
                    (int) ALLY_FRAME_H,
                    ALLY_TILE_SIZE
                ));
            }
        }
        Set<AllyUnit> toRemove = new HashSet<>();
        for (AllyUnit ally : allyVisuals.keys()) {
            if (!world.allies.contains(ally, true) || ally.isDead()) {
                AllyUnitVisual v = allyVisuals.remove(ally);
                if (v != null) v.dispose();
                toRemove.add(ally);
            }
        }
        for (AllyUnit ally : toRemove) allyVisuals.remove(ally);
    }

    private void updateEnemyVisuals(float dt) {
        for (ObjectMap.Entry<Enemy, EnemyVisual> en : enemyVisuals.entries()) {
            en.value.update(en.key, dt);
        }
        Set<Enemy> remove = new HashSet<>();
        for (ObjectMap.Entry<Enemy, EnemyVisual> en : dyingEnemyVisuals.entries()) {
            en.value.update(en.key, dt);
            if (en.value.isReadyToRemove(en.key)) {
                en.value.dispose();
                remove.add(en.key);
            }
        }
        for (Enemy e : remove) dyingEnemyVisuals.remove(e);
    }

    @Override
    public void render(float delta) {
        if (!gamePaused) {
            world.update(delta);

            if (world.isVictory() && !victoryShown) {
                victoryShown = true;
                SaveManager.clearCheckpoint();
                if (game.assets.win_sound != null && game.soundEnabled)
                    game.playSound(game.assets.win_sound);
                showVictoryDialog();
            }

            if (world.gameOver && !gameOverShown && !victoryShown) {
                gameOverShown = true;
                SaveManager.clearCheckpoint();
                if (game.assets.lose_sound != null && game.soundEnabled)
                    game.playSound(game.assets.lose_sound);
                showGameOverDialog();
            }

            syncEnemyVisuals();
            updateEnemyVisuals(delta);
            syncAllyVisuals();
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        AnimatedTiledMapTile.updateAnimationBaseTime();
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        // Range circle
        if (selectedTower != null && shapeRenderer != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0.8f, 1f, 0.15f);
            shapeRenderer.circle(selectedTower.pos.x, selectedTower.pos.y, selectedTower.getRange(), 80);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0f, 0.95f, 1f, 0.55f);
            shapeRenderer.circle(selectedTower.pos.x, selectedTower.pos.y, selectedTower.getRange(), 80);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Enemies
        for (Enemy e : world.enemies) {
            EnemyVisual v = enemyVisuals.get(e);
            if (v != null) v.draw(game.batch, e);
        }
        // Dying
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : dyingEnemyVisuals.entries()) {
            entry.value.draw(game.batch, entry.key);
        }
        // HP bars
        for (Enemy e : world.enemies) {
            if (e.isDead()) continue;
            float pct = Math.max(0f, Math.min(1f, e.getHpPercent()));
            float bw = HP_BAR_WIDTH, bh = HP_BAR_HEIGHT;
            float bx = e.getPos().x - bw / 2f;
            float by = e.getPos().y + HP_BAR_Y_OFFSET;
            game.batch.setColor(0,0,0,0.6f);
            game.batch.draw(game.assets.whitePixel, bx, by, bw, bh);
            game.batch.setColor(0,1,0,1);
            game.batch.draw(game.assets.whitePixel, bx, by, bw * pct, bh);
            game.batch.setColor(1,1,1,1);
        }
        // Towers
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
        // Allies – vẽ sau towers để đứng trên
        for (AllyUnit ally : world.allies) {
            AllyUnitVisual v = allyVisuals.get(ally);
            if (v != null) v.draw(game.batch, ally, ally.stateTime);
        }
        // Bullets
        for (Bullet b : world.bullets) {
            Texture tex = game.assets.arrowTex != null ? game.assets.arrowTex : game.assets.bulletTex;
            if (tex == null) continue;
            float texW = tex.getWidth();
            float texH = tex.getHeight();
            float originX = texW * 0.15f;
            float originY = texH / 2f;
            game.batch.draw(
                tex,
                b.pos.x - originX,
                b.pos.y - originY,
                originX,
                originY,
                texW,
                texH,
                1f,
                1f,
                b.angleDeg,
                0, 0,
                (int)texW, (int)texH,
                false, false
            );
        }

        // Effects abilities
        world.abilityManager.draw(game.batch);

        game.batch.end();

        hud.act(delta);
        hud.updateHudValues(world.waveManager.isInWave());
        hud.draw();

        if (abilityHud != null) abilityHud.update();

        overlayStage.act(delta);
        overlayStage.draw();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Nếu đang chọn ability: CHẠM LÊN MAP để dùng
        if (abilityHud != null && abilityHud.hasSelection()) {
            Vector3 v = new Vector3(screenX, screenY, 0);
            viewport.unproject(v);
            float wx = v.x, wy = v.y;

            // Ngoài map thì bỏ qua
            if (wx < 0 || wx > viewport.getWorldWidth() || wy < 0 || wy > viewport.getWorldHeight()) {
                return false;
            }

            boolean ok = false;
            switch (abilityHud.getSelected()) {
                case LIGHTNING:
                    ok = world.abilityManager.useLightningAt(wx, wy);
                    break;
                case BARREL:
                    ok = world.abilityManager.placeBarrel(wx, wy);
                    break;
            }
            // Mặc định: nếu dùng thành công thì tự bỏ chọn
            if (ok) abilityHud.clearSelection();
            return true; // chặn mở popup tower khi đang chọn ability
        }

        // Logic chọn tower/spot như cũ
        camera.update();
        Vector3 touch = new Vector3(screenX, screenY, 0);
        viewport.unproject(touch);
        float wx = touch.x, wy = touch.y;

        if (wx < 0 || wx > viewport.getWorldWidth() || wy < 0 || wy > viewport.getWorldHeight()) return false;
        if (settingsShown) return false;

        Tower tower = findTowerAt(wx, wy, 40);
        if (tower != null) {
            selectedTower = tower;
            selectedSpot = null;
            hud.showUpgradePopupHUD(tower, () -> {
                if (world.upgradeTower(tower)) {
                    TowerVisual v = towerVisuals.get(tower);
                    if (v != null) v.triggerUpgrade();
                }
                hud.hideUpgradePopupHUD();
            });
            return true;
        }

        TowerSpot spot = findHoverSpot(wx, wy);
        if (spot != null && !spot.used) {
            selectedSpot = spot;
            selectedTower = null;
            hud.showTowerPopupHUD();
            return true;
        } else {
            if (spot == null) selectedTower = null;
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

    /* ================= SETTINGS UI (giữ nguyên) ================= */

    private void buildSettingsOverlay() {
        prefs = Gdx.app.getPreferences("td_settings");
        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        NinePatch framePatch = new NinePatch(new Texture(Gdx.files.internal("ui/banner_11.9.png")),16,16,16,16);
        frameDrawable = new NinePatchDrawable(framePatch);

        NinePatch sliderPatch = new NinePatch(new Texture(Gdx.files.internal("ui/bg_slider.9.png")),8,8,8,8);
        sliderBgDrawable = new NinePatchDrawable(sliderPatch);

        TextureAtlas sliderAtlas = new TextureAtlas(Gdx.files.internal("ui/knob.atlas"));
        sliderSkin = new Skin(sliderAtlas);

        TextureAtlas activeIconAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
        TextureAtlas inActiveIconAtlas = new TextureAtlas(Gdx.files.internal("ui/metal_buttons_icon.atlas"));
        for (Texture t : activeIconAtlas.getTextures()) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (Texture t : inActiveIconAtlas.getTextures()) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconSkinActive = new Skin(activeIconAtlas);
        iconSkinInactive = new Skin(inActiveIconAtlas);

        TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
        for (Texture t : buttonAtlas.getTextures()) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        buttonSkin = new Skin(buttonAtlas);

        overlayRoot = new Table();
        overlayRoot.setFillParent(true);
        overlayStage.addActor(overlayRoot);

        dimBg = new Image(game.assets.whitePixel);
        dimBg.setColor(0,0,0,0.55f);
        dimBg.setFillParent(true);
        dimBg.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onBackNotSave(); }
        });

        settingsContent = new Table();
        settingsContent.pad(24f).align(Align.topLeft);

        Image frameImg = new Image(frameDrawable);
        frameImg.setScaling(Scaling.stretch);
        frameImg.setColor(1,1,1,0.93f);

        frameStack = new Stack(frameImg, settingsContent);
        buildSettingsInnerContent();

        Table center = new Table();
        center.setFillParent(true);
        overlayStage.addActor(center);

        float frameWidth = 560f, frameHeight = 230f;
        overlayRoot.add(dimBg).grow();
        center.add(frameStack).width(frameWidth).height(frameHeight).center();

        setOverlayVisible(false);
    }

    private ImageButton.ImageButtonStyle mkBtnStyle(Skin upPack, Skin overPack, String up, String over, String down) {
        ImageButton.ImageButtonStyle s = new ImageButton.ImageButtonStyle();
        s.up = upPack.getDrawable(up);
        s.over = overPack.getDrawable(over);
        s.down = overPack.getDrawable(down);
        return s;
    }

    private TextButton.TextButtonStyle mkTextStyle(String base, BitmapFont f) {
        TextButton.TextButtonStyle st = new TextButton.TextButtonStyle();
        st.up   = buttonSkin.getDrawable(base + "_over");
        st.over = buttonSkin.getDrawable(base + "_up");
        st.down = buttonSkin.getDrawable(base + "_down");
        st.font = f;
        return st;
    }

    private void buildSettingsInnerContent() {
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = sliderBgDrawable;
        sliderStyle.knob = sliderSkin.getDrawable("knob");

        ImageButton.ImageButtonStyle musicOn  = mkBtnStyle(iconSkinActive, iconSkinInactive, "row-8-column-8","row-8-column-7","row-8-column-9");
        ImageButton.ImageButtonStyle musicOff = mkBtnStyle(iconSkinInactive, iconSkinActive, "row-8-column-9","row-8-column-7","row-8-column-8");
        ImageButton.ImageButtonStyle soundOn  = mkBtnStyle(iconSkinActive, iconSkinInactive, "row-7-column-8","row-7-column-7","row-7-column-9");
        ImageButton.ImageButtonStyle soundOff = mkBtnStyle(iconSkinInactive, iconSkinActive, "row-7-column-9","row-7-column-7","row-7-column-8");

        boolean musicEnabled = prefs.getBoolean("musicEnabled", game.musicEnabled);
        boolean soundEnabled = prefs.getBoolean("soundEnabled", game.soundEnabled);
        float mVol = prefs.getFloat("musicVolume", game.musicVolume);
        float sVol = prefs.getFloat("soundVolume", game.soundVolume);
        game.musicEnabled = musicEnabled;
        game.soundEnabled = soundEnabled;
        game.musicVolume  = mVol;
        game.soundVolume  = sVol;

        musicBtn = new ImageButton(game.musicEnabled ? musicOn : musicOff);
        musicBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.musicEnabled = !game.musicEnabled;
                musicBtn.setStyle(game.musicEnabled ? musicOn : musicOff);
                applyMusicNow();
            }
        });

        soundBtn = new ImageButton(game.soundEnabled ? soundOn : soundOff);
        soundBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.soundEnabled = !game.soundEnabled;
                soundBtn.setStyle(game.soundEnabled ? soundOn : soundOff);
            }
        });

        musicSlider = new Slider(0f,1f,0.01f,false, sliderStyle);
        soundSlider = new Slider(0f,1f,0.01f,false, sliderStyle);
        musicSlider.setValue(game.musicVolume);
        soundSlider.setValue(game.soundVolume);
        applyMusicNow();

        musicSlider.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            game.musicVolume = musicSlider.getValue(); applyMusicNow();
        }});
        soundSlider.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            game.soundVolume = soundSlider.getValue();
        }});

        settingsContent.clear();
        settingsContent.defaults().left();

        BitmapFont f = titleFont;
        settingsContent.add(new Label("Music", new Label.LabelStyle(f, Color.WHITE))).padRight(12);
        settingsContent.add(musicBtn).size(48,48).padRight(12);
        settingsContent.add(musicSlider).width(320);
        settingsContent.row().padTop(16);

        settingsContent.add(new Label("Sound", new Label.LabelStyle(f, Color.WHITE))).padRight(12);
        settingsContent.add(soundBtn).size(48,48).padRight(12);
        settingsContent.add(soundSlider).width(320);
        settingsContent.row().padTop(14);

        TextButton.TextButtonStyle saveStyle = mkTextStyle("SAVE", f);
        TextButton.TextButtonStyle backStyle = mkTextStyle("BACK", f);

        Drawable measure = buttonSkin.getDrawable("START_up");
        float bw = measure.getMinWidth() * 1.2f;
        float bh = measure.getMinHeight() * 1.2f;

        saveBtn = new TextButton(" ", saveStyle);
        backBtn = new TextButton(" ", backStyle);

        saveBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) {
            saveSettings();
            game.playSound(game.assets.gameClickSound);
            hideSettingsOverlay();
        }});
        backBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) {
            game.playSound(game.assets.gameClickSound);
            onBackNotSave();
        }});

        Table btnRow = new Table();
        btnRow.add(saveBtn).size(bw, bh).padRight(12);
        btnRow.add(backBtn).size(bw, bh);

        settingsContent.add(btnRow).colspan(3).center().padTop(24);
    }

    private void applyMusicNow() {
        if (game.assets.themeMusic == null) return;
        game.assets.themeMusic.setLooping(true);
        game.assets.themeMusic.setVolume(game.musicEnabled ? game.musicVolume : 0f);
        if (game.musicEnabled) {
            if (!game.assets.themeMusic.isPlaying()) game.assets.themeMusic.play();
        } else if (game.assets.themeMusic.isPlaying()) {
            game.assets.themeMusic.pause();
        }
    }

    private void saveSettings() {
        prefs.putBoolean("musicEnabled", game.musicEnabled);
        prefs.putBoolean("soundEnabled", game.soundEnabled);
        prefs.putFloat("musicVolume", game.musicVolume);
        prefs.putFloat("soundVolume", game.soundVolume);
        prefs.flush();
        applyMusicNow();
    }

    private void setOverlayVisible(boolean v) {
        settingsShown = v;
        overlayRoot.setVisible(v);
        frameStack.setVisible(v);
    }

    private void toggleSettingsOverlay() { if (settingsShown) hideSettingsOverlay(); else showSettingsOverlay(); }

    private void showSettingsOverlay() {
        prevMusicEnabled = game.musicEnabled;
        prevSoundEnabled = game.soundEnabled;
        prevMusicVolume  = game.musicVolume;
        prevSoundVolume  = game.soundVolume;
        setOverlayVisible(true);
    }

    private void onBackNotSave() {
        game.musicEnabled = prevMusicEnabled;
        game.soundEnabled = prevSoundEnabled;
        game.musicVolume  = prevMusicVolume;
        game.soundVolume  = prevSoundVolume;
        applyMusicNow();
        hideSettingsOverlay();
    }

    private void hideSettingsOverlay() { setOverlayVisible(false); }

    private Table baseDialog(String text, float scale) {
        Table dialog = new Table();
        dialog.setBackground(frameDrawable);
        dialog.pad(16f);
        Label msg = new Label(text, new Label.LabelStyle(titleFont, Color.WHITE));
        msg.setFontScale(scale);
        msg.setAlignment(Align.center);
        msg.setWrap(true);
        Table wrap = new Table();
        wrap.add(msg).width(520f - 48f).center();
        dialog.add(wrap).expand().fill().row();
        return dialog;
    }

    private void centerDialog(Table dialog, float w, float h) {
        dialog.setSize(w, h);
        dialog.setPosition(
            overlayStage.getViewport().getWorldWidth()/2f - w/2f,
            overlayStage.getViewport().getWorldHeight()/2f - h/2f
        );
    }

    private TextButton.TextButtonStyle mkTextStyle(String base, float scale) {
        TextButton.TextButtonStyle st = new TextButton.TextButtonStyle();
        st.up   = buttonSkin.getDrawable(base + "_over");
        st.over = buttonSkin.getDrawable(base + "_up");
        st.down = buttonSkin.getDrawable(base + "_down");
        st.font = titleFont;
        return st;
    }

    private void sizeDialogButton(TextButton btn) {
        Drawable measure = buttonSkin.getDrawable("START_up");
        float w = measure.getMinWidth() * 1.35f;
        float h = measure.getMinHeight() * 1.35f;
        btn.setSize(w, h);
    }

    private void showVictoryDialog() {
        final float W = 520f, H = 220f;
        Table dialog = baseDialog("VICTORY!\nYOU CLEARED ALL WAVES", 0.9f);
        TextButton ok = new TextButton(" ", mkTextStyle("SAVE", 1f));
        sizeDialogButton(ok);
        Table btn = new Table(); btn.add(ok);
        dialog.add(btn).padTop(14);
        centerDialog(dialog, W, H);
        overlayStage.addActor(dialog);
        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
                world.clearVictory();
                exitToSelectLevel();
            }
        });
    }

    private void showGameOverDialog() {
        final float W = 520f, H = 220f;
        Table dialog = baseDialog("DEFEAT!\nTRY AGAIN?", 0.9f);
        TextButton retry = new TextButton(" ", mkTextStyle("SAVE", 1f));
        TextButton exit  = new TextButton(" ", mkTextStyle("BACK", 1f));
        sizeDialogButton(retry);
        sizeDialogButton(exit);
        Table btns = new Table();
        btns.add(retry).padRight(16);
        btns.add(exit);
        dialog.add(btns).padTop(18);
        centerDialog(dialog, W, H);
        overlayStage.addActor(dialog);
        retry.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
                world.reset();
                victoryShown = false;
                gameOverShown = false;
            }
        });
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
                exitToSelectLevel();
            }
        });
    }

    private void showExitToSelectConfirm() {
        final float W = 520f, H = 220f;
        Table dialog = baseDialog("EXIT TO LEVEL SELECT?\nPROGRESS WILL BE SAVED", 0.8f);
        TextButton ok = new TextButton(" ", mkTextStyle("SAVE", 1f));
        TextButton cancel = new TextButton(" ", mkTextStyle("BACK", 1f));
        sizeDialogButton(ok);
        sizeDialogButton(cancel);
        Table row = new Table();
        row.add(ok).padRight(16);
        row.add(cancel);
        dialog.add(row).padTop(24);
        centerDialog(dialog, W, H);
        overlayStage.addActor(dialog);

        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int next = computeNextWaveForSave();
                SaveManager.saveCheckpoint(level, world, next);
                dialog.remove();
                exitToSelectLevel();
            }
        });
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { dialog.remove(); }
        });
    }

    private void exitToSelectLevel() {
        setOverlayVisible(false);
        game.setScreen(new SelectLevelScreen(game));
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        camera.position.set(MAP_WIDTH_PX / 2f, MAP_HEIGHT_PX / 2f, 0);
        camera.update();
        hud.getStage().getViewport().update(w, h, true);
        overlayStage.getViewport().update(w, h, true);
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause()  { gamePaused = true; }
    @Override public void resume() { gamePaused = false; }

    @Override
    public void dispose() {
        hud.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();
        for (TowerVisual v : towerVisuals.values()) v.dispose();
        towerVisuals.clear();
        for (EnemyVisual v : enemyVisuals.values()) v.dispose();
        enemyVisuals.clear();
        for (EnemyVisual v : dyingEnemyVisuals.values()) v.dispose();
        dyingEnemyVisuals.clear();
        for (AllyUnitVisual v : allyVisuals.values()) v.dispose();
        allyVisuals.clear();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (overlayStage != null) overlayStage.dispose();
        if (world != null && world.abilityManager != null) world.abilityManager.dispose();
        if (abilityHud != null) abilityHud.dispose();
    }
}
