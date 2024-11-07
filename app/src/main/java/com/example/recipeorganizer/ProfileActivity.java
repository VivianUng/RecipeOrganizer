package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        TextView tvEmail = findViewById(R.id.tv_email);
        Button changePwButton = findViewById(R.id.change_pw_button);
        Button btnLogout = findViewById(R.id.btn_logout);
        ImageButton btnExit = findViewById(R.id.btn_exit);

        // Set the current user's email
        tvEmail.setText(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail());

        // Handle 'Change Password' button
        // Set up the edit button click listener
        changePwButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Handle 'Logout' button
        btnLogout.setOnClickListener(v -> logoutUser());

        // Handle 'Exit' button to return to previous activity
        btnExit.setOnClickListener(v -> finish());
    }


    private void logoutUser() {
        // Create an AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User clicked Yes, log them out
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, SignupLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Close the current activity
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User cancelled the dialog, just dismiss it
                    dialog.dismiss();
                })
                .show(); // Show the dialog
    }
}

