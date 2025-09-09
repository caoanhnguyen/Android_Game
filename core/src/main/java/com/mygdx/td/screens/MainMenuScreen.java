package com.mygdx.td.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.td.TDGame;

public class MainMenuScreen implements Screen {

    private final TDGame game;
    private final Stage stage;
    private final OrthographicCamera camera;

    private boolean musicEnabled = true;
    private boolean soundEnabled = true;

    // Chọn 1 trong 2 cách:
    private static final boolean USE_RELATIVE = false;      // true = dùng % chiều cao
    private static final float ICON_SIZE_FIXED = 60f;      // kích thước cố định nếu USE_RELATIVE = false
    private static final float ICON_RELATIVE_H = 0.20f;     // 20% chiều cao nếu USE_RELATIVE = true

    // Bật tạm debug bounds (Set true nếu muốn nhìn khung)
    private static final boolean ENABLE_DEBUG_BOUNDS = false;

    public MainMenuScreen(TDGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        stage = new Stage(new FitViewport(800, 480, camera), game.batch);
        camera.position.set(400, 240, 0);
        camera.update();

        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    private void buildUI() {
        if (ENABLE_DEBUG_BOUNDS) stage.setDebugAll(true);

        BitmapFont font = (game.assets.fontMedium != null) ? game.assets.fontMedium : new BitmapFont();
        font.getData().setScale(1f);
        // --------- BUTTON STYLE ----------
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up   = drawable(game.assets.btnUp);
        style.over = drawable(game.assets.btnOver != null ? game.assets.btnOver : game.assets.btnUp);
        style.down = drawable(game.assets.btnDown != null ? game.assets.btnDown : game.assets.btnUp);
        style.font = font;
        style.fontColor = Color.WHITE;
        style.overFontColor = Color.YELLOW;
        style.downFontColor = Color.LIGHT_GRAY;

        TextButton startBtn = new TextButton("START", style);
        TextButton creditsBtn = new TextButton("CREDITS", style);
        startBtn.pad(6, 40, 6, 40);
        creditsBtn.pad(6, 40, 6, 40);

        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.startGame();
            }
        });
        creditsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("MENU", "Credits pressed");
            }
        });

        // --------- ROOT LAYOUT ----------
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Image logoImg = (game.assets.logoTex != null)
            ? new Image(drawable(game.assets.logoTex))
            : new Image(drawable(null));

        if (game.assets.logoTex != null) {
            float targetW = 260f;
            float ratio = game.assets.logoTex.getHeight() / (float) game.assets.logoTex.getWidth();
            logoImg.setSize(targetW, targetW * ratio);
        }

        Table menuTable = new Table();
        menuTable.defaults().space(18f);
        menuTable.add(startBtn).row();
        menuTable.add(creditsBtn).row();

        root.add(logoImg).padTop(40f).row();
        root.add(menuTable).expandY().top();

        // --------- ICONS (LỚN) ----------
        float iconSize = USE_RELATIVE
            ? stage.getViewport().getWorldHeight() * ICON_RELATIVE_H
            : ICON_SIZE_FIXED;

        Image musicIcon = createToggleIcon(
            game.assets.musicOn, game.assets.musicOff,
            () -> musicEnabled,
            val -> {
                musicEnabled = val;
                Gdx.app.log("AUDIO", "Music = " + musicEnabled);
            },
            iconSize
        );
        musicIcon.setPosition(14, stage.getViewport().getWorldHeight() - 14 - musicIcon.getHeight());
        stage.addActor(musicIcon);

        Image soundIcon = createToggleIcon(
            game.assets.soundOn, game.assets.soundOff,
            () -> soundEnabled,
            val -> {
                soundEnabled = val;
                Gdx.app.log("AUDIO", "SFX = " + soundEnabled);
            },
            iconSize
        );
        soundIcon.setPosition(musicIcon.getX() + musicIcon.getWidth() + 20,
            stage.getViewport().getWorldHeight() - 14 - soundIcon.getHeight());
        stage.addActor(soundIcon);

        // --------- BOTTOM LABEL ----------
        Label bottom = new Label("v0.1  © 2025", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        bottom.setFontScale(0.7f);
        bottom.setAlignment(Align.left);
        bottom.setPosition(8, 6);
        stage.addActor(bottom);

        // In log xác nhận kích thước:
        Gdx.app.log("ICON DEBUG", "musicIcon size=" + musicIcon.getWidth() + "x" + musicIcon.getHeight()
            + "  source=" + (game.assets.musicOn != null
            ? game.assets.musicOn.getWidth() + "x" + game.assets.musicOn.getHeight() : "null"));
    }

    private Image createToggleIcon(
        com.badlogic.gdx.graphics.Texture onTex,
        com.badlogic.gdx.graphics.Texture offTex,
        java.util.function.BooleanSupplier getter,
        java.util.function.Consumer<Boolean> setter,
        float size
    ) {
        boolean state = getter.getAsBoolean();
        TextureRegionDrawable dr = drawable(state ? onTex : offTex);
        Image img = new Image(dr);
        img.setSize(size, size);                 // SET SIZE THẲNG
        img.setScaling(com.badlogic.gdx.utils.Scaling.stretch); // scale texture to khớp size

        img.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean newVal = !getter.getAsBoolean();
                setter.accept(newVal);
                img.setDrawable(drawable(newVal ? onTex : offTex));
            }
        });
        return img;
    }

    private TextureRegionDrawable drawable(com.badlogic.gdx.graphics.Texture t) {
        if (t == null) return null;
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    @Override
    public void render(float delta) {
        // Phím H bật/tắt debug bounds (nếu cần)
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
        if (game.assets.menuBg != null) {
            float w = stage.getViewport().getWorldWidth();
            float h = stage.getViewport().getWorldHeight();
            game.batch.draw(game.assets.menuBg, 0, 0, w, h);
        }
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width,height,true); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); }
}
