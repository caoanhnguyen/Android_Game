package com.mygdx.td;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Quản lý toàn bộ asset.
 * THÊM: arrowTex (mũi tên) ở projectiles/arrow/arrow.png
 */
public class Assets {
    public final AssetManager manager = new AssetManager();

    // Gameplay
    public Texture enemyTex;
    public Texture towerTex;
    public Texture bulletTex;   // (giữ nguyên nếu còn nơi dùng cũ)
    public Texture arrowTex;    // mũi tên mới
    public Texture backgroundTex;

    // UI
    public Texture btnUp;
    public Texture btnOver;
    public Texture btnDown;
    public Texture logoTex;
    public Texture musicOn;
    public Texture musicOff;
    public Texture soundOn;
    public Texture soundOff;
    public Texture menuBg;

    // Fonts
    public BitmapFont fontSmall;
    public BitmapFont fontMedium;

    // Utility
    public Texture whitePixel;

    // Sounds
    public Sound shootSound;
    public Music themeMusic;
    public Sound gameClickSound;
    public Sound laserGunSound;
    public Sound selectLevelSound;
    public Sound upgradeTowerSound;
    public Sound arrowShootSound;
    public Sound win_sound;
    public Sound lose_sound;

    public void loadAllAsync() {
        // Gameplay textures
        safeLoadTexture("enemy.png");
        safeLoadTexture("tower.png");
        safeLoadTexture("13.png"); // bullet cũ
        safeLoadTexture("projectiles/arrow/arrow.png"); // arrow
        safeLoadTexture("background.png");

        // UI textures
        safeLoadTexture("ui/button_up.png");
        safeLoadTexture("ui/button_over.png");
        safeLoadTexture("ui/button_down.png");
        safeLoadTexture("ui/logo.png");
        safeLoadTexture("ui/music_on.png");
        safeLoadTexture("ui/music_off.png");
        safeLoadTexture("ui/sound_on.png");
        safeLoadTexture("ui/sound_off.png");
        safeLoadTexture("ui/background.png");

        // Fonts
        safeLoadFont("font/font-small.fnt");

        // Sounds
        safeLoadSound("sounds/shoot.mp3");
        safeLoadMusic("sounds/themeMusic.mp3");
        safeLoadSound("sounds/game_click.wav");
        safeLoadSound("sounds/laser_gun.wav");
        safeLoadSound("sounds/select_level.wav");
        safeLoadSound("sounds/upgrade_tower.wav");
        safeLoadSound("sounds/arrow_shoot.mp3");
        safeLoadSound("sounds/win_sound.wav");
        safeLoadSound("sounds/lose_sound.wav");
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
    private void safeLoadSound(String path) {
        if (Gdx.files.internal(path).exists()) {
            manager.load(path, Sound.class);
        } else {
            Gdx.app.error("ASSETS", "Không tìm thấy file sound: " + path);
        }
    }
    private void safeLoadMusic(String path) {
        if (Gdx.files.internal(path).exists()) {
            manager.load(path, Music.class);
        } else {
            Gdx.app.error("ASSETS", "Không tìm thấy file music: " + path);
        }
    }

    public boolean update() { return manager.update(); }
    public float getProgress() { return manager.getProgress(); }

    public void finishLoading() {
        manager.finishLoading();

        enemyTex      = getTex("enemy.png");
        towerTex      = getTex("tower.png");
        bulletTex     = getTex("13.png");
        arrowTex      = getTex("projectiles/arrow/arrow.png");
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

        shootSound        = getSound("sounds/shoot.mp3");
        themeMusic        = getMusic("sounds/themeMusic.mp3");
        gameClickSound    = getSound("sounds/game_click.wav");
        laserGunSound     = getSound("sounds/laser_gun.wav");
        selectLevelSound  = getSound("sounds/select_level.wav");
        upgradeTowerSound = getSound("sounds/upgrade_tower.wav");
        arrowShootSound = getSound("sounds/arrow_shoot.mp3");
        win_sound = getSound("sounds/win_sound.wav");
        lose_sound = getSound("sounds/lose_sound.wav");

        if (menuBg == null) {
            Gdx.app.error("ASSETS", "menuBg NULL - kiểm tra tên file: ui/background.png");
        }

        applyFilters();

        if (manager.isLoaded("font/font-small.fnt")) {
            fontSmall  = manager.get("font/font-small.fnt", BitmapFont.class);
            fontMedium = fontSmall;
        } else {
            fontSmall = new BitmapFont();
            fontMedium = fontSmall;
        }

        createWhitePixel();
    }

    private Texture getTex(String path) {
        return manager.isLoaded(path) ? manager.get(path, Texture.class) : null;
    }
    private Sound getSound(String path) {
        return manager.isLoaded(path) ? manager.get(path, Sound.class) : null;
    }
    private Music getMusic(String path) {
        return manager.isLoaded(path) ? manager.get(path, Music.class) : null;
    }

    private void applyFilters() {
        // Arrow & bullet dùng Nearest để sắc nét
        Texture[] nearestList = {
            arrowTex, bulletTex, musicOn, musicOff, soundOn, soundOff
        };
        for (Texture t : nearestList) {
            if (t != null) t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        Texture[] linearList = {
            enemyTex, towerTex, backgroundTex,
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
        if (shootSound != null) shootSound.dispose();
        if (themeMusic != null) themeMusic.dispose();
        manager.dispose();
    }
}
