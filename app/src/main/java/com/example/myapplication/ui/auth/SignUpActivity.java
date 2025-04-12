package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput;
    private EditText phoneNumberInput;
    private EditText emailInput;
    private EditText passwordInput;
    private ImageButton togglePasswordVisibility;
    private AppCompatButton signUpButton;
    private TextView signInLink;
    private ImageButton backButton;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize views
        fullNameInput = findViewById(R.id.fullNameInput);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        signUpButton = findViewById(R.id.signUpButton);
        signInLink = findViewById(R.id.signInLink);
        backButton = findViewById(R.id.backButton);

        // Set up listeners
        setupPasswordToggle();
        setupSignUpButton();
        setupSignInLink();
        setupBackButton();
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

    private void setupSignUpButton() {
        signUpButton.setOnClickListener(v -> {
            // Skip validation and registration, go directly to the home page
            goToMainActivity();
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close this activity
    }

    private void setupSignInLink() {
        signInLink.setOnClickListener(v -> {
            // Navigate to sign in screen
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
        });
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            // Just finish this activity to go back
            finish();
        });
    }
} 