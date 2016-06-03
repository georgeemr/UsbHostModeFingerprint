package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
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
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Created by moubiao on 2016/3/23.
 * 采集指纹的界面
 */
public class GatherFingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";

    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;
    private static final String kAnsiTemplatePostfix = "(ANSI)";
    private static final String kIsoTemplatePostfix = "(ISO)";

    private ImageView fingerprintIMG;
    private ImageButton takeBT;
    private Bitmap mFingerBitmap;

    private GatherFingerThread gatherThread;
    private FingerprintHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;

    private String gatherID;
    private int fingerNum;
    private StringBuilder templeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_fingerprint_layout);

        initData();
        initView();
        setInnerListener();
        gatherFingerprint();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        usb_host_ctx.closeDevice();
        usb_host_ctx.releaseResource();
    }

    private void initData() {
        mHandler = new FingerprintHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);

        Intent data = getIntent();
        gatherID = data.getStringExtra("gatherID");
        fingerNum = data.getIntExtra("fingerNum", -1);
        templeName = new StringBuilder();
        templeName.append(gatherID);
        templeName.append("_");
        templeName.append(fingerNum);
        templeName.append(kAnsiTemplatePostfix);
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
            gatherThread = new GatherFingerThread(fingerNum, true, false);
            gatherThread.start();
        } else {
            ToastUtil.getInstance().showToast(this, "open usb host mode failed!");
        }
    }

    /**
     * 采集指纹的线程
     */
    private class GatherFingerThread extends Thread {
        private AnsiSDKLib ansi_lib = null;
        private int mFingerIndex = 0;
        private boolean mSaveAnsi = true;
        private boolean mSaveIso = false;
        private boolean mCanceled = false;

        public GatherFingerThread(int fingerIndex, boolean saveAnsi, boolean saveIso) {
            ansi_lib = new AnsiSDKLib();
            mFingerIndex = fingerIndex;
            mSaveAnsi = saveAnsi;
            mSaveIso = saveIso;
        }

        public void cancel() {
            mCanceled = true;
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean isCanceled() {
            return mCanceled;
        }

        @Override
        public void run() {
            boolean dev_open = false;
            try {
                //打开设备
                if (!ansi_lib.OpenDeviceCtx(usb_host_ctx)) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    Log.e(TAG, "run: open fingerprint module failed " + ansi_lib.GetErrorMessage());
                    return;
                }
                dev_open = true;
                //计算指纹图像的大小
                if (!ansi_lib.FillImageSize()) {
                    mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, ansi_lib.GetErrorMessage()).sendToTarget();
                    Log.e(TAG, "run: get image size failed " + ansi_lib.GetErrorMessage());
                    return;
                }
                byte[] img_buffer = new byte[ansi_lib.GetImageSize()];
                //采集指纹
                while (true) {
                    if (isCanceled()) {
                        break;
                    }

                    int templeMaxSize = ansi_lib.GetMaxTemplateSize();
                    byte[] template = new byte[templeMaxSize];
                    byte[] templateIso = new byte[templeMaxSize];
                    int[] realSize = new int[1];
                    int[] realIsoSize = new int[1];

                    if (ansi_lib.CreateTemplate(mFingerIndex, img_buffer, template, realSize)) {//采集指纹成功
                        mFingerBitmap = createFingerBitmap(ansi_lib.GetImageWidth(), ansi_lib.GetImageHeight(), img_buffer);
                        mHandler.obtainMessage(MESSAGE_SHOW_IMAGE).sendToTarget();

                        //如果是ansi格式的直接保存
                        if (mSaveAnsi) {
                            saveTemplate(template, realSize[0]);
                        }
                        //ansi格式转换为iso格式再保存
                        if (mSaveIso) {
                            realIsoSize[0] = templeMaxSize;
                            if (ansi_lib.ConvertAnsiTemplateToIso(template, templateIso, realIsoSize)) {
                                saveTemplate(templateIso, realIsoSize[0]);
                            } else {
                                String error = String.format("iso Convert to ansi failed. Error: %s.", ansi_lib.GetErrorMessage());
                                mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
                                Log.e(TAG, "run: ansi convert to iso failed " + ansi_lib.GetErrorMessage());
                            }
                        }
                        setResult(Activity.RESULT_OK);

                        break;
                    } else {//采集指纹失败
                        int lastError = ansi_lib.GetErrorCode();
                        if (lastError == AnsiSDKLib.FTR_ERROR_EMPTY_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_NO_FRAME
                                || lastError == AnsiSDKLib.FTR_ERROR_MOVABLE_FINGER) {
                            Thread.sleep(100);
                        } else {
                            String error = String.format("gather fingerprint failed. Error: %s.", ansi_lib.GetErrorMessage());
                            mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MSG, -1, -1, error).sendToTarget();
                            Log.e(TAG, "run: gather fingerprint failed = " + ansi_lib.GetErrorMessage());
                            setResult(Activity.RESULT_CANCELED);
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

            finish();
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
     * @param template 保存模板数据的数组（大于等于真实的模板数据）
     * @param size     真实的模板数据的大小
     */
    private void saveTemplate(byte[] template, int size) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "saveTemplate: external storage not mounted!");
            return;
        }

        MainApplication.fingerprintPath = getExternalFilesDir(null) + File.separator
                + String.format(Locale.getDefault(), "%1$,05d", Integer.parseInt(gatherID)) + File.separator + templeName;
        FileUtil fileUtil = new FileUtil();
        File saveFile;
        FileOutputStream fos = null;
        try {
            saveFile = fileUtil.createFile(MainApplication.fingerprintPath);
            if (saveFile == null) {
                Log.e(TAG, "saveTemplate: template file create failed!");
                return;
            }
            fos = new FileOutputStream(saveFile);
            byte[] writeTemplate = new byte[size];
            System.arraycopy(template, 0, writeTemplate, 0, size);
            fos.write(writeTemplate);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "saveTemplate: fingerprint path = " + MainApplication.fingerprintPath);
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
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    ToastUtil.getInstance().showToast(activity, showErr);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    activity.fingerprintIMG.setImageBitmap(activity.mFingerBitmap);
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE: {//同意使用usb设备的权限申请
                    activity.gatherFingerprint();

                    break;
                }
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE: {//拒绝使用usb设备的权限申请
                    activity.gatherFingerprint();
                    break;
                }
                default:
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //按返回键时表示取消指纹采集,如果采集成功会自动关闭该activity，不需要手动按返回键
        if (gatherThread != null) {
            gatherThread.cancel();
        }
        setResult(Activity.RESULT_CANCELED);
    }
}
