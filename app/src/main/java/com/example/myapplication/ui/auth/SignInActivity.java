package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class SignInActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private ImageButton togglePasswordVisibility;
    private AppCompatButton signInButton;
    private TextView signUpLink;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        signInButton = findViewById(R.id.signInButton);
        signUpLink = findViewById(R.id.signUpLink);

        // Set up listeners
        setupPasswordToggle();
        setupSignInButton();
        setupSignUpLink();
    }

    private void setupPasswordToggle() {
        togglePasswordVisibility.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                // Show password
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility);
            } else {
                // Hide password
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility);
            }
            // Move cursor to the end of text
            passwordInput.setSelection(passwordInput.getText().length());
        });
    }

    private void setupSignInButton() {
        signInButton.setOnClickListener(v -> {
            // Skip authentication and go directly to the home page
            goToMainActivity();
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close this activity
    }

    private void setupSignUpLink() {
        signUpLink.setOnClickListener(v -> {
            // Navigate to sign up screen
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
} 