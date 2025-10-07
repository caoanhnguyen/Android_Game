package com.mygdx.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.td.Constants;
import com.mygdx.td.TDGame;
import com.mygdx.td.entities.TowerType;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.world.World;

/**
 * HUD hiển thị thông tin + popup chọn trụ & nâng cấp.
 * ĐÃ chỉnh popup nâng cấp hiển thị Cost, Damage, Range (current -> next + delta).
 */
public class UIHud {

    private final TDGame game;
    private final World world;
    private final Stage stage;

    // Assets
    private Texture iconCashTex, iconHeartTex;
    private BitmapFont font, waveTitleFont;
    private NinePatch bannerPatch;
    private Skin iconSkin;
    private TextureAtlas towerIconAtlas;

    // UI nodes
    private Label goldLabel, heartLabel, waveLabel;

    // Nút play/pause
    private ImageButton playPauseBtn;
    private Drawable pauseUp, pauseOver, pauseDown;
    private Drawable playUp, playOver, playDown;

    // State
    private boolean paused = true;
    public Runnable onPauseToggle;
    public Runnable onStartWave;
    public Runnable onOpenSettings;
    public Runnable onExitRequested;

    // Kích thước
    private static final float INFO_W = 430f;
    private static final float INFO_H = 56f;
    private static final float ICON_SIZE = 24f;
    private static final float TOP_PAD = 6f;
    private static final float SIDE_PAD = 12f;
    private static final float RIGHT_BTN_SIZE = 48f;

    // Tower selection
    public interface TowerSelectListener { void onTowerSelected(TowerType selectedType); }
    private TowerType[] towerTypes;
    private TowerSelectListener towerSelectListener;
    private Table towerPopupHUD;
    private Table towerUpgradeHUD;

    private Image towerPopupScrim;
    private Image upgradePopupScrim;

    public void setTowerTypes(TowerType[] types) { this.towerTypes = types; }
    public void setTowerSelectListener(TowerSelectListener listener) { this.towerSelectListener = listener; }

