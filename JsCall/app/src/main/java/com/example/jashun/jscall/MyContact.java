package com.example.jashun.jscall;

/**
 * Created by JaShun on 2016/12/24.
 */

public class MyContact {
    private String contactName;
    private String phoneNum;
    private int pri;

    public String getContactName(){return contactName;}
    public String getPhoneNum(){return phoneNum;}
    public int getPri(){return pri;}

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }
    public void setPri(int pri){this.pri = pri;}
}
