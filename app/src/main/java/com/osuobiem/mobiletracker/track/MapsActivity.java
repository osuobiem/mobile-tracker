package com.osuobiem.mobiletracker.track;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.controls.DeviceAdmin;
import com.osuobiem.mobiletracker.controls.FindDeviceActivity;
import com.osuobiem.mobiletracker.controls.TrackerService;
import com.osuobiem.mobiletracker.controls.TrackingJobService;
import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.device.RegisterDevice;
import com.osuobiem.mobiletracker.user.LoginActivity;
import com.osuobiem.mobiletracker.user.LogoutActivity;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener{

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private FirebaseAuth firebaseAuth;
    private Toolbar toolbar;
    private Button info;
    private Button lock;
    private Button wipe;
    private Button ring;
    private String l_code;
    private Context context;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private ConnectivityManager cm;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationManager locationManager;
    private DevicePolicyManager devicePolicyManager;
    private ActivityManager activityManager;
    private ComponentName compName;
    private String lc;
    private Marker you;
    private Marker lostDevice;

    private double latitude;
    private double longitude;
    private double lat;
    private double lon;

    // Start activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        toolbar = findViewById(R.id.map_toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        compName = new ComponentName(this, DeviceAdmin.class);
        context = getApplicationContext();

        if (!isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
        }
        else {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
            final String user_id = firebaseAuth.getCurrentUser().getUid();
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.child(user_id).child("active_on").exists()) {
                        gotoRegisterDevice();
                    }
                    else {
                        lc = dataSnapshot.child(user_id).child("active_on").getValue().toString();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }

        getLastKnownLocation();
        createLocationRequest();
        createLocationCallback();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        info = findViewById(R.id.info);
        lock = findViewById(R.id.lock);
        wipe = findViewById(R.id.wipe);
        ring = findViewById(R.id.ring);

        info.setOnClickListener(this);
        lock.setOnClickListener(this);
        wipe.setOnClickListener(this);
        ring.setOnClickListener(this);


        if(getDeviceLcode() != null) {
            l_code = getDeviceLcode();
            if (!l_code.equals("No L_CODE")) {
                info.setText("INFO");
                lock.setText("LOCK");
                wipe.setText("WIPE");
                ring.setText("RING");
                info.setVisibility(Button.VISIBLE);
                lock.setVisibility(Button.VISIBLE);
                wipe.setVisibility(Button.VISIBLE);
                ring.setVisibility(Button.VISIBLE);
            }
        }

        checkDeviceAdminPermission();
        scheduleTrackJob();
        //checkRingPerm();
    }

    // Prepare google map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            snack("Please enable location permission!", "bad", -2);

            LatLng loca = new LatLng(0, 0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loca, 2));
        }
        else {
            if(getDeviceLcode() != null) {
                String l_c = getDeviceLcode();

                if(!l_c.equals("No L_CODE")) {
                    getLocationFromDB(l_c);
                    l_c = "No L_CODE";
                }
                createLocationCallback();
            }
        }

    }

    // Specify location acquisition settings
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(7000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // Acquire last known location
    protected void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(android.location.Location location) {
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    });
        }
        else {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);
        }
    }

    // Run location update loop
    protected void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    internetLoop();
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    LatLng loca = new LatLng(latitude, longitude);
                    if(you != null) {
                        you.remove();
                    }
                    if(you == null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loca, 18));
                    }
                    you = mMap.addMarker(new MarkerOptions().position(loca).title("You").icon(BitmapDescriptorFactory.fromResource(R.mipmap.green_marker)));
                }
            }
        };
    }

    // Initiate location updates (to be used when activity resumes)
    protected void startLocationUpdates() {
        if(you != null) {
            you.remove();
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);
    }

    // Resume paused activity
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    // Pause activity
    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // Tool for message reporting
    protected void snack(String message, String state, int duration) {
        int col;
        if(state.equals("bad")) {
            col = R.color.badSnack;
        }
        else {
            col = R.color.goodSnack;
        }
        Snackbar snackbar = Snackbar.make(findViewById(R.id.map_l), message, duration);
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

    // Set lock status
    protected void setLock() {
        Object nullObject = new Object();

        Database database = new Database("controls", "insert", nullObject, l_code, "lock", "true", 1);
        database.handler();
        snack("Lock request sent", "good", 0);
    }

    // Set wipe status
    protected void setWipe() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setMessage("Wiping your device will clear all existing data on the device. It will log you out of this application.");
        builder.setCancelable(true);

        builder.setPositiveButton(
                "YES",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Object nullObject = new Object();

                        Database database = new Database("controls", "insert", nullObject, l_code, "wipe", "true", 1);
                        database.handler();
                        snack("Wipe request sent", "good", 0);
                    }
                }
        );

        builder.setNegativeButton(
                "NO",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }
        );

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    // Set ring status
    protected void setRing(String val) {
        Object nullObject = new Object();

        Database database = new Database("controls", "insert", nullObject, l_code, "ring", val, 1);
        database.handler();
        if(val.equals("true")) {
            ring.setText("RINGING");
            snack("Ring request sent", "good", 0);
        }
        else {
            ring.setText("RING");
            snack("Stop Ring request sent", "good", 0);
        }
    }

    // Show device info popup
    protected void showInfoPopup(View v) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup_info, null);

        TextView brand = popup.findViewById(R.id.info_brand);
        TextView model = popup.findViewById(R.id.info_model);
        TextView imei = popup.findViewById(R.id.info_imei);
        TextView sdk = popup.findViewById(R.id.info_sdk);
        TextView sdk_version = popup.findViewById(R.id.info_sdk_version);
        TextView sdk_name = popup.findViewById(R.id.info_sdk_name);
        TextView last_seen = popup.findViewById(R.id.info_lasetseen);
        TextView status = popup.findViewById(R.id.info_status);
        TextView owner = popup.findViewById(R.id.info_owner);
        ImageView close = popup.findViewById(R.id.info_close);

        fetchDeviceInfo(brand, model, imei, sdk, sdk_version, sdk_name, last_seen, status, owner);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = false;

        final PopupWindow popupWindow = new PopupWindow(popup, width, height, focusable);

        popupWindow.setElevation(35);
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, -10);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    // Update device info popup
    protected void fetchDeviceInfo(final TextView brand, final TextView model, final TextView imei, final TextView sdk, final TextView sdk_version,
                                   final TextView sdk_name, final TextView last_seen, final TextView status, final TextView owner){

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("devices");

        databaseReference.child(l_code).addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                brand.setText("Brand: "+Objects.requireNonNull(dataSnapshot.child("brand").getValue()).toString());
                model.setText("Model: "+Objects.requireNonNull(dataSnapshot.child("model").getValue()).toString());
                imei.setText("IMEI: "+Objects.requireNonNull(dataSnapshot.child("device_imei").getValue()).toString());
                sdk.setText("SDK: "+Objects.requireNonNull(dataSnapshot.child("sdk").getValue()).toString());
                sdk_version.setText("SDK Version: "+Objects.requireNonNull(dataSnapshot.child("version").getValue()).toString());
                sdk_name.setText("SDK Name: "+Objects.requireNonNull(dataSnapshot.child("sdk_name").getValue()).toString());

                String owner_id = Objects.requireNonNull(dataSnapshot.child("user").getValue()).toString();

                DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("users");
                dbref.child(owner_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        owner.setText("Owner: "+Objects.requireNonNull(dataSnapshot.child("firstname").getValue()).toString()
                        +" "+Objects.requireNonNull(dataSnapshot.child("lastname").getValue()).toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
           }

           @Override
           public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        databaseReference.child(l_code).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                last_seen.setText("Last Seen: "+Objects.requireNonNull(dataSnapshot.child("last_seen").getValue()).toString());
                String last = Objects.requireNonNull(dataSnapshot.child("last_seen").getValue()).toString();

                DateFormat format = new SimpleDateFormat("E dd/MM/yyyy 'at' HH:mm");
                try {
                    Date date = format.parse(last);
                    Timestamp ts = new Timestamp(date.getTime());

                    Date moment = new Date();
                    Timestamp tm = new Timestamp(moment.getTime());

                    if((tm.getTime() - ts.getTime()) > 300000) {
                        status.setText("Status: Offline");
                    }
                    else {
                        status.setText("Status: Online");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    // Get found device L-CODE
    protected String getDeviceLcode() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            return bundle.getString("L_CODE");
        }
        return "No L_CODE";
    }

    // Go to logout activity
    protected void gotoLogout() {
        startActivity(new Intent(this, LogoutActivity.class));
    }

    // Goto finddevice activity
    protected void gotoFindDevice() {
        startActivity(new Intent(this, FindDeviceActivity.class));
    }

    // Check if user is logged in
    protected boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Goto registerdevice activity
    protected void gotoRegisterDevice() {
        startActivity(new Intent(this, RegisterDevice.class));
    }

    // Create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_menu, menu);

        return true;
    }

    // Creaye listener for menu item select
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_map:
                return true;

            case R.id.menu_find_device:
                gotoFindDevice();
                return true;

            case R.id.menu_logout:
                gotoLogout();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    // Check for button clicks
    @Override
    public void onClick(View v) {
        if(v == info) {
            showInfoPopup(v);
        }
        else if(v == lock) {
            setLock();
        }
        else if(v == wipe) {
            setWipe();
        }
        else {
            updateRingStatus();
        }
    }

    // Start foreground tracking service
    protected void startTrackService() {
        Intent trackIntent = new Intent(this, TrackerService.class);

        ContextCompat.startForegroundService(this, trackIntent);
    }

    // Check if foreground dervice is running
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // Get found device location from database
    protected void getLocationFromDB(final String lcode) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("locations");
        databaseReference.child(lcode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                lat = (double) dataSnapshot.child("latitude").getValue();
                lon = (double) dataSnapshot.child("longitude").getValue();

                final LatLng loca = new LatLng(lat, lon);
                if(lostDevice != null) {
                    lostDevice.remove();
                }
                if(lostDevice == null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loca, 17));
                }

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("devices");
                databaseReference.child(lcode).addListenerForSingleValueEvent(
                        new ValueEventListener() {
                      @Override
                      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                          if(lostDevice != null) {
                              lostDevice.remove();
                          }
                          lostDevice = mMap.addMarker(new MarkerOptions().position(loca).title(
                                  Objects.requireNonNull(dataSnapshot.child("model").getValue()).toString()
                          ).icon(BitmapDescriptorFactory.fromResource(R.mipmap.red_marker)));
                      }

                      @Override
                      public void onCancelled(@NonNull DatabaseError databaseError) {

                      }
                });
           }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Check for internet connection
    protected boolean internetLoop() {
        if(firebaseAuth.getUid() != null) {
            if(!isMyServiceRunning()) {
                startTrackService();
            }
            return true;
        }
        else {
            return false;
        }
    }

    // Update device ring status
    protected void updateRingStatus() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("controls");
        databaseReference.child(l_code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.child("ring").getValue()) {
                    setRing("false");
                }
                else {
                    setRing("true");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Check for ring permission
    protected void checkRingPerm() {
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(n.isNotificationPolicyAccessGranted()) {
            return;
        }
        else {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, 12);
        }
    }

    // Check for device admin permission
    protected void checkDeviceAdminPermission() {
        boolean active = devicePolicyManager.isAdminActive(compName);

        if (!active) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    this.getString(R.string.device_admin_description));
            startActivityForResult(intent, 800);
        }
    }

    // Schedule tracking job service
    protected void scheduleTrackJob() {
        ComponentName componentName = new ComponentName(this, TrackingJobService.class);
        JobInfo info = new JobInfo.Builder(808, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int result_code = scheduler.schedule(info);

        if(result_code == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Tracking job scheduled successfully!");
        }
        else {
            Log.d(TAG, "Tracking job not scheduled!");
        }
    }
}