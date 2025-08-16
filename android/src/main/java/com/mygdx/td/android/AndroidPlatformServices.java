package com.mygdx.td.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.mygdx.td.PlatformServices;

public class AndroidPlatformServices implements PlatformServices {

    private final Context context;

    public AndroidPlatformServices(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void vibrate(int millis) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(millis);
            }
        } catch (Exception ignored) {}
    }
}
