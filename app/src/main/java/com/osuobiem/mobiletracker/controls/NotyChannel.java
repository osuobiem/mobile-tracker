package com.osuobiem.mobiletracker.controls;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotyChannel extends Application {
    public static final String TRACKER_CHANNEL_ID = "TrackerChannel";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotyChannel();
    }

    private void createNotyChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel trackerChannel = new NotificationChannel(
                    TRACKER_CHANNEL_ID,
                    "Mobile Tracker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            trackerChannel.setDescription("Mobile Tracker updates your device location. Do not disable this channel.");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(trackerChannel);
        }
    }
}
