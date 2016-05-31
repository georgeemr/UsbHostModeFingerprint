package com.xiongdi.recognition.activity;

import android.content.Intent;
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
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.util.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Created by moubiao on 2016/3/23.
 * 采集指纹的界面
 */
public class GatherFingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;
    private int KEY_CODE_FRONT_CAMERA = 27;//前置摄像头

    private static final int MESSAGE_SHOW_MSG = 1;
    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;
    private static final String kAnsiTemplatePostfix = "(ANSI)";
    private static final String kIsoTemplatePostfix = "(ISO)";

    private ImageView fingerprintIMG;
    private ImageButton takeBT;
    private Bitmap mFingerBitmap;

    FingerprintHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;

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
        mHandler = new FingerprintHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);

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
        if (usb_host_ctx.OpenDevice(0, true)) {
            new GatherFingerThread(fingerNum, true, false, gatherID + "_" + fingerNum).start();
        } else {
            ToastUtil.getInstance().showToast(this, "open module failed!");
        }
    }


    /**
     * 采集指纹的线程
     */
    private class GatherFingerThread extends Thread {
        private AnsiSDKLib ansi_lib = null;
        private int mFinger = 0;
        private boolean mSaveAnsi = true;
        private boolean mSaveIso = false;
        private String mTempleName = "";

        public GatherFingerThread(int finger, boolean saveAnsi, boolean saveIso, String templeName) {
            ansi_lib = new AnsiSDKLib();
            mFinger = finger;
            mSaveAnsi = saveAnsi;
            mSaveIso = saveIso;
            mTempleName = templeName;
        }

        private boolean mCanceled = false;

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
                //采集指纹
                for (; ; ) {
                    if (isCanceled()) {
                        break;
                    }

                    int templeMaxSize = ansi_lib.GetMaxTemplateSize();
                    byte[] template = new byte[templeMaxSize];
                    byte[] templateIso = new byte[templeMaxSize];
                    int[] realSize = new int[1];
                    int[] realIsoSize = new int[1];

                    if (ansi_lib.CreateTemplate(mFinger, img_buffer, template, realSize)) {
                        mFingerBitmap = createFingerBitmap(ansi_lib.GetImageWidth(), ansi_lib.GetImageHeight(), img_buffer);
                        mHandler.obtainMessage(MESSAGE_SHOW_IMAGE).sendToTarget();

                        //如果是ansi格式的直接保存
                        if (mSaveAnsi) {
                            saveTemplate(mTempleName + kAnsiTemplatePostfix, template, realSize[0]);
                        }
                        //如果是iso格式的先转换成ansi再保存
                        if (mSaveIso) {
                            realIsoSize[0] = templeMaxSize;
                            if (ansi_lib.ConvertAnsiTemplateToIso(template, templateIso, realIsoSize)) {
                                saveTemplate(mTempleName + kIsoTemplatePostfix, templateIso, realIsoSize[0]);
                            } else {
                                String error = String.format("iso Convert to ansi failed. Error: %s.", ansi_lib.GetErrorMessage());
                                mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
                            }
                        }

                        break;
                    } else {
                        int lastError = ansi_lib.GetErrorCode();
                        if (lastError == AnsiSDKLib.FTR_ERROR_EMPTY_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_NO_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_MOVABLE_FINGER) {
                            Thread.sleep(100);
                        } else {
                            String error = String.format("Create failed. Error: %s.", ansi_lib.GetErrorMessage());
                            mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, e.getMessage()).sendToTarget();
            }

            //关闭设备
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

    /**
     * 保存模板
     *
     * @param name     模板的名字
     * @param template 保存模板数据的数组（大于等于真实的模板数据）
     * @param size     真实的模板数据的大小
     */
    private void saveTemplate(String name, byte[] template, int size) {
        String savePath = getExternalFilesDir(null).getPath() + File.separator + name;
        Log.d(TAG, "saveTemplate: file = " + savePath);

        File saveFile;
        FileOutputStream fs;
        try {
            saveFile = new File(savePath);
            fs = new FileOutputStream(saveFile);

            byte[] writeTemplate = new byte[size];
            System.arraycopy(template, 0, writeTemplate, 0, size);
            fs.write(writeTemplate);
            fs.close();
        } catch (Exception e) {
            String error = String.format("Failed to save template to file %s. Error: %s.", name, e.toString());
            mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
            e.printStackTrace();
        }
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

    private static class FingerprintHandler extends Handler {
        private WeakReference<GatherFingerprintActivity> mWeakReference;

        public FingerprintHandler(GatherFingerprintActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final GatherFingerprintActivity activity = mWeakReference.get();
            switch (msg.what) {
                case MESSAGE_SHOW_MSG:
                    String showMsg = (String) msg.obj;
                    break;
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    ToastUtil.getInstance().showToast(activity, showErr);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    activity.fingerprintIMG.setImageBitmap(activity.mFingerBitmap);
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
