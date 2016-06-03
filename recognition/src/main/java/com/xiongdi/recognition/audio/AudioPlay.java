package com.xiongdi.recognition.audio;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.util.Log;

import java.io.IOException;

public class AudioPlay {
    private static final String TAG = "moubiao";
    public static final int VERIFY_PASSED = 0;
    public static final int VERIFY_FAILED = 1;

    private static ToneGenerator mToneGenerator;
    private static MediaPlayer sMediaPlayer;
    private final Object mLock = new Object();

    public AudioPlay() {
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        sMediaPlayer = new MediaPlayer();
    }

    public int PlayTone(int toneType, int durationMs) {
        synchronized (mLock) {
            if (mToneGenerator == null) {
                Log.e(TAG, "playTone: mToneGenerator == null");
                return 0;
            }
            if (sMediaPlayer == null) {
                Log.e(TAG, "sMediaPlayer == null");
                return 0;
            }
        }

        mToneGenerator.startTone(toneType, durationMs);
        return 0;
    }

    public int PlayFile(String filePath) {
        if (sMediaPlayer != null) {
            sMediaPlayer.reset();
            try {
                sMediaPlayer.setDataSource(filePath);
                sMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sMediaPlayer.start();
        } else {
            Log.e(TAG, "MediaPlayer Invalid !");
            return -1;
        }
        return 0;
    }


    public void PlayAsset(int key, AssetManager am) {
        try {
            AssetFileDescriptor assetFileDescriptor = am.openFd("verification_fail.wav");
            switch (key) {
                case VERIFY_PASSED:
                    playRelease();
                    assetFileDescriptor = am.openFd("verification_passed.wav");
                    break;
                case VERIFY_FAILED:
                    assetFileDescriptor = am.openFd("verification_fail.wav");

                    break;
                default:
                    break;
            }

            if (!sMediaPlayer.isPlaying()) {
                sMediaPlayer.reset();
                sMediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                sMediaPlayer.prepare();
                sMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playRelease() {
        if (mToneGenerator != null) {
            mToneGenerator = null;
        }
        if (sMediaPlayer != null) {
            sMediaPlayer.stop();
            sMediaPlayer.reset();
        }
    }
}
