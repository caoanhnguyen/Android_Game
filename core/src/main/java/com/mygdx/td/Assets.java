package com.mygdx.td;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Phiên bản đơn giản: chỉ load texture cơ bản giống bản cũ + tạo whitePixel & shapeRenderer.
 * KHÔNG dùng FreeType để tránh lỗi native nếu chưa cấu hình gdx-freetype.
 */
public class Assets {
    public final AssetManager manager = new AssetManager();

    public Texture enemyTex;
    public Texture towerTex;
    public Texture bulletTex;
    public Texture backgroundTex;

    public BitmapFont fontSmall;
    public BitmapFont fontMedium;

    // tiện cho HUD & debug
    public Texture whitePixel;
    public ShapeRenderer shapeRenderer;

    public void loadAllAsync() {
        manager.load("enemy.png", Texture.class);
        manager.load("tower.png", Texture.class);
        manager.load("bullet.png", Texture.class);
        if (Gdx.files.internal("background.png").exists()) {
            manager.load("background.png", Texture.class);
        }
        manager.load("font/font-small.fnt", BitmapFont.class); // thêm dòng này
    }

    public boolean update() {
        return manager.update();
    }

    public float getProgress() {
        return manager.getProgress();
    }

    public void finishLoading() {
        manager.finishLoading();

        enemyTex  = safeGet("enemy.png");
        towerTex  = safeGet("tower.png");
        bulletTex = safeGet("bullet.png");
        if (manager.isLoaded("background.png")) {
            backgroundTex = manager.get("background.png", Texture.class);
        }
        setLinear(enemyTex);
        setLinear(towerTex);
        setLinear(bulletTex);
        setLinear(backgroundTex);

        // Load font bitmap nếu có, nếu không thì fallback mặc định
        if (manager.isLoaded("font/font-small.fnt")) {
            fontSmall  = manager.get("font/font-small.fnt", BitmapFont.class);
            fontMedium = fontSmall; // hoặc load thêm font khác nếu muốn
        } else {
            fontSmall  = new BitmapFont();
            fontMedium = new BitmapFont();
        }

        createWhitePixel();
        shapeRenderer = new ShapeRenderer();
    }
    private Texture safeGet(String name) {
        return manager.isLoaded(name) ? manager.get(name, Texture.class) : null;
    }

    private void setLinear(Texture t) {
        if (t != null) t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void createWhitePixel() {
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(1,1,1,1);
        pm.fill();
        whitePixel = new Texture(pm);
        pm.dispose();
    }

    public TextureRegion getWhiteRegion() {
        return whitePixel == null ? null : new TextureRegion(whitePixel);
    }

    public void dispose() {
        if (whitePixel != null) whitePixel.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (fontSmall != null)  fontSmall.dispose();
        if (fontMedium != null) fontMedium.dispose();
        manager.dispose();
    }
}
