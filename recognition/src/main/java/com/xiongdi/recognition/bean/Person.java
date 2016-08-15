package com.xiongdi.recognition.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by moubiao on 2016/3/22.
 * 保存采集信息的表
 */
@DatabaseTable(tableName = "person")
public class Person {
    @DatabaseField(generatedId = true)
    private int ID;

    @DatabaseField(canBeNull = false)
    private String mName;

    @DatabaseField(canBeNull = false)
    private String mGender;

    @DatabaseField(canBeNull = false, defaultValue = "18")
    private int mAge;

    @DatabaseField(canBeNull = false)
    private String mBirthday;

    @DatabaseField(canBeNull = false)
    private String mAddress;

    @DatabaseField(canBeNull = false)
    private String ID_NO;

    @DatabaseField(canBeNull = false, defaultValue = "/sdcard1/EmpDatabase/")
    private String mFingerprint;

    @DatabaseField
    private String mPicture;

    @DatabaseField
    private int mChecked;

    public Person() {
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getGender() {
        return mGender;
    }

    public void setGender(String gender) {
        this.mGender = gender;
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int age) {
        mAge = age;
    }

    public String getBirthday() {
        return mBirthday;
    }

    public void setBirthday(String birthday) {
        this.mBirthday = birthday;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public String getID_NO() {
        return ID_NO;
    }

    public void setID_NO(String ID_NO) {
        this.ID_NO = ID_NO;
    }

    public String getFingerprint() {
        return mFingerprint;
    }

    public void setFingerprint(String fingerprint) {
        mFingerprint = fingerprint;
    }

    public String getPicture() {
        return mPicture;
    }

    public void setPicture(String picture) {
        this.mPicture = picture;
    }


    public int getChecked() {
        return mChecked;
    }

    public void setChecked(int checked) {
        this.mChecked = checked;
    }
}
