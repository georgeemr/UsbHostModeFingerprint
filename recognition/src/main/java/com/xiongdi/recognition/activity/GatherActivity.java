package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.xiongdi.OpenJpeg;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.adapter.GatherInfoVpAdapter;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.fragment.LeftHandFragment;
import com.xiongdi.recognition.fragment.PictureFragment;
import com.xiongdi.recognition.fragment.RightHandFragment;
import com.xiongdi.recognition.util.BmpUtil;
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.widget.ProgressDialogFragment;
import com.xiongdi.recognition.widget.crop.Crop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moubiao on 2016/3/22.
 * 采集指纹和头像的activity
 */
public class GatherActivity extends AppCompatActivity implements View.OnClickListener {
    private int KEY_CODE_RIGHT_BOTTOM = 249;
    private int KEY_CODE_LEFT_BOTTOM = 250;
    private int KEY_CODE_LEFT_TOP = 251;
    private int KEY_CODE_RIGHT_TOP = 252;

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
    private String fingerprintUrl;
    Uri mImageCaptureUri;
    FileUtil fileUtil;

    PictureFragment pictureFg;

    private boolean haveInformation = false;//判断是否有有效信息

    public void setFingerNUM(int fingerNUM) {
        this.fingerNUM = fingerNUM;
    }

    private int fingerNUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gather_info_layout);

        iniData();
        initView();
        setListener();
    }

    private void initView() {
        backBT = (ImageButton) findViewById(R.id.bottom_left_bt);
        takePictureBT = (ImageButton) findViewById(R.id.bottom_middle_bt);
        saveBT = (ImageButton) findViewById(R.id.bottom_right_bt);

        gatherTab = (TabLayout) findViewById(R.id.gather_tab);
        gatherVP = (ViewPager) findViewById(R.id.gather_viewpager);
        gatherVP.setAdapter(gatherAdapter);
        gatherVP.setOffscreenPageLimit(2);

        gatherTab.setupWithViewPager(gatherVP);
    }

    private void iniData() {
        LeftHandFragment leftFg = new LeftHandFragment();
        RightHandFragment rightFg = new RightHandFragment();
        pictureFg = new PictureFragment();
        gatherData = new ArrayList<>();
        gatherData.add(leftFg);
        gatherData.add(rightFg);
        gatherData.add(pictureFg);
        List<String> titleVp = new ArrayList<>();
        titleVp.add(getString(R.string.tab_indicator_title_Left));
        titleVp.add(getString(R.string.tab_indicator_title_right));
        titleVp.add(getString(R.string.tab_indicator_title_picture));

        gatherAdapter = new GatherInfoVpAdapter(getSupportFragmentManager(), gatherData, titleVp);

        Intent data = getIntent();
        gatherID = data.getStringExtra("gatherID");

        fileUtil = new FileUtil();
    }

    private void setListener() {
        backBT.setOnClickListener(this);
        takePictureBT.setOnClickListener(this);
        saveBT.setOnClickListener(this);
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
                    new SaveTask().execute();
                } else {
                    Toast.makeText(GatherActivity.this, getString(R.string.no_fingerprint), Toast.LENGTH_SHORT).show();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KEY_CODE_LEFT_BOTTOM == keyCode || KEY_CODE_LEFT_TOP == keyCode
                || KEY_CODE_RIGHT_BOTTOM == keyCode || KEY_CODE_RIGHT_TOP == keyCode) {
            startGatherPictureActivity();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void startGatherPictureActivity() {
        Intent intent = new Intent();
        intent.setClass(GatherActivity.this, GatherPictureActivity.class);
        intent.putExtra("pictureName", gatherID);
        startActivityForResult(intent, PICTURE_ACTIVITY);
    }

    private class SaveTask extends AsyncTask<Void, Void, Void> {
        ProgressDialogFragment progressDialog = new ProgressDialogFragment();

        @Override
        protected void onPreExecute() {
            progressDialog.setData(getString(R.string.saving_to_card));
            progressDialog.show(getSupportFragmentManager(), "save");
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            progressDialog.dismiss();
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
        }
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
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        int size = list.size();
        if (size == 0) {
            ToastUtil.getInstance().showToast(this, "Can not find image crop app");
        } else {
            mImageCaptureUri = Uri.fromFile(new File(pictureUrl));
            beginCrop(mImageCaptureUri);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        deleteTemporaryFile();
    }

    /**
     * 只能删除指纹的中间文件不能删除照片的，如果删除了照片的会影响裁剪
     */
    private void deleteTemporaryFile() {
        fileUtil.deleteFile(getExternalFilesDir(null) + "/" + getResources().getString(R.string.app_name) + "/"
                + gatherID + "/" + gatherID + "_" + fingerNUM + ".bmp");
    }
}
