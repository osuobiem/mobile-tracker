package com.osuobiem.mobiletracker.controls;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.device.Device;
import com.osuobiem.mobiletracker.track.MapsActivity;

import java.util.Objects;

import static com.osuobiem.mobiletracker.controls.NotyChannel.TRACKER_CHANNEL_ID;

public class TrackerService extends Service {

    private LocationRequest locationRequest;
    private String user_id;
    private String lc;
    private FirebaseAuth firebaseAuth;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName compName;
    private double latitude;
    private double longitude;
    private AudioManager audioManager;
    private Uri uri;
    private Ringtone ringtone;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent mapsIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, mapsIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, TRACKER_CHANNEL_ID)
                .setContentTitle("Mobile Tracker Running")
                .setSmallIcon(R.drawable.ic_adb)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(22, notification);

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, DeviceAdmin.class);

        uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (cm != null && cm.getActiveNetworkInfo() != null) {
            if (firebaseAuth.getCurrentUser() != null) {
                user_id = firebaseAuth.getCurrentUser().getUid();
                setLCode();
                if(lc != "" || lc != null){
                    getLastKnownLocation();
                    createLocationRequest();
                    createLocationCallback();
                    startLocationUpdates();
                    if(devicePolicyManager.isAdminActive(compName)) {
                        stopRing();
                        listenForControls();
                    }
                }
            }
        }

        return START_STICKY;
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(7000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void createLocationCallback() {
        final Device dev = new Device();
        final int[] walk = {1};
        walk[0] = 0;
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (lc != null) {
                        if(devicePolicyManager.isAdminActive(compName)) {
                            if(walk[0] == 6) {
                                walk[0] = 0;
                                stopRing();
                                listenForControls();
                            }
                        }
                        Object o = new Object();
                        Database database = new Database("locations", "update", o, lc, "latitude", "", location.getLatitude());
                        database.handler();
                        database = new Database("locations", "update", o, lc, "longitude", "", location.getLongitude());
                        database.handler();
                        database = new Database("devices", "update", o, lc, "last_seen", dev.getLast_seen(), 0);
                        database.handler();
                    }
                    walk[0]=walk[0]+1;
                }
            }
        };
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);
        }
    }

    public void setLCode() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(user_id).child("active_on").exists()) {
                    lc = dataSnapshot.child(user_id).child("active_on").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void listenForControls() {
        DatabaseReference dReference = FirebaseDatabase.getInstance().getReference("users");
        dReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(user_id).child("active_on").exists()) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("controls");
                    databaseReference.child(lc).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if ((boolean) dataSnapshot.child("lock").getValue()) {
                                if (devicePolicyManager.isAdminActive(compName)) {
                                    devicePolicyManager.lockNow();
                                    Object nullObject = new Object();

                                    Database database = new Database("controls", "insert", nullObject, lc, "lock", "false", 1);
                                    database.handler();
                                }
                            }
                            if ((boolean) dataSnapshot.child("wipe").getValue()) {
                                if (devicePolicyManager.isAdminActive(compName)) {
                                    devicePolicyManager.wipeData(0);
                                    Object nullObject = new Object();

                                    Database database = new Database("controls", "insert", nullObject, lc, "wipe", "false", 1);
                                    database.handler();
                                }
                            }
                            if ((boolean) dataSnapshot.child("ring").getValue()) {
                                int ringerMode = audioManager.getRingerMode();
                                if(ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager
                                            .getStreamMaxVolume(AudioManager.STREAM_RING), 0);
                                    ringtone.play();
                                }
                                else {
                                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                    audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager
                                            .getStreamMaxVolume(AudioManager.STREAM_RING), 0);
                                    ringtone.play();
                                }
                            }
                            else {
                                ringtone.stop();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    public void stopRing() {
        ringtone.stop();
    }

}