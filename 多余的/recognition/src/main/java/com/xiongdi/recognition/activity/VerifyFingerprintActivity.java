package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.jy.demo.fingerprint.CallDecoder;
import com.example.jy.demo.fingerprint.CallFprint;
import com.opencv.LibImgFun;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.audio.AudioPlay;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by moubiao on 2016/3/25.
 * 验证指纹界面
 */
public class VerifyFingerprintActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, Camera.AutoFocusCallback {
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;
    private int KEY_CODE_FRONT_CAMERA = 27;//前置摄像头

    private SurfaceView previewSFV;
    private ImageButton takeBT;

    private Camera verifyCamera;
    private SurfaceHolder verifyHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initView();
        setListener();
    }

    private void initView() {
        previewSFV = (SurfaceView) findViewById(R.id.fingerprint_SurfaceView);
        takeBT = (ImageButton) findViewById(R.id.take_fingerprint_bt);

        verifyHolder = previewSFV.getHolder();
        verifyHolder.addCallback(this);
        verifyHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void setListener() {
        takeBT.setOnClickListener(this);
    }

    private void initCamera() {
        try {
            verifyCamera = Camera.open(1);//一共两个摄像头，1是采集指纹的. 0是拍照的.
            verifyCamera.setPreviewDisplay(verifyHolder);
        } catch (IOException e) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

        setCameraParams();
    }

    private void setCameraParams() {
        Camera.Parameters parameters = verifyCamera.getParameters();
        verifyCamera.setDisplayOrientation(180);
        parameters.setRotation(180);

        parameters.setPictureSize(640, 480);
        parameters.setPreviewSize(640, 480);

        verifyCamera.setParameters(parameters);
        verifyCamera.startPreview();
    }

    private void releaseCamera() {
        if (verifyCamera != null) {
            verifyCamera.stopPreview();
            verifyCamera.release();
            verifyCamera = null;
        }
    }

    private boolean focus = false;
    private boolean active = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((KEY_CODE_LEFT_BOTTOM == keyCode || KEY_CODE_LEFT_TOP == keyCode
                || KEY_CODE_RIGHT_BOTTOM == keyCode || KEY_CODE_RIGHT_TOP == keyCode) && !focus && !active) {
            active = true;
            verifyCamera.autoFocus(this);
        }

        if (KEY_CODE_FRONT_CAMERA == keyCode) {
            if (event.getRepeatCount() == 25 && verifyCamera != null && !focus) {
                verifyCamera.autoFocus(this);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_fingerprint_bt:
                if (!focus) {
                    verifyCamera.autoFocus(this);
                }
                break;
            default:
                break;
        }
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

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        focus = success;
        if (success) {
            verifyCamera.takePicture(new Camera.ShutterCallback() {
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
                        String filePath = getFilesDir().getParent() + "/" + getResources().getString(R.string.app_name) + ".bmp";
                        int result = LibImgFun.mySaveImage(pix, w, h, filePath);
                        Log.v("crjlog", "result = " + result + "filePath = " + filePath);
                        verifyResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 验证指纹后的结果
     */
    private void verifyResult() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String bmpFile = getFilesDir().getParent() + "/" + getResources().getString(R.string.app_name) + ".bmp";
            String pgmFile = getFilesDir().getParent() + "/" + getResources().getString(R.string.app_name) + ".pgm";
            String xytFile = getFilesDir().getParent() + "/" + getResources().getString(R.string.app_name) + ".xyt";

            CallDecoder.Bmp2Pgm(bmpFile, pgmFile);
            CallFprint.pgmChangeToXyt(pgmFile, xytFile);

            AudioPlay ap = new AudioPlay();
            final int audioType;
            if (null == GatherActivity.fingerPrint_pic_path || GatherActivity.fingerPrint_pic_path.equals("")) {
                //以前是将.xyt文件按的路径保存在sharedPreferences里面，现在将.xyt的路径固定在下面的新路径下
//                SharedPreferences sp = getSharedPreferences("fingerprintPath", Context.MODE_PRIVATE);
//                GatherActivity.fingerPrint_pic_path = sp.getString("fingerprintPath", null);
                GatherActivity.fingerPrint_pic_path = getExternalFilesDir("card") + File.separator + "cardFingerprint.xyt";
            }
            if (null != GatherActivity.fingerPrint_pic_path && !GatherActivity.fingerPrint_pic_path.equals("")) {
                int ret = CallFprint.fprintCompare(xytFile, GatherActivity.fingerPrint_pic_path);
                if (ret >= 16) {
                    Log.d("moubiao", "verify success---->");
                    Toast.makeText(this, "Verify the fingerprint success!", Toast.LENGTH_SHORT).show();
                    audioType = AudioPlay.PLAY_SOUND_FINGER_VERIFICATION;
                    setResult(Activity.RESULT_OK);
                } else {
                    Log.e("moubiao", "verify failed");
                    Toast.makeText(this, "Verify the fingerprint failed!", Toast.LENGTH_SHORT).show();
                    audioType = AudioPlay.PLAY_SOUND_FINGER_AUTHENTICATION_FAIL;
                }
            } else {
                Toast.makeText(this, "No fingerprint data!", Toast.LENGTH_SHORT).show();
                audioType = AudioPlay.PLAY_SOUND_FINGER_AUTHENTICATION_FAIL;
            }

            try {
                AssetManager am = getAssets();
                ap.PlayAsset(audioType, am);
            } catch (IOException e) {
                e.printStackTrace();
            }

            active = false;
            //延时一秒后结束是因为当快速拍照键时，相机服务会报错
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    finish();
                }
            }, 1000);
        }
    }
}
