package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Random;

// Registration screen. Creates a new user account after verifying their
// phone number using a randomly generated code sent via real SMS.
public class SignupActivity extends AppCompatActivity {

    Button btnSignup, btnSendCode;
    TextView tvGoToLogin;
    EditText etUsername, etPhone, etPassword, etConfirmPassword, etVerificationCode;
    UserDAO userDAO;
    String generatedCode = null; // holds the code we sent, compared against user input later
    private static final int SMS_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        userDAO = new UserDAO(this);

        btnSignup = findViewById(R.id.btnSignup);
        btnSendCode = findViewById(R.id.btnSendCode);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        etUsername = findViewById(R.id.etUsername);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etVerificationCode = findViewById(R.id.etVerificationCode);

        requestSmsPermission();

        // "Send verification code" button - generates a random 6-digit code
        // and sends it as a real SMS to the entered phone number
        btnSendCode.setOnClickListener(v -> {
            // double-check permission before attempting to send, in case it was revoked
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                Toast.makeText(this, "Please grant SMS permission and try again", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userDAO.phoneExists(phone)) {
                Toast.makeText(this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            // generate a random 6-digit code (between 100000 and 999999)
            generatedCode = String.valueOf(new Random().nextInt(900000) + 100000);
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phone, null, "Your verification code: " + generatedCode, null, null);
                etVerificationCode.setVisibility(View.VISIBLE); // reveal the code input field
                Toast.makeText(this, "Verification code sent!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // "Sign Up" button - validates every field in order, then creates the account
        btnSignup.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String code = etVerificationCode.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 4) {
                Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (generatedCode == null) {
                Toast.makeText(this, "Please send a verification code first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!code.equals(generatedCode)) {
                Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                return;
            }

            // all validations passed - create the account and mark it verified immediately
            // since the correct SMS code was already confirmed above
            if (userDAO.registerUser(username, password, phone)) {
                userDAO.setVerified(phone);
                Toast.makeText(this, "Account created! Please login", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Username or phone already taken", Toast.LENGTH_SHORT).show();
            }
        });

        // Link back to the login screen for users who already have an account
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });
    }

    // Requests SEND_SMS permission as soon as the screen opens,
    // since the signup flow depends on being able to send the verification code.
    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // Callback after the user responds to the SMS permission dialog.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission is required to verify your phone", Toast.LENGTH_LONG).show();
            }
        }
    }
}