package com.zerocubed.zerovrheadtracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.view.Display;
import android.view.WindowManager;

import java.util.concurrent.TimeUnit;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 * Made some modifications and optimizations
 */

public class HeadTracker implements SensorEventListener {
    private static final float DEFAULT_NECK_HORIZONTAL_OFFSET = 0.08F;
    private static final float DEFAULT_NECK_VERTICAL_OFFSET = 0.075F;
    private static final float DEFAULT_NECK_MODEL_FACTOR = 1.0F;
    private static final float PREDICTION_TIME_IN_SECONDS = 0.0F; //modified by Zero
    private final Display display;
    private final float[] ekfToHeadTracker = new float[16];
    private final float[] sensorToDisplay = new float[16];
    private float displayRotation = 0.0F / 0.0F;
    private final float[] neckModelTranslation = new float[16];
    private final float[] tmpHeadView = new float[16];
    private final float[] tmpHeadView2 = new float[16];
    private float neckModelFactor = 1.0F;
    private final Object neckModelFactorMutex = new Object();
    private volatile boolean tracking;
    private final OrientationEKF tracker;
    private final Object gyroBiasEstimatorMutex = new Object();
    private final Object orientationMutex = new Object();
    private GyroscopeBiasEstimator gyroBiasEstimator;
    private SensorEventProvider sensorEventProvider;
    private Clock clock;
    private long latestGyroEventClockTimeNs;
    private volatile boolean firstGyroValue = true;
    private float[] initialSystemGyroBias = new float[3];
    private final Vector3d gyroBias = new Vector3d();
    private final Vector3d latestGyro = new Vector3d();
    private final Vector3d latestAcc = new Vector3d();
    private OnSensorDataUpdatedListener mOnSensorDataUpdatedListener;

    //added by Zero
    private float orientation = 0.0F;
    private float tempOrientation = 0.0F;
    private float initOrientation = 0.0F;
    private volatile boolean initOrientationGot = false;

