package com.zerocubed.zerovrheadtracker;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Based on cardboard.jar in gvr-android-sdk
 */

public class MathUtil {
    public static float getYawAngle(float matrix[]) {
        if (matrix == null || matrix.length < 16) {
            return 0.0f;
        }

        if (Math.sqrt((double) (1.0F - matrix[6] * matrix[6])) >= 0.01f) {
            return (float) Math.toDegrees(-Math.atan2((double) (-matrix[2]), (double) matrix[10]));
        } else {
            return 0.0f;
        }
    }

    public static void getQuaternionFromMatrix(float[] matrix, float[] quaternion) {
        float[] m = matrix;
        float t = m[0] + m[5] + m[10];
        float x;
        float y;
        float z;
        float w;
        float s;
        if (t >= 0.0F) {
            s = (float) Math.sqrt((double) (t + 1.0F));
            w = 0.5F * s;
            s = 0.5F / s;
            x = (m[9] - m[6]) * s;
            y = (m[2] - m[8]) * s;
            z = (m[4] - m[1]) * s;
        } else if (m[0] > m[5] && m[0] > m[10]) {
            s = (float) Math.sqrt((double) (1.0F + m[0] - m[5] - m[10]));
            x = s * 0.5F;
            s = 0.5F / s;
            y = (m[4] + m[1]) * s;
            z = (m[2] + m[8]) * s;
            w = (m[9] - m[6]) * s;
        } else if (m[5] > m[10]) {
            s = (float) Math.sqrt((double) (1.0F + m[5] - m[0] - m[10]));
            y = s * 0.5F;
            s = 0.5F / s;
            x = (m[4] + m[1]) * s;
            z = (m[9] + m[6]) * s;
            w = (m[2] - m[8]) * s;
        } else {
            s = (float) Math.sqrt((double) (1.0F + m[10] - m[0] - m[5]));
            z = s * 0.5F;
            s = 0.5F / s;
            x = (m[2] + m[8]) * s;
            y = (m[9] + m[6]) * s;
            w = (m[4] - m[1]) * s;
        }

        quaternion[0] = x;
        quaternion[1] = y;
        quaternion[2] = z;
        quaternion[3] = w;
    }

    public static void getMatrixFromQuaternion(float[] quaternion, float[] matrix) {
        float x2, y2, z2, xx, xy, xz, yy, yz, zz, wx, wy, wz;

        x2 = quaternion[0] + quaternion[0];
        y2 = quaternion[1] + quaternion[1];
        z2 = quaternion[2] + quaternion[2];

        xx = quaternion[0] * x2;
        xy = quaternion[0] * y2;
        xz = quaternion[0] * z2;
        yy = quaternion[1] * y2;
        yz = quaternion[1] * z2;
        zz = quaternion[2] * z2;
        wx = quaternion[3] * x2;
        wy = quaternion[3] * y2;
        wz = quaternion[3] * z2;

        matrix[0] = 1.0f - (yy + zz);
        matrix[1] = xy - wz;
        matrix[2] = xz + wy;
        matrix[3] = 0.0f;

        matrix[4] = xy + wz;
        matrix[5] = 1.0f - (xx + zz);
        matrix[6] = yz - wx;
        matrix[7] = 0.0f;

        matrix[8] = xz - wy;
        matrix[9] = yz + wx;
        matrix[10] = 1.0f - (xx + yy);
        matrix[11] = 0.0f;

        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = 0.0f;
        matrix[15] = 1.0f;
    }

    public static void lerp(float[] startQuaternion, float[] endQuaternion, float[]
            resultQuaternion, float t) {
        if (t < 0.0f) {
            t = 0.0f;
        }

        if (t > 1.0f) {
            t = 1.0f;
        }
        float t1 = 1.0f - t;
        resultQuaternion[0] = t1 * startQuaternion[0] + t * endQuaternion[0];
        resultQuaternion[1] = t1 * startQuaternion[1] + t * endQuaternion[1];
        resultQuaternion[2] = t1 * startQuaternion[2] + t * endQuaternion[2];
        resultQuaternion[3] = t1 * startQuaternion[3] + t * endQuaternion[3];
    }

    public static void slerp(float[] startQuaternion, float[] endQuaternion, float[]
            resultQuaternion, float t) {
        if (t < 0.0f) {
            t = 0.0f;
        }

        if (t > 1.0f) {
            t = 1.0f;
        }

        if (startQuaternion[0] == endQuaternion[0] && startQuaternion[1] == endQuaternion[1] &&
                startQuaternion[2] == endQuaternion[2] && startQuaternion[3] == endQuaternion[3]) {
            resultQuaternion[0] = startQuaternion[0];
            resultQuaternion[1] = startQuaternion[1];
            resultQuaternion[2] = startQuaternion[2];
            resultQuaternion[3] = startQuaternion[3];
            return;
        }


        float result = startQuaternion[0] * endQuaternion[0] + startQuaternion[1] * endQuaternion[1]
                + startQuaternion[2] * endQuaternion[2] + startQuaternion[3] * endQuaternion[3];

        if (result < 0.0) {
            // Negate the second quaternion and the result of the dot product
            endQuaternion[0] = -endQuaternion[0];
            endQuaternion[1] = -endQuaternion[1];
            endQuaternion[2] = -endQuaternion[2];
            endQuaternion[3] = -endQuaternion[3];
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t;
        float scale1 = t;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double theta = Math.acos(result);
            final double invSinTheta = 1f / Math.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (float) (Math.sin((1 - t) * theta) * invSinTheta);
            scale1 = (float) (Math.sin((t * theta)) * invSinTheta);
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        resultQuaternion[0] = (scale0 * startQuaternion[0]) + (scale1 * endQuaternion[0]);
        resultQuaternion[1] = (scale0 * startQuaternion[1]) + (scale1 * endQuaternion[1]);
        resultQuaternion[2] = (scale0 * startQuaternion[2]) + (scale1 * endQuaternion[2]);
        resultQuaternion[3] = (scale0 * startQuaternion[3]) + (scale1 * endQuaternion[3]);
    }
}
