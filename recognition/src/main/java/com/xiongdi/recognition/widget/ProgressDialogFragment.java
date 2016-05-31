package com.xiongdi.recognition.widget;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by moubiao on 2016/4/7.
 * 进度条对话框
 */
public class ProgressDialogFragment extends DialogFragment {
    private String message;

    public void setData(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(message);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
