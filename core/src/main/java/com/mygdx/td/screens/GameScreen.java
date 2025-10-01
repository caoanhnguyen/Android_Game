package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.mygdx.td.entities.Bullet;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.render.OrderedOrthogonalTiledMapRenderer;
import com.mygdx.td.save.GameState;
import com.mygdx.td.save.SaveManager;
import com.mygdx.td.ui.UIHud;
import com.mygdx.td.world.TowerSpot;
import com.mygdx.td.world.World;
import com.mygdx.td.screens.SelectLevelScreen;

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

    private final ObjectMap<Enemy, EnemyVisual> enemyVisuals = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyVisual> dyingEnemyVisuals = new ObjectMap<>();
    private final Set<Enemy> rewardedEnemies = new HashSet<>();

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
    private static final int    E_FRAMES_WALK  = -1;
    private static final float  E_WALK_FRAME_SEC = 0.12f;
    private static final int    E_FRAMES_DEATH = -1;
    private static final float  E_DEATH_FRAME_SEC = 0.10f;
    private static final float  ENEMY_TILE_SIZE = 64f;
    private static final boolean ENEMY_STRIP_FACES_RIGHT = false;

    private static final float HP_BAR_WIDTH = 40f;
    private static final float HP_BAR_HEIGHT = 6f;
    private static final float HP_BAR_Y_OFFSET = 38f;

    // ========= In-Game Settings Overlay =========
    private Stage overlayStage;
    private boolean settingsShown = false;

    private Table overlayRoot;
    private Image dimBg;
    private Stack frameStack;
    private Table settingsContent;
    private Slider musicSlider, soundSlider;
    private ImageButton musicBtn, soundBtn;

    private Skin buttonSkin;
    private TextButton saveBtn, backBtn;

    private NinePatchDrawable frameDrawable;
    private NinePatchDrawable sliderBgDrawable;
    private Skin iconSkinActive, iconSkinInactive, sliderSkin;
    private BitmapFont titleFont; // font_title.fnt
    private Preferences prefs;

    private boolean prevMusicEnabled, prevSoundEnabled;
    private float prevMusicVolume, prevSoundVolume;

    private final int level;

    private boolean victoryShown = false;

    public GameScreen(TDGame game) { this(game, 1); }

    public GameScreen(TDGame game, int level) {
        this.game = game;
        this.level = level;
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

        // Gắn các callback HUD
        hud.onStartWave = () -> {
            if (world.waveManager != null && !world.waveManager.isInWave() && !world.gameOver) {
                world.waveManager.startNextWave();
                Gdx.app.log("GAME", "Start wave " + world.waveManager.getCurrentWave());
            }
        };
        hud.onPauseToggle = () -> {
            gamePaused = hud.isPaused();
            if (!gamePaused && world.waveManager != null && !world.waveManager.isInWave() && !world.gameOver) {
                world.waveManager.startNextWave();
            }
        };
        hud.onOpenSettings = this::toggleSettingsOverlay;
        hud.onExitRequested = this::showExitToSelectConfirm; // bấm X -> confirm

        gamePaused = hud.isPaused();

        overlayStage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT), game.batch);
        buildSettingsOverlay();

        // Resume checkpoint nếu có
        attemptResumeFromCheckpoint();

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(overlayStage);
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

    private void attemptResumeFromCheckpoint() {
        GameState s = SaveManager.loadCheckpoint();
        if (s == null) return;
        if (s.level != this.level) return;
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

    private void syncEnemyVisuals() {
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
        Set<Enemy> toMove = new HashSet<>();
        for (Enemy e : enemyVisuals.keys()) {
            if (!world.enemies.contains(e, true)) toMove.add(e);
        }
        for (Enemy e : toMove) {
            EnemyVisual v = enemyVisuals.remove(e);
            if (v != null) {
                if (v.isReadyToRemove(e)) {
                    v.dispose();
                } else {
                    dyingEnemyVisuals.put(e, v);
                }
            }
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

    @Override
    public void render(float delta) {
        if (!gamePaused) {
            world.update(delta);

            // Victory dialog
            if (world.isVictory() && !victoryShown) {
                victoryShown = true;
                SaveManager.clearCheckpoint(); // clear checkpoint khi thắng
                showVictoryDialog();
            }

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

        for (Enemy e : world.enemies) {
            EnemyVisual v = enemyVisuals.get(e);
            if (v != null) v.draw(game.batch, e);
        }
        for (ObjectMap.Entry<Enemy, EnemyVisual> entry : dyingEnemyVisuals.entries()) {
            entry.value.draw(game.batch, entry.key);
        }
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
        for (Bullet b : world.bullets) {
            game.batch.draw(game.assets.bulletTex, b.pos.x - 8, b.pos.y - 8, 16, 16);
        }

        game.batch.end();

        hud.act(delta);
        hud.updateHudValues(world.waveManager.isInWave());
        hud.draw();

        overlayStage.act(delta);
        overlayStage.draw();
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

        if (settingsShown) return false;

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

    // ================= In-Game Settings Overlay =================

    private void buildSettingsOverlay() {
        prefs = Gdx.app.getPreferences("td_settings");

        // Font chính: font_title.fnt
        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        NinePatch framePatch = new NinePatch(new Texture(Gdx.files.internal("ui/banner_11.9.png")), 16, 16, 16, 16);
        frameDrawable = new NinePatchDrawable(framePatch);

        NinePatch sliderBgPatch = new NinePatch(new Texture(Gdx.files.internal("ui/bg_slider.9.png")), 8, 8, 8, 8);
        sliderBgDrawable = new NinePatchDrawable(sliderBgPatch);

        TextureAtlas sliderAtlas = new TextureAtlas(Gdx.files.internal("ui/knob.atlas"));
        sliderSkin = new Skin(sliderAtlas);

        TextureAtlas activeIconAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
        TextureAtlas inActiveIconAtlas = new TextureAtlas(Gdx.files.internal("ui/metal_buttons_icon.atlas"));
        for (Texture tex : activeIconAtlas.getTextures()) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (Texture tex : inActiveIconAtlas.getTextures()) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconSkinActive = new Skin(activeIconAtlas);
        iconSkinInactive = new Skin(inActiveIconAtlas);

        TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
        for (Texture t : buttonAtlas.getTextures()) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        buttonSkin = new Skin(buttonAtlas);

        overlayRoot = new Table();
        overlayRoot.setFillParent(true);
        overlayStage.addActor(overlayRoot);

        dimBg = new Image(game.assets.whitePixel);
        dimBg.setColor(0, 0, 0, 0.55f);
        dimBg.setFillParent(true);
        dimBg.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onBackNotSave(); }
        });

        settingsContent = new Table();
        settingsContent.pad(24f);
        settingsContent.align(Align.topLeft);

        Image frameImg = new Image(frameDrawable);
        frameImg.setScaling(Scaling.stretch);
        frameImg.setColor(1, 1, 1, 0.93f);

        frameStack = new Stack();
        frameStack.add(frameImg);
        frameStack.add(settingsContent);

        buildSettingsInnerContent();

        Table center = new Table();
        center.setFillParent(true);
        overlayStage.addActor(center);

        float frameWidth = 560f;
        float frameHeight = 230f;
        overlayRoot.add(dimBg).grow();
        center.add(frameStack).width(frameWidth).height(frameHeight).center();

        setOverlayVisible(false);
    }

    private void buildSettingsInnerContent() {
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = sliderBgDrawable;
        sliderStyle.knob = sliderSkin.getDrawable("knob");

        ImageButton.ImageButtonStyle musicToggleOnStyle = new ImageButton.ImageButtonStyle();
        musicToggleOnStyle.up = iconSkinActive.getDrawable("row-8-column-8");
        musicToggleOnStyle.over = iconSkinInactive.getDrawable("row-8-column-7");
        musicToggleOnStyle.down = iconSkinInactive.getDrawable("row-8-column-9");

        ImageButton.ImageButtonStyle musicToggleOffStyle = new ImageButton.ImageButtonStyle();
        musicToggleOffStyle.up = iconSkinInactive.getDrawable("row-8-column-9");
        musicToggleOffStyle.over = iconSkinActive.getDrawable("row-8-column-7");
        musicToggleOffStyle.down = iconSkinActive.getDrawable("row-8-column-8");

        ImageButton.ImageButtonStyle soundToggleOnStyle = new ImageButton.ImageButtonStyle();
        soundToggleOnStyle.up = iconSkinActive.getDrawable("row-7-column-8");
        soundToggleOnStyle.over = iconSkinInactive.getDrawable("row-7-column-7");
        soundToggleOnStyle.down = iconSkinInactive.getDrawable("row-7-column-9");

        ImageButton.ImageButtonStyle soundToggleOffStyle = new ImageButton.ImageButtonStyle();
        soundToggleOffStyle.up = iconSkinInactive.getDrawable("row-7-column-9");
        soundToggleOffStyle.over = iconSkinActive.getDrawable("row-7-column-7");
        soundToggleOffStyle.down = iconSkinActive.getDrawable("row-7-column-8");

        BitmapFont mainFont = titleFont;

        musicBtn = new ImageButton(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
        musicBtn.setChecked(!game.musicEnabled);
        musicBtn.getImage().setScaling(Scaling.stretch);
        musicBtn.getImage().setSize(48, 48);
        musicBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.musicEnabled = !game.musicEnabled;
                musicBtn.setStyle(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
                applyMusicNow();
            }
        });

        soundBtn = new ImageButton(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
        soundBtn.setChecked(!game.soundEnabled);
        soundBtn.getImage().setScaling(Scaling.stretch);
        soundBtn.getImage().setSize(48, 48);
        soundBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.soundEnabled = !game.soundEnabled;
                soundBtn.setStyle(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
            }
        });

        musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        soundSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);

        float mVol = prefs.getFloat("musicVolume", game.musicVolume);
        float sVol = prefs.getFloat("soundVolume", game.soundVolume);
        game.musicVolume = mVol;
        game.soundVolume = sVol;
        musicSlider.setValue(mVol);
        soundSlider.setValue(sVol);
        applyMusicNow();

        musicSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.musicVolume = musicSlider.getValue();
                applyMusicNow();
            }
        });
        soundSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.soundVolume = soundSlider.getValue();
            }
        });

        settingsContent.clear();
        settingsContent.defaults().left();

        settingsContent.add(new Label("Music", new Label.LabelStyle(mainFont, com.badlogic.gdx.graphics.Color.WHITE))).padRight(12);
        settingsContent.add(musicBtn).size(48, 48).padRight(12);
        settingsContent.add(musicSlider).width(320).height(48).padLeft(8).padRight(8);
        settingsContent.row().padTop(16);

        settingsContent.add(new Label("Sound", new Label.LabelStyle(mainFont, com.badlogic.gdx.graphics.Color.WHITE))).padRight(12);
        settingsContent.add(soundBtn).size(48, 48).padRight(12);
        settingsContent.add(soundSlider).width(320).height(48).padLeft(8).padRight(8);
        settingsContent.row().padTop(14);

        // ===================== NÚT SAVE/BACK =====================
        TextButton.TextButtonStyle saveStyle = new TextButton.TextButtonStyle();
        saveStyle.up   = buttonSkin.getDrawable("SAVE_over");
        saveStyle.over = buttonSkin.getDrawable("SAVE_up");
        saveStyle.down = buttonSkin.getDrawable("SAVE_down");
        saveStyle.font = mainFont;

        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.up   = buttonSkin.getDrawable("BACK_over");
        backStyle.over = buttonSkin.getDrawable("BACK_up");
        backStyle.down = buttonSkin.getDrawable("BACK_down");
        backStyle.font = mainFont;

        Drawable measure = buttonSkin.getDrawable("START_up");
        float btnWidth = measure.getMinWidth() * 1.2f;
        float btnHeight = measure.getMinHeight() * 1.2f;

        saveBtn = new TextButton(" ", saveStyle);
        backBtn = new TextButton(" ", backStyle);

        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                saveSettings();
                game.playSound(game.assets.gameClickSound);
                hideSettingsOverlay();
            }
        });
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                onBackNotSave();
            }
        });

        Table buttonTable = new Table();
        buttonTable.center();
        buttonTable.add(saveBtn).size(btnWidth, btnHeight).padRight(12);
        buttonTable.add(backBtn).size(btnWidth, btnHeight);

        settingsContent.add(buttonTable).colspan(3).center().padTop(24);
        settingsContent.row();
    }

    private void applyMusicNow() {
        if (game.assets.themeMusic != null) {
            game.assets.themeMusic.setLooping(true);
            game.assets.themeMusic.setVolume(game.musicEnabled ? game.musicVolume : 0f);
            if (game.musicEnabled) {
                if (!game.assets.themeMusic.isPlaying()) game.assets.themeMusic.play();
            } else {
                if (game.assets.themeMusic.isPlaying()) game.assets.themeMusic.pause();
            }
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

    private void setOverlayVisible(boolean visible) {
        settingsShown = visible;
        overlayRoot.setVisible(visible);
        frameStack.setVisible(visible);
        if (visible) {
            overlayStage.setKeyboardFocus(frameStack);
            overlayStage.setScrollFocus(frameStack);
        } else {
            overlayStage.setKeyboardFocus(null);
            overlayStage.setScrollFocus(null);
        }
    }

    private void toggleSettingsOverlay() {
        if (settingsShown) hideSettingsOverlay();
        else showSettingsOverlay();
    }

    private void showSettingsOverlay() {
        prevMusicEnabled = game.musicEnabled;
        prevSoundEnabled = game.soundEnabled;
        prevMusicVolume  = game.musicVolume;
        prevSoundVolume  = game.soundVolume;

        musicBtn.setChecked(!game.musicEnabled);
        soundBtn.setChecked(!game.soundEnabled);
        musicSlider.setValue(game.musicVolume);
        soundSlider.setValue(game.soundVolume);
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

    // ========= Exit confirm & Victory =========

    private void showVictoryDialog() {
        final float DIALOG_W = 520f, DIALOG_H = 220f;
        final Table dialog = new Table();
        dialog.setBackground(frameDrawable);
        dialog.pad(16f);
        dialog.setSize(DIALOG_W, DIALOG_H);

        Label.LabelStyle ls = new Label.LabelStyle(titleFont, com.badlogic.gdx.graphics.Color.WHITE);
        Label msg = new Label("VICTORY!\nYOU CLEARED ALL 20 WAVES", ls);
        msg.setAlignment(Align.center);
        msg.setFontScale(0.9f);
        msg.setWrap(true);

        Table msgWrap = new Table();
        msgWrap.add(msg).width(DIALOG_W - 48f).center();

        TextButton.TextButtonStyle okStyle = new TextButton.TextButtonStyle();
        okStyle.up   = buttonSkin.getDrawable("SAVE_over");
        okStyle.over = buttonSkin.getDrawable("SAVE_up");
        okStyle.down = buttonSkin.getDrawable("SAVE_down");
        okStyle.font = titleFont;

        TextButton ok = new TextButton("", okStyle);

        Drawable measure = buttonSkin.getDrawable("START_up");
        float w = measure.getMinWidth() * 1.35f;
        float h = measure.getMinHeight() * 1.35f;

        Table btns = new Table();
        btns.add(ok).size(w, h);

        dialog.add(msgWrap).expand().fill().center().row();
        dialog.add(btns).center().padTop(12);

        dialog.setPosition(
            overlayStage.getViewport().getWorldWidth() / 2f - dialog.getWidth() / 2f,
            overlayStage.getViewport().getWorldHeight() / 2f - dialog.getHeight() / 2f
        );

        overlayStage.addActor(dialog);

        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
                world.clearVictory();
                exitToSelectLevel();
            }
        });
    }

    private void showExitToSelectConfirm() {
        final float DIALOG_W = 520f, DIALOG_H = 220f;
        final Table dialog = new Table();
        dialog.setBackground(frameDrawable);
        dialog.pad(16f);
        dialog.setSize(DIALOG_W, DIALOG_H);

        Label.LabelStyle ls = new Label.LabelStyle(titleFont, com.badlogic.gdx.graphics.Color.WHITE);
        Label msg = new Label("EXIT TO LEVEL SELECT?\nPROGRESS WILL BE SAVED AS A CHECKPOINT", ls);
        msg.setAlignment(Align.center);
        msg.setFontScale(0.8f);
        msg.setWrap(true);

        Table msgWrap = new Table();
        msgWrap.add(msg).width(DIALOG_W - 48f).center();

        TextButton.TextButtonStyle okStyle = new TextButton.TextButtonStyle();
        okStyle.up   = buttonSkin.getDrawable("SAVE_over");
        okStyle.over = buttonSkin.getDrawable("SAVE_up");
        okStyle.down = buttonSkin.getDrawable("SAVE_down");
        okStyle.font = titleFont;

        TextButton.TextButtonStyle cancelStyle = new TextButton.TextButtonStyle();
        cancelStyle.up   = buttonSkin.getDrawable("BACK_over");
        cancelStyle.over = buttonSkin.getDrawable("BACK_up");
        cancelStyle.down = buttonSkin.getDrawable("BACK_down");
        cancelStyle.font = titleFont;

        TextButton ok = new TextButton("", okStyle);
        TextButton cancel = new TextButton("", cancelStyle);

        Drawable measure = buttonSkin.getDrawable("START_up");
        float w = measure.getMinWidth() * 1.35f;
        float h = measure.getMinHeight() * 1.35f;

        Table btns = new Table();
        btns.add(ok).size(w, h).padRight(16);
        btns.add(cancel).size(w, h);

        dialog.add(msgWrap).expand().fill().center().row();
        dialog.add(btns).center().padTop(24);

        dialog.setPosition(
            overlayStage.getViewport().getWorldWidth() / 2f - dialog.getWidth() / 2f,
            overlayStage.getViewport().getWorldHeight() / 2f - dialog.getHeight() / 2f
        );

        overlayStage.addActor(dialog);

        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int nextWave = computeNextWaveForSave();
                SaveManager.saveCheckpoint(level, world, nextWave);
                dialog.remove();
                exitToSelectLevel();
            }
        });
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
            }
        });
    }

    private void exitToSelectLevel() {
        setOverlayVisible(false);
        game.setScreen(new SelectLevelScreen(game));
    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, true);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();
        if (hud != null) hud.getStage().getViewport().update(w, h, true);
        if (overlayStage != null) overlayStage.getViewport().update(w, h, true);
    }
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

        if (overlayStage != null) {
            overlayStage.dispose();
            overlayStage = null;
        }
    }
}
