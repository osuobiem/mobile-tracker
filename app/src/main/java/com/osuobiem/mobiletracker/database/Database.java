package com.osuobiem.mobiletracker.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class Database {

    private String root;
    private String operation;
    private Object object;
    private String parent_node;
    private String child_node;
    private String leaf_node;
    private double extra_leaf;
    private String feedback;

    public Database(){}

    public Database(String root, String operation, Object object, 
                    String parent_node, String child_node, String leaf_node, double extra_leaf) {
        this.root = root;
        this.operation = operation;
        this.object = object;
        this.parent_node = parent_node;
        this.leaf_node = leaf_node;
        this.child_node = child_node;
        this.extra_leaf = extra_leaf;
    }

    public String handler() {
        switch (operation) {
            case "insert":
                feedback = insertData();
                break;

            case "delete":
                feedback = deleteData();
                break;

            case "update":
                feedback = updateData();
                break;

            default:
                feedback = "Invalid Database Operation";
                break;
        }
        return feedback;
    }

    private String insertData() {
        final String[] result = new String[1];
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(root);

        if(extra_leaf == 1) {
            boolean val;
            val = leaf_node.equals("true");

            databaseReference.child(parent_node).child(child_node).setValue(val, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        result[0] = "Successful";
                    }
                    else {
                        result[0] = "Error";
                    }
                }
            });
        }
        else {
            if (!child_node.equals("") && !leaf_node.equals("")) {
                databaseReference.child(parent_node).child(child_node).setValue(leaf_node, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            result[0] = "Successful";
                        } else {
                            result[0] = "Error";
                        }
                    }
                });
            } else {
                databaseReference.child(parent_node).setValue(object, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            result[0] = "Successful";
                        } else {
                            result[0] = "Error";
                        }
                    }
                });
            }
        }
        return result[0];
    }

    private String deleteData() {
        String result;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(root);
        if(!child_node.equals("")) {
            databaseReference.child(parent_node).child(child_node).removeValue();
            result = "Successful";
        }
        else {
            databaseReference.child(parent_node).removeValue();
            result = "Successful";
        }
        return result;
    }

    private String updateData() {
        final String[] result = new String[1];
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(root);

        if(extra_leaf != 0) {
            databaseReference.child(parent_node).child(child_node).setValue(extra_leaf, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        result[0] = "Successful";
                    }
                    else {
                        result[0] = "Error";
                    }
                }
            });
        }
        else {
            databaseReference.child(parent_node).child(child_node).setValue(leaf_node, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        result[0] = "Successful";
                    }
                    else {
                        result[0] = "Error";
                    }
                }
            });
        }

        return result[0];
    }

    public String generateKey() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String key = databaseReference.push().getKey();

        return key;
    }
}