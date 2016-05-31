package com.xiongdi.recognition.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.util.ToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

/**
 * Created by moubiao on 2016/3/25.
 * 验证指纹界面
 */
public class VerifyFingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";

    private static final int MESSAGE_SHOW_MSG = 1;
    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;

    private ImageView fingerIMG;
    private ImageButton takeBT;
    private Bitmap mFingerBitmap;

    VerifyHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initData();
        initView();
        setListener();
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
        if (usb_host_ctx.OpenDevice(0, true)) {
            byte[] templateContent = null;
            String savePath = getExternalFilesDir(null).getPath() + File.separator + "null_5(ANSI)";
            FileInputStream fs;
            File f;
            try {
                f = new File(savePath);
                if (!f.exists() || !f.canRead()) {
                    throw new FileNotFoundException();
                }
                long nFileSize = f.length();
                fs = new FileInputStream(f);
                byte[] fileContent = new byte[(int) nFileSize];
                fs.read(fileContent);
                fs.close();
                templateContent = fileContent;
            } catch (Exception e) {
                String error = String.format("Failed to load template from file %s. Error: %s.", "null_5(ANSI)", e.toString());
                Log.d(TAG, "verifyFingerprint: " + error);
                e.printStackTrace();
            }

            new VerifyThread(1, templateContent, 0).start();
        } else {
            ToastUtil.getInstance().showToast(this, "open module failed!");
        }
    }

    /**
     * 验证指纹的线程
     */
    private class VerifyThread extends Thread {
        private AnsiSDKLib ansi_lib = null;
        private int mFinger = 0;
        private byte[] mTmpl = null;
        private float mMatchScore = 0;
        private boolean mCanceled = false;

        public VerifyThread(int finger, byte[] template, float matchScore) {
            ansi_lib = new AnsiSDKLib();
            mFinger = finger;
            mTmpl = template;
            mMatchScore = matchScore;
        }

        public boolean isCanceled() {
            return mCanceled;
        }

        public void cancel() {
            mCanceled = true;
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean dev_open = false;
            try {
                //打开设备
                if (!ansi_lib.OpenDeviceCtx(usb_host_ctx)) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    return;
                }
                dev_open = true;
                //计算指纹图像的大小
                if (!ansi_lib.FillImageSize()) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    return;
                }
                byte[] img_buffer = new byte[ansi_lib.GetImageSize()];
                //验证指纹
                for (; ; ) {
                    if (isCanceled()) {
                        break;
                    }
                    float[] matchResult = new float[1];
                    if (ansi_lib.VerifyTemplate(mFinger, mTmpl, img_buffer, matchResult)) {
                        mFingerBitmap = createFingerBitmap(ansi_lib.GetImageWidth(), ansi_lib.GetImageHeight(), img_buffer);
                        mHandler.obtainMessage(MESSAGE_SHOW_IMAGE).sendToTarget();
                        Log.d(TAG, "run: verify passed");
                        break;
                    } else {
                        Log.e(TAG, "run: verify failed");
                        int lastError = ansi_lib.GetErrorCode();
                        if (lastError == AnsiSDKLib.FTR_ERROR_EMPTY_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_NO_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_MOVABLE_FINGER) {
                            Thread.sleep(100);
                        } else {
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
                case MESSAGE_SHOW_MSG:
                    String showMsg = (String) msg.obj;
                    break;
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    ToastUtil.getInstance().showToast(activity, showErr);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    activity.fingerIMG.setImageBitmap(activity.mFingerBitmap);
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE: {
                    if (activity.usb_host_ctx.ValidateContext()) {

                    } else {
                    }

                    break;
                }
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE: {
                    break;
                }
                default:
                    break;

            }
        }
    }
}
