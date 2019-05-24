package com.osuobiem.mobiletracker.user;

import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osuobiem.mobiletracker.database.Database;

import java.util.Objects;

public class User {

    private String firstname;
    private String lastname;
    private String email;
    private String phone_number;

    public User(){
    }

    public User(String firstname, String lastname, String email, String phone_number) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone_number = phone_number;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public String fetchId() {
        return Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    public FirebaseUser fetchUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

}
