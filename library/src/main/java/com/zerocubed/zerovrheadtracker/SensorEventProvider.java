package com.zerocubed.zerovrheadtracker;

import android.hardware.SensorEventListener;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 */

public interface SensorEventProvider {
    void start();

    void stop();

    void registerListener(SensorEventListener var1);

    void unregisterListener(SensorEventListener var1);
}
