package com.xiongdi.recognition.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.util.ToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by moubiao on 2016/3/25.
 * 验证指纹界面
 */
public class VerifyFingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";

    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;

    private final float MATCH_SCORE = 93.0f;

    private ImageView fingerIMG;
    private ImageButton takeBT;
    private Bitmap mFingerBitmap;

    private UsbDeviceDataExchangeImpl usb_host_ctx;
    private VerifyHandler mHandler;
    private VerifyThread mVerifyThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initData();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyFingerprint();
    }

    private void initData() {
        mHandler = new VerifyHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);
    }

    private void initView() {
        fingerIMG = (ImageView) findViewById(R.id.fingerprint_img);
        takeBT = (ImageButton) findViewById(R.id.take_fingerprint_bt);
    }

    private void setListener() {
        takeBT.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_fingerprint_bt:
                verifyFingerprint();
                break;
            default:
                break;
        }
    }

    /**
     * 验证指纹
     */
    private void verifyFingerprint() {
        if (MainApplication.fingerprintPath == null) {
            Log.e(TAG, "verifyFingerprint: fingerprint file path is null");
            ToastUtil.getInstance().showToast(this, "fingerprint file path is null");
            return;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "verifyFingerprint: external storage is not mounted");
            ToastUtil.getInstance().showToast(this, "external storage is not mounted");
            return;
        }
        if (usb_host_ctx.OpenDevice(0, true)) {
            byte[] fingerprintContent = null;
            File fingerprintFile;
            FileInputStream fis = null;
            try {
                Log.d(TAG, "verifyFingerprint: fingerprint path " + MainApplication.fingerprintPath);
                fingerprintFile = new File(MainApplication.fingerprintPath);
                if (!fingerprintFile.exists() || !fingerprintFile.canRead()) {
                    Log.e(TAG, "verifyFingerprint: fingerprint file no exist");
                    return;
                }
                fingerprintContent = new byte[(int) fingerprintFile.length()];
                fis = new FileInputStream(fingerprintFile);
                if (-1 == fis.read(fingerprintContent)) {
                    Log.e(TAG, "verifyFingerprint: fingerprint content is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mVerifyThread = new VerifyThread(0, fingerprintContent, MATCH_SCORE);
            mVerifyThread.start();
        } else {
            ToastUtil.getInstance().showToast(this, "open usb host mode failed!");
        }
    }

    /**
     * 验证指纹的线程
     */
    private class VerifyThread extends Thread {
        private AnsiSDKLib ansi_lib = null;
        private int fingerIndex = 0;//手指的编号
        private byte[] templeData = null;//从文件里读取到的指纹数据
        private float matchScore = 0;//匹配的标准值
        private boolean cancel = false;

        public VerifyThread(int fingerIndex, byte[] templeData, float matchScore) {
            ansi_lib = new AnsiSDKLib();
            this.fingerIndex = fingerIndex;
            this.templeData = templeData;
            this.matchScore = matchScore;
        }

        public void cancel() {
            cancel = true;
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean isCanceled() {
            return cancel;
        }

        @Override
        public void run() {
            boolean dev_open = false;
            try {
                //打开设备
                if (!ansi_lib.OpenDeviceCtx(usb_host_ctx)) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    Log.e(TAG, "run: open fingerprint device failed!");
                    return;
                }
                dev_open = true;
                //计算指纹图像的大小
                if (!ansi_lib.FillImageSize()) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    Log.e(TAG, "run: get fingerprint image size failed!");
                    return;
                }
                byte[] img_buffer = new byte[ansi_lib.GetImageSize()];
                //验证指纹
                while (true) {
                    Log.d(TAG, "run: search fingerprint---->");
                    if (isCanceled()) {
                        break;
                    }
                    float[] matchResult = new float[1];
                    if (ansi_lib.VerifyTemplate(fingerIndex, templeData, img_buffer, matchResult)) {
                        mFingerBitmap = createFingerBitmap(ansi_lib.GetImageWidth(), ansi_lib.GetImageHeight(), img_buffer);
                        mHandler.obtainMessage(MESSAGE_SHOW_IMAGE).sendToTarget();
                        boolean pass = matchResult[0] > matchScore;
                        Log.d(TAG, "run: verify passed match result = " + matchResult[0] + " success = " + pass);
                        break;
                    } else {
                        int lastError = ansi_lib.GetErrorCode();
                        if (lastError == AnsiSDKLib.FTR_ERROR_EMPTY_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_NO_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_MOVABLE_FINGER) {
                            Thread.sleep(100);
                        } else {
                            Log.e(TAG, "run: verify failed");
                            String error = String.format("Verify failed. Error: %s.", ansi_lib.GetErrorMessage());
                            mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, e.getMessage()).sendToTarget();
            }

            //关闭模块
            if (dev_open) {
                ansi_lib.CloseDevice();
            }
        }
    }

    private static class VerifyHandler extends Handler {
        private WeakReference<VerifyFingerprintActivity> mWeakReference;

        public VerifyHandler(VerifyFingerprintActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final VerifyFingerprintActivity activity = mWeakReference.get();
            switch (msg.what) {
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    ToastUtil.getInstance().showToast(activity, showErr);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    activity.fingerIMG.setImageBitmap(activity.mFingerBitmap);
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE: {//同意使用usb设备的权限申请
                    activity.verifyFingerprint();
                    break;
                }
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE: {//拒绝使用usb设备的权限申请
                    activity.verifyFingerprint();
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * 创建指纹的bitmap
     *
     * @param imgWidth  bitmap的宽
     * @param imgHeight bitmap的高
     * @param imgBytes  bitmap的数据
     * @return
     */
    private Bitmap createFingerBitmap(int imgWidth, int imgHeight, byte[] imgBytes) {
        int[] pixels = new int[imgWidth * imgHeight];
        for (int i = 0; i < imgWidth * imgHeight; i++) {
            pixels[i] = imgBytes[i];
        }

        Bitmap emptyBmp = Bitmap.createBitmap(pixels, imgWidth, imgHeight, Bitmap.Config.RGB_565);
        int width, height;
        height = emptyBmp.getHeight();
        width = emptyBmp.getWidth();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(emptyBmp, 0, 0, paint);

        return result;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (mVerifyThread != null) {
            mVerifyThread.cancel();
        }
    }
}
