package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.application.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by moubiao on 2016/3/23.
 * 拍照的activity
 */
public class GatherPictureActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private final String TAG = "moubiao";
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;

    private SurfaceView previewSFV;
    private ImageButton takeBT;

    private Camera mCamera;
    private SurfaceHolder holder;

    private String gatherID;
    private String pictureUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_picture_layout);

        initView();
        setListener();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initView() {
        previewSFV = (SurfaceView) findViewById(R.id.picture_SurfaceView);
        holder = previewSFV.getHolder();
        holder.addCallback(GatherPictureActivity.this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        takeBT = (ImageButton) findViewById(R.id.take_picture_bt);
    }

    private void setListener() {
        takeBT.setOnClickListener(this);
    }

    private void initData() {
        Intent data = getIntent();
        gatherID = data.getStringExtra("pictureName");

        DetectScreenOrientation detectScreenOrientation = new DetectScreenOrientation(this);
        detectScreenOrientation.enable();
    }

    private boolean focus = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture_bt:
                if (!focus) {
                    takePicture();
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
            takePicture();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 拍照
     */
    private void takePicture() {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                focus = success;
                if (success) {
                    mCamera.cancelAutoFocus();
                    mCamera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {
                        }
                    }, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            savePicture(data);
                            Intent intentData = new Intent();
                            intentData.putExtra("pictureUrl", pictureUrl);
                            setResult(Activity.RESULT_OK, intentData);
                            finish();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
        setCameraParams();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void initCamera() {
        try {
            mCamera = Camera.open(0);//1:采集指纹的摄像头. 0:拍照的摄像头.
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Toast.makeText(this, "camera open failed!", Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
    }

    private void setCameraParams() {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters parameters = mCamera.getParameters();

            int orientation = judgeScreenOrientation();
            if (Surface.ROTATION_0 == orientation) {
                mCamera.setDisplayOrientation(90);
                parameters.setRotation(90);
            } else if (Surface.ROTATION_90 == orientation) {
                mCamera.setDisplayOrientation(0);
                parameters.setRotation(0);
            } else if (Surface.ROTATION_180 == orientation) {
                mCamera.setDisplayOrientation(180);
                parameters.setRotation(180);
            } else if (Surface.ROTATION_270 == orientation) {
                mCamera.setDisplayOrientation(180);
                parameters.setRotation(180);
            }

            parameters.setPictureSize(320, 240);//192 144  160 120 240 180 264 198 320 240
            parameters.setPreviewSize(320, 240);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断屏幕方向
     *
     * @return 0：竖屏 1：左横屏 2：反向竖屏 3：右横屏
     */
    private int judgeScreenOrientation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void savePicture(byte[] data) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String pictureName = gatherID + ".png";
//            String savePath = getExternalFilesDir(null) + "/" + gatherID + "/";
            String savePath = App.EXTERNAL_SD_PATH + File.separator + gatherID;
            pictureUrl = savePath + File.separator + pictureName;
            File saveFolder = new File(savePath);
            if (!saveFolder.exists()) {
                if (saveFolder.mkdirs()) {
                    Log.e(TAG, "savePicture: create picture directory failed!");
                }
            }
            File pictureFile = new File(saveFolder, pictureName);
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 用来监测左横屏和右横屏切换时旋转摄像头的角度
     */
    private class DetectScreenOrientation extends OrientationEventListener {
        public DetectScreenOrientation(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (260 < orientation && orientation < 290) {
                setCameraParams();
            } else if (80 < orientation && orientation < 100) {
                setCameraParams();
            }
        }
    }
}
