package com.xiongdi.recognition.widget;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/4/6.
 * 询问对话框
 */
public class AskDialogFragment extends DialogFragment {
    private DialogInterface.OnClickListener listener;
    private String title;
    private String message;

    public void setData(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public void setListener(DialogInterface.OnClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.common_sure, listener)
                .setNegativeButton(R.string.common_cancel, listener)
                .create();
    }
}
