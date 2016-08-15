package com.xiongdi.recognition.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.bean.Account;
import com.xiongdi.recognition.bean.Person;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by moubiao on 2016/3/22.
 * 操作数据库的帮助类
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    //    private static String DB_NAME = "person.db";
    private static String DB_NAME = MainApplication.EXTERNAL_SD_PATH + File.separator + "database" + File.separator + "person.db";
    private static int DB_VERSION = 1;
    private static DatabaseHelper instance;
    private Map<String, Dao> daoMap = new HashMap<>();

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 通过单例获取DatabaseHelper
     */
    public static synchronized DatabaseHelper getHelper(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null)
                    instance = new DatabaseHelper(context);
            }
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        createTable(connectionSource);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {
        dropTable(connectionSource);
        onCreate(sqLiteDatabase, connectionSource);
    }

    /**
     * 创建表
     */
    private void createTable(ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Account.class);
            TableUtils.createTableIfNotExists(getConnectionSource(), Person.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除表
     */
    private void dropTable(ConnectionSource connectionSource) {
        try {
            TableUtils.dropTable(connectionSource, Account.class, true);
            TableUtils.dropTable(connectionSource, Person.class, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取dao类
     */
    public synchronized Dao getDao(Class clazz) throws SQLException {
        Dao dao = null;
        String className = clazz.getSimpleName();

        if (daoMap.containsKey(className)) {
            dao = daoMap.get(className);
        }
        if (dao == null) {
            dao = super.getDao(clazz);
            daoMap.put(className, dao);
        }
        return dao;
    }

    /**
     * 释放资源
     */
    @Override
    public void close() {
        super.close();

        for (String key : daoMap.keySet()) {
            Dao dao = daoMap.get(key);
            dao = null;
        }
    }
}
