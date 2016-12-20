package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.widget.MediaController;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.util.Calendar;

public class SimpleVideoStream extends Activity implements
	MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
	MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener {
	private String TAG = getClass().getSimpleName();
	private VideoView mVideoView = null;
	private MediaPlayer mMediaPlayer = null;
	private MediaController mMediaController = null;
	private ProgressBar mProgressBar = null;
	private String mVideoUrl;
	private Boolean mShouldAutoClose = true;
	  private int progress = 0;
	  private int currentPosition = 0;
	  private int duration = 0;
	private int seekPosition = 0;
	private int lastPosition = 0;
	private boolean isComplete = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Bundle b = getIntent().getExtras();
		mVideoUrl = b.getString("mediaUrl");
		seekPosition = b.getInt("seekPosition", 0);
		lastPosition = seekPosition;
		isComplete = b.getBoolean("isComplete", false);
		mShouldAutoClose = b.getBoolean("shouldAutoClose");
		mShouldAutoClose = mShouldAutoClose == null ? true : mShouldAutoClose;
		mShouldAutoClose = true;
		RelativeLayout relLayout = new RelativeLayout(this);
		relLayout.setBackgroundColor(Color.BLACK);
		RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mVideoView = new VideoView(this);
		mVideoView.setLayoutParams(relLayoutParam);
		relLayout.addView(mVideoView);

		// Create progress throbber
		mProgressBar = new ProgressBar(this);
		mProgressBar.setIndeterminate(true);
		// Center the progress bar
		RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mProgressBar.setLayoutParams(pblp);
		// Add progress throbber to view
		relLayout.addView(mProgressBar);
		mProgressBar.bringToFront();

		setOrientation(b.getString("orientation"));

		setContentView(relLayout, relLayoutParam);

		play();
        rigisterCast();
	}

	private void play() {
		mProgressBar.setVisibility(View.VISIBLE);
		Uri videoUri = Uri.parse(mVideoUrl);
		try {
			mVideoView.setOnCompletionListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnErrorListener(this);
    //  mVideoView.setOnInfoListener(this);
			mVideoView.setVideoURI(videoUri);
			mMediaController = new MediaController(this, false);
			mMediaController.setAnchorView(mVideoView);
			mMediaController.setMediaPlayer(mVideoView);
			//mMediaController.
		//	mMediaController.setEnabled(false);

			mVideoView.setMediaController(mMediaController);
		} catch (Throwable t) {
			Log.d(TAG, t.toString());
		}

   // mHandler.sendEmptyMessage(SHOW_PROGRESS);
	}

	private void setOrientation(String orientation) {
		if ("landscape".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}else if("portrait".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	private Runnable checkIfPlaying = new Runnable() {
		@Override
		public void run() {
			if (mVideoView.getCurrentPosition() > 0) {
				// Video is not at the very beginning anymore.
				// Hide the progress bar.
				mProgressBar.setVisibility(View.GONE);
			} else {
				// Video is still at the very beginning.
				// Check again after a small amount of time.
				mVideoView.postDelayed(checkIfPlaying, 100);
			}
		}
	};

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Stream is prepared");
		mMediaPlayer = mp;
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mVideoView.requestFocus();
		mVideoView.start();
		mVideoView.postDelayed(checkIfPlaying, 0);
		mVideoView.seekTo(seekPosition);
	}

	private void pause() {
    Log.d(TAG, "Pausing video.");
    mVideoView.pause();
  }

	private void stop() {
		Log.d(TAG, "Stopping video.");
		mVideoView.stopPlayback();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
	}

	private void wrapItUp(int resultCode, String message) {
		Intent intent = new Intent();
		intent.putExtra("message", message);
    	intent.putExtra("progress", progress);
    	intent.putExtra("position", currentPosition/1000);
		intent.putExtra("duration", duration/1000);
    	Log.e(TAG, "wrapItUp . progress " + progress);
    	Log.e(TAG, "wrapItUp . position " + currentPosition);
		Log.e(TAG, "wrapItUp . duration " + duration);
		setResult(resultCode, intent);
		unRigisterCast();
		setOrientation("portrait");
		finish();
	}

	public void onCompletion(MediaPlayer mp) {
	//	stop();
	    this.currentPosition = mp.getCurrentPosition();
		this.progress = 100;
		if (mShouldAutoClose) {
      wrapItUp(RESULT_OK, null);
    }
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
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
		return true;
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(TAG, "onBufferingUpdate : " + percent + "%");

//    this.percent = percent;
//    this.currentPosition = mVideoView.getCurrentPosition();
    setProgress();
   // Log.e(TAG, "onBufferingUpdate . percent  " + percent);
   // Log.e(TAG, "onBufferingUpdate . currentPosition  " + currentPosition);

//    Intent intent = new Intent();  //Itent就是我们要发送的内容
//    intent.putExtra("data", "this is data from broadcast "+ Calendar.getInstance().get(Calendar.SECOND));
//    intent.setAction(StreamingMedia.flag);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
//    sendBroadcast(intent);   //发送广播
	}

	@Override
	public void onBackPressed() {
		// If we're leaving, let's finish the activity
    //unRigisterCast();
    wrapItUp(RESULT_OK, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// The screen size changed or the orientation changed... don't restart the activity
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMediaController != null)
			mMediaController.show();
		return false;
	}

  SimpleVideoStream.ReceiveBroadCast receiveBroadCast;
  public static final String flag = "controlFlag";
  private void rigisterCast(){
    receiveBroadCast = new SimpleVideoStream.ReceiveBroadCast();
    IntentFilter filter = new IntentFilter();
    filter.addAction(flag);    //只有持有相同的action的接受者才能接收此广播
    this.registerReceiver(receiveBroadCast, filter);
  }
  private void unRigisterCast(){
    this.unregisterReceiver(receiveBroadCast);
  }

  @Override
  public boolean onInfo(MediaPlayer mp, int what, int extra) {
    Log.e(TAG, "onInfo . what  " + what);
    Log.e(TAG, "onInfo . extra  " + extra);

    return false;
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

  private int setProgress() {

    int position = mVideoView.getCurrentPosition();
    int duration = mVideoView.getDuration();
    long pos = 0;
      if (duration > 0) {
        // use long to avoid overflow
       pos = 1000L * position / duration;

      }
    this.progress = (int)pos;
    this.currentPosition = position;
    this.duration = duration;
    Log.e(TAG, "setProgress . progress  " + pos);
    Log.e(TAG, "setProgress . currentPosition  " + position);
    Intent intent = new Intent();  //Itent就是我们要发送的内容
    intent.putExtra("progress", progress);
    intent.putExtra("currentPosition", position);
    intent.putExtra("duration", duration);
    intent.setAction(StreamingMedia.flag);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
    sendBroadcast(intent);   //发送广播

    return position;
  }

//  private static final int SHOW_PROGRESS = 2;
//
//  private final Handler mHandler = new Handler() {
//    @Override
//    public void handleMessage(Message msg) {
//      int pos;
//      switch (msg.what) {
//
//        case SHOW_PROGRESS:
//          pos = setProgress();
//
//            msg = obtainMessage(SHOW_PROGRESS);
//            sendMessageDelayed(msg, 1000 - (pos % 1000));
//
//          break;
//      }
//    }
//  };


}
