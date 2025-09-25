package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.mygdx.td.TDGame;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mygdx.td.utils.SoundUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn chọn level – khi chọn, nút sáng lên rõ ràng và scale to hơn.
 */
public class SelectLevelScreen implements Screen {

    private final TDGame game;
    private final Stage stage;

    private BitmapFont titleFont;
    private final NinePatch titleNinePatch;
    private final NinePatch frameNinePatch;
    private final NinePatch levelButtonPatch;

    private static final int LEVEL_COUNT = 5;
    private static final int LEVELS_PER_ROW = 3;

    private static final float LEVEL_BTN_WIDTH = 210f;
    private static final float LEVEL_BTN_HEIGHT = 56f;
    private static final float BTN_SCALE_SELECTED = 1.13f;
    private static final float BTN_SCALE_NORMAL = 1f;
    private static final float LEVEL_BTN_ANIM_TIME = 0.13f;
    private static final float LEVEL_CELL_PAD = 18f;

    // Trạng thái level được chọn
    private int selectedLevel = 1;

    private final List<LevelItem> levelItems = new ArrayList<>();

    private SoundUtils soundUtils = new SoundUtils();

    private static class LevelItem {
        int level;
        TextButton button;
        Label label;
        Stack stack;
    }

    public SelectLevelScreen(TDGame game) {
        this.game = game;
        OrthographicCamera camera = new OrthographicCamera();
        stage = new Stage(new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera), game.batch);
        camera.position.set(400, 240, 0);
        camera.update();

        try {
            titleFont = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            titleFont.setUseIntegerPositions(false);
        } catch (Exception e) {
            titleFont = new BitmapFont();
        }

        titleNinePatch = new NinePatch(new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/title_banner.9.png")), 16,16,16,16);
        frameNinePatch = new NinePatch(new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/banner_11.9.png")), 16,16,16,16);
        levelButtonPatch = new NinePatch(new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/banner_11.9.png")), 16,16,16,16);

        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    private void buildUI() {
        stage.clear();
        levelItems.clear();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ====== TITLE ======
        float titleBannerWidth = 400f;
        float titleBannerHeight = 80f;

        NinePatchDrawable titleBannerDrawable = new NinePatchDrawable(titleNinePatch);
        Image titleBannerImg = new Image(titleBannerDrawable);
        titleBannerImg.setScaling(Scaling.stretch);
        titleBannerImg.setColor(1,1,1,0.95f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(1f,0.90f,0.10f,1f));
        Label titleLabel = new Label("SELECT LEVEL", titleStyle);
        titleLabel.setFontScale(1.2f);
        titleLabel.setAlignment(Align.center);

        Stack titleStack = new Stack();
        titleStack.add(titleBannerImg);
        Table titleTable = new Table();
        titleTable.setFillParent(true);
        titleTable.add(titleLabel).expand().fill().center();
        titleStack.add(titleTable);

        // ====== FRAME + LEVEL LIST ======
        float frameWidth = 760f;
        float frameHeight = 300f;

        NinePatchDrawable frameDrawable = new NinePatchDrawable(frameNinePatch);
        Image frameImg = new Image(frameDrawable);
        frameImg.setScaling(Scaling.stretch);
        frameImg.setColor(1,1,1,0.92f);

        Table levelContent = new Table();
        levelContent.pad(24f);

        for (int i = 1; i <= LEVEL_COUNT; i++) {
            final int idx = i;

            TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
            style.up = new NinePatchDrawable(levelButtonPatch);
            style.font = titleFont;
            style.fontColor = Color.WHITE;

            final TextButton btn = new TextButton("", style);
            Label label = new Label("Level " + toRoman(idx), new Label.LabelStyle(titleFont, Color.WHITE));
            label.setAlignment(Align.center);

            Stack stack = new Stack();
            stack.setTransform(true); // enable scale
            Image bg = new Image(new NinePatchDrawable(levelButtonPatch));
            bg.setScaling(Scaling.stretch);

            stack.add(bg);
            stack.add(label);

            LevelItem item = new LevelItem();
            item.level = idx;
            item.button = btn;
            item.label = label;
            item.stack = stack;
            levelItems.add(item);

            stack.setSize(LEVEL_BTN_WIDTH, LEVEL_BTN_HEIGHT);
            stack.setOrigin(Align.center);

            stack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playSound(game.assets.selectLevelSound);
                    if (selectedLevel != idx) {
                        selectedLevel = idx;
                        updateSelection();
                        Gdx.app.log("SelectLevelScreen", "Selected level = " + selectedLevel);
                    }
                }
            });

            levelContent.add(stack)
                .width(LEVEL_BTN_WIDTH)
                .height(LEVEL_BTN_HEIGHT)
                .pad(LEVEL_CELL_PAD);
            if (i % LEVELS_PER_ROW == 0) levelContent.row();
        }

        // Sau khi tạo xong, cập nhật trạng thái ban đầu
        updateSelection();

        Stack frameStack = new Stack();
        frameStack.add(frameImg);
        frameStack.add(levelContent);

        // ====== PLAY / BACK ======
        TextureAtlas buttonAtlas = new TextureAtlas(Gdx.files.internal("ui/orange_buttons_text.atlas"));
        for (com.badlogic.gdx.graphics.Texture t : buttonAtlas.getTextures())
            t.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest, com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest);
        Skin buttonSkin = new Skin(buttonAtlas);

