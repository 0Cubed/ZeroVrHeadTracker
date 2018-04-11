package com.zerocubed.zerovrheadtracker;

import java.util.concurrent.TimeUnit;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 */

public class LowPassFilter {
    private static final double NANOS_TO_SECONDS;
    private final double timeConstantSecs;
    private final Vector3d filteredData = new Vector3d();
    private long lastTimestampNs;
    private int numSamples;
    private final Vector3d temp = new Vector3d();

    public LowPassFilter(double cutoffFrequency) {
        timeConstantSecs = 1.0D / (Math.PI * 2.0D * cutoffFrequency);
    }

    public int getNumSamples() {
        return numSamples;
    }

    public void addSample(Vector3d sampleData, long timestampNs) {
        addWeightedSample(sampleData, timestampNs, 1.0D);
    }

    public void addWeightedSample(Vector3d sampleData, long timestampNs, double weight) {
        ++numSamples;
        if (numSamples == 1) {
            filteredData.set(sampleData);
            lastTimestampNs = timestampNs;
        } else {
            double weightedDeltaSecs = weight * (double) (timestampNs - lastTimestampNs) *
                    NANOS_TO_SECONDS;
            double alpha = weightedDeltaSecs / (timeConstantSecs + weightedDeltaSecs);
            filteredData.scale(1.0D - alpha);
            temp.set(sampleData);
            temp.scale(alpha);
            Vector3d.add(temp, filteredData, filteredData);
            lastTimestampNs = timestampNs;
        }
    }

    public Vector3d getFilteredData() {
        return filteredData;
    }

    static {
        NANOS_TO_SECONDS = 1.0D / (double) TimeUnit.NANOSECONDS.convert(1L, TimeUnit.SECONDS);
    }
}

