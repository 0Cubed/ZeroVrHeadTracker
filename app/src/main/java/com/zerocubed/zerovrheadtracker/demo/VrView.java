package com.zerocubed.zerovrheadtracker.demo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.zerocubed.zerovrheadtracker.HeadTracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Zero
 * Created on 2018/4/11
 * VrView extending GLSurfaceView used to render vr video with HeadTracker to get head view
 */

public class VrView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = VrView.class.getSimpleName();

    private static final String VERTEX_SHADER_SOURCE =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec3 aPosition;\n" +
                    "attribute vec2 aTexCoor;\n" +
                    "uniform mat4 textureTransform;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main()\n" +
                    "{\n" +
                    "gl_Position = uMVPMatrix * vec4(aPosition,1);\n" +
                    "vTextureCoord=(textureTransform* vec4(aTexCoor, 0, 1) ).xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_SOURCE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES st;\n" +
                    "void main()\n" +
                    "{\n" +
                    "gl_FragColor= texture2D(st, vTextureCoord);\n" +
                    "}\n";

    private int mMvpMatrixLocation;
    private int mPositionLocation;
    private int mTextureTransformLocation;
    private int mTextureCoordinateLocation;

    protected int mVertexNum;
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureCoordinateBuffer;

    protected static final int VERTEX_DIM = 3;
    protected static final int TEXTURE_COORDINATE_DIM = 2;
    protected static final int BYTES_PER_FLOAT = 4;

    private SurfaceTexture mSurfaceTexture;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private boolean mSurfaceCreated;
    private boolean mSurfaceSizeInvalid;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;
    private static final float DEFAULT_FOV = 82.5f;

    private float[] mViewMatrix;
    private float[] mModelMatrix;
    private float[] mProjectionMatrix;
    private float[] mMvpMatrix;
    private float[] mVideoTextureTransformMatrix;

    private boolean mAreTrackingSensorsAvailable;

    private HeadTracker mHeadTracker;
    private Callback mCallback;

    public VrView(Context context) {
        super(context);
        init();
    }

    public VrView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mAreTrackingSensorsAvailable = areTrackingSensorsAvailable(getContext());
        mViewMatrix = new float[16];
        Matrix.setIdentityM(mViewMatrix, 0);
        mModelMatrix = new float[16];
        Matrix.setIdentityM(mModelMatrix, 0);
        mProjectionMatrix = new float[16];
        Matrix.setIdentityM(mProjectionMatrix, 0);
        mMvpMatrix = new float[16];
        Matrix.setIdentityM(mMvpMatrix, 0);
        mViewMatrix = new float[16];
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);

        mVideoTextureTransformMatrix = new float[16];
        Matrix.setIdentityM(mVideoTextureTransformMatrix, 0);

        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
    }

    public void setHeadTracker(HeadTracker headTracker) {
        mHeadTracker = headTracker;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        int program = createProgram(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
        if (program == 0) {
            throw new RuntimeException("Could not create program");
        }
        GLES20.glUseProgram(program);
        mPositionLocation = GLES20.glGetAttribLocation(program, "aPosition");
        mTextureCoordinateLocation = GLES20.glGetAttribLocation(program, "aTexCoor");
        mMvpMatrixLocation = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        mTextureTransformLocation = GLES20.glGetUniformLocation(program, "textureTransform");

        final float radius = 1.0f;

        final float vAngleSpan = 2.5f;
        final float hAngleSpan = 2.5f;

        final int vSpanNum = (int) (180.0f / vAngleSpan);
        final int hSpanNum = (int) (360.0f / hAngleSpan);

        mVertexNum = vSpanNum * hSpanNum * 6;

        float[] mVertexes = new float[mVertexNum * VERTEX_DIM];
        float[] mTextureCoordinates = new float[mVertexNum * TEXTURE_COORDINATE_DIM];

        int indexVertex = 0;
        int indexTextureCoordinate = 0;

        float vTextureSpanValue = 1.0f / vSpanNum;
        float hTextureSpanValue = 1.0f / hSpanNum;

        float hTextureBase;
        float vTextureBase;

        float x2;
        float y2;
        float z2;

        float u2;
        float v2;

        float x4;
        float y4;
        float z4;

        float u4;
        float v4;

        float hRadius;
        float hRadiusMinus;

        float sinH;
        float cosH;

        float sinHMinus;
        float cosHMinus;

        float vRadius;
        float vRadiusMinus;

        float sinV;
        float cosV;

        float sinVMinus;
        float cosVMinus;

        float len;
        float lenMinus;

        float vAngle;
        float hAngle;

        float vSpans;
        float hSpans;

        for (vAngle = 90.0f, vSpans = 0; vAngle > -90.0f; vAngle -= vAngleSpan, vSpans++) {
            for (hAngle = 450.0f, hSpans = 0; hAngle > 90.0f; hAngle -= hAngleSpan, hSpans++) {
                hTextureBase = hSpans * hTextureSpanValue;
                vTextureBase = vSpans * vTextureSpanValue;

                hRadius = (float) Math.toRadians(hAngle);
                vRadius = (float) Math.toRadians(vAngle);

                hRadiusMinus = (float) Math.toRadians(hAngle - hAngleSpan);
                vRadiusMinus = (float) Math.toRadians(vAngle - vAngleSpan);

                sinH = (float) Math.sin(hRadius);
                cosH = (float) Math.cos(hRadius);

                sinHMinus = (float) Math.sin(hRadiusMinus);
                cosHMinus = (float) Math.cos(hRadiusMinus);

                sinV = (float) Math.sin(vRadius);
                cosV = (float) Math.cos(vRadius);

                sinVMinus = (float) Math.sin(vRadiusMinus);
                cosVMinus = (float) Math.cos(vRadiusMinus);

                len = radius * cosV;
                lenMinus = radius * cosVMinus;

                //VERTEX 1
                mVertexes[indexVertex++] = len * cosH;
                mVertexes[indexVertex++] = radius * sinV;
                mVertexes[indexVertex++] = len * sinH;

                mTextureCoordinates[indexTextureCoordinate++] = 1.0f - hTextureBase;
                mTextureCoordinates[indexTextureCoordinate++] = 1.0f - vTextureBase;

                //VERTEX 2
                x2 = lenMinus * cosH;
                y2 = radius * sinVMinus;
                z2 = lenMinus * sinH;
                u2 = 1.0f - hTextureBase;
                v2 = 1.0f - vTextureBase - vTextureSpanValue;

                mVertexes[indexVertex++] = x2;
                mVertexes[indexVertex++] = y2;
                mVertexes[indexVertex++] = z2;

                mTextureCoordinates[indexTextureCoordinate++] = u2;
                mTextureCoordinates[indexTextureCoordinate++] = v2;

                //VERTEX 4
                x4 = len * cosHMinus;
                y4 = radius * sinV;
                z4 = len * sinHMinus;
                u4 = 1.0f - hTextureBase - hTextureSpanValue;
                v4 = 1.0f - vTextureBase;

                mVertexes[indexVertex++] = x4;
                mVertexes[indexVertex++] = y4;
                mVertexes[indexVertex++] = z4;

                mTextureCoordinates[indexTextureCoordinate++] = u4;
                mTextureCoordinates[indexTextureCoordinate++] = v4;

                //VERTEX 4
                mVertexes[indexVertex++] = x4;
                mVertexes[indexVertex++] = y4;
                mVertexes[indexVertex++] = z4;

                mTextureCoordinates[indexTextureCoordinate++] = u4;
                mTextureCoordinates[indexTextureCoordinate++] = v4;

                //VERTEX 2
                mVertexes[indexVertex++] = x2;
                mVertexes[indexVertex++] = y2;
                mVertexes[indexVertex++] = z2;

                mTextureCoordinates[indexTextureCoordinate++] = u2;
                mTextureCoordinates[indexTextureCoordinate++] = v2;

                //VERTEX 3
                mVertexes[indexVertex++] = lenMinus * cosHMinus;
                mVertexes[indexVertex++] = radius * sinVMinus;
                mVertexes[indexVertex++] = lenMinus * sinHMinus;

                mTextureCoordinates[indexTextureCoordinate++] = 1.0f - hTextureBase -
                        hTextureSpanValue;
                mTextureCoordinates[indexTextureCoordinate++] = 1.0f - vTextureBase -
                        vTextureSpanValue;
            }
        }

        ByteBuffer byteBufferVertexes = ByteBuffer.allocateDirect(mVertexNum * VERTEX_DIM *
                BYTES_PER_FLOAT);
        byteBufferVertexes.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBufferVertexes.asFloatBuffer();
        mVertexBuffer.put(mVertexes);
        mVertexBuffer.position(0);

        ByteBuffer byteBufferTextureCoordinates = ByteBuffer.allocateDirect(mVertexNum *
                TEXTURE_COORDINATE_DIM * BYTES_PER_FLOAT);
        byteBufferTextureCoordinates.order(ByteOrder.nativeOrder());
        mTextureCoordinateBuffer = byteBufferTextureCoordinates.asFloatBuffer();
        mTextureCoordinateBuffer.put(mTextureCoordinates);
        mTextureCoordinateBuffer.position(0);

        mSurfaceCreated = true;
        mSurfaceTexture = new SurfaceTexture(createTexture());

        if (mCallback != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onSurfaceCreated(new Surface(mSurfaceTexture));
                }
            });
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, final int width, final int height) {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "invalid surface size: " + width + ", " + height);
            mSurfaceSizeInvalid = true;
            return;
        } else {
            mSurfaceSizeInvalid = false;
        }

        if ((width == mSurfaceWidth) && (height == mSurfaceHeight)) {
            return;
        }

        mSurfaceWidth = width;
        mSurfaceHeight = height;

        Matrix.perspectiveM(mProjectionMatrix, 0, DEFAULT_FOV, (float) mSurfaceWidth /
                mSurfaceHeight, Z_NEAR, Z_FAR);

        if (mCallback != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onSurfaceChanged(width, height);
                }
            });
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (!mSurfaceCreated || mSurfaceSizeInvalid) {
            return;
        }

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mVideoTextureTransformMatrix);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        GLES20.glScissor(0, 0, mSurfaceWidth, mSurfaceHeight);

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (mAreTrackingSensorsAvailable) {
            mHeadTracker.getLastHeadView(mModelMatrix, 0);
        }

        Matrix.multiplyMM(mMvpMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mMvpMatrix, 0);

        GLES20.glUniformMatrix4fv(mMvpMatrixLocation, 1, false, mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(mTextureTransformLocation, 1, false,
                mVideoTextureTransformMatrix, 0);

        GLES20.glVertexAttribPointer(mPositionLocation, VERTEX_DIM, GLES20.GL_FLOAT, false,
                VERTEX_DIM * BYTES_PER_FLOAT, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoordinateLocation, TEXTURE_COORDINATE_DIM,
                GLES20.GL_FLOAT, false, TEXTURE_COORDINATE_DIM * BYTES_PER_FLOAT,
                mTextureCoordinateBuffer);

        GLES20.glEnableVertexAttribArray(mPositionLocation);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateLocation);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexNum);
    }

    public void addCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onSurfaceCreated(Surface surface);

        void onSurfaceChanged(int width, int height);
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexShaderSrc, String fragmentShaderSrc) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc);
        if (vertexShader == 0) {
            return 0;
        }

        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSrc);
        if (fragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            clearGLError();
            GLES20.glAttachShader(program, vertexShader);
            checkGLError("Attach Vertex Shader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGLError("Attach Fragment Shader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: " + program);
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void clearGLError() {
        while (GLES20.glGetError() != 0) ;
    }

    private void checkGLError(String operation) {
        int error = GLES20.glGetError();
        if (error != 0) {
            String errorInfo = operation + ": glError " + error;
            Log.e(TAG, errorInfo);
            throw new RuntimeException(errorInfo);
        }
    }

    private int createTexture() {
        int[] textureIds = {0};
        clearGLError();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textureIds, 0);
        checkGLError("Generate texture");
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0]);
        checkGLError("Bind texture");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        return textureIds[0];
    }

    private boolean areTrackingSensorsAvailable(Context context) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) && pm
                        .hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
            }
        }

        return false;
    }
}