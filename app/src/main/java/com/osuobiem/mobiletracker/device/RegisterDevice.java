package com.osuobiem.mobiletracker.device;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.controls.ControlDevice;
import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.track.DeviceLocation;
import com.osuobiem.mobiletracker.track.MapsActivity;
import com.osuobiem.mobiletracker.user.User;

public class RegisterDevice extends AppCompatActivity implements View.OnClickListener {

    private TextView device_brand;
    private TextView device_model;
    private TextView device_imei;
    private TextView device_sdk;
    private TextView device_sdk_name;
    private TextView device_sdk_version;
    private TextView device_l_code;
    private Button add_device_btn;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout add_lay;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private double[] locate = {0, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_device);
        toolbar = findViewById(R.id.reg_device_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add Device");

        device_brand = findViewById(R.id.device_brand);
        device_model = findViewById(R.id.device_model);
        device_imei = findViewById(R.id.device_imei);
        device_sdk = findViewById(R.id.device_sdk);
        device_sdk_name = findViewById(R.id.device_sdk_name);
        device_sdk_version = findViewById(R.id.device_sdk_version);
        device_l_code = findViewById(R.id.device_l_code);

        add_device_btn = findViewById(R.id.add_device_btn);
        progressBar = findViewById(R.id.add_pbar);
        add_lay = findViewById(R.id.add_lay);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        add_device_btn.setOnClickListener(this);

        getLocation();
        updateUI();
    }

    protected void updateUI() {
        Device device = new Device();

        String brand = device.getBrand();
        String model = device.getModel();
        String imei = getImei();
        String sdk_name = device.getSdk_name();
        String sdk_version = device.getVersion();
        int sdk = device.getSdk();
        String l_code = device.fetchLcode();

        device_brand.setText("Brand:    "+brand);
        device_model.setText("Model:    "+model);
        device_imei.setText("IMEI:  "+imei);
        device_sdk.setText("SDK:    "+sdk);
        device_sdk_name.setText("SDK Name:    "+sdk_name);
        device_sdk_version.setText("Version:    "+sdk_version);
        device_l_code.setText("L-Code:  "+l_code);

    }

    protected boolean checkPhoneStatePermission(int val) {

        if(val == 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED){

                int PERMISSION_REQUEST_CODE = 12;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
            }
            else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 12: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPhoneStatePermission(0);
                } else {
                    checkPhoneStatePermission(1);
                }
            }
        }
    }

    protected void addDevice() {
        User currentUser = new User();

        if (currentUser.fetchUser() != null) {
            String imei = getImei();
            Device device = new Device(imei);
            String l_code = device.fetchLcode();
            String user_id = currentUser.fetchId();

            Database database = new Database("devices", "insert", device, l_code, "", "", 0);
            database.handler();

            addControls(l_code, user_id);
        }
    }

    private void addControls(String l_code, String user_id) {
        ControlDevice controlDevice = new ControlDevice(false, false, false);

        Database database = new Database("controls", "insert", controlDevice, l_code, "", "", 0);
        database.handler();

        addLocation(l_code, user_id);
    }

    private void addLocation(String l_code, String user_id) {
        DeviceLocation deviceLocation = new DeviceLocation(getLocation()[0], getLocation()[1]);

        Database database = new Database("locations", "insert", deviceLocation, l_code, "", "", 0);
        database.handler();

        updateUser(l_code, user_id);
    }

    private void updateUser(String l_code, String user_id) {
        Object nullObject = new Object();
        Database database = new Database("users", "insert", nullObject, user_id, "active_on", l_code, 0);
        database.handler();

        goToMainMap();
    }

    public double[] getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(android.location.Location location) {
                            if (location != null) {
                                locate[0] = location.getLatitude();
                                locate[1] = location.getLongitude();
                            }
                            else {
                                locate[0] = 0;
                                locate[1] = 0;
                            }
                        }
                    });
        }
        else {
            ActivityCompat.requestPermissions(RegisterDevice.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);
        }

        return locate;
    }

    public String getImei() {
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        if(checkPhoneStatePermission(0)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return telephonyManager.getImei();
            }
        }
        return "Not Available";
    }

    private void goToMainMap() {
        startActivity(new Intent(this, MapsActivity.class));
    }

    public void snack(String message) {
        int col = R.color.badSnack;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.add_l), message, -2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            snackbar.getView().setBackgroundColor(getColor(col));
        }
        else {
            snackbar.getView().setBackgroundColor(Color.RED);
        }
        View view = snackbar.getView();
        FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view.getLayoutParams();
        view.setLayoutParams(params);

        snackbar.show();
    }

    public void progress(boolean val) {
        if(val) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.transWColor;
                add_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.VISIBLE);
            add_device_btn.setEnabled(false);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.trans;
                add_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.GONE);
            add_device_btn.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == add_device_btn) {
           // if(getLocation()[0] != 0 && getLocation()[1] != 0) {
                progress(true);
                new Thread(new Runnable() {
                    public void run() {
                        addDevice();
                    }
                }).start();
            /*}
            else {
                snack("Could not get location, try again.");
            }*/
        }
    }
}