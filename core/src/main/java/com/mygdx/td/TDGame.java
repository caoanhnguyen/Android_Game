package com.mygdx.td;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.td.screens.GameScreen;
import com.mygdx.td.screens.LoadingScreen;

public class TDGame extends Game {
    public SpriteBatch batch;
    public final PlatformServices platform;
    public final Assets assets = new Assets();

    public TDGame(PlatformServices platform) {
        this.platform = platform;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets.loadAllAsync();
        setScreen(new LoadingScreen(this));
    }

    public void onAssetsLoaded() {
        setScreen(new GameScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (batch != null) batch.dispose();
        assets.dispose();
    }
}
