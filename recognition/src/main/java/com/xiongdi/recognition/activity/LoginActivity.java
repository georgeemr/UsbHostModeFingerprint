package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Account;
import com.xiongdi.recognition.db.AccountDao;
import com.xiongdi.recognition.util.ToastUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AppCompatActivity implements OnClickListener, RadioGroup.OnCheckedChangeListener {
    private final String TAG = "moubiao";

    private RadioGroup mChooseTypeRG;
    private Button loginBT;
    private TextView mSettingBT;
    private EditText nameET, passwordET;
    private View mSecondPasswordView;
    private PopupWindow mPopupWindow;

    //连续按两次返回键后退出应用
    private boolean isExit = false;
    private boolean hasTask = false;
    private boolean isAdmin = false;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        initData();
        initView();
        saveOrReadAccount(false);
        setListener();
    }

    @Override
    public void onBackPressed() {
        if (!isExit) {
            isExit = true;
            ToastUtil.getInstance().showToast(this, getString(R.string.common_exit_app));
            if (!hasTask) {
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isExit = false;
                        hasTask = false;
                        cancel();
                    }
                }, 2000);
            }
        } else {
            super.onBackPressed();
        }
    }

    private void initData() {
        mTimer = new Timer();
    }

    private void initView() {
        mChooseTypeRG = (RadioGroup) findViewById(R.id.choose_type_rg);
        nameET = (EditText) findViewById(R.id.editText_name);
        if (nameET != null) {
            nameET.setSelection(nameET.getText().length());
        }
        passwordET = (EditText) findViewById(R.id.first_psw_editText);
        if (passwordET != null) {
            passwordET.setSelection(passwordET.getText().length());
        }
        nameET.requestFocus();
        loginBT = (Button) findViewById(R.id.login_bt);
        mSettingBT = (TextView) findViewById(R.id.app_setting_bt);
        mSecondPasswordView = findViewById(R.id.second_password);

        mPopupWindow = new PopupWindow(this);
        RadioGroup view = (RadioGroup) LayoutInflater.from(this).inflate(R.layout.language_options, null);
        view.setOnCheckedChangeListener(this);
        mPopupWindow.setContentView(view);
        mPopupWindow.setWidth(RadioGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(RadioGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
    }

    private void setListener() {
        mChooseTypeRG.setOnCheckedChangeListener(this);
        loginBT.setOnClickListener(this);
        mSettingBT.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_bt:
                if (true) {
                    Intent intent = new Intent();
                    if (isAdmin) {
                        intent.setClass(LoginActivity.this, AdminActivity.class);
                    } else {
                        if ("userc".equals(nameET.getText().toString())) {
                            intent.setClass(LoginActivity.this, FillInfoActivity.class);
                        } else if ("userv".equals(nameET.getText().toString())) {
                            intent.setClass(LoginActivity.this, VerifyResultActivity.class);
                            intent.putExtra("haveData", false);
                        } else {
                            intent.setClass(LoginActivity.this, FillInfoActivity.class);
                        }
                    }

                    saveOrReadAccount(true);
                    startActivity(intent);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.login_failed_tips), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                break;
            case R.id.app_setting_bt:
                setLanguage();
                break;
            default:
                break;
        }
    }

    /**
     * 保存或读取账户信息
     */
    private void saveOrReadAccount(boolean isSave) {
        SharedPreferences sp = getSharedPreferences("account", Activity.MODE_PRIVATE);
        if (isSave) {
            Editor editor = sp.edit();
            editor.putString("userName", nameET.getText().toString());
            editor.apply();
        } else {
            String userName = sp.getString("userName", null);
            if (userName != null) {
                nameET.setText(userName);
            }
        }
    }

    /**
     * 验证账户
     */
    private boolean verifyAccount() {
        String userName = nameET.getText().toString();
        String password = passwordET.getText().toString();

        if (0 == userName.length() || 0 == password.length()) {
            return false;
        }

        try {
            AccountDao accountDao = new AccountDao(getApplicationContext());
            QueryBuilder<Account, Integer> queryBuilder = accountDao.getQueryBuilder();
            Where<Account, Integer> where = queryBuilder.where();
            where.eq("name", userName);
            PreparedQuery<Account> preparedQuery = where.prepare();
            List<Account> actList = accountDao.query(preparedQuery);

            if (0 == actList.size()) {
                return false;
            }
            String correctPassword = actList.get(0).getPassword();

            if (correctPassword.equals(password)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void setLanguage() {
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        } else {
            mPopupWindow.showAsDropDown(mSettingBT, 100, 20);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.choose_type_rg) {
            switch (checkedId) {
                case R.id.general_rb:
                    isAdmin = false;
                    mSecondPasswordView.setVisibility(View.GONE);
                    break;
                case R.id.administrator_rb:
                    isAdmin = true;
                    mSecondPasswordView.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        } else if (group.getId() == R.id.language_rg) {
            switch (checkedId) {
                case R.id.chinese_rb:

                    break;
                case R.id.english_rb:

                    break;
                default:
                    break;
            }
            mPopupWindow.dismiss();
        }
    }
}
