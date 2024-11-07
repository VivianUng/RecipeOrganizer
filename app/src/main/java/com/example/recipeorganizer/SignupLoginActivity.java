package com.example.recipeorganizer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SignupLoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth auth;

    // Define password strength criteria
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^" +
                    "(?=.*[0-9])" +         // at least one digit
                    "(?=.*[a-z])" +         // at least one lowercase letter
                    "(?=.*[A-Z])" +         // at least one uppercase letter
                    "(?=.*[@#$%^&+=!_])" +  // at least one special character
                    "(?=\\S+$)" +           // no whitespace
                    ".{8,}" +               // at least 8 characters
                    "$");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        Button signupButton = findViewById(R.id.signupButton);
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        ImageView passwordInfoIcon = findViewById(R.id.passwordInfoIcon);

        passwordInfoIcon.setOnClickListener(v -> showPasswordInfoDialog());
        loginButton.setOnClickListener(v -> loginUser());
        signupButton.setOnClickListener(v -> signupUser());
        forgotPasswordButton.setOnClickListener(v -> forgotPassword());
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }


    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignupLoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isValidEmail(email)){
            Toast.makeText(SignupLoginActivity.this, "Invalid email", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        // Navigate to RecipeListActivity
                        Toast.makeText(SignupLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupLoginActivity.this, RecipeListActivity.class));
                    } else {
                        Toast.makeText(SignupLoginActivity.this, "Email or password incorrect", Toast.LENGTH_SHORT).show();
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

        if(!isValidEmail(email)){
            Toast.makeText(SignupLoginActivity.this, "Invalid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            Toast.makeText(SignupLoginActivity.this, "Password not strong enough", Toast.LENGTH_SHORT).show();
            showPasswordInfoDialog();
            return;
        }


        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupLoginActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupLoginActivity.this, RecipeListActivity.class));
                    } else {
                        Toast.makeText(SignupLoginActivity.this, "Sign Up Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void forgotPassword() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(SignupLoginActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isValidEmail(email)){
            Toast.makeText(SignupLoginActivity.this, "Invalid email", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupLoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignupLoginActivity.this, "Error in sending password reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPasswordInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Requirements")
                .setMessage("Password must be at least 8 characters long, include uppercase, lowercase, digit, and special character.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

}