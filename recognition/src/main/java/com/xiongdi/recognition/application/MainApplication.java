package com.xiongdi.recognition.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.xiongdi.recognition.bean.Account;
import com.xiongdi.recognition.db.AccountDao;
import com.xiongdi.recognition.util.CrashHandlerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by moubiao on 2016/3/22.
 * 自定义的application
 */
public class MainApplication extends Application {
    public static String fingerprintPath;
    private List<Activity> mActivityList;

    @Override
    public void onCreate() {
        super.onCreate();

        initData();
        initDatabase();
    }

    private void initData() {
        mActivityList = new ArrayList<>();
        CrashHandlerUtil.getInstance().initCrashHandlerUtil(this);
    }

    private void initDatabase() {
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        if (!sp.getBoolean("addAccCompleted", false)) {
            AccountDao accountDao = new AccountDao(this);
            Account collectionUser = new Account();
            //采集指纹的账号
            collectionUser.setName("userc");
            collectionUser.setPassword("123");
            accountDao.add(collectionUser);
            //验证指纹的账号
            Account verifyUser = new Account();
            verifyUser.setName("userv");
            verifyUser.setPassword("123");
            accountDao.add(verifyUser);
        }
    }


    public List<Activity> getActivityList() {
        return mActivityList;
    }

    public void addActivity(Activity activity) {
        mActivityList.add(activity);
    }
}
