package com.xiongdi.recognition.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by moubiao on 2016/3/22.
 * 单选对话框
 */
public class SingleChoiceDialogFragment extends DialogFragment {
    private String dialogTitle;
    private String[] dialogItems;
    private int selectedID;
    DialogInterface.OnClickListener clickListener;

    public void setData(String dialogTitle, String[] dialogItems, int selectID) {
        this.dialogTitle = dialogTitle;
        this.dialogItems = dialogItems;
        this.selectedID = selectID;
    }

    public void setListener(DialogInterface.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setSingleChoiceItems(dialogItems, selectedID, clickListener)
                .setNegativeButton("Done", null)
                .create();
    }
}
