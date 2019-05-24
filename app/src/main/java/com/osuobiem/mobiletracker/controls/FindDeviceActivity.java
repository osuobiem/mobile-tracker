package com.osuobiem.mobiletracker.controls;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.track.MapsActivity;
import com.osuobiem.mobiletracker.user.LogoutActivity;

public class FindDeviceActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText l_code;
    private Button find_button;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout find_lay;

    // Start activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);
        toolbar = findViewById(R.id.find_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Find Device");

        firebaseAuth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase.getInstance().getReference("devices");

        l_code = findViewById(R.id.find_l_code);
        find_button = findViewById(R.id.find_button);
        progressBar = findViewById(R.id.find_pbar);
        find_lay = findViewById(R.id.find_lay);

        find_button.setOnClickListener(this);
    }

    // Create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.menu_map:
                startActivity(new Intent(this, MapsActivity.class));
                return true;

            case R.id.menu_find_device:
                return true;

            case R.id.menu_logout:
                logoutUser();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    protected void checkLcode() {
        final String device_l_code = l_code.getText().toString().trim();

        if(TextUtils.isEmpty(device_l_code)) {
            snack("Please input Device L-Code");
        }
        else {
            progress(true);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(device_l_code).exists()) {
                        gotoMapActivity(device_l_code);
                    }
                    else {
                        progress(false);
                        snack("No device exists with L-Code \""+device_l_code+"\"");
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progress(false);
                    snack("Opps, something went wrong. Try again.");
                }
            });
        }
    }

    private void logoutUser() {
        startActivity(new Intent(this, LogoutActivity.class));
    }

    protected void gotoMapActivity(String device_l_code) {

        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("L_CODE", device_l_code);

        finish();
        startActivity(intent);
    }

    public void snack(String message) {
        int col = R.color.badSnack;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.find_l), message, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            snackbar.getView().setBackgroundColor(getColor(col));
        }
        else {
            snackbar.getView().setBackgroundColor(Color.RED);
        }
        View view = snackbar.getView();
        FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);

        snackbar.show();
    }

    public void progress(boolean val) {
        if(val) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.transWColor;
                find_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.VISIBLE);
            l_code.setEnabled(false);
            find_button.setEnabled(false);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.trans;
                find_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.GONE);
            l_code.setEnabled(true);
            find_button.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == find_button) {
            checkLcode();
        }
    }
}














