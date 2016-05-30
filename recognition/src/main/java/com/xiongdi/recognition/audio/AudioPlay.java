package com.xiongdi.recognition.audio;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.util.Log;

import java.io.IOException;

public class AudioPlay {
    public static final int PLAY_SOUND_PUCODE_ERROR = 1;
    public static final int PLAY_SOUND_FINGER_SCAN_FAIL = 3;
    public static final int PLAY_SOUND_FINGER_AUTHENTICATION_FAIL = 4;
    public static final int PLAY_SOUND_FINGER_VERIFICATION = 5;

    //ToneGenerator.MAX_VOLUME = 100
    private static final String TAG = "audioPlay";
    private static ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
    private static MediaPlayer mp = new MediaPlayer();
    private Object mLock = new Object();//������������

    public int PlayTone(int toneType, int durationMs) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        synchronized (mLock) {
            if (mToneGenerator == null) {
                Log.e(TAG, "playTone: mToneGenerator == null");
                return 0;
            }
            if (mp == null) {
                Log.e(TAG, "mp == null");
                return 0;
            }
        }

        if (mToneGenerator != null) {
            mToneGenerator.startTone(toneType, durationMs);
        } else {
            Log.e(TAG, "mToneGenerator Invalid !");
            return -1;
        }
        return 0;
    }

    public int PlayFile(String filewithpath) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        if (mp != null) {
            mp.reset();
            mp.setDataSource(filewithpath);
            mp.prepare();
            mp.start();
        } else {
            Log.e(TAG, "MediaPlayer Invalid !");
            return -1;
        }
        return 0;
    }


    public void PlayAsset(int key, AssetManager am) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        switch (key) {
            case 1:
                Log.v("crjlog", "1");
                if (mp != null) {
                    AssetFileDescriptor afd = am.openFd("finger_scan_fail.wav");
                    mp.reset();
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength());
                    mp.prepare();
                    mp.start();
                } else {
                    Log.e(TAG, "MediaPlayer Invalid !");
                }

                break;
            case 2:
                Log.v("crjlog", "2");
                if (mp != null) {
                    AssetFileDescriptor afd = am.openFd("put_finger.wav");
                    mp.reset();
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength());
                    mp.prepare();
                    mp.start();
                } else {
                    Log.e(TAG, "MediaPlayer Invalid !");
                }

                break;
            case 3:
                break;

            case 4:    //fail
                if (mp != null) {
                    AssetFileDescriptor afd = am.openFd("verification_fail.wav");
                    mp.reset();
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength());
                    mp.prepare();
                    mp.start();
                } else {
                    Log.e(TAG, "MediaPlayer Invalid !");
                }
                break;
            case 5:
                if (mp != null) {
                    AssetFileDescriptor afd = am.openFd("verification_passed.wav");
                    mp.reset();
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength());
                    mp.prepare();
                    mp.start();
                } else {
                    Log.e(TAG, "MediaPlayer Invalid !");
                }

                break;
        }
    }

    public int PlayRelease() {
        if (mToneGenerator != null) {
        }
        if (mp != null) {
            mp.stop();
            mp.reset();
        }
        return 0;
    }
}
