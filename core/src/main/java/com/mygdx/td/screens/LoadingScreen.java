package com.mygdx.td.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.mygdx.td.TDGame;

public class LoadingScreen implements Screen {
    private final TDGame game;
    private float minShowTime = 0.3f; // tránh blink

    public LoadingScreen(TDGame game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        if (game.assets.update()) {
            minShowTime -= delta;
            if (minShowTime <= 0) {
                Gdx.app.log("LoadingScreen", "Assets loaded: " +
                    "fontSmall=" + (game.assets.fontSmall != null) +
                    ", fontMedium=" + (game.assets.fontMedium != null));
                game.assets.finishLoading();
                game.onAssetsLoaded();
                return;
            }
        }
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        String text = "Loading... " + Math.round(game.assets.manager.getProgress() * 100) + "%";
        if (game.assets.fontMedium != null)
            game.assets.fontMedium.draw(game.batch, text, 40, 120);
        else
            new BitmapFont().draw(game.batch, "LOADING...", 40, 120);
        game.batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}
}
