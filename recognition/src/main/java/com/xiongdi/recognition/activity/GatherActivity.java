package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.OpenJpeg;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.adapter.GatherInfoVpAdapter;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.fragment.GatherFingerDialogFragment;
import com.xiongdi.recognition.fragment.LeftHandFragment;
import com.xiongdi.recognition.fragment.PictureFragment;
import com.xiongdi.recognition.util.BmpUtil;
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.widget.crop.Crop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by moubiao on 2016/3/22.
 * 采集指纹和头像的activity
 */
public class GatherActivity extends AppCompatActivity implements View.OnClickListener, GatherFingerDialogFragment.GatherResultCallback {
    private final String TAG = "moubiao";
    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;
    private static final String kAnsiTemplatePostfix = "(ANSI)";
    private static final String kIsoTemplatePostfix = "(ISO)";

    public final static int PICTURE_ACTIVITY = 0;//采集照片
    public final static int FINGERPRINT_ACTIVITY = 1;//采集指纹
    private static final int CROP_FROM_CAMERA = 6709;//裁剪照片

    public static String fingerPrint_pic_path = "";

    private TabLayout gatherTab;
    private ViewPager gatherVP;
    private ImageButton backBT, takePictureBT, saveBT;
    private GatherInfoVpAdapter gatherAdapter;
    private List<Fragment> gatherData;

    private String gatherID;
    private String pictureUrl;
    private String compressPicUrl;
    private int fingerNUM;
    Uri mImageCaptureUri;

    FileUtil fileUtil;
    PictureFragment pictureFg;

    private boolean haveInformation = false;//判断是否有有效信息

