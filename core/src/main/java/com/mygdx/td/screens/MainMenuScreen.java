package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.mygdx.td.TDGame;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.mygdx.td.utils.SoundUtils;

public class MainMenuScreen implements Screen {

    private final TDGame game;
    private final Stage stage;
    private final OrthographicCamera camera;

    private static final boolean USE_RELATIVE = false;
    private static final float ICON_SIZE_FIXED = 60f;
    private static final float ICON_RELATIVE_H = 0.20f;
    private static final boolean ENABLE_DEBUG_BOUNDS = false;

    private BitmapFont titleFont;
    private NinePatch bannerNinePatch;

    private final SoundUtils soundUtils = new SoundUtils();

    // Lưu lại kích thước gốc banner để scale tỉ lệ không méo góc
    private final float bannerOrigW;

    public MainMenuScreen(TDGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        stage = new Stage(new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera), game.batch);
        camera.position.set(400, 240, 0);
        camera.update();

        // Load font title (giống splash)
        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        // Load NinePatch banner (cách 1: tăng chiều rộng code, giữ nguyên ảnh)
        bannerNinePatch = new NinePatch(new Texture(Gdx.files.internal("ui/banner_11.9.png")), 16, 16, 16, 16);

        // Lưu lại kích thước gốc
        bannerOrigW = bannerNinePatch.getTotalWidth();
        float bannerOrigH = bannerNinePatch.getTotalHeight();

        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    private void buildUI() {
        if (ENABLE_DEBUG_BOUNDS) stage.setDebugAll(true);

        // --- TẠO BUTTON ATLAS/SKIN ---
        TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
        for(Texture texture : buttonAtlas.getTextures()) {
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        Skin buttonSkin = new Skin(buttonAtlas);

        // START Button
        Drawable startUp   = buttonSkin.getDrawable("START_over");
        Drawable startOver = buttonSkin.getDrawable("START_up");
        Drawable startDown = buttonSkin.getDrawable("START_down");
        BitmapFont font = (game.assets.fontMedium != null) ? game.assets.fontMedium : new BitmapFont();
        TextButton.TextButtonStyle startStyle = new TextButton.TextButtonStyle(startUp, startDown, startOver, font);
        TextButton startBtn = new TextButton("", startStyle);

        float origWidth = startUp.getMinWidth();
        float origHeight = startUp.getMinHeight();
        float scale = 2f;

        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.setScreen(new SelectLevelScreen(game));
            }
        });

        // Quit button
        Drawable quitUp   = buttonSkin.getDrawable("QUIT_over");
        Drawable quitDown = buttonSkin.getDrawable("QUIT_up");
        Drawable quitOver = buttonSkin.getDrawable("QUIT_over");
        TextButton.TextButtonStyle quitStyle = new TextButton.TextButtonStyle(quitUp, quitDown, quitOver, font);
        TextButton quitBtn = new TextButton("", quitStyle);

        quitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // --------- ROOT LAYOUT ----------
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // --- Dòng chữ TOWER DEFENSE nằm trên NinePatch banner ---
        NinePatchDrawable bannerDrawable = new NinePatchDrawable(bannerNinePatch);
        Image bannerImg = new Image(bannerDrawable);
        bannerImg.setScaling(Scaling.stretch);
        bannerImg.setColor(1, 1, 1, 0.92f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(1f, 0.90f, 0.10f, 1f));
        Label titleLabel = new Label("TOWER DEFENSE", titleStyle);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);

        // Stack: chồng banner và label lên nhau
        Stack titleStack = new Stack();
        titleStack.add(bannerImg);
        titleStack.add(titleLabel);

        float bannerWidth = 600f;
        float bannerHeight = 100f;

        titleStack.setSize(bannerWidth, bannerHeight);
        bannerImg.setSize(bannerWidth, bannerHeight);

        Table menuTable = new Table();
        menuTable.defaults().space(10f);
        menuTable.add(startBtn).size(origWidth * scale, origHeight * scale).row();
        menuTable.add(quitBtn).size(origWidth * scale * 0.75f, origHeight * scale * 0.75f).row();

        // Đảm bảo layout cân giữa, các thành phần không dính lên trên
        root.add().expandY().row(); // Thêm dòng trống đẩy giữa
        root.add(titleStack).padTop(100f).padBottom(8f).width(bannerWidth).height(bannerHeight).center().row();
        root.add(menuTable).padBottom(30f).center().row();
        root.add().expandY().row();

        // --------- ICONS (LỚN) sử dụng texture atlas mới ----------
        float iconSize = USE_RELATIVE
            ? stage.getViewport().getWorldHeight() * ICON_RELATIVE_H
            : ICON_SIZE_FIXED;

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

        // Hiệu ứng toggle cho music (cam: on, metal: over/down/off)
        ImageButton.ImageButtonStyle musicToggleOnStyle = new ImageButton.ImageButtonStyle();
        musicToggleOnStyle.up = iconSkin.getDrawable("row-8-column-8");           // cam (ON)
        musicToggleOnStyle.over = iconSkinInactive.getDrawable("row-8-column-7"); // metal (hover)
        musicToggleOnStyle.down = iconSkinInactive.getDrawable("row-8-column-9"); // metal (down)

        ImageButton.ImageButtonStyle musicToggleOffStyle = new ImageButton.ImageButtonStyle();
        musicToggleOffStyle.up = iconSkinInactive.getDrawable("row-8-column-9");
        musicToggleOffStyle.over = iconSkin.getDrawable("row-8-column-7");
        musicToggleOffStyle.down = iconSkin.getDrawable("row-8-column-8");

        final ImageButton musicBtn = new ImageButton(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
        musicBtn.setChecked(!game.musicEnabled);
        musicBtn.getImage().setScaling(Scaling.stretch);
        musicBtn.getImage().setSize(iconSize * 2.0f, iconSize * 2.0f);

        musicBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.musicEnabled = !game.musicEnabled;
                if(!game.musicEnabled) {
                    // Tắt hết âm thanh đang phát
                    game.assets.themeMusic.stop();
                } else {
                    // Phát âm thanh bật lại
                    game.resumeThemeMusic();
                }
                musicBtn.setStyle(game.musicEnabled ? musicToggleOnStyle : musicToggleOffStyle);
            }
        });

        // Hiệu ứng toggle cho sound (cam: on, metal: over/down/off)
        ImageButton.ImageButtonStyle soundToggleOnStyle = new ImageButton.ImageButtonStyle();
        soundToggleOnStyle.up = iconSkin.getDrawable("row-7-column-8");           // cam (ON)
        soundToggleOnStyle.over = iconSkinInactive.getDrawable("row-7-column-7"); // metal (hover)
        soundToggleOnStyle.down = iconSkinInactive.getDrawable("row-7-column-9"); // metal (down)

        ImageButton.ImageButtonStyle soundToggleOffStyle = new ImageButton.ImageButtonStyle();
        soundToggleOffStyle.up = iconSkinInactive.getDrawable("row-7-column-9");
        soundToggleOffStyle.over = iconSkin.getDrawable("row-7-column-7");
        soundToggleOffStyle.down = iconSkin.getDrawable("row-7-column-8");

        final ImageButton soundBtn = new ImageButton(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
        soundBtn.setChecked(!game.soundEnabled);
        soundBtn.getImage().setScaling(Scaling.stretch);
        soundBtn.getImage().setSize(iconSize * 2.0f, iconSize * 2.0f);

        soundBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.soundEnabled = !game.soundEnabled;
                game.playSound(game.assets.gameClickSound);
                soundBtn.setStyle(game.soundEnabled ? soundToggleOnStyle : soundToggleOffStyle);
            }
        });

        Table iconTable = new Table();
        iconTable.top().left();
        iconTable.setFillParent(true);
        iconTable.add(musicBtn).size(iconSize, iconSize).pad(14, 14, 0, 0);
        iconTable.add(soundBtn).size(iconSize, iconSize).pad(14, 20, 0, 0);
        stage.addActor(iconTable);

        // Nút setting

        // 1. Tạo Drawable cho nút Setting (ví dụ từ atlas của bạn)
        Drawable settingUp = iconSkin.getDrawable("row-2-column-10"); // Đổi tên theo atlas của bạn
        Drawable settingOver = iconSkin.getDrawable("row-2-column-11");
        Drawable settingDown = iconSkin.getDrawable("row-2-column-12");

        ImageButton.ImageButtonStyle settingStyle = new ImageButton.ImageButtonStyle();
        settingStyle.up = settingOver;
        settingStyle.over = settingUp;
        settingStyle.down = settingDown;

        ImageButton settingBtn = new ImageButton(settingStyle);
        settingBtn.getImage().setScaling(Scaling.stretch);
        settingBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SettingScreen(game));
            }
        });

        // 2. Tạo Table cho góc phải trên
        Table settingTable = new Table();
        settingTable.top().right();
        settingTable.setFillParent(true);
        settingTable.add(settingBtn).size(iconSize, iconSize).pad(14, 14, 0, 14); // pad(right) cho sát mép
        stage.addActor(settingTable);


        // --------- BOTTOM LABEL ----------
        Label bottom = new Label("v0.1  © 2025", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        bottom.setFontScale(0.7f);
        bottom.setAlignment(Align.left);
        bottom.setPosition(8, 6);
        stage.addActor(bottom);

        Gdx.app.log("ICON DEBUG", "musicBtn size=" + musicBtn.getWidth() + "x" + musicBtn.getHeight());
        Gdx.app.log("ICON DEBUG", "soundBtn size=" + soundBtn.getWidth() + "x" + soundBtn.getHeight());
    }

    private TextureRegionDrawable drawable(Texture t) {
        if (t == null) return null;
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            stage.setDebugAll(!stage.isDebugAll());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        // Vẽ background phủ kín (cover)
        drawBackgroundCover();
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    // Hàm mới: vẽ background kiểu cover
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
            game.batch.setColor(1, 1, 1, 1f); // reset
        }
    }

    @Override
    public void resize(int width, int height) {
        // Nếu có stage riêng cho màn hình đó:
        if (stage != null) stage.getViewport().update(width, height, true);
    }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void dispose() {
        stage.dispose();
        if (titleFont != null) titleFont.dispose();
        if (bannerNinePatch != null) bannerNinePatch.getTexture().dispose();
    }
}
