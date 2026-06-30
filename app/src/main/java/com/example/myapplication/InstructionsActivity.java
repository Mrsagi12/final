package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

// Host activity for the instructions screen. Its only job is to load the
// InstructionsFragment into the screen's container and handle the back button.
public class InstructionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        // Load the Fragment into the FrameLayout placeholder defined in the XML
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new InstructionsFragment())
                .commit();

        // Custom back arrow button - simply closes this Activity and returns
        // to whichever screen opened it
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }
}