package com.mygdx.td.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.td.TDGame;

/**
 * LoadingScreen có background + chữ lớn ở giữa + progress bar.
 * Không phụ thuộc vào Assets.finishLoading(), vì ở thời điểm này AssetManager vẫn đang tải.
 */
public class LoadingScreen implements Screen {

    private final TDGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;

    private Texture bgTex;
    private Texture barBgTex;
    private Texture barFillTex;

    // Đổi: dùng font title bitmap
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private float minShowTime = 0.5f;          // tránh "flash" nếu load quá nhanh
    private float displayedProgress = 0f;      // progress hiển thị (lerp cho mượt)

    // Tùy chỉnh
    private static final float BAR_WIDTH_RATIO = 0.60f; // 60% chiều rộng viewport
    private static final float BAR_HEIGHT = 26f;
    private static final float BAR_BORDER = 3f;

    public LoadingScreen(TDGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        camera.position.set(400, 240, 0);
        camera.update();

        loadImmediateAssets();
        loadFontTitle();
        createProgressBarTextures();
    }

    private void loadImmediateAssets() {
        String[] candidates = {
            "ui/background.jpg",
            "ui/background.png",
            "backgrounds/menu_bg.png",
            "background.png"
        };
        for (String path : candidates) {
            if (Gdx.files.internal(path).exists()) {
                try {
                    bgTex = new Texture(Gdx.files.internal(path));
                    bgTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    Gdx.app.log("LoadingScreen", "Using background: " + path);
                    break;
                } catch (Exception e) {
                    Gdx.app.error("LoadingScreen", "Cannot load background: " + path + " -> " + e.getMessage());
                }
            }
        }
    }

    // Thay thế font tạm bằng font title đẹp
    private void loadFontTitle() {
        try {
            font = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            font.setUseIntegerPositions(false); // Cho phép float vị trí, tránh rung chữ
            font.setColor(Color.WHITE);
        } catch (Exception e) {
            Gdx.app.error("LoadingScreen", "Không load được font_title.fnt, fallback font mặc định", e);
            font = new BitmapFont();
            font.setUseIntegerPositions(false);
            font.getData().setScale(2.2f);
        }
    }

    private void createProgressBarTextures() {
        Pixmap pmBg = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pmBg.setColor(0f, 0f, 0f, 0.55f);
        pmBg.fill();
        barBgTex = new Texture(pmBg);
        pmBg.dispose();

        Pixmap pmFill = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pmFill.setColor(1f, 0.78f, 0.15f, 1f);
        pmFill.fill();
        barFillTex = new Texture(pmFill);
        pmFill.dispose();
    }

    @Override
    public void render(float delta) {
        boolean finished = game.assets.update();
        float targetProgress = game.assets.getProgress();

        displayedProgress += (targetProgress - displayedProgress) * Math.min(1f, delta * 8f);

        if (finished) {
            minShowTime -= delta;
            if (minShowTime <= 0f) {
                Gdx.app.log("LoadingScreen", "Assets loaded 100%");
                game.assets.finishLoading();
                game.onAssetsLoaded();
                return;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        drawBackground();
        drawCenteredText();
        drawProgressBar(displayedProgress);

        game.batch.end();
    }

    private void drawBackground() {
        if (bgTex != null) {
            float worldW = viewport.getWorldWidth();
            float worldH = viewport.getWorldHeight();
            float imgW = bgTex.getWidth();
            float imgH = bgTex.getHeight();
            // Tính scale để luôn phủ kín (cover)
            float scale = Math.max(worldW / imgW, worldH / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;
            float x = (worldW - drawW) / 2f;
            float y = (worldH - drawH) / 2f;
            game.batch.draw(bgTex, x, y, drawW, drawH);
        }
    }

    private void drawCenteredText() {
        String txt = "LOADING " + (int)(displayedProgress * 100) + "%";
        layout.setText(font, txt);
        float x = (viewport.getWorldWidth() - layout.width) / 2f;
        float y = (viewport.getWorldHeight() / 2f) + 70f;
        font.setColor(Color.WHITE);
        font.draw(game.batch, layout, x, y);
    }

    private void drawProgressBar(float progress) {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float barWidth = vw * BAR_WIDTH_RATIO;
        float barX = (vw - barWidth) / 2f;
        float barY = (vh / 2f) - BAR_HEIGHT / 2f;

        float outerW = barWidth;
        float outerH = BAR_HEIGHT;
        game.batch.setColor(Color.WHITE);
        game.batch.draw(barBgTex, barX, barY, outerW, outerH);

        float innerX = barX + BAR_BORDER;
        float innerY = barY + BAR_BORDER;
        float innerW = (outerW - BAR_BORDER * 2f) * progress;
        float innerH = outerH - BAR_BORDER * 2f;

        game.batch.draw(barFillTex, innerX, innerY, innerW, innerH);

        game.batch.setColor(1f, 1f, 1f, 0.25f);
        game.batch.draw(barFillTex, innerX, innerY + innerH * 0.55f, innerW, innerH * 0.45f);
        game.batch.setColor(Color.WHITE);
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if (bgTex != null) bgTex.dispose();
        if (barBgTex != null) barBgTex.dispose();
        if (barFillTex != null) barFillTex.dispose();
        if (font != null) font.dispose();
    }
}
