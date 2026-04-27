package com.majid.audioplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.IOException;

public class PlayerService extends Service {

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private PlayerCallback callback;
    private int currentPosition = -1;

    public interface PlayerCallback {
        void onPlaybackStarted(int position);
        void onPlaybackPaused();
        void onPlaybackCompleted();
        void onError(String message);
    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (callback != null) callback.onPlaybackCompleted();
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (callback != null) callback.onError("Playback error: " + what);
                return true;
            }
        });
    }

    public void setCallback(PlayerCallback cb) {
        this.callback = cb;
    }

    public void play(String path, int position) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentPosition = position;
            if (callback != null) callback.onPlaybackStarted(position);
        } catch (IOException e) {
            if (callback != null) callback.onError("Cannot open file");
        }
    }

    public void togglePause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (callback != null) callback.onPlaybackPaused();
        } else {
            mediaPlayer.start();
            if (callback != null && currentPosition >= 0) callback.onPlaybackStarted(currentPosition);
        }
    }

    public void seekTo(int ms) {
        mediaPlayer.seekTo(ms);
    }

    public void skip(int ms) {
        int target = mediaPlayer.getCurrentPosition() + ms;
        if (target < 0) target = 0;
        if (target > mediaPlayer.getDuration()) target = mediaPlayer.getDuration() - 1000;
        mediaPlayer.seekTo(target);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getPlayingIndex() {
        return currentPosition;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
