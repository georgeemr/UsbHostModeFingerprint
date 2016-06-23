package com.xiongdi.recognition.fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.interfaces.GatherFingerprintResultInterface;
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Created by moubiao on 2016/6/23.
 * 采集指纹的dialog
 */
public class GatherFingerprintDialogFragment extends DialogFragment {
    private final String TAG = "moubiao";
    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;
    private static final String kAnsiTemplatePostfix = "(ANSI)";
    private static final String kIsoTemplatePostfix = "(ISO)";

    private ImageView mFingerprintIMG;
    private ProgressBar mProgressBar;
    private Bitmap mFingerBitmap;

    private String gatherID;
    private int fingerNum;
    private StringBuilder templeName;

    private GatherFingerThread gatherThread;
    private ShowFingerprintHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
    }

    private void initData() {
        mHandler = new ShowFingerprintHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(getActivity(), mHandler);

        Bundle data = getArguments();
        gatherID = data.getString("gatherID");
        fingerNum = data.getInt("fingerNum", -1);
        templeName = new StringBuilder();
        templeName.append(gatherID);
        templeName.append("_");
        templeName.append(fingerNum);
        templeName.append(kAnsiTemplatePostfix);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogLayout = inflater.inflate(R.layout.gather_fingerprint_pop_layout, container, false);
        mFingerprintIMG = (ImageView) dialogLayout.findViewById(R.id.new_fingerprint_img);
        mProgressBar = (ProgressBar) dialogLayout.findViewById(R.id.open_power_progress);
        gatherFingerprint();

        return dialogLayout;
    }

    /**
     * 采集指纹
     */
    private void gatherFingerprint() {
        if (usb_host_ctx.OpenDevice(0, true)) {
            gatherThread = new GatherFingerThread(fingerNum, true, false);
            gatherThread.start();
        } else {
            ToastUtil.getInstance().showToast(getActivity(), "open usb host mode failed!");
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
     * @param template 保存模板数据的数组（大于等于真实的模板数据）
     * @param size     真实的模板数据的大小
     */
    private void saveTemplate(byte[] template, int size) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "saveTemplate: external storage not mounted!");
            return;
        }

        MainApplication.fingerprintPath = getActivity().getExternalFilesDir(null) + File.separator
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

    private static class ShowFingerprintHandler extends Handler {
        private WeakReference<GatherFingerprintDialogFragment> mWeakReference;

        public ShowFingerprintHandler(GatherFingerprintDialogFragment fragment) {
            mWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final GatherFingerprintDialogFragment fragment = mWeakReference.get();
            switch (msg.what) {
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    if (fragment != null) {
                        ToastUtil.getInstance().showToast(fragment.getActivity(), showErr);
                        fragment.dismiss();
                    }
                    break;
                case MESSAGE_SHOW_IMAGE:
                    if (fragment != null) {
                        GatherFingerprintResultInterface resultInterface = (GatherFingerprintResultInterface) fragment.getActivity();
                        resultInterface.gatherResultCallback(true);
                        fragment.mFingerprintIMG.setImageBitmap(fragment.mFingerBitmap);
                        fragment.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
