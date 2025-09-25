package com.mygdx.td.screens;

import static com.mygdx.td.Constants.VIRTUAL_HEIGHT;
import static com.mygdx.td.Constants.VIRTUAL_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.mygdx.td.TDGame;

/**
 * SplashScreen: Logo + dòng chữ "TOWER DEFENSE" với shadow.
 * KHÁC BIỆT: Không còn giữ scale font sau khi vẽ (tránh làm to font ở MainMenu).
 */
public class SplashScreen implements Screen {

    private final TDGame game;
    private final OrthographicCamera camera;
    private final StretchViewport viewport;

    private float time = 0f;

    // Thời gian
    private static final float FADE_IN  = 0.6f;
    private static final float HOLD     = 1.2f;
    private static final float FADE_OUT = 0.6f;
    private static final float TOTAL    = FADE_IN + HOLD + FADE_OUT;

    // Text
    private static final String TITLE_TEXT = "TOWER DEFENSE";
    private final GlyphLayout titleLayout = new GlyphLayout();

    // Shadow layers
    private static final ShadowLayer[] SHADOW_LAYERS = {
        new ShadowLayer(4, 3, 0.55f, 0f, 0f, 0f),
        new ShadowLayer(2, 2, 0.70f, 0f, 0f, 0f),
        new ShadowLayer(1, 1, 0.90f, 0f, 0f, 0f)
    };
    private static final Color TITLE_MAIN_COLOR = new Color(1f, 0.90f, 0.10f, 1f);

    // Animation text
    private static final float TEXT_APPEAR_DELAY = 0.15f;
    private static final float TEXT_FADE_IN_DURATION = 0.55f;
    private static final float TEXT_SLIDE_DISTANCE = 34f;
    private static final float TEXT_EXTRA_HOLD = 0f;

    private BitmapFont font;
    private float targetFontScale = 1f;
    private float originalScaleX = 1f;
    private float originalScaleY = 1f;

    public SplashScreen(TDGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(400, 240, 0);
        camera.update();

        loadFontTitle();
        prepareTitleLayout();
    }

    // Load font_title.fnt (bitmap font đẹp) trong assets/font/
    private void loadFontTitle() {
        try {
            font = new BitmapFont(Gdx.files.internal("font/font_title.fnt"));
            font.setUseIntegerPositions(false);
        } catch (Exception e) {
            Gdx.app.error("SplashScreen", "Không load được font_title.fnt, fallback font mặc định", e);
            font = new BitmapFont();
            font.setUseIntegerPositions(false);
        }
    }

    private void prepareTitleLayout() {
        originalScaleX = font.getData().scaleX;
        originalScaleY = font.getData().scaleY;

        font.getData().setScale(1f);
        titleLayout.setText(font, TITLE_TEXT);
        float baseWidth = titleLayout.width;
        float desiredWidth = viewport.getWorldWidth() * 0.70f;
        targetFontScale = desiredWidth / baseWidth;
        if (targetFontScale > 2.8f) targetFontScale = 2.8f;

        font.getData().setScale(originalScaleX, originalScaleY);
    }

    @Override
    public void render(float delta) {
        time += delta;

        float globalAlpha;
        if (time < FADE_IN) {
            globalAlpha = Interpolation.fade.apply(time / FADE_IN);
        } else if (time < FADE_IN + HOLD) {
            globalAlpha = 1f;
        } else {
            float t = (time - FADE_IN - HOLD) / FADE_OUT;
            globalAlpha = 1f - Interpolation.fade.apply(t);
        }

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Vẽ background phủ kín (cover)
        drawBackgroundCover(0.15f * globalAlpha);

        if (game.assets.logoTex != null) {
            float pulse = 1.0f + 0.05f * (float)Math.sin(time * 3f);
            float baseW = 260;
            float ratio = game.assets.logoTex.getHeight() / (float) game.assets.logoTex.getWidth();
            float baseH = baseW * ratio;
            float w = baseW * pulse;
            float h = baseH * pulse;

            float cx = viewport.getWorldWidth() / 2f;
            float cy = viewport.getWorldHeight() / 2f + 40;

            game.batch.setColor(1,1,1,globalAlpha);
            game.batch.draw(game.assets.logoTex, cx - w/2f, cy - h/2f, w, h);

            drawTitleText(globalAlpha, cx, cy - h/2f - 28);
        }

        game.batch.setColor(1,1,1,1);
        game.batch.end();

        if (time >= TOTAL + TEXT_EXTRA_HOLD) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    // Hàm mới: vẽ background kiểu cover (phủ kín, không viền đen)
    private void drawBackgroundCover(float alpha) {
        if (game.assets.menuBg != null) {
            float worldW = viewport.getWorldWidth();
            float worldH = viewport.getWorldHeight();
            float imgW = game.assets.menuBg.getWidth();
            float imgH = game.assets.menuBg.getHeight();
            float scale = Math.max(worldW / imgW, worldH / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;
            float x = (worldW - drawW) / 2f;
            float y = (worldH - drawH) / 2f;
            game.batch.setColor(1, 1, 1, alpha);
            game.batch.draw(game.assets.menuBg, x, y, drawW, drawH);
            game.batch.setColor(1, 1, 1, 1f); // reset
        }
    }

    private void drawTitleText(float globalAlpha, float centerX, float baseY) {
        float localTime = time - TEXT_APPEAR_DELAY;
        if (localTime <= 0f) return;

        float textAlpha = (localTime < TEXT_FADE_IN_DURATION)
            ? Interpolation.fade.apply(localTime / TEXT_FADE_IN_DURATION)
            : 1f;
        textAlpha *= globalAlpha;

        float slideT = Math.min(1f, localTime / TEXT_FADE_IN_DURATION);
        float easedSlide = Interpolation.sineOut.apply(slideT);
        float yOffset = -(1f - easedSlide) * TEXT_SLIDE_DISTANCE;

        font.getData().setScale(targetFontScale);
        titleLayout.setText(font, TITLE_TEXT);

        float textX = centerX - titleLayout.width / 2f;
        float textY = baseY + yOffset;

        for (ShadowLayer layer : SHADOW_LAYERS) {
            font.setColor(layer.r, layer.g, layer.b, layer.alpha * textAlpha);
            font.draw(game.batch, TITLE_TEXT, textX + layer.offsetX, textY + layer.offsetY);
        }

        font.setColor(TITLE_MAIN_COLOR.r, TITLE_MAIN_COLOR.g, TITLE_MAIN_COLOR.b, textAlpha);
        font.draw(game.batch, TITLE_TEXT, textX, textY);

        font.getData().setScale(originalScaleX, originalScaleY);
        font.setColor(1,1,1,1);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}

    private static class ShadowLayer {
        final float offsetX, offsetY, alpha, r, g, b;
        ShadowLayer(float offsetX, float offsetY, float alpha, float r, float g, float b) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.alpha = alpha;
            this.r = r; this.g = g; this.b = b;
        }
    }
}
