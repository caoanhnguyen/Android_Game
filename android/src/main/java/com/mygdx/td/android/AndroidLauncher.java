package com.mygdx.td.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mygdx.td.TDGame;

public class AndroidLauncher extends AndroidApplication {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
//        configuration.useImmersiveMode = true;
//        configuration.useAccelerometer = false;
//        configuration.useCompass = false;
//        configuration.useGyroscope = false;
//        initialize(new TDGame(new AndroidPlatformServices(this)), configuration);
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // (Có thể bật/ tắt accelerometer/compass nếu muốn)
        initialize(new TDGame(new AndroidPlatformServices(this)), config);
    }
}
