package com.osuobiem.mobiletracker.user;

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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.controls.FindDeviceActivity;
import com.osuobiem.mobiletracker.controls.TrackerService;
import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.track.MapsActivity;

import java.util.Objects;

public class LogoutActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText l_code;
    private Button logout_button;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout logout_lay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);
        toolbar = findViewById(R.id.logout_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Logout");

        l_code = findViewById(R.id.logout_l_code);
        logout_button = findViewById(R.id.logout_button);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("devices");
        progressBar = findViewById(R.id.logout_pbar);
        logout_lay = findViewById(R.id.logout_lay);

        logout_button.setOnClickListener(this);
    }

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
                gotoMaps();
                return true;

            case R.id.menu_find_device:
                startActivity(new Intent(this, FindDeviceActivity.class));
                return true;

            case R.id.menu_logout:
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void checkLcode() {
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
                        confirmUser(device_l_code);
                    }
                    else {
                        progress(false);
                        snack("Incorrect L-Code");
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

    public void confirmUser(final String l_code) {
        databaseReference.child(l_code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = new User();
                if(user.fetchId().equals(Objects.requireNonNull(dataSnapshot.child("user").getValue()).toString())) {
                    logoutUser(l_code);
                }
                else {
                    progress(false);
                    snack("Incorrect L-Code");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progress(false);
                snack("Opps, something went wrong. Try again.");
            }
        });
    }

    public void logoutUser(String l_code) {
        Object null_object = new Object();
        User user = new User();
        String user_id = user.fetchId();
        Database database = new Database("controls", "delete", null_object, l_code, "", "", 0);
        database.handler();

        database = new Database("locations", "delete", null_object, l_code, "", "", 0);
        database.handler();

        database = new Database("devices", "delete", null_object, l_code, "", "", 0);
        database.handler();

        database = new Database("users", "delete", null_object, user_id, "active_on", "", 0);
        database.handler();

        Intent trackIntent = new Intent(this, TrackerService.class);
        stopService(trackIntent);

        firebaseAuth.signOut();

        gotoMaps();
    }

    public void gotoMaps() {
        startActivity(new Intent(this, MapsActivity.class));
    }

    public void snack(String message) {
        int col = R.color.badSnack;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.logout_l), message, 0);
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
                logout_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.VISIBLE);
            l_code.setEnabled(false);
            logout_button.setEnabled(false);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.trans;
                logout_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.GONE);
            l_code.setEnabled(true);
            logout_button.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == logout_button) {
            checkLcode();
        }
    }
}








