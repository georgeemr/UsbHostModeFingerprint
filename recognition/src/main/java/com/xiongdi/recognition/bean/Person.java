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
    private int personID;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private String gender;

    @DatabaseField(canBeNull = false)
    private String birthday;

    @DatabaseField(canBeNull = false)
    private String address;

    @DatabaseField(canBeNull = false)
    private String ID_NO;

    @DatabaseField
    private String gatherPictureUrl;

    @DatabaseField
    private int checked;

    public Person() {
    }

    public int getPersonID() {
        return personID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getID_NO() {
        return ID_NO;
    }

    public void setID_NO(String ID_NO) {
        this.ID_NO = ID_NO;
    }

    public String getGatherPictureUrl() {
        return gatherPictureUrl;
    }

    public void setGatherPictureUrl(String gatherPictureUrl) {
        this.gatherPictureUrl = gatherPictureUrl;
    }


    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }
}
