package com.xiongdi.recognition.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.xiongdi.recognition.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by moubiao on 2016/4/20.
 * 保存程序崩溃信息到本地文件
 */
public class CrashHandlerUtil implements Thread.UncaughtExceptionHandler {
    private static String TAG = "moubiao";
    private static CrashHandlerUtil mCrashHandlerUtil;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private Context mContext;

    public CrashHandlerUtil() {
    }

    public static CrashHandlerUtil getInstance() {
        if (mCrashHandlerUtil == null) {
            synchronized (CrashHandlerUtil.class) {
                if (mCrashHandlerUtil == null) {
                    mCrashHandlerUtil = new CrashHandlerUtil();
                }
            }
        }

        return mCrashHandlerUtil;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "uncaughtException: external storage not mounted!");
            return;
        }

        if (mCrashHandlerUtil != null) {
            try {
                File directory = new File(Environment.getExternalStorageDirectory() + File.separator + mContext.getResources().getString(R.string.app_name));
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        Log.e(TAG, "uncaughtException: create directory recognition failed!");
                    }
                }
                File file = new File(directory, "crash_log.txt");
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        Log.e(TAG, "uncaughtException: create file crash_log.txt failed!");
                    }
                }

                FileOutputStream fos = new FileOutputStream(file, true);
                PrintStream printStream = new PrintStream(fos);
                ex.printStackTrace(printStream);
                printStream.flush();
                printStream.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mDefaultExceptionHandler != null) {
                mDefaultExceptionHandler.uncaughtException(thread, ex);
            }
        }
    }

    public void initCrashHandlerUtil(Context context) {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context;
    }
}
