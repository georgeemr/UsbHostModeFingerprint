package com.xiongdi.recognition.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.xiongdi.recognition.bean.Account;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moubiao on 2016/3/25.
 * Account的dao类
 */
public class AccountDao {
    private Context context;
    private Dao<Account, Integer> accountDao;
    private DatabaseHelper helper;

    public AccountDao(Context context) {
        this.context = context;
        helper = DatabaseHelper.getHelper(context);
        try {
            accountDao = helper.getDao(Account.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 增加一个用户
     */
    public void add(Account account) {
        SharedPreferences.Editor editor = context.getSharedPreferences("config", Context.MODE_PRIVATE).edit();
        try {
            accountDao.createIfNotExists(account);
            editor.putBoolean("addAccCompleted", true);
        } catch (SQLException e) {
            editor.putBoolean("addAccCompleted", false);
            e.printStackTrace();
        }

        editor.apply();
    }

    /**
     * 通过where条件查询
     */
    public List<Account> query(PreparedQuery<Account> preparedQuery) {
        List<Account> accounts = new ArrayList<>();
        try {
            accounts.addAll(accountDao.query(preparedQuery));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public QueryBuilder<Account, Integer> getQueryBuilder() {
        return accountDao.queryBuilder();
    }
}
