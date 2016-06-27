package com.xiongdi.recognition.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.activity.GatherActivity;

/**
 * Created by moubiao on 2016/3/22.
 * 采集左手指纹的fragment
 */
public class RightHandFragment extends Fragment implements View.OnClickListener {
    private final static int RIGHT_LITTER_FINGER = 6;
    private final static int RIGHT_RING_FINGER = 7;
    private final static int RIGHT_MIDDLE_FINGER = 8;
    private final static int RIGHT_INDEX_FINGER = 9;
    private final static int RIGHT_THUMB_FINGER = 10;

    private Button rightLittleBT, rightRingBT, rightMiddleBT, rightIndexBT, rightThumbBT;
    private GatherActivity gatherActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gatherActivity = (GatherActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.right_hand_layout, container, false);

        initView(view);
        setListener();

        return view;
    }

    private void initView(View view) {
        rightLittleBT = (Button) view.findViewById(R.id.right_little_finger_bt);
        rightRingBT = (Button) view.findViewById(R.id.right_ring_finger_bt);
        rightMiddleBT = (Button) view.findViewById(R.id.right_middle_finger_bt);
        rightIndexBT = (Button) view.findViewById(R.id.right_index_finger_bt);
        rightThumbBT = (Button) view.findViewById(R.id.right_thumb_bt);
    }

    private void setListener() {
        rightLittleBT.setOnClickListener(this);
        rightRingBT.setOnClickListener(this);
        rightMiddleBT.setOnClickListener(this);
        rightIndexBT.setOnClickListener(this);
        rightThumbBT.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.right_little_finger_bt:
                gatherActivity.setFingerNUM(RIGHT_LITTER_FINGER);
                gatherFingerprint(intent, RIGHT_LITTER_FINGER);
                break;
            case R.id.right_ring_finger_bt:
                gatherActivity.setFingerNUM(RIGHT_RING_FINGER);
                gatherFingerprint(intent, RIGHT_RING_FINGER);
                break;
            case R.id.right_middle_finger_bt:
                gatherActivity.setFingerNUM(RIGHT_MIDDLE_FINGER);
                gatherFingerprint(intent, RIGHT_MIDDLE_FINGER);
                break;
            case R.id.right_index_finger_bt:
                gatherActivity.setFingerNUM(RIGHT_INDEX_FINGER);
                gatherFingerprint(intent, RIGHT_INDEX_FINGER);
                break;
            case R.id.right_thumb_bt:
                gatherActivity.setFingerNUM(RIGHT_THUMB_FINGER);
                gatherFingerprint(intent, RIGHT_THUMB_FINGER);
                break;
            default:
                break;
        }
    }

    private void gatherFingerprint(Intent intent, int fingerNUm) {
//        intent.setClass(gatherActivity, GatherFingerprintActivity.class);
//        intent.putExtra("gatherID", gatherActivity.getGatherID());
//        intent.putExtra("fingerNum", fingerNUm);
//        getActivity().startActivityForResult(intent, GatherActivity.FINGERPRINT_ACTIVITY);
    }
}