    public UIHud(TDGame game, World world) {
        this.game = game;
        this.world = world;
        this.stage = new Stage(new FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT));
        loadAssets();
        buildUI();
    }

    private void loadAssets() {
        try {
            waveTitleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            waveTitleFont.setUseIntegerPositions(false);
            font = new BitmapFont(Gdx.files.internal("font/font_middle.fnt"));
            font.setUseIntegerPositions(false);
        } catch (Throwable t) {
            waveTitleFont = new BitmapFont();
            waveTitleFont.setUseIntegerPositions(false);
            font = new BitmapFont();
            font.setUseIntegerPositions(false);
            Gdx.app.error("UIHud", "Font load fail: " + t.getMessage());
        }

        try {
            Texture bannerTex = new Texture(Gdx.files.internal("ui/banner_11.9.png"));
            bannerTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            bannerPatch = new NinePatch(bannerTex, 16, 16, 16, 16);
        } catch (Throwable t) { bannerPatch = null; }

        try {
            iconCashTex = new Texture(Gdx.files.internal("ui/icon_cash.png"));
            iconCashTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } catch (Throwable t) { iconCashTex = null; }

        try {
            iconHeartTex = new Texture(Gdx.files.internal("ui/icon_health.png"));
            iconHeartTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } catch (Throwable t) { iconHeartTex = null; }

        try {
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
            for (Texture tex : atlas.getTextures()) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            iconSkin = new Skin(atlas);
        } catch (Throwable t) { iconSkin = null; }

        try {
            towerIconAtlas = new TextureAtlas(Gdx.files.internal("towers/tower_icons.atlas"));
        } catch (Throwable t) {
            towerIconAtlas = null;
            Gdx.app.error("UIHud", "Không load được towers/tower_icons.atlas: " + t.getMessage());
        }
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(TOP_PAD).padLeft(SIDE_PAD).padRight(SIDE_PAD);
        stage.addActor(root);

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
        infoTable.padLeft(12f).padRight(12f);

        if (iconHeartTex != null) {
            Image heartIcon = new Image(iconHeartTex);
            heartIcon.setScaling(Scaling.fit);
            infoTable.add(heartIcon).size(ICON_SIZE, ICON_SIZE).padRight(6f);
        }
        heartLabel = new Label("0", new Label.LabelStyle(font, Color.WHITE));
        heartLabel.setAlignment(Align.left);
        heartLabel.setFontScale(0.65f);
        infoTable.add(heartLabel).minWidth(40).left().padRight(10f);

        if (iconCashTex != null) {
            Image cashIcon = new Image(iconCashTex);
            cashIcon.setScaling(Scaling.fit);
            infoTable.add(cashIcon).size(ICON_SIZE, ICON_SIZE).padRight(6f);
        }
        goldLabel = new Label("0", new Label.LabelStyle(font, Color.valueOf("ffe96b")));
        goldLabel.setAlignment(Align.left);
        goldLabel.setFontScale(0.65f);
        infoTable.add(goldLabel).minWidth(60).left().padRight(10f);

        Label waveTitle = new Label("WAVE", new Label.LabelStyle(waveTitleFont, new Color(1f, 0.90f, 0.10f, 1f)));
        waveTitle.setAlignment(Align.center);
        waveTitle.setFontScale(0.65f);
        infoTable.add(waveTitle).minWidth(66).padRight(24f);

        waveLabel = new Label("0", new Label.LabelStyle(font, Color.WHITE));
        waveLabel.setAlignment(Align.left);
        waveLabel.setFontScale(0.65f);
        infoTable.add(waveLabel).minWidth(54).left();

        infoStack.add(infoTable);

        Table controls = new Table();
        controls.right().top();

        playPauseBtn = createPausePlayButton();
        ImageButton settingsBtn = createSettingsButton();
        ImageButton closeBtn    = createCloseButton();

        controls.add(playPauseBtn).size(RIGHT_BTN_SIZE, RIGHT_BTN_SIZE).padLeft(6f);
        controls.add(settingsBtn).size(RIGHT_BTN_SIZE, RIGHT_BTN_SIZE).padLeft(6f);
        controls.add(closeBtn).size(RIGHT_BTN_SIZE, RIGHT_BTN_SIZE).padLeft(6f);

        root.add(infoStack).width(INFO_W).height(INFO_H).center();
        root.add().expandX();
        root.add(controls).right().padRight(4f);
        root.row();

        refreshPlayPauseIcon();
    }

    private ImageButton createPausePlayButton() {
        if (iconSkin == null) return new ImageButton(new Image().getDrawable());

        pauseUp   = safeDrawable("row-9-column-5");
        pauseDown = safeDrawable("row-9-column-6");
        pauseOver = safeDrawable("row-9-column-4");

        playDown  = safeDrawable("row-4-column-6");
        playUp    = safeDrawable("row-4-column-5");
        playOver  = safeDrawable("row-4-column-4");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = paused ? playUp : pauseUp;
        style.over = paused ? (playOver != null ? playOver : playUp) : (pauseOver != null ? pauseOver : pauseUp);
        style.down = paused ? (playDown != null ? playDown : playUp) : (pauseDown != null ? pauseDown : pauseUp);

        final ImageButton btn = new ImageButton(style);

        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);

                boolean inWave = world != null && world.waveManager.isInWave();

                if (inWave) {
                    paused = !paused;
                    if (onPauseToggle != null) onPauseToggle.run();
                } else {
                    if (paused) {
                        paused = false;
                        if (onPauseToggle != null) onPauseToggle.run();
                    }
                    if (onStartWave != null) onStartWave.run();
                }
                refreshPlayPauseIcon();
            }
        });
        return btn;
    }

    private void refreshPlayPauseIcon() {
        if (playPauseBtn == null) return;
        ImageButton.ImageButtonStyle s = playPauseBtn.getStyle();
        boolean inWave = world != null && world.waveManager.isInWave();

        if (!inWave) {
            s.up = playUp;
            s.over = (playOver != null ? playOver : playUp);
            s.down = (playDown != null ? playDown : playUp);
        } else {
            boolean showPlay = paused;
            if (showPlay) {
                s.up = playUp;
                s.over = (playOver != null ? playOver : playUp);
                s.down = (playDown != null ? playDown : playUp);
            } else {
                s.up = pauseUp;
                s.over = (pauseOver != null ? pauseOver : pauseUp);
                s.down = (pauseDown != null ? pauseDown : pauseUp);
            }
        }
        playPauseBtn.setStyle(s);
    }

    private ImageButton createSettingsButton() {
        if (iconSkin == null) return new ImageButton(new Image().getDrawable());
        Drawable up = safeDrawable("row-2-column-11");
        Drawable over = safeDrawable("row-2-column-10");
        Drawable down = safeDrawable("row-2-column-12");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = up;
        style.over = (over != null) ? over : up;
        style.down = (down != null) ? down : up;

        final ImageButton btn = new ImageButton(style);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                if (onOpenSettings != null) onOpenSettings.run();
            }
        });
        return btn;
    }

    private ImageButton createCloseButton() {
        if (iconSkin == null) return new ImageButton(new Image().getDrawable());
        Drawable up = safeDrawable("row-4-column-8");
        Drawable over = safeDrawable("row-4-column-7");
        Drawable down = safeDrawable("row-4-column-9");
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = up;
        style.over = over;
        style.down = down;

        final ImageButton btn = new ImageButton(style);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                if (onExitRequested != null) onExitRequested.run();
            }
        });
        return btn;
    }

    private Drawable safeDrawable(String name) {
        try { return iconSkin.getDrawable(name); }
        catch (Throwable t) { return null; }
    }

    public void act(float delta) { stage.act(delta); }
    public void draw() { stage.draw(); }
    public Stage getStage() { return stage; }

    /* ===================== Tower Selection Popup ===================== */

    private void buildTowerPopupHUD() {
        if (towerPopupHUD != null) towerPopupHUD.remove();
        if (towerTypes == null || towerTypes.length == 0) return;

        float cellW = 110, cellH = 130, n = towerTypes.length;
        float pad = 16, popupW = n * cellW + (n + 1) * pad, popupH = cellH + 42;

        towerPopupHUD = new Table();
        towerPopupHUD.setSize(popupW, popupH);

        float screenW = stage.getViewport().getWorldWidth();
        float padBottom = 32;
        towerPopupHUD.setPosition((screenW - popupW) / 2f, padBottom);

        if (bannerPatch != null)
            towerPopupHUD.setBackground(new NinePatchDrawable(bannerPatch));

        Table row = new Table();
        for (TowerType type : towerTypes) {
            TextureRegion icon = null;
            if (towerIconAtlas != null && type.iconRegion != null) {
                icon = towerIconAtlas.findRegion(type.iconRegion);
            }
            if (icon == null) {
                Gdx.app.error("UIHud", "Không tìm thấy region icon: " + type.iconRegion + " trong towers/tower_icons.atlas");
            }

            Drawable btnDrawable = (icon != null)
                ? new TextureRegionDrawable(icon)
                : (bannerPatch != null ? new NinePatchDrawable(bannerPatch) : new Image().getDrawable());

            ImageButton btn = new ImageButton(btnDrawable);
            btn.getImageCell().size(96, 96);

            // Tên
            Label nameLabel = new Label(type.name, new Label.LabelStyle(font, Color.WHITE));
            nameLabel.setFontScale(0.65f);

            // Cost
            Label costLabel = new Label(String.valueOf(type.cost),
                new Label.LabelStyle(font, Color.valueOf("ffe96b")));
            costLabel.setFontScale(0.6f);

            Table cell = new Table();
            cell.add(btn).size(80, 80).padTop(8).row();
            cell.add(nameLabel).padTop(4).row();
            cell.add(costLabel).padTop(2);

            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playSound(game.assets.gameClickSound);
                    hideTowerPopupHUD();
                    if (towerSelectListener != null) towerSelectListener.onTowerSelected(type);
                }
            });

            row.add(cell).pad(pad, pad, pad, pad);
        }

        towerPopupHUD.add(row).padTop(10).padBottom(16);
    }

    public void showTowerPopupHUD() {
        hideUpgradePopupHUD();

        buildTowerPopupHUD();
        if (towerPopupHUD == null) return;

        if (towerPopupScrim != null) towerPopupScrim.remove();
        towerPopupScrim = new Image(game.assets.whitePixel);
        towerPopupScrim.setColor(0, 0, 0, 0.001f);
        towerPopupScrim.setFillParent(true);
        towerPopupScrim.setTouchable(Touchable.enabled);
        towerPopupScrim.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { hideTowerPopupHUD(); }
        });

        towerPopupHUD.setVisible(false);
        stage.addActor(towerPopupScrim);
        stage.addActor(towerPopupHUD);

        towerPopupHUD.setVisible(true);
        towerPopupHUD.setColor(1,1,1,0f);
        towerPopupHUD.addAction(Actions.fadeIn(0.2f));
    }

    public void hideTowerPopupHUD() {
        if (towerPopupHUD != null) {
            towerPopupHUD.clearActions();
            towerPopupHUD.remove();
            towerPopupHUD = null;
        }
        if (towerPopupScrim != null) {
            towerPopupScrim.remove();
            towerPopupScrim = null;
        }
    }

    /* ===================== Tower Upgrade Popup ===================== */

    public void showUpgradePopupHUD(Tower tower, Runnable onUpgrade) {
        hideTowerPopupHUD();

        if (towerUpgradeHUD != null) towerUpgradeHUD.remove();

        TowerType nextType = tower.type.nextLevel();
        float popupW = 460, popupH = 210;

        towerUpgradeHUD = new Table();
        towerUpgradeHUD.setSize(popupW, popupH);

        float screenW = stage.getViewport().getWorldWidth();
        float padBottom = 32;
        towerUpgradeHUD.setPosition((screenW - popupW) / 2f, padBottom);

        if (bannerPatch != null)
            towerUpgradeHUD.setBackground(new NinePatchDrawable(bannerPatch));

        Table content = new Table();
        content.defaults().center();

        // Level hiện tại
        Label levelLabel = new Label("LEVEL: " + (tower.getUpgradeLevel() + 1),
            new Label.LabelStyle(font, Color.valueOf("ffe96b")));
        levelLabel.setFontScale(0.7f);
        content.add(levelLabel).width(popupW - 16).padTop(12).center().row();

        if (nextType != null) {
            // Thông tin cấp tiếp
            float dmgNow = tower.getDamage();
            float rngNow = tower.getRange();
            float dmgNext = nextType.damage;
            float rngNext = nextType.range;
            float dmgDiff = dmgNext - dmgNow;
            float rngDiff = rngNext - rngNow;

            Label nextLabel = new Label(
                "NEXT: " + nextType.name + "  (Cost: " + nextType.cost + ")",
                new Label.LabelStyle(font, Color.valueOf("36e7e2"))
            );
            nextLabel.setFontScale(0.62f);
            nextLabel.setAlignment(Align.center);
            nextLabel.setWrap(true);
            content.add(nextLabel).width(popupW - 24).padTop(6).center().row();

            Label statsDmg = new Label(
                "Damage: " + (int)dmgNow + " -> " + (int)dmgNext + " (+" + (int)dmgDiff + ")",
                new Label.LabelStyle(font, Color.WHITE));
            statsDmg.setFontScale(0.6f);
            content.add(statsDmg).width(popupW - 24).padTop(6).center().row();

            Label statsRange = new Label(
                "Range: " + (int)rngNow + " -> " + (int)rngNext + " (+" + (int)rngDiff + ")",
                new Label.LabelStyle(font, Color.WHITE));
            statsRange.setFontScale(0.6f);
            content.add(statsRange).width(popupW - 24).padTop(4).center().row();

            boolean affordable = world.gold >= nextType.cost;

            Label upgradeBtn = new Label(
                affordable ? "UPGRADE" : "NOT ENOUGH GOLD",
                new Label.LabelStyle(font, affordable ? Color.WHITE : Color.valueOf("ff5a5a"))
            );
            upgradeBtn.setFontScale(0.7f);
            upgradeBtn.setAlignment(Align.center);

            if (affordable) {
                upgradeBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        if (onUpgrade != null) onUpgrade.run();
                    }
                });
            }

            content.add(upgradeBtn).width(popupW - 24).padTop(12).center().row();
        } else {
            Label maxLabel = new Label("MAX LEVEL",
                new Label.LabelStyle(font, Color.valueOf("ff5959")));
            maxLabel.setFontScale(0.75f);
            maxLabel.setAlignment(Align.center);
            content.add(maxLabel).width(popupW - 16).padTop(18).center().row();
        }

        towerUpgradeHUD.add(content).expand().center();

        if (upgradePopupScrim != null) upgradePopupScrim.remove();
        upgradePopupScrim = new Image(game.assets.whitePixel);
        upgradePopupScrim.setColor(0, 0, 0, 0.001f);
        upgradePopupScrim.setFillParent(true);
        upgradePopupScrim.setTouchable(Touchable.enabled);
        upgradePopupScrim.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { hideUpgradePopupHUD(); }
        });

        towerUpgradeHUD.setVisible(false);
        stage.addActor(upgradePopupScrim);
        stage.addActor(towerUpgradeHUD);

        towerUpgradeHUD.setVisible(true);
        towerUpgradeHUD.setColor(1, 1, 1, 0);
        towerUpgradeHUD.addAction(Actions.fadeIn(0.19f));
    }

    public void hideUpgradePopupHUD() {
        if (towerUpgradeHUD != null) {
            towerUpgradeHUD.clearActions();
            towerUpgradeHUD.remove();
            towerUpgradeHUD = null;
        }
        if (upgradePopupScrim != null) {
            upgradePopupScrim.remove();
            upgradePopupScrim = null;
        }
    }

    /* ===================== HUD update / state ===================== */

    public void updateHudValues(boolean inWave) {
        if (goldLabel != null) goldLabel.setText(String.valueOf(world.gold));
        if (heartLabel != null) heartLabel.setText(String.valueOf(world.lives));
        if (waveLabel != null) {
            int cur = world.waveManager.getCurrentWave();
            int total = world.waveManager.getTotalWaves();
            if (total > 0) waveLabel.setText(cur + "/" + total);
            else waveLabel.setText(String.valueOf(cur));
        }
        refreshPlayPauseIcon();
    }

    public boolean isPaused() { return paused; }
    public Stage stage() { return stage; }

    public void dispose() {
        stage.dispose();
        if (iconCashTex != null) iconCashTex.dispose();
        if (iconHeartTex != null) iconHeartTex.dispose();
        if (bannerPatch != null && bannerPatch.getTexture() != null) bannerPatch.getTexture().dispose();
        if (iconSkin != null) iconSkin.dispose();
        if (waveTitleFont != null && waveTitleFont != font) waveTitleFont.dispose();
        if (towerIconAtlas != null) towerIconAtlas.dispose();
    }
}
