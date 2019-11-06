package io.zbox.treno;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.lifecycle.Observer;

import io.zbox.zboxfs.File;

class VideoPlayer extends MediaSessionCompat.Callback implements
        Observer<File>,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener
{

    private static final String TAG = VideoPlayer.class.getName();

    private Context context;
    private MediaPlayer player;
    private SurfaceView surfaceView;

    VideoPlayer(Context context) {
        this.context = context;

        player = new MediaPlayer();

        player.setScreenOnWhilePlaying(true);

        player.setOnErrorListener(this);
        player.setOnPreparedListener(this);
        player.setOnVideoSizeChangedListener(this);
        player.setOnInfoListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnCompletionListener(this);
    }

    /* =====================================
       Observer<File>, this is called when file is opened
       ===================================== */
    public void onChanged(File file) {
        Log.d(TAG, "file is opened");
        ZboxMediaSource dataSource = new ZboxMediaSource(file);
        player.setDataSource(dataSource);
        player.prepareAsync();
    }

    /* =====================================
       MediaPlayer.Callback
       ===================================== */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: what: " + what + ", extra" + extra);
        return false;
    }

    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared() called");
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Point screenSize = new Point();
        ((Activity)context).getWindowManager().getDefaultDisplay().getSize(screenSize);
        ViewGroup.LayoutParams videoParams = surfaceView.getLayoutParams();

        // adjust surface view size according to video aspect
        if (width > height)
        {
            videoParams.width = screenSize.x;
            videoParams.height = screenSize.x * height / width;
        }
        else
        {
            videoParams.width = screenSize.y * width / height;
            videoParams.height = screenSize.y;
        }
        surfaceView.setLayoutParams(videoParams);
    }

    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo: what: " + what + ", extra" + extra);
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.d(TAG, "onInfo: MEDIA_INFO_BUFFERING_START");
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.d(TAG, "onInfo: MEDIA_INFO_BUFFERING_END");
                break;
            default:
                break;
        }
        return false;
    }

    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete() called");
    }

    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion() called");
    }

    /* =====================================
       MediaSessionCompat.Callback
       ===================================== */
    @Override
    public void onPrepare() {
        Log.d(TAG, "callback ==> prepare");
    }

    @Override
    public void onPlay() {
        Log.d(TAG, "callback ==> play");
        player.start();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "callback ==> pause");
        player.pause();
    }

    @Override
    public void onSeekTo(long pos) {
        Log.d(TAG, "callback ==> seek to " + pos);
        player.seekTo((int)pos);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "callback ==> stop");
        player.stop();
    }

    /* =====================================
       Normal methods
       ===================================== */
    void setSurface(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        player.setDisplay(surfaceView.getHolder());
    }

    void release() {
        player.release();
        player = null;
    }
}