    private GatherFingerThread gatherThread;
    private ShowFingerprintHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;
    private StringBuilder templeName;
    GatherFingerDialogFragment mFingerDialogFG;
    private Bitmap mFingerBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_info_layout);

        iniData();
        initView();
        setListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelGather();
    }

    private void iniData() {
        LeftHandFragment leftFg = new LeftHandFragment();
        pictureFg = new PictureFragment();
        gatherData = new ArrayList<>();
        gatherData.add(leftFg);
        gatherData.add(pictureFg);
        List<String> titleVp = new ArrayList<>();
        titleVp.add(getString(R.string.tab_indicator_title_finger));
        titleVp.add(getString(R.string.tab_indicator_title_picture));

        gatherAdapter = new GatherInfoVpAdapter(getSupportFragmentManager(), gatherData, titleVp);

        Intent data = getIntent();
        gatherID = data.getStringExtra("gatherID");
        templeName = new StringBuilder();
        templeName.append(gatherID);
        templeName.append("_");
        templeName.append(fingerNUM);
        templeName.append(kAnsiTemplatePostfix);
        mFingerDialogFG = new GatherFingerDialogFragment();
        mFingerDialogFG.setCancelable(false);
        mFingerDialogFG.setResultCallback(this);
        mHandler = new ShowFingerprintHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(getApplicationContext(), mHandler);

        fileUtil = new FileUtil();
    }

    private void initView() {
        backBT = (ImageButton) findViewById(R.id.bottom_left_bt);
        takePictureBT = (ImageButton) findViewById(R.id.bottom_middle_bt);
        saveBT = (ImageButton) findViewById(R.id.bottom_right_bt);
        if (saveBT != null) {
            saveBT.setBackgroundResource(R.drawable.common_save_bg);
        }

        gatherTab = (TabLayout) findViewById(R.id.gather_tab);
        gatherVP = (ViewPager) findViewById(R.id.gather_viewpager);
        if (gatherVP != null) {
            gatherVP.setAdapter(gatherAdapter);
            gatherVP.setOffscreenPageLimit(2);
        }

        gatherTab.setupWithViewPager(gatherVP);
    }

    private void setListener() {
        backBT.setOnClickListener(this);
        takePictureBT.setOnClickListener(this);
        saveBT.setOnClickListener(this);
    }

    public void setFingerNUM(int fingerNUM) {
        this.fingerNUM = fingerNUM;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_left_bt:
                deleteTemporaryFile();
                finish();
                break;
            case R.id.bottom_middle_bt:
                startGatherPictureActivity();
                break;
            case R.id.bottom_right_bt:
                if (haveInformation) {
                    Intent data = new Intent();
                    if (pictureUrl != null) {
                        data.putExtra("pictureUrl", pictureUrl);
                        data.putExtra("compressPicUrl", compressPicUrl);
                    }
                    if (MainApplication.fingerprintPath != null) {
                        data.putExtra("fingerPrintUrl", MainApplication.fingerprintPath);
                    }
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } else {
                    Toast.makeText(GatherActivity.this, getString(R.string.no_fingerprint), Toast.LENGTH_SHORT).show();
                }

                break;
            default:
                break;
        }
    }

    private void startGatherPictureActivity() {
        Intent intent = new Intent();
        intent.setClass(GatherActivity.this, GatherPictureActivity.class);
        intent.putExtra("pictureName", gatherID);
        startActivityForResult(intent, PICTURE_ACTIVITY);
    }

    public String getGatherID() {
        return gatherID;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICTURE_ACTIVITY:
                    pictureUrl = data.getStringExtra("pictureUrl");
                    doCrop();
                    break;
                case FINGERPRINT_ACTIVITY:
                    haveInformation = true;
                    break;
                case CROP_FROM_CAMERA:
                    new CompressTask().execute();
                    break;
                default:
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case FINGERPRINT_ACTIVITY:
                    haveInformation = false;
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void gatherFailed() {
        haveInformation = false;
        cancelGather();
    }

    @Override
    public void gatherSuccess() {
        haveInformation = true;
        cancelGather();
    }

    @Override
    public void gatherAgain() {
        gatherFingerprint();
    }

    private void cancelGather() {
        if (gatherThread != null) {
            gatherThread.cancel();
        }
    }

    private class CompressTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            return compressPicture();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                pictureFg.setPicture(pictureUrl);
            } else {
                ToastUtil.getInstance().showToast(GatherActivity.this, "compress picture failed");
            }
        }
    }

    /**
     * 将照片压缩为.jp2格式
     */

    private boolean compressPicture() {
        Bitmap croppedImage = BitmapFactory.decodeFile(pictureUrl);
        BmpUtil bmpUtil = new BmpUtil();
        String bmpPictureUrl = pictureUrl.substring(0, pictureUrl.length() - 4) + ".bmp";
        bmpUtil.save(croppedImage, bmpPictureUrl);
        compressPicUrl = pictureUrl.substring(0, pictureUrl.length() - 4) + ".jp2";
        OpenJpeg.GetLibVersion();
        if (0 != OpenJpeg.CompressImage(bmpPictureUrl, compressPicUrl, String.valueOf(40))) {
            compressPicUrl = null;
            return false;
        }
        fileUtil.deleteFile(bmpPictureUrl);

        return true;
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(pictureUrl));
        Crop.of(source, destination).asSquare().start(this);
    }

    /**
     * 裁剪照片
     */
    private void doCrop() {
        mImageCaptureUri = Uri.fromFile(new File(pictureUrl));
        beginCrop(mImageCaptureUri);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        deleteTemporaryFile();
        cancelGather();
    }

    /**
     * 只能删除指纹的中间文件不能删除照片的，如果删除了照片的会影响裁剪
     */
    private void deleteTemporaryFile() {
        fileUtil.deleteFile(getExternalFilesDir(null) + "/" + getResources().getString(R.string.app_name) + "/"
                + gatherID + "/" + gatherID + "_" + fingerNUM + ".bmp");
    }

    public void showGatherFingerDialog() {
        mFingerDialogFG.show(getSupportFragmentManager(), "gather");
    }

    public void dismissGatherFingerDialog() {
        mFingerDialogFG.dismiss();
    }

    /**
     * 采集指纹
     */
    public void gatherFingerprint() {
        if (usb_host_ctx.OpenDevice(0, true)) {
            gatherThread = new GatherFingerThread(fingerNUM, true, false);
            gatherThread.start();
        } else {
            ToastUtil.getInstance().showToast(this, "open usb host mode failed!");
            dismissGatherFingerDialog();
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
                        Log.d(TAG, "run: width = " + mFingerBitmap.getWidth() + " height = " + mFingerBitmap.getHeight());
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

    private static class ShowFingerprintHandler extends Handler {
        private WeakReference<GatherActivity> mWeakReference;

        public ShowFingerprintHandler(GatherActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final GatherActivity activity = mWeakReference.get();
            switch (msg.what) {
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    if (activity != null) {
                        ToastUtil.getInstance().showToast(activity, showErr);
                        activity.haveInformation = false;
                        activity.dismissGatherFingerDialog();
                    }
                    break;
                case MESSAGE_SHOW_IMAGE:
                    if (activity != null) {
                        activity.mFingerDialogFG.setFingerprint(activity.mFingerBitmap);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
