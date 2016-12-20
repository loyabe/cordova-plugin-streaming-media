package com.shuyu.gsyvideoplayer.listener;

/**
 * Created by Administrator on 2016/12/2.
 */

public interface GSYVideoPlayerListener {

    void onProgressAndTime(int progress, int secProgress, int currentTime, int totalTime);
    public void onError(int what, int extra);
    public void onAutoCompletion();
}
