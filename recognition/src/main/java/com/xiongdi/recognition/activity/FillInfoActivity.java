package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.helper.OperateCardHelper;
import com.xiongdi.recognition.interfaces.DatePickerInterface;
import com.xiongdi.recognition.util.DateUtil;
import com.xiongdi.recognition.util.FileUtil;
import com.xiongdi.recognition.util.StringUtil;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.widget.AskDialogFragment;
import com.xiongdi.recognition.widget.DatePickerFragment;
import com.xiongdi.recognition.widget.ProgressDialogFragment;
import com.xiongdi.recognition.widget.SingleChoiceDialogFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Created by moubiao on 2016/3/22.
 * 填写身份信息的activity
 */
public class FillInfoActivity extends AppCompatActivity implements View.OnClickListener, DatePickerInterface, DialogInterface.OnClickListener {
    private static final int DIALOG_SURE = -1;
    private static final int DIALOG_CANCEL = -2;
    private static final int MALE = 0;
    private static final int FEMALE = 1;
    private static final int GATHER_ACTIVITY_CODE = 0;//跳转到采集指纹和拍照的页面
    private static final String TXT_NAME = "BASIC INFO";
    private static final int WRITE_CARD_FLAG = 0;

    private EditText nameET, addressET, ID_NO_ET;
    private TextView fill_ID_tx, genderTX, birthdayTX;
    private ImageButton backBT, entryBT;
    private PersonDao personDao;

    FragmentManager fgManager;
    ProgressDialogFragment progressDialog;
    AskDialogFragment askDialog;

    private int gatherID;
    private String gatherName;
    private String gatherGender;
    private String gatherBirthday;
    private String gatherAddress;
    private String gatherIDNO;
    private String gatherPicUrl;
    private String gatherFingerUrl;
    private String compressPicUrl;

    private int selectedID = 0;

