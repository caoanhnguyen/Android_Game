package com.mygdx.td;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Quản lý toàn bộ asset.
 * LƯU Ý: .fnt phải load bằng BitmapFont.class, không phải Texture.class.
 */
public class Assets {
    public final AssetManager manager = new AssetManager();

    // Gameplay
    public Texture enemyTex;
    public Texture towerTex;
    public Texture bulletTex;
    public Texture backgroundTex; // gameplay background (nếu có)

    // UI
    public Texture btnUp;
    public Texture btnOver;
    public Texture btnDown;
    public Texture logoTex;
    public Texture musicOn;
    public Texture musicOff;
    public Texture soundOn;
    public Texture soundOff;
    public Texture menuBg; // ui/background.jpg (background menu)

    // Fonts
    public BitmapFont fontSmall;
    public BitmapFont fontMedium;

    // Utility
    public Texture whitePixel;

    public void loadAllAsync() {
        // Gameplay textures
        safeLoadTexture("enemy.png");
        safeLoadTexture("tower.png");
        safeLoadTexture("bullet.png");
        safeLoadTexture("background.png"); // optional

        // UI textures
        safeLoadTexture("ui/button_up.png");
        safeLoadTexture("ui/button_over.png");
        safeLoadTexture("ui/button_down.png");
        safeLoadTexture("ui/logo.png");
        safeLoadTexture("ui/music_on.png");
        safeLoadTexture("ui/music_off.png");
        safeLoadTexture("ui/sound_on.png");
        safeLoadTexture("ui/sound_off.png");
        safeLoadTexture("ui/background.png"); // menu background

        // Fonts (.fnt)
        safeLoadFont("font/font-small.fnt");
        // Nếu sau này có thêm font khác thì thêm dòng tương tự.
    }

    private void safeLoadTexture(String path) {
        if (Gdx.files.internal(path).exists()) {
            manager.load(path, Texture.class);
        } else {
            Gdx.app.error("ASSETS", "Không tìm thấy file texture: " + path);
        }
    }

    private void safeLoadFont(String path) {
        if (Gdx.files.internal(path).exists()) {
            manager.load(path, BitmapFont.class);
        } else {
            Gdx.app.error("ASSETS", "Không tìm thấy file font: " + path);
        }
    }

    public boolean update() { return manager.update(); }
    public float getProgress() { return manager.getProgress(); }

    public void finishLoading() {
        manager.finishLoading();

        enemyTex      = getTex("enemy.png");
        towerTex      = getTex("tower.png");
        bulletTex     = getTex("bullet.png");
        backgroundTex = getTex("background.png");

        btnUp    = getTex("ui/button_up.png");
        btnOver  = getTex("ui/button_over.png");
        btnDown  = getTex("ui/button_down.png");
        logoTex  = getTex("ui/logo.png");
        musicOn  = getTex("ui/music_on.png");
        musicOff = getTex("ui/music_off.png");
        soundOn  = getTex("ui/sound_on.png");
        soundOff = getTex("ui/sound_off.png");
        menuBg   = getTex("ui/background.png");

        if (menuBg == null) {
            Gdx.app.error("ASSETS", "menuBg NULL - kiểm tra tên file: ui/background.png");
        }

        applyFilters();

        if (manager.isLoaded("font/font-small.fnt")) {
            fontSmall  = manager.get("font/font-small.fnt", BitmapFont.class);
            fontMedium = fontSmall;
        } else {
            // fallback nếu không có font
            fontSmall = new BitmapFont();
            fontMedium = fontSmall;
        }

        createWhitePixel();
    }

    private Texture getTex(String path) {
        return manager.isLoaded(path) ? manager.get(path, Texture.class) : null;
    }

    private void applyFilters() {
        // Pixel art nhỏ (icon) dùng Nearest để phóng không mờ
        Texture[] pixelIcons = { musicOn, musicOff, soundOn, soundOff };
        for (Texture t : pixelIcons) {
            if (t != null) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        Texture[] linearList = {
            enemyTex, towerTex, bulletTex, backgroundTex,
            btnUp, btnOver, btnDown, logoTex, menuBg
        };
        for (Texture t : linearList) {
            if (t != null) t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
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
        if (fontSmall != null) fontSmall.dispose();
        if (fontMedium != null && fontMedium != fontSmall) fontMedium.dispose();
        manager.dispose();
    }
}
