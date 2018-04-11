package com.zerocubed.zerovrheadtracker.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.zerocubed.zeromediaplayer.ZeroMediaPlayer;
import com.zerocubed.zeromediaplayer.android.ZeroAndroidMediaPlayer;
import com.zerocubed.zeromediaplayer.exo.ZeroExoPlayer;
import com.zerocubed.zeromediaplayer.exo2.ZeroExoPlayer2;
import com.zerocubed.zeromediaplayer.ijk.ZeroIjkPlayer;
import com.zerocubed.zerovrheadtracker.HeadTracker;

/**
 * Created by Zero
 * Created on 2018/04/11
 * ZeroMediaPlayer(combined with VrView as well as HeadTracker to support vr playback) Activity
 * Copied from ZeroMediaPlayer: https://github.com/0Cubed/ZeroMediaPlayer with some modifications
 */

public class PlayerActivity extends AppCompatActivity implements VrView.Callback {
    private static final String TAG = PlayerActivity.class.getSimpleName();

    private AspectRatioFrameLayout mAspectRatioFrameLayout;
    private ZeroMediaPlayer mPlayer;
    private AspectRatioFrameLayout mAspectRatioFrameLayoutPlayer;
    private VrView mVrView;
    private PlayerController mPlayerController;
    private ImageView mImageViewLoading;
    private String mFilePath;
    private String mPlayerCore;
    private int mScreenWidth;
    private int mScreenHeight;
    private HeadTracker mHeadTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.ALPHA_CHANGED);

        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        mFilePath = intent.getStringExtra("filePath");
        mPlayerCore = intent.getStringExtra("playerCore");

        mPlayerController = (PlayerController) findViewById(R.id.playerController);
        mImageViewLoading = (ImageView) findViewById(R.id.ivLoading);

        mHeadTracker = HeadTracker.createFromContext(this);

        mVrView = (VrView) findViewById(R.id.svPlayer);
        mVrView.addCallback(this);
        mVrView.setHeadTracker(mHeadTracker);

        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = Math.max(outMetrics.widthPixels, outMetrics.heightPixels);
        mScreenHeight = Math.min(outMetrics.widthPixels, outMetrics.heightPixels);

        mAspectRatioFrameLayout = (AspectRatioFrameLayout) findViewById(R.id
                .aspectRatioFrameLayout);
        mAspectRatioFrameLayout.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mAspectRatioFrameLayout.setAspectRatio(mScreenWidth * 1.0f / mScreenHeight);

        }
        mAspectRatioFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerController != null) {
                    hideOrShowPlayerController();
                }
            }
        });

        mAspectRatioFrameLayoutPlayer = (AspectRatioFrameLayout) findViewById(R.id.arflPlayer);
        mAspectRatioFrameLayoutPlayer.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    }

    @Override
    public void onSurfaceCreated(Surface surface) {
        try {
            switch (mPlayerCore) {
                case "AndroidMediaPlayer":
                    Log.i(TAG, "Create Android MediaPlayer");
                    mPlayer = new ZeroAndroidMediaPlayer();
                    break;
                case "ExoPlayer1.X":
                    Log.i(TAG, "Create ExoPlayer with version 1.X");
                    mPlayer = new ZeroExoPlayer(getApplicationContext());
                    break;
                case "ExoPlayer2.X":
                    Log.i(TAG, "Create ExoPlayer with version 2.X");
                    mPlayer = new ZeroExoPlayer2(getApplicationContext());
                    break;
                case "IJKPlayer":
                    Log.i(TAG, "Create IJKPlayer");
                    mPlayer = new ZeroIjkPlayer();
                    break;
                default:
                    Log.i(TAG, "Create Android MediaPlayer");
                    mPlayer = new ZeroAndroidMediaPlayer();
                    break;
            }

            mPlayer.setDataSource(mFilePath);
            mPlayer.setSurface(surface);

            mPlayer.setOnPreparedListener(mOnPreparedListener);
            mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mPlayer.setOnCompletionListener(mOnCompletionListener);
            mPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mPlayer.setOnInfoListener(mOnInfoListener);
            mPlayer.setOnErrorListener(mOnErrorListener);

            mPlayer.prepareAsync();

            showLoading();
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in MediaPlayer initialization: " + e.toString());
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, String.format("Surface changed, width: %d, height: %d", width, height));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mHeadTracker != null) {
            mHeadTracker.startTracking();
        }

        if (mPlayer != null) {
            mPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mHeadTracker != null) {
            mHeadTracker.stopTracking();
        }

        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mHeadTracker != null) {
            mHeadTracker.stopTracking();
        }

        if (mPlayerController != null) {
            mPlayerController.stopUpdatingProgress();
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mAspectRatioFrameLayout.setAspectRatio(0.0f);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mAspectRatioFrameLayout.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
            mAspectRatioFrameLayout.setAspectRatio(mScreenWidth * 1.0f / mScreenHeight);
        }
    }

    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mPlayerController.onBackPressed();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void hideLoading() {
        mImageViewLoading.setVisibility(View.GONE);
    }

    private void showLoading() {
        mImageViewLoading.setVisibility(View.VISIBLE);
    }

    private void hideOrShowPlayerController() {
        if (mPlayerController.isShown()) {
            mPlayerController.hide();
        } else {
            mPlayerController.show(true);
        }
    }

    public void switchToFullScreen() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void switchToSmallScreen() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private ZeroMediaPlayer.OnPreparedListener mOnPreparedListener = new ZeroMediaPlayer
            .OnPreparedListener() {
        @Override
        public void onPrepared(ZeroMediaPlayer mediaPlayer) {
            hideLoading();

            mPlayerController.setPlayer(mPlayer);
            mPlayerController.setPlayerActivity(PlayerActivity.this);
            mPlayerController.setInfo();
            mPlayerController.startUpdatingProgress();
            mPlayerController.show(true);

            mPlayer.start();
        }
    };

    private ZeroMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new
            ZeroMediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(ZeroMediaPlayer mediaPlayer, int percent) {
                    mPlayerController.setSecondaryProgress(percent);
                }
            };

    private ZeroMediaPlayer.OnCompletionListener mOnCompletionListener = new ZeroMediaPlayer
            .OnCompletionListener() {
        @Override
        public void onCompletion(ZeroMediaPlayer mediaPlayer) {
            Log.i(TAG, "Video playback complete");
            mPlayer.seekTo(0);
            mPlayerController.show(true);
        }
    };

    private ZeroMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new
            ZeroMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(ZeroMediaPlayer mediaPlayer, int width, int height) {
                    Log.i(TAG, String.format("Video size changed, width: %d, height: %d", width,
                            height));
                    if (width != 0 && height != 0) {
                        mAspectRatioFrameLayoutPlayer.setAspectRatio(width / height);
                    }
                }
            };

    private ZeroMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new ZeroMediaPlayer
            .OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(ZeroMediaPlayer mediaPlayer) {
            Log.i(TAG, "Seek complete");
        }
    };

    private ZeroMediaPlayer.OnErrorListener mOnErrorListener = new ZeroMediaPlayer
            .OnErrorListener() {
        @Override
        public boolean onError(ZeroMediaPlayer mediaPlayer, int what, int extra) {
            Log.e(TAG, String.format("On error, what: %d, extra: %d", what, extra));
            return false;
        }
    };

    private ZeroMediaPlayer.OnInfoListener mOnInfoListener = new ZeroMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(ZeroMediaPlayer mediaPlayer, int what, int extra) {
            Log.i(TAG, String.format("On error, what: %d, extra: %d", what, extra));
            switch (what) {
                case ZeroMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    showLoading();
                    break;
                case ZeroMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    hideLoading();
                    break;
            }

            return false;
        }
    };
}
