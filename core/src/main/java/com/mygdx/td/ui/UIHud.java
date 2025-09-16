package com.mygdx.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * HUD góc trên trái (heart, gold, wave) + góc trên phải (pause/play toggle, settings).
 * Chữ "WAVE" dùng font_title.fnt như yêu cầu.
 */
public class UIHud {

    private final TDGame game;
    private final Object world;
    private final Stage stage;

    // Assets
    private Texture iconCashTex, iconHeartTex;
    private BitmapFont font, waveTitleFont;
    private NinePatch bannerPatch;
    private Skin iconSkin;

    // UI nodes
    private Label goldLabel, heartLabel, waveLabel;

    // State
    private boolean paused = false;
    public Runnable onPauseToggle;
    public Runnable onStartWave;
    public Runnable onOpenSettings;

    // Kích thước
    private static final float INFO_W = 360f;
    private static final float INFO_H = 56f;
    private static final float ICON_SIZE = 24f;
    private static final float TOP_PAD = 6f;
    private static final float SIDE_PAD = 12f;
    private static final float RIGHT_BTN_SIZE = 44f;

    public UIHud(TDGame game, Object world) {
        this.game = game;
        this.world = world;
        this.stage = new Stage(new FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT));

        loadAssets();
        buildUI();
    }

    private void loadAssets() {
        font = (game.assets != null && game.assets.fontMedium != null) ? game.assets.fontMedium : new BitmapFont();
        try {
            waveTitleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            waveTitleFont.setUseIntegerPositions(false);
        } catch (Throwable t) {
            waveTitleFont = font;
        }

        try {
            Texture bannerTex = new Texture(Gdx.files.internal("ui/banner_11.9.png"));
            bannerTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            bannerPatch = new NinePatch(bannerTex, 16, 16, 16, 16);
        } catch (Throwable t) {
            bannerPatch = null;
        }

        try {
            iconCashTex = new Texture(Gdx.files.internal("ui/icon_cash.png"));
            iconCashTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } catch (Throwable t) {
            iconCashTex = null;
        }
        try {
            iconHeartTex = new Texture(Gdx.files.internal("ui/icon_health.png"));
            iconHeartTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } catch (Throwable t) {
            iconHeartTex = null;
        }

        try {
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
            for (Texture tex : atlas.getTextures()) {
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
            iconSkin = new Skin(atlas);
        } catch (Throwable t) {
            iconSkin = null;
        }
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(TOP_PAD).padLeft(SIDE_PAD).padRight(SIDE_PAD);
        stage.addActor(root);

        // ===== Left top: Info (banner + heart/gold/wave) =====
        Stack infoStack = new Stack();
        infoStack.setSize(INFO_W, INFO_H);

        if (bannerPatch != null) {
            Image bg = new Image(new NinePatchDrawable(bannerPatch));
            bg.setScaling(Scaling.stretch);
            bg.setColor(1, 1, 1, 0.96f);
            infoStack.add(bg);
        }

        Table infoTable = new Table();
        infoTable.defaults().center();
        infoTable.padLeft(24f).padRight(12f); // Sửa: padLeft tăng để cân giữa, padRight giảm

        // Heart
        if (iconHeartTex != null) {
            Image heartIcon = new Image(iconHeartTex);
            heartIcon.setScaling(Scaling.fit);
            infoTable.add(heartIcon).size(ICON_SIZE, ICON_SIZE).padRight(6f);
        }
        heartLabel = new Label("0", new Label.LabelStyle(font, Color.WHITE));
        heartLabel.setAlignment(Align.left);
        heartLabel.setFontScale(1.0f);
        infoTable.add(heartLabel).minWidth(40).left().padRight(10f);

        // Gold
        if (iconCashTex != null) {
            Image cashIcon = new Image(iconCashTex);
            cashIcon.setScaling(Scaling.fit);
            infoTable.add(cashIcon).size(ICON_SIZE, ICON_SIZE).padRight(6f);
        }
        goldLabel = new Label("0", new Label.LabelStyle(font, Color.valueOf("ffe96b")));
        goldLabel.setAlignment(Align.left);
        goldLabel.setFontScale(1.0f);
        infoTable.add(goldLabel).minWidth(60).left().padRight(10f);

        // ======= Thêm chữ "WAVE" dùng font title nhỏ lại =======
        Label waveTitle = new Label("WAVE", new Label.LabelStyle(waveTitleFont, new Color(1f, 0.90f, 0.10f, 1f)));
        waveTitle.setAlignment(Align.center);
        waveTitle.setFontScale(0.65f); // nhỏ lại
        infoTable.add(waveTitle).minWidth(66).padRight(24f); // cách xa số wave hơn

        // Wave số
        waveLabel = new Label("0/0", new Label.LabelStyle(font, Color.WHITE));
        waveLabel.setAlignment(Align.left);
        waveLabel.setFontScale(1.0f);
        infoTable.add(waveLabel).minWidth(54).left();

        infoStack.add(infoTable);

        // ===== Right top: Buttons (Pause/Play toggle, Settings) =====
        Table controls = new Table();
        controls.right().top();

        // --- Pause/Play toggle button ---
        ImageButton pausePlayBtn = createPausePlayButton();
        controls.add(pausePlayBtn).size(RIGHT_BTN_SIZE, RIGHT_BTN_SIZE).padLeft(6f);

        // --- Settings button giống mainmenu ---
        ImageButton setBtn = createSettingsButton();
        controls.add(setBtn).size(RIGHT_BTN_SIZE, RIGHT_BTN_SIZE).padLeft(6f);

        // Compose top bar
        root.add(infoStack).width(INFO_W).height(INFO_H).center(); // Sửa: căn giữa infoStack trong banner
        root.add().expandX(); // spacer
        root.add(controls).right().padRight(4f);
        root.row();
    }

    private ImageButton createPausePlayButton() {
        if (iconSkin == null) return new ImageButton(new Image().getDrawable());
        Drawable pauseDrawable = safeDrawable("row-9-column-4"); // icon pause
        Drawable playDrawable = safeDrawable("row-4-column-4");  // icon play

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = paused ? playDrawable : pauseDrawable;
        style.over = style.up;
        style.down = style.up;

        final ImageButton btn = new ImageButton(style);

        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                paused = !paused;
                style.up = paused ? playDrawable : pauseDrawable;
                style.over = style.up;
                style.down = style.up;
                btn.setStyle(style);

                if (onPauseToggle != null) onPauseToggle.run();
                if (!paused && onStartWave != null) onStartWave.run();
            }
        });
        return btn;
    }

    private ImageButton createSettingsButton() {
        if (iconSkin == null) return new ImageButton(new Image().getDrawable());
        Drawable up = safeDrawable("row-2-column-10");
        Drawable over = safeDrawable("row-2-column-11");
        Drawable down = safeDrawable("row-2-column-12");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = up;
        style.over = (over != null) ? over : up;
        style.down = (down != null) ? down : up;

        final ImageButton btn = new ImageButton(style);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (onOpenSettings != null) onOpenSettings.run();
            }
        });
        return btn;
    }

    private Drawable safeDrawable(String name) {
        try { return iconSkin.getDrawable(name); }
        catch (Throwable t) { return null; }
    }

    public void act(float delta) {
        stage.act(delta);
    }

    public void draw() {
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    // Cập nhật HUD
    public void updateHudValues(boolean inWave) {
        int gold = tryReadInt(world, "gold", "coins", "money");
        int hearts = tryReadInt(world, "hearts", "lives", "hp", "health");

        Object waveMgr = tryReadObject(world, "waveManager");
        int curWave = readWaveCurrent(waveMgr);
        int totalWave = readWaveTotal(waveMgr);

        if (goldLabel != null) goldLabel.setText(String.valueOf(gold));
        if (heartLabel != null) heartLabel.setText(String.valueOf(hearts));
        if (waveLabel != null) {
            if (totalWave > 0) waveLabel.setText(Math.max(0, curWave) + "/" + totalWave);
            else waveLabel.setText(String.valueOf(Math.max(0, curWave)));
        }
    }

    // ===== Reflection helpers =====
    private int tryReadInt(Object obj, String... names) {
        if (obj == null) return 0;
        for (String n : names) {
            try {
                Field f = obj.getClass().getField(n);
                f.setAccessible(true);
                return f.getInt(obj);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private Object tryReadObject(Object obj, String name) {
        if (obj == null) return null;
        try {
            Field f = obj.getClass().getField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int readWaveCurrent(Object mgr) {
        if (mgr == null) return 0;
        Integer byMethod = tryInvokeIntObj(mgr, "getCurrentWave", "current", "getWave", "getIndex");
        if (byMethod != null) return byMethod;
        int byField = tryReadInt(mgr, "currentWave", "waveIndex", "wave");
        return byField;
    }

    private int readWaveTotal(Object mgr) {
        if (mgr == null) return 0;
        Integer byMethod = tryInvokeIntObj(mgr, "getTotalWaves", "getWavesCount", "getWaveCount", "getMaxWave", "size");
        if (byMethod != null) return byMethod;
        int byField = tryReadInt(mgr, "totalWaves", "waves", "maxWave", "count");
        return byField;
    }

    private Integer tryInvokeIntObj(Object obj, String... methods) {
        for (String mname : methods) {
            try {
                Method m = obj.getClass().getMethod(mname);
                Object r = m.invoke(obj);
                if (r instanceof Integer) return (Integer) r;
                if (r instanceof Number) return ((Number) r).intValue();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public boolean isPaused() {
        return paused;
    }

    public void dispose() {
        stage.dispose();
        if (iconCashTex != null) iconCashTex.dispose();
        if (iconHeartTex != null) iconHeartTex.dispose();
        if (bannerPatch != null && bannerPatch.getTexture() != null) bannerPatch.getTexture().dispose();
        if (iconSkin != null) iconSkin.dispose();
        if (waveTitleFont != null && waveTitleFont != font) waveTitleFont.dispose();
    }
}