    private WriteCardThread mWriteCardThread;
    private OperateCardHelper mOperateCardHelper;
    private WriteCardHandler mWriteCardHandler;
    private boolean writeSuccess = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_info_layout);

        initView();
        setListener();
        iniData();
    }

    private void initView() {
        fill_ID_tx = (TextView) findViewById(R.id.fill_ID_tx);
        nameET = (EditText) findViewById(R.id.fill_name_text);
        addressET = (EditText) findViewById(R.id.address_ET);
        ID_NO_ET = (EditText) findViewById(R.id.ID_NO_et);

        genderTX = (TextView) findViewById(R.id.gender_tv);
        birthdayTX = (TextView) findViewById(R.id.birthday_tv);

        backBT = (ImageButton) findViewById(R.id.bottom_left_bt);
        entryBT = (ImageButton) findViewById(R.id.bottom_right_bt);
        View view = findViewById(R.id.bottom_middle_bt);
        if (view != null) {
            view.setVisibility(View.GONE);
        }

        fgManager = getSupportFragmentManager();
        progressDialog = new ProgressDialogFragment();
        progressDialog.setData(getString(R.string.saving_to_card));
        askDialog = new AskDialogFragment();
        askDialog.setData(getString(R.string.common_tips), getString(R.string.save_to_card_message));
    }

    private void iniData() {
        mOperateCardHelper = new OperateCardHelper(this);
        mOperateCardHelper.openRFModel();
        mWriteCardHandler = new WriteCardHandler(this);
        personDao = new PersonDao(getApplicationContext());
        gatherID = Integer.parseInt(String.valueOf(personDao.getQuantity()));
        refreshView();
    }

    private void refreshView() {
        ++gatherID;
        fill_ID_tx.setText(String.format(Locale.getDefault(), "%1$,05d", gatherID));
//        nameET.setText("");
//        addressET.setText("");
        ID_NO_ET.setText(String.format(Locale.getDefault(), "%1$,05d", gatherID));
    }

    private void setListener() {
        nameET.setOnClickListener(this);
        addressET.setOnClickListener(this);
        ID_NO_ET.setOnClickListener(this);
        genderTX.setOnClickListener(this);
        birthdayTX.setOnClickListener(this);

        backBT.setOnClickListener(this);
        entryBT.setOnClickListener(this);
        askDialog.setListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gender_tv:
                SingleChoiceDialogFragment singleDialog = new SingleChoiceDialogFragment();
                singleDialog.setData("gender", new String[]{"Male", "Female"}, selectedID);
                singleDialog.setListener(this);
                singleDialog.show(fgManager, "gender");
                break;
            case R.id.birthday_tv:
                DatePickerFragment datePickerFragment = new DatePickerFragment();
                datePickerFragment.setData(birthdayTX.getText().toString(), this);
                datePickerFragment.show(fgManager, "date");
                break;
            case R.id.bottom_left_bt:
                finish();
                break;
            case R.id.bottom_right_bt:
                if (checkInformation()) {
                    Intent intent = new Intent(FillInfoActivity.this, GatherActivity.class);
                    intent.putExtra("gatherID", fill_ID_tx.getText().toString());
                    startActivityForResult(intent, GATHER_ACTIVITY_CODE);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void setDate(String date) {
        birthdayTX.setText(date);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DIALOG_SURE:
                saveData2Card();
                break;
            case DIALOG_CANCEL:
                break;
            case MALE:
                selectedID = 0;
                genderTX.setText("Male");
                break;
            case FEMALE:
                selectedID = 1;
                genderTX.setText("Female");
                break;
            default:
                break;
        }
        dialog.dismiss();
    }

    /**
     * 将数据保存到卡里
     */
    private void saveData2Card() {
        askDialog.dismiss();
        String[] saveData = new String[]{
                String.format(Locale.getDefault(), "%1$,05d", (gatherID - 1)),
                gatherName,
                gatherGender,
                gatherBirthday,
                gatherAddress,
                gatherIDNO,
                compressPicUrl,
                gatherFingerUrl};
        mOperateCardHelper.setSaveData(saveData);
        progressDialog.show(fgManager, "progress");
        mWriteCardThread = new WriteCardThread();
        mWriteCardThread.start();
    }

    /**
     * 写卡的线程
     */
    private class WriteCardThread extends Thread {
        @Override
        public void run() {
            writeSuccess = mOperateCardHelper.writeM1Card();
            Message message = Message.obtain();
            message.what = WRITE_CARD_FLAG;
            mWriteCardHandler.sendMessage(message);
        }
    }

    /**
     * 处理写卡的消息
     */
    private static class WriteCardHandler extends Handler {
        WeakReference<FillInfoActivity> mWeakReference;

        public WriteCardHandler(FillInfoActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final FillInfoActivity activity = mWeakReference.get();
            switch (msg.what) {
                case WRITE_CARD_FLAG:
                    activity.progressDialog.dismiss();
                    if (activity.writeSuccess) {
                        ToastUtil.getInstance().showToast(activity, "success");
                        new FileUtil().deleteFile(activity.compressPicUrl);
                    } else {
                        activity.askDialog.setData(activity.getString(R.string.common_tips), activity.getString(R.string.save_failed_message));
                        activity.askDialog.show(activity.fgManager, "saveDialog");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private boolean checkInformation() {
        gatherID = Integer.valueOf(fill_ID_tx.getText().toString());
        gatherName = nameET.getText().toString();
        gatherAddress = addressET.getText().toString();
        gatherGender = genderTX.getText().toString();
        gatherBirthday = birthdayTX.getText().toString();
        gatherIDNO = ID_NO_ET.getText().toString();
        if (!StringUtil.hasLength(gatherName)) {
            showToast(getString(R.string.information_incomplete));
            return false;
        }
        if (!StringUtil.hasLength(gatherAddress)) {
            showToast(getString(R.string.information_incomplete));
            return false;
        }
        if (!StringUtil.hasLength(String.valueOf(gatherIDNO))) {
            showToast(getString(R.string.information_incomplete));
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GATHER_ACTIVITY_CODE:
                    gatherPicUrl = data.getStringExtra("pictureUrl");
                    compressPicUrl = data.getStringExtra("compressPicUrl");
                    gatherFingerUrl = data.getStringExtra("fingerPrintUrl");
                    saveInformation();
                    refreshView();
                    askDialog.show(fgManager, "saveDialog");

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 保存新信息到数据库和本地文件
     */
    private void saveInformation() {
        StringBuffer Buffer = new StringBuffer();
        String data = DateUtil.getCurrentTime();
        String txt = "[BasicInfo]" + "\r\n"
                + "NAME: " + gatherName + "\r\n"
                + "SEX: " + gatherGender + "\r\n"
                + "BIRTHDAY: " + gatherBirthday + "\r\n"
                + "ADDRESS: " + gatherAddress + "\r\n"
                + "ISSUEDATE: " + data + "\r\n"
                + "ID NO.: " + gatherIDNO;
        Buffer = Buffer.append(txt);

        Log.d("moubiao", "Buffer.toString() = " + Buffer.toString());

        saveFileToDevice(Buffer.toString());

        Person person = new Person();
        person.setName(gatherName);
        person.setGender(gatherGender);
        person.setBirthday(gatherBirthday);
        person.setAddress(gatherAddress);
        person.setID_NO(gatherIDNO);
        if (gatherPicUrl != null) {
            person.setGatherPictureUrl(gatherPicUrl);
        }
        personDao.add(person);
    }

    private void saveFileToDevice(String toSaveString) {
        try {
            String filePath = getExternalFilesDir(null) + "/" + getResources().getString(R.string.app_name) + "/" +
                    String.format(Locale.getDefault(), "%1$,05d", gatherID) + "/" + TXT_NAME + ".ini";

            File saveFile = new File(filePath);
            if (!saveFile.exists()) {
                File dir = new File(saveFile.getParent());
                dir.mkdirs();
                saveFile.createNewFile();
            }

            FileOutputStream outStream = new FileOutputStream(saveFile);
            outStream.write(toSaveString.getBytes());
            outStream.close();

            showToast(getString(R.string.save_success));
        } catch (FileNotFoundException e) {
            showToast(getString(R.string.file_no_exit));
            e.printStackTrace();
        } catch (IOException e) {
            showToast(getString(R.string.save_failed));
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        ToastUtil.getInstance().showToast(getApplicationContext(), message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOperateCardHelper.closeRFModel();
    }
}
