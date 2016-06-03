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

    /**
     * 播放铃声
     */
    public int PlayTone(int toneType, int durationMs) {
        synchronized (mLock) {
            if (mToneGenerator == null) {
                Log.e(TAG, "playTone: mToneGenerator == null");
                return 0;
            }
        }

        mToneGenerator.startTone(toneType, durationMs);
        return 0;
    }

    /**
     * 播放本地文件
     */
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


    /**
     * 播放asset里的文件
     */
    public void playAsset(int key, AssetManager am) {
        AssetFileDescriptor assetFile;
        try {
            switch (key) {
                case VERIFY_PASSED:
                    assetFile = am.openFd("verification_passed.wav");
                    break;
                case VERIFY_FAILED:
                    assetFile = am.openFd("verification_fail.wav");
                    break;
                default:
                    assetFile = am.openFd("verification_fail.wav");
                    break;
            }

            if (!sMediaPlayer.isPlaying()) {
                sMediaPlayer.reset();
                sMediaPlayer.setDataSource(assetFile.getFileDescriptor(), assetFile.getStartOffset(), assetFile.getLength());
                sMediaPlayer.prepare();
                sMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重置mediaPlayer
     */
    public void resetMediaPlayer() {
        if (sMediaPlayer != null) {
            sMediaPlayer.stop();
            sMediaPlayer.reset();
        }
    }
}
