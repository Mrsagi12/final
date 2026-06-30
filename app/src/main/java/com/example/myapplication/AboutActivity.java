package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

// Host activity for the about screen. Loads the AboutFragment into the
// screen's container and handles the back button, same pattern as InstructionsActivity.
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new AboutFragment())
                .commit();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }
}