package com.mygdx.td;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.td.screens.GameScreen;
import com.mygdx.td.screens.LoadingScreen;
import com.mygdx.td.screens.SplashScreen;

/**
 * TDGame central class.
 *
 * Flow:
 *  1) create(): start loading assets async, show LoadingScreen
 *  2) LoadingScreen polls assets.update(); when done it calls game.onAssetsLoaded()
 *  3) onAssetsLoaded(): finishLoading() -> show SplashScreen
 *  4) SplashScreen tự chuyển sang MainMenuScreen (trong code màn Splash đã gửi)
 *  5) MainMenuScreen khi bấm START gọi game.startGame()
 */
public class TDGame extends Game {
    public SpriteBatch batch;
    public final PlatformServices platform;
    public final Assets assets = new Assets();

    private boolean assetsFinished = false;

    public TDGame(PlatformServices platform) {
        this.platform = platform;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets.loadAllAsync();
        setScreen(new LoadingScreen(this)); // màn hình loading hiện tiến trình
    }

    /**
     * Được gọi bởi LoadingScreen khi AssetManager báo đã load xong (manager.update() == true).
     * Hoàn tất nạp (finishLoading) tạo whitePixel, font... sau đó chuyển sang SplashScreen.
     */
    public void onAssetsLoaded() {
        if (assetsFinished) return;
        assets.finishLoading(); // rất quan trọng để tạo whitePixel + lấy texture
        assetsFinished = true;
        setScreen(new SplashScreen(this));
    }

    /**
     * Được MainMenuScreen hoặc SplashScreen (sau khi splash) gọi khi người chơi chọn START.
     */
    public void startGame() {
        setScreen(new GameScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (batch != null) batch.dispose();
        assets.dispose();
    }
}
