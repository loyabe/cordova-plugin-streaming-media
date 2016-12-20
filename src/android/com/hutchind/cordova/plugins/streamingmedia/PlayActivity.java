package com.hutchind.cordova.plugins.streamingmedia;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.util.Log;
import android.view.View;

import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.GSYVideoPlayerListener;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.xmexe.exe.R;


/**
 * 单独的视频播放页面
 * Created by shuyu on 2016/11/11.
 */
public class PlayActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();
    public final static String IMG_TRANSITION = "IMG_TRANSITION";
    public final static String TRANSITION = "TRANSITION";


    StandardGSYVideoPlayer videoPlayer;

  //  OrientationUtils orientationUtils;

    private boolean isTransition;

    private String mVideoUrl;

    private int progress = 0;
    private int currentPosition = 0;
    private int duration = 0;
    private int seekPosition = 0;
    private boolean isComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        videoPlayer = (StandardGSYVideoPlayer)findViewById(R.id.video_player);
//        ButterKnife.bind(this);
       // isTransition = getIntent().getBooleanExtra(TRANSITION, false);
        isTransition = false;
        Bundle b = getIntent().getExtras();
        mVideoUrl = b.getString("mediaUrl");
        seekPosition = b.getInt("seekPosition", 0);
       // seekPosition;
        isComplete = b.getBoolean("isComplete", false);
        init();
    }

    private void init() {
    //    String url = "http://baobab.wdjcdn.com/14564977406580.mp4";
        //String url = "http://cdn.exexm.com/cw_146444270184770";

        Log.e(TAG, "init mVideoUrl " +  mVideoUrl);

        if(mVideoUrl.startsWith("file")){
            mVideoUrl = Uri.parse(mVideoUrl).getPath();
        }
        Log.e(TAG, "init mVideoUrl " +  mVideoUrl);
        Log.e(TAG, "init seekPosition " +  seekPosition);
        rigisterCast();
        videoPlayer.setUp(mVideoUrl, true, "");
        videoPlayer.setComplete(isComplete);
        if(!isComplete){
            videoPlayer.setSeekPosstion(seekPosition*1000);
        }

        //增加title
        videoPlayer.getTitleTextView().setVisibility(View.VISIBLE);
        videoPlayer.getTitleTextView().setText("");

        //设置返回键
        videoPlayer.getBackButton().setVisibility(View.VISIBLE);

//        //设置旋转
//        orientationUtils = new OrientationUtils(this, videoPlayer);
//
//        //设置全屏按键功能
//        videoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                orientationUtils.resolveByClick();
//            }
//        });

        videoPlayer.getFullscreenButton().performClick();
        videoPlayer.getFullscreenButton().setVisibility(View.INVISIBLE);
        videoPlayer.setBottomProgressBarDrawable(getResources().getDrawable(R.drawable.video_new_progress));
        videoPlayer.setDialogVolumeProgressBar(getResources().getDrawable(R.drawable.video_new_volume_progress_bg));
        videoPlayer.setDialogProgressBar(getResources().getDrawable(R.drawable.video_new_progress));
        videoPlayer.setBottomShowProgressBarDrawable(getResources().getDrawable(R.drawable.video_new_seekbar_progress),
                getResources().getDrawable(R.drawable.video_new_seekbar_thumb));
        videoPlayer.setDialogProgressColor(getResources().getColor(R.color.colorAccent), -11);

        //是否可以滑动调整
        videoPlayer.setIsTouchWiget(true);

        //设置返回按键功能
        videoPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        videoPlayer.setGSYVideoPlayerListener(new GSYVideoPlayerListener() {
            @Override
            public void onProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
                Log.e(TAG, "setProgress . progress  " + progress);
                Log.e(TAG, "setProgress . currentPosition  " + currentTime);
                Log.e(TAG, "duration . duration  " + totalTime);
                PlayActivity.this.progress = progress;
                currentPosition = currentTime;
                duration = totalTime;
                Intent intent = new Intent();  //Itent就是我们要发送的内容
                intent.putExtra("progress", progress);
                intent.putExtra("currentPosition", currentTime);
                intent.putExtra("duration", totalTime);
                intent.setAction(StreamingMedia.flag);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
                sendBroadcast(intent);   //发送广播
            }

            @Override
            public void onError(int what, int extra) {
                StringBuilder sb = new StringBuilder();
                sb.append("MediaPlayer Error: ");
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                        sb.append("Not Valid for Progressive Playback");
                        break;
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        sb.append("Server Died");
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        sb.append("Unknown");
                        break;
                    default:
                        sb.append(" Non standard (");
                        sb.append(what);
                        sb.append(")");
                }
                sb.append(" (" + what + ") ");
                sb.append(extra);
                Log.e(TAG, sb.toString());

                wrapItUp(RESULT_CANCELED, sb.toString());
            }

            @Override
            public void onAutoCompletion() {
                //保证回退百分百
                currentPosition = duration;
                progress = 100;
                wrapItUp(RESULT_OK, null);
            }
        });

        //过渡动画
        initTransition();
    }


    //接收更新更新值
    public class ReceiveBroadCast extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //得到广播中得到的数据，并显示出来
            String message = intent.getStringExtra("data");

            if ("stop".equals(message)){
                //  stop();
                onBackPressed();
            }
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void exit(){
//        //先返回正常状态
//        if (orientationUtils.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//            videoPlayer.getFullscreenButton().performClick();
//            //return;
//        }
//        videoPlayer.getFullscreenButton().performClick();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //释放所有
        videoPlayer.setStandardVideoAllCallBack(null);
        videoPlayer.setGSYVideoPlayerListener(null);
        GSYVideoPlayer.releaseAllVideos();
       // GSYVideoPlayer.setGSYVideoPlayerListener(null);
        if (isTransition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onBackPressed();
        } else {
            finish();
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
        }

    }

    @Override
    public void onBackPressed() {
        wrapItUp(RESULT_OK, null);
    }


    private void initTransition() {
        if (isTransition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            ViewCompat.setTransitionName(videoPlayer, IMG_TRANSITION);
//            addTransitionListener();
            startPostponedEnterTransition();
        } else {
            videoPlayer.startPlayLogic();
        }
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    private boolean addTransitionListener() {
//        final Transition transition = getWindow().getSharedElementEnterTransition();
//        if (transition != null) {
//            transition.addListener(new OnTransitionListener() {
//                @Override
//                public void onTransitionEnd(Transition transition) {
//                    videoPlayer.startPlayLogic();
//                    transition.removeListener(this);
//                }
//            });
//            return true;
//        }
//        return false;
//    }


    private void wrapItUp(int resultCode, String message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        intent.putExtra("progress", progress);
        intent.putExtra("position", currentPosition/1000);
        intent.putExtra("duration", duration/1000);
        Log.e(TAG, "wrapItUp . progress " + progress);
        Log.e(TAG, "wrapItUp . position " + currentPosition/1000);
        Log.e(TAG, "wrapItUp . duration " + duration/1000);
        setResult(resultCode, intent);
        unRigisterCast();
       // finish();
        exit();
    }
    PlayActivity.ReceiveBroadCast receiveBroadCast;
    public static final String flag = "controlFlag";
    private void rigisterCast(){
        receiveBroadCast = new PlayActivity.ReceiveBroadCast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(flag);    //只有持有相同的action的接受者才能接收此广播
        this.registerReceiver(receiveBroadCast, filter);
    }
    private void unRigisterCast(){
        this.unregisterReceiver(receiveBroadCast);
    }

}
