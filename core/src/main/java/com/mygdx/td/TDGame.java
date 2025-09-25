package com.mygdx.td;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.mygdx.td.screens.GameScreen;
import com.mygdx.td.screens.LoadingScreen;
import com.mygdx.td.screens.SplashScreen;

/**
 * TDGame central class.
 */
public class TDGame extends Game {
    public SpriteBatch batch;
    public final PlatformServices platform;
    public final Assets assets = new Assets();
    public boolean soundEnabled = true;
    public boolean musicEnabled = true;
    public float musicVolume = 1.0f;
    public float soundVolume = 1.0f;

    private boolean assetsFinished = false;

    public TDGame(PlatformServices platform) {
        this.platform = platform;
        // KHÔNG gọi loadAudioSettings() ở đây!
    }

    // Load audio settings from Preferences
    public void loadAudioSettings() {
        Preferences prefs = Gdx.app.getPreferences("td_settings");
        musicEnabled  = prefs.getBoolean("musicEnabled", true);
        soundEnabled  = prefs.getBoolean("soundEnabled", true);
        musicVolume   = prefs.getFloat("musicVolume", 1.0f);
        soundVolume   = prefs.getFloat("soundVolume", 1.0f);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        loadAudioSettings(); // <-- Đúng, gọi ở đây!
        assets.loadAllAsync();
        setScreen(new LoadingScreen(this)); // màn hình loading hiện tiến trình
    }

    public void onAssetsLoaded() {
        if (assetsFinished) return;
        assets.finishLoading(); // rất quan trọng để tạo whitePixel + lấy texture
        assetsFinished = true;

        // Áp dụng setting nhạc/sound từ Preferences khi bắt đầu game
        applyAudioSettings();

        setScreen(new SplashScreen(this));
    }

    // Áp dụng trạng thái nhạc/sound và volume đúng theo setting
    public void applyAudioSettings() {
        if (assets.themeMusic != null) {
            assets.themeMusic.setLooping(true);
            assets.themeMusic.setVolume(musicEnabled ? musicVolume : 0f);
            if (musicEnabled) {
                if (!assets.themeMusic.isPlaying()) {
                    assets.themeMusic.play();
                }
            } else {
                if (assets.themeMusic.isPlaying()) {
                    assets.themeMusic.pause();
                }
            }
        }
        // Sound effect sẽ dùng soundEnabled và soundVolume ở nơi play sound
    }

    public void startGame() {
        // Khi vào GameScreen, dừng nhạc nền
        if (assets.themeMusic != null && assets.themeMusic.isPlaying()) {
            assets.themeMusic.pause();
        }
        setScreen(new GameScreen(this));
    }

    public void startGameWithLevel(int level) {
        if (assets.themeMusic != null && assets.themeMusic.isPlaying()) {
            assets.themeMusic.pause();
        }
        setScreen(new GameScreen(this, level));
    }

    public void resumeThemeMusic() {
        // Khi quay về menu, splash,... phát lại theme music đúng volume/toggle
        if (assets.themeMusic != null) {
            assets.themeMusic.setLooping(true);
            assets.themeMusic.setVolume(musicEnabled ? musicVolume : 0f);
            if (musicEnabled && !assets.themeMusic.isPlaying()) {
                assets.themeMusic.play();
            }
            if (!musicEnabled && assets.themeMusic.isPlaying()) {
                assets.themeMusic.pause();
            }
        }
    }

    public void playSound(Sound sound) {
        if (soundEnabled && sound != null) {
            sound.play(soundVolume);
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (batch != null) batch.dispose();
        assets.dispose();
    }
}
