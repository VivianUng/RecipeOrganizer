package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupLoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, signupButton;
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);


        loginButton.setOnClickListener(v -> loginUser());
        signupButton.setOnClickListener(v -> signupUser());
    }


    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignupLoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User logged in successfully, get the user UID
                        FirebaseUser user = auth.getCurrentUser();
                        String userId = user.getUid();

                        // Navigate to RecipeListActivity
                        Toast.makeText(SignupLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupLoginActivity.this, RecipeListActivity.class));
                    } else {
                        // Log error message for debugging
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Toast.makeText(SignupLoginActivity.this, "Authentication Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("LoginError", errorMessage);
                    }
                });
    }

    private void signupUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignupLoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User signed up successfully, get the user UID
                        FirebaseUser user = auth.getCurrentUser();
                        String userId = user.getUid();


                        Toast.makeText(SignupLoginActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupLoginActivity.this, RecipeListActivity.class));
                    } else {
                        // Log error message for debugging
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Toast.makeText(SignupLoginActivity.this, "Sign Up Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("SignUpError", errorMessage);
                    }
                });
    }
}