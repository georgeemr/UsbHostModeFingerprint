package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.futronictech.AnsiSDKLib;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.application.App;
import com.xiongdi.recognition.audio.AudioPlay;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.fragment.ProgressDialogFragment;
import com.xiongdi.recognition.helper.OperateCardHelper;
import com.xiongdi.recognition.util.AESUtil;
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.StringUtil;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.util.UsbManagerUtil;
import com.xiongdi.recognition.widget.progressBar.ProgressBarView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by moubiao on 2016/3/25.
 * 验证身份信息界面
 */
public class VerifyResultActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "moubiao";
    private final String KEY = "0123456789abcdef";
    private final int SCAN_BARCODE_REQUEST_CODE = 10000;
    private final int SEARCH_REQUEST_CODE = 10001;
    private static final int READ_CARD_FLAG = 0;
    private final float MATCH_SCORE = 93.0f;
    private static final int MESSAGE_SHOW_IMAGE = 2;
    private static final int MESSAGE_SHOW_ERROR_MSG = 3;

    private DrawerLayout drawer;
    private ImageView pictureIMG, fingerIMG;
    private ProgressBarView mProgressBarView;
    private TextView personIDTV, personNameTV, personGenderTV, personBirthdayTV, personAddressTV;
    private ImageButton backTB, readCardBT, passportBT, verifyBT;

    private OperateCardHelper mOperateCardHelper;
    private ReadCardHandler mReadCardHandler;
    private ReadCardThread mReadCardThread;
    private boolean mReadSuccess = false;
    private ProgressDialogFragment progressDialog;

    private UsbManagerUtil mUsbManagerUtil;
    private VerifyThread mVerifyThread;
    private VerifyHandler mHandler;
    private UsbDeviceDataExchangeImpl usb_host_ctx;
    private boolean verifyPass = false;
    private Bitmap mFingerBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_result_ayout);

        initData();
        initView();
        setListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOperateCardHelper.closeRFModel();

        if (mVerifyThread != null) {
            mVerifyThread.cancel();
        }

        usb_host_ctx.closeDevice();
        usb_host_ctx.releaseResource();
    }

    private void initData() {
        mUsbManagerUtil = new UsbManagerUtil(getApplicationContext(), new RequestPermissionHandler(this));
        mOperateCardHelper = new OperateCardHelper(this);
        mOperateCardHelper.openRFModel();
        mReadCardHandler = new ReadCardHandler(this);
        progressDialog = new ProgressDialogFragment();
        mHandler = new VerifyHandler(this);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.common_navigation_drawer_open, R.string.common_navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();

        ((TextView) findViewById(R.id.verify_ID).findViewById(R.id.verify_title_tv)).setText(R.string.info_title_ID);
        ((TextView) findViewById(R.id.verify_name).findViewById(R.id.verify_title_tv)).setText(R.string.info_item_title_name);
        ((TextView) findViewById(R.id.verify_gender).findViewById(R.id.verify_title_tv)).setText(R.string.info_item_title_gender);
        ((TextView) findViewById(R.id.verify_birthday).findViewById(R.id.verify_title_tv)).setText(R.string.info_item_title_birthday);
        ((TextView) findViewById(R.id.verify_address).findViewById(R.id.verify_title_tv)).setText(R.string.info_item_title_address);

        pictureIMG = (ImageView) findViewById(R.id.verify_photo_img);
        fingerIMG = (ImageView) findViewById(R.id.verify_finger_img);
        mProgressBarView = (ProgressBarView) findViewById(R.id.scan_finger_progress);
        personIDTV = (TextView) findViewById(R.id.verify_ID).findViewById(R.id.verify_content_tv);
        personNameTV = (TextView) findViewById(R.id.verify_name).findViewById(R.id.verify_content_tv);
        personGenderTV = (TextView) findViewById(R.id.verify_gender).findViewById(R.id.verify_content_tv);
        personBirthdayTV = (TextView) findViewById(R.id.verify_birthday).findViewById(R.id.verify_content_tv);
        personAddressTV = (TextView) findViewById(R.id.verify_address).findViewById(R.id.verify_content_tv);

        backTB = (ImageButton) findViewById(R.id.bottom_left_bt);
        verifyBT = (ImageButton) findViewById(R.id.bottom_right_bt);
        if (verifyBT != null) {
            verifyBT.setBackgroundResource(R.drawable.common_gather_fingerprint);
        }

        passportBT = (ImageButton) findViewById(R.id.bottom_second_bt);
        if (passportBT != null) {
            passportBT.setVisibility(View.GONE);
            passportBT.setBackgroundResource(R.drawable.common_passport_bg);
        }

        readCardBT = (ImageButton) findViewById(R.id.bottom_first_bt);
        if (readCardBT != null) {
            readCardBT.setBackgroundResource(R.drawable.common_read_card_bg);
        }
    }

    private void setListener() {
        backTB.setOnClickListener(this);
        verifyBT.setOnClickListener(this);
        passportBT.setOnClickListener(this);
        readCardBT.setOnClickListener(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_left_bt:
                finish();
                break;
            case R.id.bottom_right_bt:
                if (mUsbManagerUtil.OpenDevice(0, true)) {
                    verifyFingerprint();
                }
                break;
            case R.id.bottom_first_bt:
                readCard();
                break;
            case R.id.bottom_second_bt:

                break;
            default:
                break;
        }
    }

    private void showProgressBar(boolean show) {
        mProgressBarView.setVisibility(show ? View.VISIBLE : View.GONE);
        fingerIMG.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * 验证指纹
     */
    private void verifyFingerprint() {
        if (App.FINGERPRINT_PATH == null) {
            Log.e(TAG, "verifyFingerprint: fingerprint file path is null");
            ToastUtil.getInstance().showToast(this, getString(R.string.no_fingerprint_template));
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
                Log.d(TAG, "verifyFingerprint: fingerprint path " + App.FINGERPRINT_PATH);
                fingerprintFile = new File(App.FINGERPRINT_PATH);
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

            showProgressBar(true);
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
                        verifyPass = matchResult[0] > matchScore;
                        mHandler.obtainMessage(MESSAGE_SHOW_IMAGE).sendToTarget();
                        Log.d(TAG, "run: verify passed match result = " + matchResult[0] + " success = " + verifyPass);
                        if (verifyPass) {
                            break;
                        }
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
        private WeakReference<VerifyResultActivity> mWeakReference;
        private int audioType;
        private AudioPlay mAudioPlay;
        private AssetManager mAssetManager;


        public VerifyHandler(VerifyResultActivity activity) {
            mWeakReference = new WeakReference<>(activity);
            mAudioPlay = new AudioPlay();
            mAssetManager = activity.getAssets();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final VerifyResultActivity activity = mWeakReference.get();
            switch (msg.what) {
                case MESSAGE_SHOW_ERROR_MSG:
                    String showErr = (String) msg.obj;
                    ToastUtil.getInstance().showToast(activity, showErr);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    activity.fingerIMG.setImageBitmap(activity.mFingerBitmap);
                    if (activity.verifyPass) {
                        mAudioPlay.resetMediaPlayer();
                        audioType = AudioPlay.VERIFY_PASSED;
                        mAudioPlay.playAsset(audioType, mAssetManager);
                        activity.showProgressBar(false);
                        PersonDao personDao = new PersonDao(activity);
                        Long recordCount = personDao.getQuantity();
                        Person person = personDao.queryById(Integer.parseInt(String.valueOf(recordCount)));
                        personDao.updateColumn("UPDATE person SET mChecked = 1 WHERE ID = " + person.getID());
                        activity.setResultDetail(person);
                    } else {
                        audioType = AudioPlay.VERIFY_FAILED;
                        mAudioPlay.playAsset(audioType, mAssetManager);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 读卡
     */
    private void readCard() {
        showProgressBar(false);
        progressDialog.setData(getString(R.string.reading_from_card));
        progressDialog.show(getSupportFragmentManager(), "save");

        mReadCardThread = new ReadCardThread();
        mReadCardThread.start();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (mVerifyThread != null) {
            showProgressBar(false);
            mVerifyThread.cancel();
        }
        Intent intent = new Intent();
        switch (item.getItemId()) {
            case R.id.nav_scan_barcode:
                intent.setClass(VerifyResultActivity.this, ScanBarcodeActivity.class);
                startActivityForResult(intent, SCAN_BARCODE_REQUEST_CODE);
                break;
            case R.id.nav_input_CNID:
                intent.setClass(VerifyResultActivity.this, SearchActivity.class);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
                break;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mVerifyThread != null) {
            showProgressBar(false);
            mVerifyThread.cancel();
        }
        Intent intent = new Intent();
        switch (item.getItemId()) {
            case R.id.menu_search:
                intent.setClass(VerifyResultActivity.this, SearchActivity.class);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
                break;
            case R.id.menu_scan_barcode:
                intent.setClass(VerifyResultActivity.this, ScanBarcodeActivity.class);
                startActivityForResult(intent, SCAN_BARCODE_REQUEST_CODE);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 读卡的线程
     */
    private class ReadCardThread extends Thread {
        @Override
        public void run() {
            mReadSuccess = mOperateCardHelper.readM1Card();
            Message message = Message.obtain();
            message.what = READ_CARD_FLAG;
            mReadCardHandler.sendMessage(message);
        }
    }

    /**
     * 处理读卡的handler
     */
    private static class ReadCardHandler extends Handler {
        private WeakReference<VerifyResultActivity> mWeakReference;

        public ReadCardHandler(VerifyResultActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final VerifyResultActivity activity = mWeakReference.get();
            switch (msg.what) {
                case READ_CARD_FLAG:
                    activity.progressDialog.dismiss();
                    if (activity.mReadSuccess) {
                        String[] cardData = activity.mOperateCardHelper.getBaseData();
                        PersonDao personDao = new PersonDao(activity);
                        Person person = personDao.queryById(Integer.parseInt(cardData[0]));
                        if (person.getChecked() == 1) {
                            ToastUtil.getInstance().showToast(activity, "has checked!");
                            activity.refreshView();
                            return;
                        }
                        activity.personIDTV.setText(String.valueOf(cardData[0]));
                        activity.personNameTV.setText(cardData[1]);
                        activity.personGenderTV.setText(cardData[2]);
                        activity.personBirthdayTV.setText(cardData[3]);
                        activity.personAddressTV.setText(cardData[4]);
                        Bitmap bitmap = activity.mOperateCardHelper.getPicture();
                        if (bitmap != null) {
                            activity.pictureIMG.setImageBitmap(bitmap);
                        } else {
                            activity.pictureIMG.setImageResource(R.drawable.person_photo);
                        }
                        if (!StringUtil.hasLength(cardData[1])) {
                            ToastUtil.getInstance().showToast(activity, activity.getString(R.string.common_no_data));
                        }
                        activity.verifyFingerprint();
                        break;
                    }
                default:
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SCAN_BARCODE_REQUEST_CODE:
                    if (data != null) {
                        try {
                            int id = Integer.parseInt(data.getStringExtra("scan_id"));
                            showBarcodeData(id);
                        } catch (java.lang.NumberFormatException e) {
                            ToastUtil.getInstance().showToast(this, getString(R.string.common_invalid_data));
                            e.printStackTrace();
                        }
                    }
                    break;
                case SEARCH_REQUEST_CODE:
                    App app = (App) getApplication();
                    setResultDetail(app.getPerson());
                    break;
                default:
                    break;
            }
        } else {
            switch (requestCode) {
                case SCAN_BARCODE_REQUEST_CODE:
                    refreshView();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 显示扫描二维码得到的数据
     */
    private void showBarcodeData(int id) {
        Observable.just(id)
                .map(new Func1<Integer, Person>() {
                    @Override
                    public Person call(Integer ID) {
                        PersonDao personDao = new PersonDao(getApplicationContext());
                        return personDao.queryById(ID);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Person>() {
                    @Override
                    public void call(Person person) {
                        if (person == null) {
                            ToastUtil.getInstance().showToast(VerifyResultActivity.this, getString(R.string.common_no_data));
                        } else if (person.getChecked() == 0) {
                            setResultDetail(person);
                        } else {
                            ToastUtil.getInstance().showToast(VerifyResultActivity.this, "has checked!");
                        }
                    }
                });
    }

    private void setResultDetail(final Person person) {
        if (person != null) {
            App.FINGERPRINT_PATH = person.getFingerprint();
            final String picPath = person.getPicture();
            final String decryptPath = picPath + ".png";
            Observable.create(new Observable.OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> subscriber) {
                    if (picPath != null) {
                        decryptFile(picPath, decryptPath);
                    }
                    subscriber.onCompleted();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Object>() {
                        @Override
                        public void onCompleted() {
                            personIDTV.setText(String.format(Locale.getDefault(), "%1$,05d", person.getID()));
                            personNameTV.setText(person.getName());
                            personGenderTV.setText(person.getGender());
                            personBirthdayTV.setText(person.getBirthday());
                            personAddressTV.setText(person.getAddress());
                            Bitmap bitmap = BitmapFactory.decodeFile(decryptPath);
                            if (bitmap != null) {
                                pictureIMG.setImageBitmap(bitmap);
                            }
                            new FileUtil().deleteFile(decryptPath);
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Object o) {

                        }
                    });
        }
    }

    /**
     * 解密照片
     */
    private void decryptFile(String filePath, String decryptPath) {
        AESUtil aesUtil2 = new AESUtil.Builder()
                .key(KEY)
                .keySize(256)
                .mode(Cipher.DECRYPT_MODE)
                .transformation("AES/CFB/PKCS5Padding")
                .ivParameter(new IvParameterSpec(new byte[16]))
                .builder();
        aesUtil2.decryptFile(filePath, decryptPath);
    }

    private void refreshView() {
        personIDTV.setText("");
        personNameTV.setText("");
        personGenderTV.setText("");
        personBirthdayTV.setText("");
        personAddressTV.setText("");
        pictureIMG.setImageResource(R.drawable.person_photo);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 处理请求USB设备权限的handler
     */
    private static class RequestPermissionHandler extends Handler {
        private WeakReference<VerifyResultActivity> mWeakReference;

        public RequestPermissionHandler(VerifyResultActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final VerifyResultActivity activity = mWeakReference.get();
            switch (msg.what) {
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE: {//同意使用usb设备的权限申请
                    if (activity != null) {
                        activity.verifyFingerprint();
                    }
                    break;
                }
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE: {//拒绝使用usb设备的权限申请
                    break;
                }
                default:
                    break;
            }
        }
    }
}
