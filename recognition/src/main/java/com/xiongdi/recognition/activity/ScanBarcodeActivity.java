package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.util.ToastUtil;

import cn.bingoogolapple.qrcode.core.QRCodeView;

/**
 * Created by moubiao on 2016/6/8.
 * 扫描二维码的activity
 */
public class ScanBarcodeActivity extends AppCompatActivity implements QRCodeView.Delegate, View.OnClickListener {
    private final String TAG = "moubiao";
    private QRCodeView mQRCodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_barcode_layout);

        initView();
        setInnerListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera();
        mQRCodeView.startSpot();
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        super.onDestroy();
    }

    private void initView() {
        mQRCodeView = (QRCodeView) findViewById(R.id.scan_zxing_view);
        if (mQRCodeView != null) {
            mQRCodeView.setDelegate(this);
        }
        View middleView = findViewById(R.id.bottom_first_bt);
        if (middleView != null) {
            middleView.setVisibility(View.GONE);
        }
        View rightView = findViewById(R.id.bottom_right_bt);
        if (rightView != null) {
            rightView.setVisibility(View.GONE);
        }
    }

    private void setInnerListener() {
        View backView = findViewById(R.id.bottom_left_bt);
        if (backView != null) {
            backView.setOnClickListener(this);
        }
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        ToastUtil.getInstance().showToast(this, result);
        vibrate();
        Intent data = new Intent();
        data.putExtra("barcode", result);
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        ToastUtil.getInstance().showToast(this, getString(R.string.camera_open_failed));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_left_bt:
                finish();
                break;
            default:
                break;
        }
    }
}
