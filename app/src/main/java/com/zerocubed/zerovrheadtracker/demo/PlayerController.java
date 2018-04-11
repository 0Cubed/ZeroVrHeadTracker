package com.zerocubed.zerovrheadtracker.demo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zerocubed.zeromediaplayer.ZeroMediaPlayer;

/**
 * Created by Zero
 * Created on 2018/04/11
 * Controller of the player with UI as well as control logic
 * Copied from ZeroMediaPlayer: https://github.com/0Cubed/ZeroMediaPlayer
 */

public class PlayerController extends FrameLayout implements View.OnClickListener {
    private static final int AUTO_HIDE_INTERVAL_IN_MS = 3000;
    private static final int PROGRESS_UPDATE_INTERVAL_IN_MS = 500;
    private static final int MAX_PROGRESS = 1000;
    private static final int MESSAGE_UPDATE_PROGRESS = 0;
    private static final int MESSAGE_HIDE = 1;

    private ImageView ivBack;
    private ImageView ivSetting;
    private ImageView ivPlayPause;
    private ImageView ivFullSmall;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar sbProgress;

    private PlayerActivity mPlayerActivity;
    private ZeroMediaPlayer mPlayer;

    private int mDuration;

    public PlayerController(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PlayerController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayerController(@NonNull Context context, @Nullable AttributeSet attrs, int
            defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_player_controller, this, true);

        ivBack = (ImageView) findViewById(R.id.ivBack);
        ivSetting = (ImageView) findViewById(R.id.ivSetting);
        ivPlayPause = (ImageView) findViewById(R.id.ivPlayPause);
        ivFullSmall = (ImageView) findViewById(R.id.ivFullSmall);

        tvCurrentTime = (TextView) findViewById(R.id.tvCurrentPosition);
        tvTotalTime = (TextView) findViewById(R.id.tvDuration);

        sbProgress = (SeekBar) findViewById(R.id.sbProgress);

        ivBack.setOnClickListener(this);
        ivSetting.setOnClickListener(this);
        ivPlayPause.setOnClickListener(this);
        ivFullSmall.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivBack:
                onBackPressed();
                break;
            case R.id.ivSetting:
                break;
            case R.id.ivPlayPause:
                if (mPlayer == null) {
                    return;
                }
                if (mPlayer.isPlaying()) {
                    ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
                    mPlayer.pause();
                } else {
                    ivPlayPause.setImageResource(R.drawable.ic_pause);
                    mPlayer.start();
                }
                break;
            case R.id.ivFullSmall:
                if (mPlayerActivity.getRequestedOrientation() == ActivityInfo
                        .SCREEN_ORIENTATION_PORTRAIT) {
                    ivFullSmall.setImageResource(R.drawable.ic_fullscreen_exit);
                    mPlayerActivity.switchToFullScreen();
                } else if (mPlayerActivity.getRequestedOrientation() == ActivityInfo
                        .SCREEN_ORIENTATION_LANDSCAPE) {
                    ivFullSmall.setImageResource(R.drawable.ic_fullscreen);
                    mPlayerActivity.switchToSmallScreen();
                }
                break;
        }
    }

    public void setPlayer(ZeroMediaPlayer player) {
        mPlayer = player;
    }

    public void setPlayerActivity(PlayerActivity playerActivity) {
        mPlayerActivity = playerActivity;
    }

    private String getFormattedTimeString(int msec) {
        int totalSec = msec / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return ((min < 10) ? ("0" + String.valueOf(min)) : String.valueOf(min)) + ":" +
                ((sec < 10) ? ("0" + String.valueOf(sec)) : String.valueOf(sec));
    }

    public void onBackPressed() {
        if (mPlayerActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mPlayerActivity.finish();
        } else if (mPlayerActivity.getRequestedOrientation() == ActivityInfo
                .SCREEN_ORIENTATION_LANDSCAPE) {
            mPlayerActivity.switchToSmallScreen();
        }
    }

    public void setInfo() {
        if (mPlayer != null) {
            sbProgress.setMax(MAX_PROGRESS);
            mDuration = mPlayer.getDuration();
            tvTotalTime.setText(getFormattedTimeString(mDuration));
            sbProgress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        }
    }

    public void setSecondaryProgress(int percent) {
        sbProgress.setSecondaryProgress(percent * MAX_PROGRESS / 100);
    }

    public void startUpdatingProgress() {
        mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
        mHandler.sendEmptyMessage(MESSAGE_UPDATE_PROGRESS);
    }

    public void stopUpdatingProgress() {
        mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
    }

    public void show(boolean autoHide) {
        mHandler.removeMessages(MESSAGE_HIDE);
        setVisibility(VISIBLE);
        if (autoHide) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, AUTO_HIDE_INTERVAL_IN_MS);
        }
    }

    public void hide() {
        setVisibility(GONE);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_PROGRESS:
                    if (mPlayer != null) {
                        int currentPosition = mPlayer.getCurrentPosition();
                        sbProgress.setProgress((int) (1.0f * currentPosition / mDuration *
                                MAX_PROGRESS));
                        tvCurrentTime.setText(getFormattedTimeString(currentPosition));
                    }
                    mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS,
                            PROGRESS_UPDATE_INTERVAL_IN_MS);
                    break;
                case MESSAGE_HIDE:
                    hide();
                    break;
            }
            return false;
        }
    });

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar
            .OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            int newPosition = progress * mDuration / MAX_PROGRESS;
            mPlayer.seekTo(newPosition);
            tvCurrentTime.setText(getFormattedTimeString(newPosition));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
}
