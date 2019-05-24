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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.osuobiem.mobiletracker.R;
import com.osuobiem.mobiletracker.device.RegisterDevice;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText email;
    private EditText password;
    private Button log_button;
    private TextView go_register;
    private Toolbar toolbar;
    private LinearLayout log_lay;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();

        if (isUserLoggedIn()) {
            goToAddDevice();
        }

        setContentView(R.layout.activity_login);
        toolbar = findViewById(R.id.log_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Login");

        email = findViewById(R.id.log_email);
        password = findViewById(R.id.log_pass);
        log_button = findViewById(R.id.log_button);
        go_register = findViewById(R.id.go_register);
        log_lay = findViewById(R.id.log_lay);
        progressBar = findViewById(R.id.log_pbar);

        log_button.setOnClickListener(this);
        go_register.setOnClickListener(this);
    }

    private void loginUser() {
        String uemail = email.getText().toString().toLowerCase().trim();
        String pass = password.getText().toString().toLowerCase().trim();

        firebaseAuth.signInWithEmailAndPassword(uemail, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            goToAddDevice();
                        }
                        else {
                            progress(false);
                            snack("Incorrect email or password");
                            return;
                        }
                    }
                });
    }

    private void goToAddDevice() {
        startActivity(new Intent(this, RegisterDevice.class));
    }

    private boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public void snack(String message) {
        int col = R.color.badSnack;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.log_l), message, 0);
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
                log_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.VISIBLE);
            email.setEnabled(false);
            password.setEnabled(false);
            log_button.setEnabled(false);
            go_register.setEnabled(false);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.trans;
                log_lay.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.GONE);
            email.setEnabled(true);
            password.setEnabled(true);
            log_button.setEnabled(true);
            go_register.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == log_button) {
            String uemail = email.getText().toString().toLowerCase().trim();
            String pass = password.getText().toString().toLowerCase().trim();
            if(TextUtils.isEmpty(uemail)) {
                snack("Email field is required");
                return;
            }

            if(TextUtils.isEmpty(pass)) {
                snack("Password field is required");
                return;
            }

            progress(true);
            new Thread(new Runnable() {
                public void run() {
                    loginUser();
                }
            }).start();
        }

        if(v == go_register) {
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }
}