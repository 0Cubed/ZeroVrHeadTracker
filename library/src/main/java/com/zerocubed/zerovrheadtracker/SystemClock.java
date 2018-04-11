package com.zerocubed.zerovrheadtracker;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 */

public class SystemClock implements Clock {
    public SystemClock() {
    }

    public long nanoTime() {
        return System.nanoTime();
    }
}
