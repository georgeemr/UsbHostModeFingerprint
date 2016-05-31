package com.xiongdi.recognition.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/3/23.
 * 采集指纹的界面
 */
public class GatherFingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;
    private int KEY_CODE_FRONT_CAMERA = 27;//前置摄像头

    private ImageView fingerprintIMG;
    private ImageButton takeBT;

    private String gatherID;
    private int fingerNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initData();
        initView();
        setInnerListener();
    }

    private void initData() {
        Intent data = getIntent();
        gatherID = data.getStringExtra("gatherID");
        fingerNum = data.getIntExtra("fingerNum", -1);
    }

    private void initView() {
        fingerprintIMG = (ImageView) findViewById(R.id.fingerprint_img);
        takeBT = (ImageButton) findViewById(R.id.take_fingerprint_bt);
    }

    private void setInnerListener() {
        takeBT.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_fingerprint_bt:
                gatherFingerprint();

                break;
            default:
                break;
        }
    }

    /**
     * 采集指纹
     */
    private void gatherFingerprint() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((KEY_CODE_LEFT_BOTTOM == keyCode || KEY_CODE_LEFT_TOP == keyCode
                || KEY_CODE_RIGHT_BOTTOM == keyCode || KEY_CODE_RIGHT_TOP == keyCode)) {
            gatherFingerprint();
        }

        if (KEY_CODE_FRONT_CAMERA == keyCode) {
            if (event.getRepeatCount() == 25) {
                gatherFingerprint();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}
