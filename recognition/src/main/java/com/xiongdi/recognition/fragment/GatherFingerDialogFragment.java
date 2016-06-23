package com.xiongdi.recognition.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/6/23.
 * 采集指纹的dialog
 */
public class GatherFingerDialogFragment extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gather_fingerprint_pop_layout, container, false);
    }
}
