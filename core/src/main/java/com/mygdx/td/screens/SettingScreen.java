package com.mygdx.td.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.td.TDGame;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class SettingScreen implements Screen {

    private final TDGame game;
    private final Stage stage;

    private BitmapFont titleFont;
    private final NinePatch bannerNinePatch;
    private final NinePatch titleNinePatch;

    // Trạng thái toggle, nên gắn vào Assets hoặc Prefs thực tế
    private boolean musicEnabled = true;
    private boolean soundEnabled = true;

    public SettingScreen(TDGame game) {
        this.game = game;
        OrthographicCamera camera = new OrthographicCamera();
        stage = new Stage(new FitViewport(800, 480, camera), game.batch);
        camera.position.set(400, 240, 0);
        camera.update();

        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        bannerNinePatch = new NinePatch(new Texture(Gdx.files.internal("ui/banner_11.9.png")), 16, 16, 16, 16);
        titleNinePatch = new NinePatch(new Texture(Gdx.files.internal("ui/title_banner.9.png")), 16, 16, 16, 16);

        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ===== TIÊU ĐỀ (BANNER NỀN + CHỮ) =====
        float titleBannerWidth = 300f;
        float titleBannerHeight = 80f;

        NinePatchDrawable titleBannerDrawable = new NinePatchDrawable(titleNinePatch);
        Image titleBannerImg = new Image(titleBannerDrawable);
        titleBannerImg.setScaling(Scaling.stretch);
        titleBannerImg.setColor(1, 1, 1, 0.93f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(1f, 0.90f, 0.10f, 1f));
        Label titleLabel = new Label("SETTING", titleStyle);
        titleLabel.setFontScale(1.2f);
        titleLabel.setAlignment(Align.center);

        Stack titleStack = new Stack();
        titleStack.add(titleBannerImg);
        // Đảm bảo label luôn fill banner
        Table titleTable = new Table();
        titleTable.setFillParent(true);
        titleTable.add(titleLabel).expand().fill().center();
        titleStack.add(titleTable);

        // ===================== KHUNG CHÍNH =====================
        float frameWidth = 650f;
        float frameHeight = 300f;

        NinePatchDrawable frameDrawable = new NinePatchDrawable(bannerNinePatch);
        Image frameImg = new Image(frameDrawable);
        frameImg.setScaling(Scaling.stretch);
        frameImg.setColor(1, 1, 1, 0.93f);

        Table settingsContent = new Table();
        settingsContent.pad(14f);

        Stack frameStack = new Stack();
        frameStack.add(frameImg);
        frameStack.add(settingsContent);

        // ===================== NÚT SAVE/BACK =====================
        TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
        for(Texture texture : buttonAtlas.getTextures()) {
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        Skin buttonSkin = new Skin(buttonAtlas);

        Drawable saveUp   = buttonSkin.getDrawable("START_up");

        BitmapFont btnFont = (game.assets.fontMedium != null) ? game.assets.fontMedium : new BitmapFont();

        TextButton.TextButtonStyle saveStyle = new TextButton.TextButtonStyle();
        saveStyle.up = buttonSkin.getDrawable("SAVE_over");    // bình thường
        saveStyle.over = buttonSkin.getDrawable("SAVE_up");// hover
        saveStyle.down = buttonSkin.getDrawable("SAVE_down");// nhấn giữ
        saveStyle.font = btnFont;

        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.up = buttonSkin.getDrawable("BACK_over");
        backStyle.over = buttonSkin.getDrawable("BACK_up");
        backStyle.down = buttonSkin.getDrawable("BACK_down");
        backStyle.font = btnFont;

        TextButton saveBtn = new TextButton(" ", saveStyle);
        TextButton backBtn = new TextButton(" ", backStyle);

        float btnWidth = saveUp.getMinWidth() * 1.5f;
        float btnHeight = saveUp.getMinHeight() * 1.5f;

        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // TODO: Lưu setting nếu cần
                game.setScreen(new MainMenuScreen(game));
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(saveBtn).size(btnWidth, btnHeight).pad(10);
        buttonTable.add(backBtn).size(btnWidth, btnHeight).pad(10);

        // =========== SLIDER =============
        // 1. Tạo NinePatch cho slider background (dùng file 9patch mới)
        NinePatch sliderBgNinePatch = new NinePatch(
            new Texture(Gdx.files.internal("ui/bg_slider.9.png")),
            8, 8, 8, 8 // chỉnh thông số này đúng với vùng viền không kéo dãn của bạn, nếu slider bị méo hoặc lòi viền thì tăng giảm số này cho phù hợp
        );
        NinePatchDrawable sliderBgDrawable = new NinePatchDrawable(sliderBgNinePatch);

// 2. Lấy knob từ atlas như cũ
        TextureAtlas sliderAtlas = new TextureAtlas(Gdx.files.internal("ui/knob.atlas"));
        Skin sliderSkin = new Skin(sliderAtlas);

// 3. Tạo SliderStyle
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = sliderBgDrawable;
        sliderStyle.knob = sliderSkin.getDrawable("knob");

// 4. Tạo slider, muốn to thì set height thật lớn
        Slider musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        Slider soundSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        musicSlider.setValue(1f);
        soundSlider.setValue(1f);

        // =========== NÚT TOGGLE MUSIC ===========
        TextureAtlas activeIconAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_icon_buttons.atlas"));
        TextureAtlas inActiveIconAtlas = new TextureAtlas(Gdx.files.internal("ui/metal_buttons_icon.atlas"));
        for (Texture tex : activeIconAtlas.getTextures()) {
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        for (Texture tex : inActiveIconAtlas.getTextures()) {
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        Skin iconSkin = new Skin(activeIconAtlas);
        Skin iconSkinInactive = new Skin(inActiveIconAtlas);

        // Music
        ImageButton.ImageButtonStyle musicToggleOnStyle = new ImageButton.ImageButtonStyle();
        musicToggleOnStyle.up = iconSkin.getDrawable("row-8-column-8");
        musicToggleOnStyle.over = iconSkinInactive.getDrawable("row-8-column-7");
        musicToggleOnStyle.down = iconSkinInactive.getDrawable("row-8-column-9");

        ImageButton.ImageButtonStyle musicToggleOffStyle = new ImageButton.ImageButtonStyle();
        musicToggleOffStyle.up = iconSkinInactive.getDrawable("row-8-column-9");
        musicToggleOffStyle.over = iconSkin.getDrawable("row-8-column-7");
        musicToggleOffStyle.down = iconSkin.getDrawable("row-8-column-8");

        // Sound
        ImageButton.ImageButtonStyle soundToggleOnStyle = new ImageButton.ImageButtonStyle();
        soundToggleOnStyle.up = iconSkin.getDrawable("row-7-column-8");
        soundToggleOnStyle.over = iconSkinInactive.getDrawable("row-7-column-7");
        soundToggleOnStyle.down = iconSkinInactive.getDrawable("row-7-column-9");

        ImageButton.ImageButtonStyle soundToggleOffStyle = new ImageButton.ImageButtonStyle();
        soundToggleOffStyle.up = iconSkinInactive.getDrawable("row-7-column-9");
        soundToggleOffStyle.over = iconSkin.getDrawable("row-7-column-7");
        soundToggleOffStyle.down = iconSkin.getDrawable("row-7-column-8");

        // Music button
        final ImageButton musicBtn = new ImageButton(musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
        musicBtn.setChecked(!musicEnabled);
        musicBtn.getImage().setScaling(Scaling.stretch);
        musicBtn.getImage().setSize(48, 48);
        musicBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                musicEnabled = !musicEnabled;
                musicBtn.setStyle(musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
            }
        });

        // Sound button
        final ImageButton soundBtn = new ImageButton(soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
        soundBtn.setChecked(!soundEnabled);
        soundBtn.getImage().setScaling(Scaling.stretch);
        soundBtn.getImage().setSize(48, 48);
        soundBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundEnabled = !soundEnabled;
                soundBtn.setStyle(soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
            }
        });

        // LABEL
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = titleFont;
        labelStyle.fontColor = Color.WHITE;


// 5. Khi add vào Table, set height lớn (VD: 80 hoặc hơn), width tuỳ ý
        settingsContent.add(new Label("Music", labelStyle)).padRight(12);
        settingsContent.add(musicBtn).size(48, 48).padRight(18);
        settingsContent.add(musicSlider).width(320).height(100).padLeft(10).padRight(10);
        settingsContent.row().padTop(24);

        settingsContent.add(new Label("Sound", labelStyle)).padRight(12);
        settingsContent.add(soundBtn).size(48, 48).padRight(18);
        settingsContent.add(soundSlider).width(320).height(100).padLeft(10).padRight(10);
        settingsContent.row();

        // ===== GHÉP LAYOUT =====
        root.add().expandY().row();
        root.add(titleStack).width(titleBannerWidth).height(titleBannerHeight).padBottom(12f).center().row();
        root.add(frameStack).width(frameWidth).height(frameHeight).padBottom(10f).center().row();
        root.add(buttonTable).center().row();
        root.add().expandY().row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        drawBackgroundCover();
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void drawBackgroundCover() {
        if (game.assets.menuBg != null) {
            float worldW = stage.getViewport().getWorldWidth();
            float worldH = stage.getViewport().getWorldHeight();
            float imgW = game.assets.menuBg.getWidth();
            float imgH = game.assets.menuBg.getHeight();
            float scale = Math.max(worldW / imgW, worldH / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;
            float x = (worldW - drawW) / 2f;
            float y = (worldH - drawH) / 2f;
            game.batch.setColor(1, 1, 1, 1f);
            game.batch.draw(game.assets.menuBg, x, y, drawW, drawH);
            game.batch.setColor(1, 1, 1, 1f);
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width,height,true); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void dispose() {
        stage.dispose();
        if (titleFont != null) titleFont.dispose();
        if (bannerNinePatch != null) bannerNinePatch.getTexture().dispose();
        if (titleNinePatch != null) titleNinePatch.getTexture().dispose();
    }
}
