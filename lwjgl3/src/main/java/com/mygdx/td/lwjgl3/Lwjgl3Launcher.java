package com.mygdx.td.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.td.PlatformServices;
import com.mygdx.td.TDGame;

public class Lwjgl3Launcher {

    private static class DesktopPlatformServices implements PlatformServices {
        @Override public void vibrate(int millis) {
            // Desktop: bỏ qua
        }
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tower Defense");
        config.setWindowedMode(960, 540);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new TDGame(new DesktopPlatformServices()), config);
    }
}
