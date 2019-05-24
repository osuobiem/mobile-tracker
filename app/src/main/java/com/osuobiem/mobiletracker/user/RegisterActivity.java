package com.osuobiem.mobiletracker.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
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
import com.osuobiem.mobiletracker.database.Database;
import com.osuobiem.mobiletracker.device.RegisterDevice;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText first_name;
    private EditText last_name;
    private EditText email;
    private EditText phone_number;
    private EditText password;
    private EditText cpassword;
    private Button reg_button;
    private TextView go_login;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout linearLayout;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        toolbar = findViewById(R.id.reg_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Register");


        firebaseAuth = FirebaseAuth.getInstance();

        first_name = findViewById(R.id.reg_firstname);
        last_name= findViewById(R.id.reg_lastname);
        email = findViewById(R.id.reg_email);
        phone_number = findViewById(R.id.reg_phone);
        password = findViewById(R.id.reg_pass);
        cpassword= findViewById(R.id.reg_cpass);
        progressBar = findViewById(R.id.reg_pbar);
        linearLayout = findViewById(R.id.linearLayout);

        reg_button = findViewById(R.id.reg_button);
        go_login = findViewById(R.id.go_login);

        reg_button.setOnClickListener(this);
        go_login.setOnClickListener(this);
    }

    private void registerUser() {
        String uemail = email.getText().toString().toLowerCase().trim();
        String pass = password.getText().toString().toLowerCase().trim();

        firebaseAuth.createUserWithEmailAndPassword(uemail, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            addUserToDB();
                        }
                        else {
                            progress(false);
                            snack("Email is already registered");
                        }
                    }
                });
    }

   public void addUserToDB() {
        User currentUser = new User();

        if(currentUser.fetchUser() != null) {
           String user_id = currentUser.fetchId();
           String user_email = email.getText().toString().toLowerCase().trim();
           String fname = first_name.getText().toString().trim();
           String lname = last_name.getText().toString().trim();
           String phone = phone_number.getText().toString().trim();

           User user = new User(fname, lname, user_email, phone);

           Database database = new Database("users", "insert", user, user_id, "", "", 0);
           database.handler();

           goToRegisterDevice();
        }
    }

    public boolean checkPasswordLength(String pass) {
        return pass.length() >= 6;
    }

    //public boolean checkPasswordMatch(String pass, String cpass) {
       // return pass == cpass;
   // }

    public void goToRegisterDevice() {
        startActivity(new Intent(this, RegisterDevice.class));
    }

    public void progress(boolean val) {
        if(val) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.transWColor;
                linearLayout.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.VISIBLE);
            first_name.setEnabled(false);
            last_name.setEnabled(false);
            email.setEnabled(false);
            phone_number.setEnabled(false);
            password.setEnabled(false);
            cpassword.setEnabled(false);
            reg_button.setEnabled(false);
            go_login.setEnabled(false);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int col = R.color.trans;
                linearLayout.setForeground(new ColorDrawable(ContextCompat.getColor(this, col)));
            }
            progressBar.setVisibility(View.GONE);
            first_name.setEnabled(true);
            last_name.setEnabled(true);
            email.setEnabled(true);
            phone_number.setEnabled(true);
            password.setEnabled(true);
            cpassword.setEnabled(true);
            reg_button.setEnabled(true);
            go_login.setEnabled(true);
        }
    }

    public void snack(String message) {
        int col = R.color.badSnack;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.reg_l), message, 0);
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

    @Override
    public void onClick(View v) {
        if(v == reg_button) {
            String fname = first_name.getText().toString().trim();
            String lname = last_name.getText().toString().trim();
            String phone = phone_number.getText().toString().trim();
            String uemail = email.getText().toString().toLowerCase().trim();
            String pass = password.getText().toString().toLowerCase().trim();
            //String cpass = cpassword.getText().toString().toLowerCase().trim();

            if(TextUtils.isEmpty(fname)) {
                snack("First Name field is required");
                return;
            }

            if(TextUtils.isEmpty(lname)) {
                snack("Last Name field is required");
                return;
            }

            if(TextUtils.isEmpty(phone)) {
                snack("Phone Number field is required");
                return;
            }

            if(TextUtils.isEmpty(uemail)) {
                snack("Email field is required");
                return;
            }

            if(!checkPasswordLength(pass)) {
                snack("Password must be up to 6 characters");
                return;
            }
        /*else {
            if(!checkPasswordMatch(pass, cpass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
        }*/
            progress(true);
            new Thread(new Runnable() {
                public void run() {
                    registerUser();
                }
            }).start();
        }

        if(v == go_login) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}


























