package com.mygdx.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.mygdx.td.TDGame;

public class InGameSettingsOverlay extends Group {

    private final TDGame game;
    private final Preferences prefs;

    private Table frameContent;

    private Slider musicSlider, soundSlider;
    private ImageButton musicBtn, soundBtn;
    private TextButton backBtn;

    private NinePatchDrawable frameDrawable;
    private NinePatchDrawable sliderBgDrawable;
    private Skin iconSkinActive, iconSkinInactive, sliderSkin;
    private Skin buttonSkin; // may be null -> fallback
    private BitmapFont titleFont;

    private boolean prevMusicEnabled, prevSoundEnabled;
    private float prevMusicVolume, prevSoundVolume;

    private boolean built = false;

    private static final float OVERLAY_TOP_PAD = 12f;
    private static final float BUTTONS_TOP_PAD = 36f;

    public InGameSettingsOverlay(TDGame game) {
        this.game = game;
        this.prefs = Gdx.app.getPreferences("td_settings");
        setVisible(false);
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
    }

    private void ensureBuilt(Stage stageIfAny) {
        if (built) return;

        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        NinePatch framePatch = new NinePatch(new Texture(Gdx.files.internal("ui/banner_11.9.png")), 16,16,16,16);
        frameDrawable = new NinePatchDrawable(framePatch);

        NinePatch sliderBgPatch = new NinePatch(new Texture(Gdx.files.internal("ui/bg_slider.9.png")), 8,8,8,8);
        sliderBgDrawable = new NinePatchDrawable(sliderBgPatch);

        TextureAtlas sliderAtlas = new TextureAtlas(Gdx.files.internal("ui/knob.atlas"));
        sliderSkin = new Skin(sliderAtlas);

        TextureAtlas activeIconAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
        TextureAtlas inActiveIconAtlas = new TextureAtlas(Gdx.files.internal("ui/metal_buttons_icon.atlas"));
        for (Texture tex : activeIconAtlas.getTextures()) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (Texture tex : inActiveIconAtlas.getTextures()) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconSkinActive = new Skin(activeIconAtlas);
        iconSkinInactive = new Skin(inActiveIconAtlas);

        // Try to load button atlas like SettingScreen
        try {
            TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
            for (Texture t : buttonAtlas.getTextures()) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            buttonSkin = new Skin(buttonAtlas);
        } catch (Exception e) {
            buttonSkin = null; // will fallback
        }

        Table rootFill = new Table();
        rootFill.setFillParent(true);
        addActor(rootFill);

        Image px = game.assets.whitePixel != null
            ? new Image(game.assets.whitePixel)
            : new Image(new Texture(Gdx.files.internal("ui/white1x1.png")));
        px.setColor(0, 0, 0, 0.55f);
        px.setFillParent(true);

        frameContent = new Table();
        frameContent.setBackground(frameDrawable);
        frameContent.align(Align.topLeft);
        // Tăng padding top bên trong khung bằng OVERLAY_TOP_PAD
        frameContent.pad(16f + OVERLAY_TOP_PAD, 16f, 16f, 16f);

        Container<Table> frameContainer = new Container<>(frameContent);
        frameContainer.fill();

        buildFrameInner();

        Table center = new Table();
        center.setFillParent(true);
        addActor(center);

        float frameWidth = 650f;
        float frameHeight = 340f; // inside-frame buttons need a bit taller than SettingScreen
        rootFill.add(px).grow();
        center.add(frameContainer).width(frameWidth).height(frameHeight).center();

        px.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onBackNotSave(); }
        });
        addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) { onBackNotSave(); return true; }
                return false;
            }
        });

        built = true;
    }

    private void buildFrameInner() {
        // Slider
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = sliderBgDrawable;
        sliderStyle.knob = sliderSkin.getDrawable("knob");

        // Toggles (same as SettingScreen)
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

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = titleFont;
        labelStyle.fontColor = Color.WHITE;

        musicBtn = new ImageButton(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
        musicBtn.setChecked(!game.musicEnabled);
        soundBtn = new ImageButton(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
        soundBtn.setChecked(!game.soundEnabled);

        musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        soundSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);

        // Layout: inside the same frame
        frameContent.clear();
        frameContent.defaults().center();

        frameContent.add(new Label("Music", labelStyle)).padRight(12);
        frameContent.add(musicBtn).size(48, 48).padRight(18);
        frameContent.add(musicSlider).width(320).height(64).padLeft(10).padRight(10);
        frameContent.row().padTop(16);

        frameContent.add(new Label("Sound", labelStyle)).padRight(12);
        frameContent.add(soundBtn).size(48, 48).padRight(18);
        frameContent.add(soundSlider).width(320).height(64).padLeft(10).padRight(10);
        frameContent.row().padTop(10);

        frameContent.add().colspan(3).expandY().padBottom(24);
        frameContent.row();

        // Buttons – exactly like SettingScreen (with fallback)
        float btnWidth, btnHeight;
        TextButton.TextButtonStyle saveStyle = new TextButton.TextButtonStyle();
        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        BitmapFont btnFont = game.assets.fontMedium != null ? game.assets.fontMedium : titleFont;

        if (buttonSkin != null
            && buttonSkin.has("SAVE_up", Drawable.class)
            && buttonSkin.has("SAVE_down", Drawable.class)
            && buttonSkin.has("SAVE_over", Drawable.class)
            && buttonSkin.has("BACK_up", Drawable.class)
            && buttonSkin.has("BACK_down", Drawable.class)
            && buttonSkin.has("BACK_over", Drawable.class)
            && buttonSkin.has("START_up", Drawable.class)) {

            saveStyle.up   = buttonSkin.getDrawable("SAVE_over");
            saveStyle.over = buttonSkin.getDrawable("SAVE_up");
            saveStyle.down = buttonSkin.getDrawable("SAVE_down");
            saveStyle.font = btnFont;

            backStyle.up   = buttonSkin.getDrawable("BACK_over");
            backStyle.over = buttonSkin.getDrawable("BACK_up");
            backStyle.down = buttonSkin.getDrawable("BACK_down");
            backStyle.font = btnFont;

            Drawable measure = buttonSkin.getDrawable("START_up");
            btnWidth = measure.getMinWidth() * 1.5f;
            btnHeight = measure.getMinHeight() * 1.5f;
        } else {
            NinePatchDrawable up = frameDrawable; // reuse frame 9patch as simple button bg
            NinePatchDrawable down = frameDrawable;
            saveStyle.up = up; saveStyle.down = down; saveStyle.over = down; saveStyle.font = btnFont;
            backStyle.up = up; backStyle.down = down; backStyle.over = down; backStyle.font = btnFont;
            btnWidth = 180f; btnHeight = 64f;
        }

        TextButton saveBtn = new TextButton(" ", saveStyle);
        backBtn = new TextButton(" ", backStyle);

        Table btnRow = new Table();
        btnRow.center();
        btnRow.add(saveBtn).size(btnWidth, btnHeight).padRight(10);
        btnRow.add(backBtn).size(btnWidth, btnHeight);

        // Tăng khoảng cách giữa slider và hàng nút bằng BUTTONS_TOP_PAD
        frameContent.add(btnRow).colspan(3).center().padTop(BUTTONS_TOP_PAD).padBottom(8);
        frameContent.row();

        // Listeners
        musicBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.musicEnabled = !game.musicEnabled;
                musicBtn.setStyle(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
                applyMusicNow();
            }
        });
        soundBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.soundEnabled = !game.soundEnabled;
                soundBtn.setStyle(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
            }
        });
        musicSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.musicVolume = musicSlider.getValue();
                applyMusicNow();
            }
        });
        soundSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.soundVolume = soundSlider.getValue();
            }
        });
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { game.playSound(game.assets.gameClickSound); onBackNotSave(); }
        });
        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { game.playSound(game.assets.gameClickSound); saveSettings(); hide(); }
        });
    }

    private void saveSettings() {
        prefs.putBoolean("musicEnabled", game.musicEnabled);
        prefs.putBoolean("soundEnabled", game.soundEnabled);
        prefs.putFloat("musicVolume", game.musicVolume);
        prefs.putFloat("soundVolume", game.soundVolume);
        prefs.flush();
        applyMusicNow();
    }

    private void onBackNotSave() {
        game.musicEnabled = prevMusicEnabled;
        game.soundEnabled = prevSoundEnabled;
        game.musicVolume = prevMusicVolume;
        game.soundVolume = prevSoundVolume;

        if (musicBtn != null) musicBtn.setChecked(!game.musicEnabled);
        if (soundBtn != null) soundBtn.setChecked(!game.soundEnabled);
        if (musicSlider != null) musicSlider.setValue(game.musicVolume);
        if (soundSlider != null) soundSlider.setValue(game.soundVolume);

        applyMusicNow();
        hide();
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

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        if (!built) ensureBuilt(stage);
    }

    public void show() {
        if (!built) ensureBuilt(getStage());
        prevMusicEnabled = game.musicEnabled;
        prevSoundEnabled = game.soundEnabled;
        prevMusicVolume = game.musicVolume;
        prevSoundVolume = game.soundVolume;

        if (musicBtn != null) musicBtn.setChecked(!game.musicEnabled);
        if (soundBtn != null) soundBtn.setChecked(!game.soundEnabled);
        if (musicSlider != null) musicSlider.setValue(game.musicVolume);
        if (soundSlider != null) soundSlider.setValue(game.soundVolume);

        setVisible(true);
        if (getStage() != null) {
            getStage().setKeyboardFocus(this);
            getStage().setScrollFocus(this);
        }
    }

    public void hide() {
        setVisible(false);
        if (getStage() != null) {
            getStage().setKeyboardFocus(null);
            getStage().setScrollFocus(null);
        }
    }

    public boolean isShown() { return isVisible(); }
}
