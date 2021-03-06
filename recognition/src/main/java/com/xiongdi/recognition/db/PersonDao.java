package com.xiongdi.recognition.db;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.PreparedUpdate;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.xiongdi.recognition.bean.Person;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moubiao on 2016/3/25.
 * person的到类
 */
public class PersonDao {
    private Context context;
    private Dao<Person, Integer> personDao;
    private DatabaseHelper helper;

    public PersonDao(Context context) {
        this.context = context;
        helper = DatabaseHelper.getHelper(context);
        try {
            personDao = helper.getDao(Person.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 增加一个用户
     */
    public void add(Person person) {
        try {
            personDao.createIfNotExists(person);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取person的数量
     */
    public Long getQuantity() {
        try {
            return personDao.countOf();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0L;
    }

    /**
     * 通过id查询数据
     */
    public Person queryById(int id) {
        try {
            return personDao.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public QueryBuilder<Person, Integer> getQueryBuilder() {
        return personDao.queryBuilder();
    }

    public List<Person> query(PreparedQuery<Person> preparedQuery) {
        List<Person> personList = new ArrayList<>();
        try {
            personList.addAll(personDao.query(preparedQuery));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return personList;
    }

    public UpdateBuilder<Person, Integer> getUpdateBuilder() {
        return personDao.updateBuilder();
    }

    public boolean updatePerson(PreparedUpdate<Person> preparedUpdate) {
        try {
            personDao.update(preparedUpdate);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void updateColumn(String statement, String... arguments) {
        try {
            personDao.updateRaw(statement, arguments);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
