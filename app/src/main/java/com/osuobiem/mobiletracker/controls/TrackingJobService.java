package com.osuobiem.mobiletracker.controls;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class TrackingJobService extends JobService {
    private static final String TAG = "TrackingJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Tracking job started!");
        if(!isMyServiceRunning()) {
            runInForeground(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Tracking job stopped prematurely!");
        return true;
    }

    private void runInForeground(JobParameters params) {
        Intent trackIntent = new Intent(getApplicationContext(), TrackerService.class);
        ContextCompat.startForegroundService(getApplicationContext(), trackIntent);
        jobFinished(params, true);
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
