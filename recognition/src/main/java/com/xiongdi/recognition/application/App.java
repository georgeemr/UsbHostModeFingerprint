package com.xiongdi.recognition.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.xiongdi.recognition.bean.Account;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.AccountDao;
import com.xiongdi.recognition.util.Converter;
import com.xiongdi.recognition.util.CrashHandlerUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by moubiao on 2016/3/22.
 * 自定义的application
 */
public class App extends Application {
    private final String TAG = "moubiao";
    public static String FINGERPRINT_PATH;
    public static final String EXTERNAL_SD_PATH = "/storage/sdcard0/EmpDatabase";
    private List<Activity> mActivityList;
    private Person mPerson;

    private volatile static ThreadPoolExecutor mPoolExecutor;

    @Override
    public void onCreate() {
        super.onCreate();

        initData();
        initDatabase();
    }

    private void initData() {
        setAppLanguage();
        mActivityList = new ArrayList<>();
        CrashHandlerUtil.getInstance().initCrashHandlerUtil(this);

        File dir = new File(EXTERNAL_SD_PATH + File.separator + "database");
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                Log.e(TAG, "initData: create save data directory failed!");
            }
        }
    }

    /**
     * 设置应用语言
     */
    private void setAppLanguage() {
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        config.locale = getSetLocale();
        resources.updateConfiguration(config, dm);
    }

    /**
     * 获取本地保存的应用语言
     */
    private Locale getSetLocale() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("language", Context.MODE_PRIVATE);
            if (sharedPreferences.contains("language")) {
                String string = sharedPreferences.getString("language", "");
                if (TextUtils.isEmpty(string)) {
                    return null;
                } else {
                    //将16进制的数据转为数组，准备反序列化
                    byte[] stringToBytes = Converter.string2Hex(string);
                    ByteArrayInputStream bis = new ByteArrayInputStream(stringToBytes);
                    ObjectInputStream is = new ObjectInputStream(bis);
                    //返回反序列化得到的对象
                    return (Locale) is.readObject();
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return null;
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


    public Person getPerson() {
        return mPerson;
    }

    public void setPerson(Person person) {
        mPerson = person;
    }

    public static ThreadPoolExecutor getThreadPoolExecutor() {
        if (mPoolExecutor == null) {
            synchronized (ThreadPoolExecutor.class) {
                mPoolExecutor = new ThreadPoolExecutor(5, 7, 100, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(5),
                        new ThreadPoolExecutor.CallerRunsPolicy());
            }
        }

        return mPoolExecutor;
    }
}