    public static HeadTracker createFromContext(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context
                .SENSOR_SERVICE);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        return new HeadTracker(new DeviceSensorLooper(sensorManager), new SystemClock(), display);
    }

    public HeadTracker(SensorEventProvider sensorEventProvider, Clock clock, Display display) {
        this.clock = clock;
        this.sensorEventProvider = sensorEventProvider;
        this.tracker = new OrientationEKF();
        this.display = display;
        this.gyroBiasEstimator = new GyroscopeBiasEstimator();
        Matrix.setIdentityM(this.neckModelTranslation, 0);
    }

    public void setOnSensorDataUpdatedListener(OnSensorDataUpdatedListener listener) {
        mOnSensorDataUpdatedListener = listener;
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            latestAcc.set((double) event.values[0], (double) event.values[1], (double) event
                    .values[2]);
            tracker.processAcc(latestAcc, event.timestamp);
            synchronized (gyroBiasEstimatorMutex) {
                if (gyroBiasEstimator != null) {
                    gyroBiasEstimator.processAccelerometer(latestAcc, event.timestamp);
                }
            }

            //added by Zero
            synchronized (orientationMutex) {
                tempOrientation = 0.0F;
                float X = -event.values[0];
                float Y = -event.values[1];
                float Z = -event.values[2];
                float magnitude = X * X + Y * Y;
                // Don't trust the angle if the magnitude is small compared to the y value
                if (magnitude * 4 >= Z * Z) {
                    float OneEightyOverPi = 57.29577957855f;
                    float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                    tempOrientation = 90.0F - angle;
                    // normalize to 0 - 359 range
                    while (tempOrientation >= 360.0F) {
                        tempOrientation -= 360.0F;
                    }
                    while (tempOrientation < 0.0F) {
                        tempOrientation += 360.0F;
                    }
                }
                orientation = tempOrientation;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE || event.sensor.getType() ==
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            latestGyroEventClockTimeNs = clock.nanoTime();
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
                if (firstGyroValue && event.values.length == 6) {
                    initialSystemGyroBias[0] = event.values[3];
                    initialSystemGyroBias[1] = event.values[4];
                    initialSystemGyroBias[2] = event.values[5];
                }

                latestGyro.set((double) (event.values[0] - initialSystemGyroBias[0]), (double)
                        (event.values[1] - initialSystemGyroBias[1]), (double) (event.values[2] -
                        initialSystemGyroBias[2]));
            } else {
                latestGyro.set((double) event.values[0], (double) event.values[1], (double) event
                        .values[2]);
            }

            firstGyroValue = false;
            synchronized (gyroBiasEstimatorMutex) {
                if (gyroBiasEstimator != null) {
                    gyroBiasEstimator.processGyroscope(latestGyro, event.timestamp);
                    gyroBiasEstimator.getGyroBias(gyroBias);
                    Vector3d.sub(latestGyro, gyroBias, latestGyro);
                }
            }

            tracker.processGyro(latestGyro, event.timestamp);
        }

        if (mOnSensorDataUpdatedListener != null) {
            mOnSensorDataUpdatedListener.onSensorDataUpdated();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startTracking() {
        if (!tracking) {
            tracker.reset();
            synchronized (gyroBiasEstimatorMutex) {
                if (gyroBiasEstimator != null) {
                    gyroBiasEstimator.reset();
                }
            }

            initOrientationGot = false;
            firstGyroValue = true;
            sensorEventProvider.registerListener(this);
            sensorEventProvider.start();
            tracking = true;
        }
    }

    public void resetTracker() {
        tracker.reset();
    }

    public void stopTracking() {
        if (tracking) {
            sensorEventProvider.unregisterListener(this);
            sensorEventProvider.stop();
            tracking = false;
        }
    }

    public void setNeckModelEnabled(boolean enabled) {
        if (enabled) {
            setNeckModelFactor(DEFAULT_NECK_MODEL_FACTOR);
        } else {
            setNeckModelFactor(0.0F);
        }

    }

    public float getNeckModelFactor() {
        synchronized (neckModelFactorMutex) {
            return neckModelFactor;
        }
    }

    public void setNeckModelFactor(float factor) {
        synchronized (neckModelFactorMutex) {
            if (factor >= 0.0F && factor <= 1.0F) {
                neckModelFactor = factor;
            } else {
                throw new IllegalArgumentException("factor should be within [0.0, 1.0]");
            }
        }
    }

    public void getLastHeadView(float[] headView, int offset) {
        if (offset + 16 > headView.length) {
            throw new IllegalArgumentException("Not enough space to write the result");
        } else {
            float rotation = 0.0F;
            switch (display.getRotation()) {
                case 0:
                    rotation = 0.0F;
                    break;
                case 1:
                    rotation = 90.0F;
                    break;
                case 2:
                    rotation = 180.0F;
                    break;
                case 3:
                    rotation = 270.0F;
            }

            if (rotation != displayRotation) {
                displayRotation = rotation;
                Matrix.setRotateEulerM(sensorToDisplay, 0, 0.0F, 0.0F, -rotation);
                Matrix.setRotateEulerM(ekfToHeadTracker, 0, -90.0F, 0.0F, rotation);
            }

            synchronized (tracker) {
                if (!tracker.isReady()) {
                    return;
                }

                double secondsSinceLastGyroEvent = (double) TimeUnit.NANOSECONDS.toSeconds(clock
                        .nanoTime() - latestGyroEventClockTimeNs);
                double secondsToPredictForward = secondsSinceLastGyroEvent +
                        PREDICTION_TIME_IN_SECONDS;
                double[] mat = tracker.getPredictedGLMatrix(secondsToPredictForward);
                int i = 0;

                while (true) {
                    if (i >= headView.length) {
                        break;
                    }

                    tmpHeadView[i] = (float) mat[i];
                    ++i;
                }
            }

            //modified by Zero
            Matrix.multiplyMM(tmpHeadView2, 0, sensorToDisplay, 0, tmpHeadView, 0);
            Matrix.multiplyMM(headView, offset, tmpHeadView2, 0, ekfToHeadTracker, 0);
            //System.arraycopy(tmpHeadView, 0, headView, 0, 16);
            Matrix.setIdentityM(neckModelTranslation, 0);
            Matrix.translateM(neckModelTranslation, 0, 0.0F, -neckModelFactor *
                    DEFAULT_NECK_VERTICAL_OFFSET, neckModelFactor * DEFAULT_NECK_HORIZONTAL_OFFSET);
            Matrix.multiplyMM(tmpHeadView, 0, neckModelTranslation, 0, headView, offset);
            Matrix.translateM(headView, offset, tmpHeadView, 0, 0.0F, neckModelFactor *
                    DEFAULT_NECK_VERTICAL_OFFSET, 0.0F);

            //added by Zero
            if (!initOrientationGot) {
                initOrientationGot = true;
                initOrientation = orientation;
            }
            Matrix.rotateM(headView, 0, rotation + initOrientation, 0, 1, 0);
        }
    }

    Matrix3x3d getCurrentPoseForTest() {
        return new Matrix3x3d(tracker.getRotationMatrix());
    }

    void setGyroBiasEstimator(GyroscopeBiasEstimator estimator) {
        synchronized (gyroBiasEstimatorMutex) {
            gyroBiasEstimator = estimator;
        }
    }

    //added by Zero
    public interface OnSensorDataUpdatedListener {
        void onSensorDataUpdated();
    }

    public int getDisplayRotation() {
        return display.getRotation();
    }

    public float getDeviceRotation() {
        synchronized (orientationMutex) {
            return orientation;
        }
    }

    public void reset() {
        tracker.reset();
        synchronized (gyroBiasEstimatorMutex) {
            if (gyroBiasEstimator != null) {
                gyroBiasEstimator.reset();
            }
        }

        initOrientationGot = false;
        firstGyroValue = true;
    }
}
