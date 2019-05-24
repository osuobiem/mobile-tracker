package com.osuobiem.mobiletracker.device;

import android.os.Build;

import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.user.User;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class Device {

    private String[] l_code = {"No Code"};
    private String device_imei;

    public Device(){}

    public Device(String device_imei) {
        this.device_imei = device_imei;
    }

    public String getBrand() { return Build.BRAND; }

    public String getModel() {
        return Build.MODEL;
    }

    public int getSdk() {
        return Build.VERSION.SDK_INT;
    }

    public String getSdk_name() {
        String name = "";
        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 23) {
            name = "Lollipop";
        }
        else if(Build.VERSION.SDK_INT == 23) {
            name = "Marshmallow";
        }
        else if(Build.VERSION.SDK_INT >= 24 && Build.VERSION.SDK_INT < 25) {
            name = "Nougat";
        }
        else if(Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 28) {
            name = "Oreo";
        }
        else if(Build.VERSION.SDK_INT == 28){
            name = "Pie";
        }

        return name;
    }

    public String getVersion() {
        return Build.VERSION.RELEASE;
    }

    public String getDevice_imei() {
        return device_imei;
    }

    public String getUser() {
        User user = new User();
        return user.fetchId();
    }

    public String getLast_seen() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E dd/MM/yyyy 'at' HH:mm");

        return simpleDateFormat.format(date);
    }

    public String fetchLcode() {
        if(l_code[0].equals("No Code")) {
            Database database = new Database();
            final String code = database.generateKey();

            l_code[0] = code;

            return code;
        }
        else {
            return l_code[0];
        }
    }
}
