package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.helper.OperateCardHelper;
import com.xiongdi.recognition.util.StringUtil;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.widget.ProgressDialogFragment;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by moubiao on 2016/3/25.
 * 验证身份信息界面
 */
public class VerifyResultActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "moubiao";
    private final int VERIFY_ACTIVITY = 0;
    private final int KEY_CODE_SCAN_CARD_RIGHT = 249;
    private final int KEY_CODE_SCAN_CARD_LEFT = 250;
    private final int KEY_CODE_VERIFY_FINGERPRINT_LEFT = 251;
    private final int KEY_CODE_VERIFY_FINGERPRINT_RIGHT = 252;
    private final int SCAN_BARCODE_CODE = 1000;
    private static final int READ_CARD_FLAG = 0;

    DrawerLayout drawer;
    private ImageView pictureIMG;
    private TextView personIDTV, personNameTV, personGenderTV, personBirthdayTV, personAddressTV;
    private ImageButton backTB, readCardBT, verifyBT;

    private OperateCardHelper mOperateCardHelper;
    private ReadCardHandler mReadCardHandler;
    private ReadCardThread mReadCardThread;
    private boolean mReadSuccess = false;
    ProgressDialogFragment progressDialog;

    private boolean isExit = false;
    private boolean hasTask = false;
    private Timer tExit;
    private TimerTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_result_ayout);

        initData();
        initView();
        setListener();
    }

    private void initData() {
        mOperateCardHelper = new OperateCardHelper(this);
        mOperateCardHelper.openRFModel();
        mReadCardHandler = new ReadCardHandler(this);
        progressDialog = new ProgressDialogFragment();

        tExit = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                isExit = false;
                hasTask = true;
            }
        };
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
        readCardBT = (ImageButton) findViewById(R.id.bottom_middle_bt);
        if (readCardBT != null) {
            readCardBT.setBackgroundResource(R.drawable.common_read_card_bg);
        }
    }

    private void setListener() {
        backTB.setOnClickListener(this);
        verifyBT.setOnClickListener(this);
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
                if (!isExit) {
                    isExit = true;
                    ToastUtil.getInstance().showToast(this, getString(R.string.common_exit_app));
                    if (!hasTask) {
                        tExit.schedule(task, 2000);
                    }
                } else {
                    finish();
                }
                break;
            case R.id.bottom_right_bt:
                verifyFingerPrint();
                break;
            case R.id.bottom_middle_bt:
                readCard();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KEY_CODE_SCAN_CARD_LEFT == keyCode || KEY_CODE_SCAN_CARD_RIGHT == keyCode) {
            readCard();
        } else if (KEY_CODE_VERIFY_FINGERPRINT_LEFT == keyCode || KEY_CODE_VERIFY_FINGERPRINT_RIGHT == keyCode) {
            verifyFingerPrint();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 读卡
     */
    private void readCard() {
        progressDialog.setData(getString(R.string.reading_from_card));
        progressDialog.show(getSupportFragmentManager(), "save");

        mReadCardThread = new ReadCardThread();
        mReadCardThread.start();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_scan_barcode:
                Intent intent = new Intent();
                intent.setClass(VerifyResultActivity.this, ScanBarcodeActivity.class);
                startActivityForResult(intent, SCAN_BARCODE_CODE);
                break;
            case R.id.nav_input_CNID:

                break;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
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
                        activity.verifyFingerPrint();
                        break;
                    }
                default:
                    break;
            }
        }
    }

    /**
     * 验证指纹
     */
    private void verifyFingerPrint() {
        Intent intent = new Intent(VerifyResultActivity.this, VerifyFingerprintActivity.class);
        startActivityForResult(intent, VERIFY_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case VERIFY_ACTIVITY:
                    PersonDao personDao = new PersonDao(getApplicationContext());
                    Long recordCount = personDao.getQuantity();
                    Person person = personDao.queryById(Integer.parseInt(String.valueOf(recordCount)));
                    if (person != null) {
                        personIDTV.setText(String.format(Locale.getDefault(), "%1$,05d", person.getPersonID()));
                        personNameTV.setText(person.getName());
                        personGenderTV.setText(person.getGender());
                        personBirthdayTV.setText(person.getBirthday());
                        personAddressTV.setText(person.getAddress());
                        Bitmap bitmap = BitmapFactory.decodeFile(person.getGatherPictureUrl());
                        if (bitmap != null) {
                            pictureIMG.setImageBitmap(bitmap);
                        }
                    }
                    break;
                default:
                    break;
            }
        } else {
            refreshView();
        }
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
            if (!isExit) {
                isExit = true;
                ToastUtil.getInstance().showToast(this, getString(R.string.common_exit_app));
                if (!hasTask) {
                    tExit.schedule(task, 2000);
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOperateCardHelper.closeRFModel();
    }
}
