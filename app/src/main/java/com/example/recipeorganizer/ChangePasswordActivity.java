package com.example.recipeorganizer;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword, btnCancel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnCancel = findViewById(R.id.cancel_button);

        // Set button click listener
        btnChangePassword.setOnClickListener(view -> changePassword());

        // Handle 'Cancel' button to return to previous activity
        btnCancel.setOnClickListener(v -> finish());
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation checks
        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Please enter your current password");
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("Please enter a new password");
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Please confirm your new password");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Re-authenticate the user with the current password
            reauthenticateUser(currentPassword, newPassword);
        }
    }

    private void reauthenticateUser(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        // Re-authenticate the user before updating the password
        if (user != null) {
            // Assuming email and currentPassword for re-authentication
            mAuth.signInWithEmailAndPassword(user.getEmail(), currentPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updatePassword(newPassword);  // Password update after successful re-authentication
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "Reauthentication failed. Please check your current password.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                            finish();  // Close the activity after successful password change
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "Password change failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
