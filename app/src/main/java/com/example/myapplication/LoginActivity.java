package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// The first screen shown when the app opens.
// Lets an existing user log in by checking their credentials against the SQLite database.
public class LoginActivity extends AppCompatActivity {

    Button btnLogin;
    TextView tvGoToSignup;
    EditText etUsername, etPassword;
    UserDAO userDAO; // handles all database queries related to users

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userDAO = new UserDAO(this);

        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        // Login button - validates input, then checks credentials in the database
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            // loginUser() checks username+password match AND that the account is verified
            if (userDAO.loginUser(username, password)) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("username", username); // pass the username to MainActivity
                startActivity(intent);
                finish(); // close LoginActivity so the user can't go "back" to it
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Link to the signup screen for users who don't have an account yet
        tvGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }
}