package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class StreamingMedia extends CordovaPlugin {

	//static final String TAG = getClass().getSimpleName();
	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";
    public static final String ACTION_STOP_VIDEO = "stopVideo";
    public static final String ACTION_STOP_AUDIO = "stopAudio";

	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	private CallbackContext callbackContext;

	private static final String TAG = "StreamingMediaPlugin";


  protected void pluginInitialize() {
    rigisterCast();
  }

  @Override
  public void onDestroy() {

    super.onDestroy();
   // this.cordova.getActivity().unregisterReceiver(receiveBroadCast);
  }

  @Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}


		if (ACTION_PLAY_AUDIO.equals(action)) {
			return playAudio(args.getString(0), options);
		} else if (ACTION_PLAY_VIDEO.equals(action)) {
			return playVideo(args.getString(0), options);
    } else if (ACTION_STOP_VIDEO.equals(action)) {
      return stopVideo(options);
		} else {
			callbackContext.error("streamingMedia." + action + " is not a supported method.");
			return false;
		}
	}

	private boolean playAudio(String url, JSONObject options) {

		return play(SimpleAudioStream.class, url, options);
	}
	private boolean playVideo(String url, JSONObject options) {

		Class target = null;
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN){
			target = PlayActivity.class;
		}else {
			target = PlayActivity.class;
		}


		return play(target, url, options);
	}

  private boolean stopVideo(JSONObject options){
    Intent intent = new Intent();  //Itent就是我们要发送的内容
    intent.putExtra("data", "stop");
    intent.setAction(SimpleVideoStream.flag);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
    cordova.getActivity().sendBroadcast(intent);   //发送广播
    Log.v(TAG, "stopVideo");
    return true;
  }

	private boolean play(final Class activityClass, final String url, final JSONObject options) {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;

		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
				Bundle extras = new Bundle();
				extras.putString("mediaUrl", url);

				if (options != null) {
					Iterator<String> optKeys = options.keys();
					while (optKeys.hasNext()) {
						try {
							final String optKey = (String)optKeys.next();
							if (options.get(optKey).getClass().equals(String.class)) {
								extras.putString(optKey, (String)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Boolean.class)) {
//								extras.putBoolean("shouldAutoClose", true);
								extras.putBoolean(optKey, (Boolean)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Integer.class)){
								extras.putInt(optKey, options.getInt(optKey));
							} else if (options.get(optKey).getClass().equals(Float.class)){
								extras.putInt(optKey, options.getInt(optKey));
							}

						} catch (JSONException e) {
							Log.e(TAG, "JSONException while trying to read options. Skipping option.");
						}
					}
					streamIntent.putExtras(extras);
				}

				cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
			}
		});
		return true;
	}

  ReceiveBroadCast receiveBroadCast;
  public  static final String flag = "castFlag";
  private void rigisterCast(){
    receiveBroadCast = new ReceiveBroadCast();
    IntentFilter filter = new IntentFilter();
    filter.addAction(flag);    //只有持有相同的action的接受者才能接收此广播
    cordova.getActivity().registerReceiver(receiveBroadCast, filter);
  }

  //接收更新更新值
  public class ReceiveBroadCast extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      //得到广播中得到的数据，并显示出来
      int progress = intent.getIntExtra("progress", 0);
      int currentPosition = intent.getIntExtra("currentPosition", 0);
      int duration = intent.getIntExtra("duration", 0);
      //txtShow.setText(message);
     // Log.e(TAG, "progress " + progress);
     // Log.e(TAG, "currentPosition " + currentPosition);
//      String = "{progress: "+progress+", currentPosition }";
      String js = String .format("window.plugins.streamingMedia.onBufferingUpdate('%d', '%d' , '%d');",
        progress, currentPosition, duration);
      try {
          webView.sendJavascript(js);

      } catch (NullPointerException e) {

      } catch (Exception e) {
      }
    }

  }



	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		super.onActivityResult(requestCode, resultCode, intent);
		if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
			if (Activity.RESULT_OK == resultCode) {

				int position = intent.getIntExtra("position", 0);
				int duration = intent.getIntExtra("duration", 0);
				int progress = intent.getIntExtra("progress", 0);

				JSONObject obj = new JSONObject();
				try {
					obj.put("position", position);
					obj.put("duration", duration);
					obj.put("progress", progress);
				}catch (Exception e){

				}
				this.callbackContext.success(obj);
			} else if (Activity.RESULT_CANCELED == resultCode) {
				String errMsg = "Error";
				if (intent != null && intent.hasExtra("message")) {
					errMsg = intent.getStringExtra("message");
				}
				//this.callbackContext.error();
				int position = intent.getIntExtra("position", 0);
				int duration = intent.getIntExtra("duration", 0);
				int progress = intent.getIntExtra("progress", 0);

				JSONObject obj = new JSONObject();
				try {
					obj.put("position", position);
					obj.put("duration", duration);
					obj.put("progress", progress);
				}catch (Exception e){

				}

				this.callbackContext.error(obj);
			}else if (333 == resultCode){
        Log.e(TAG, "onActivityResult: 333");
      }
		}
	}




}