        Drawable playUp = buttonSkin.getDrawable("START_up");
        BitmapFont btnFont = (game.assets.fontMedium != null) ? game.assets.fontMedium : titleFont;

        TextButton.TextButtonStyle playStyle = new TextButton.TextButtonStyle();
        playStyle.up   = buttonSkin.getDrawable("PLAY_up");
        playStyle.over = buttonSkin.getDrawable("PLAY_over");
        playStyle.down = buttonSkin.getDrawable("PLAY_down");
        playStyle.font = btnFont;

        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.up   = buttonSkin.getDrawable("BACK_over");
        backStyle.over = buttonSkin.getDrawable("BACK_up");
        backStyle.down = buttonSkin.getDrawable("BACK_down");
        backStyle.font = btnFont;

        TextButton playBtn = new TextButton("", playStyle);
        TextButton backBtn = new TextButton("", backStyle);

        float btnWidth = playUp.getMinWidth() * 1.5f;
        float btnHeight = playUp.getMinHeight() * 1.5f;

        playBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                Gdx.app.log("SelectLevelScreen", "Play level: " + selectedLevel);
                game.startGameWithLevel(selectedLevel);
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.playSound(game.assets.gameClickSound);
                game.setScreen(new MainMenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(playBtn).size(btnWidth, btnHeight).pad(10);
        buttonTable.add(backBtn).size(btnWidth, btnHeight).pad(10);

        // ====== GHÉP LAYOUT ======
        root.add().expandY().row();
        root.add(titleStack).width(titleBannerWidth).height(titleBannerHeight).padBottom(12f).center().row();
        root.add(frameStack).width(frameWidth).height(frameHeight).padBottom(12f).center().row();
        root.add(buttonTable).center().row();
        root.add().expandY().row();
    }

    /**
     * Khi chọn level, nút đó sáng lên rõ ràng và scale to, các nút khác về bình thường.
     */
    private void updateSelection() {
        for (LevelItem item : levelItems) {
            boolean sel = (item.level == selectedLevel);

            item.stack.clearActions();
            if (sel) {
                item.stack.addAction(Actions.scaleTo(BTN_SCALE_SELECTED, BTN_SCALE_SELECTED, LEVEL_BTN_ANIM_TIME));
                item.label.setColor(Color.valueOf("ffe96b")); // vàng sáng
                item.label.setFontScale(1.16f);
            } else {
                item.stack.addAction(Actions.scaleTo(BTN_SCALE_NORMAL, BTN_SCALE_NORMAL, LEVEL_BTN_ANIM_TIME));
                item.label.setColor(Color.WHITE);
                item.label.setFontScale(1f);
            }
        }
    }

    private String toRoman(int number) {
        String[] roman = {"I","II","III","IV","V","VI","VII","VIII","IX","X","XI",
            "XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"};
        if (number >= 1 && number <= 20) return roman[number - 1];
        return String.valueOf(number);
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
            game.batch.setColor(1,1,1,1f);
            game.batch.draw(game.assets.menuBg, x, y, drawW, drawH);
            game.batch.setColor(1,1,1,1f);
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
    }
}
