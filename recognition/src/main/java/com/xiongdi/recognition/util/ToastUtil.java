package com.xiongdi.recognition.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by moubiao on 2016/4/1.
 * toast工具类
 */
public class ToastUtil {
    private Toast toast;
    private volatile static ToastUtil instance = null;

    private ToastUtil() {
    }

    public static ToastUtil getInstance() {
        if (instance == null) {
            synchronized (ToastUtil.class) {
                if (instance == null) {
                    instance = new ToastUtil();
                }
            }
        }
        return instance;
    }

    public void showToast(Context context, String message) {
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        } else {
            toast.setText(message);
        }
        toast.show();
    }
}
