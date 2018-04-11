package com.zerocubed.zerovrheadtracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 */

public class DeviceSensorLooper implements SensorEventProvider {
    private static final String LOG_TAG = DeviceSensorLooper.class.getSimpleName();
    private boolean isRunning;
    private SensorManager sensorManager;
    private Looper sensorLooper;
    private SensorEventListener sensorEventListener;
    private final ArrayList<SensorEventListener> registeredListeners = new
            ArrayList<SensorEventListener>();

    public DeviceSensorLooper(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    private Sensor getUncalibratedGyro() {
        return Build.MANUFACTURER.equals("HTC") ? null : this.sensorManager.getDefaultSensor
                (Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
    }

    public void start() {
        if (!isRunning) {
            sensorEventListener = new SensorEventListener() {
                public void onSensorChanged(SensorEvent event) {
                    synchronized (registeredListeners) {
                        Iterator listenerIterator = registeredListeners.iterator();

                        while (listenerIterator.hasNext()) {
                            SensorEventListener listener = (SensorEventListener) listenerIterator
                                    .next();
                            listener.onSensorChanged(event);
                        }
                    }
                }

                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    synchronized (registeredListeners) {
                        Iterator listenerIterator = registeredListeners.iterator();

                        while (listenerIterator.hasNext()) {
                            SensorEventListener listener = (SensorEventListener) listenerIterator
                                    .next();
                            listener.onAccuracyChanged(sensor, accuracy);
                        }
                    }
                }
            };
            HandlerThread sensorThread = new HandlerThread("sensor") {
                protected void onLooperPrepared() {
                    Handler handler = new Handler(Looper.myLooper());
                    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor
                            .TYPE_ACCELEROMETER);
                    sensorManager.registerListener(sensorEventListener, accelerometer,
                            SensorManager.SENSOR_DELAY_FASTEST, handler);
                    Sensor gyroscope = getUncalibratedGyro();
                    if (gyroscope == null) {
                        Log.i(LOG_TAG, "Uncalibrated gyroscope unavailable, default to regular " +
                                "gyroscope.");
                        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    }

                    sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager
                            .SENSOR_DELAY_FASTEST, handler);
                }
            };
            sensorThread.start();
            sensorLooper = sensorThread.getLooper();
            isRunning = true;
        }
    }

    public void stop() {
        if (isRunning) {
            sensorManager.unregisterListener(sensorEventListener);
            sensorEventListener = null;
            sensorLooper.quit();
            sensorLooper = null;
            isRunning = false;
        }
    }

    public void registerListener(SensorEventListener listener) {
        synchronized (registeredListeners) {
            registeredListeners.add(listener);
        }
    }

    public void unregisterListener(SensorEventListener listener) {
        synchronized (registeredListeners) {
            registeredListeners.remove(listener);
        }
    }
}



