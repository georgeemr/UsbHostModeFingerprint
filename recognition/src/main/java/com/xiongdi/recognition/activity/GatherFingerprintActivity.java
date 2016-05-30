package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.opencv.LibImgFun;
import com.xiongdi.recognition.R;

import java.io.File;

/**
 * Created by moubiao on 2016/3/23.
 * 采集指纹的界面
 */
public class GatherFingerprintActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;
    private int KEY_CODE_FRONT_CAMERA = 27;//前置摄像头

    private SurfaceView previewSFV;
    private ImageButton takeBT;

    private Camera mCamera;
    private SurfaceHolder holder;

    private String gatherID;
    private int fingerNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initView();
        setListener();
        initData();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initView() {
        previewSFV = (SurfaceView) findViewById(R.id.fingerprint_SurfaceView);
        holder = previewSFV.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        takeBT = (ImageButton) findViewById(R.id.take_fingerprint_bt);
    }

    private void setListener() {
        takeBT.setOnClickListener(this);
    }

    private void initData() {
        Intent data = getIntent();
        gatherID = data.getStringExtra("gatherID");
        fingerNum = data.getIntExtra("fingerNum", -1);
    }

    private void initCamera() {
        try {
            mCamera = Camera.open(1);//一共两个摄像头，1是采集指纹的. 0是拍照的.
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

        setCameraParams();
    }

    private void setCameraParams() {
        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setDisplayOrientation(180);
        parameters.setRotation(180);

        parameters.setPictureSize(640, 480);
        parameters.setPreviewSize(640, 480);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    private boolean focus = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_fingerprint_bt:
                if (!focus) {
                    gatherFingerprint();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((KEY_CODE_LEFT_BOTTOM == keyCode || KEY_CODE_LEFT_TOP == keyCode
                || KEY_CODE_RIGHT_BOTTOM == keyCode || KEY_CODE_RIGHT_TOP == keyCode) && !focus) {
            gatherFingerprint();
        }

        if (KEY_CODE_FRONT_CAMERA == keyCode) {
            if (event.getRepeatCount() == 25 && mCamera != null && !focus) {
                gatherFingerprint();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 采集指纹
     */
    private void gatherFingerprint() {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                focus = success;
                if (success) {
                    mCamera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {
                        }
                    }, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            try {
                                int w = camera.getParameters().getPictureSize().width;
                                int h = camera.getParameters().getPictureSize().height;
                                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                Bitmap bmp2 = BitmapFactory.decodeByteArray(data, 0, data.length);

                                int[] pix = new int[w * h];
                                bmp.getPixels(pix, 0, w, 0, 0, w, h);
                                bmp2.getPixels(pix, 0, w, 0, 0, w, h);

                                String filename = gatherID + "_" + fingerNum + ".bmp";

                                File fingerprintDir = new File(getExternalFilesDir(null) + "/"
                                        + getResources().getString(R.string.app_name) + "/" + gatherID + "/");
                                if (!fingerprintDir.exists()) {
                                    fingerprintDir.mkdirs();
                                }

                                String filePath31 = getExternalFilesDir(null) + "/"
                                        + getResources().getString(R.string.app_name) + "/" + gatherID + "/" + filename;

                                int result = LibImgFun.mySaveImage(pix, w, h, filePath31);
                                Log.v("moubiao", "result = " + result);
                                setResult(Activity.RESULT_OK);
                                finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
}
