package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

// The main screen shown after a successful login.
// Provides navigation to all other screens and shows a notification badge
// for pending list invites.
public class MainActivity extends AppCompatActivity {

    Button btn1, btn2, btn3, btn4;
    ImageButton btnNotifications;
    TextView tvBadge; // red circle showing number of pending invites
    String currentUsername;
    private static final int SMS_PERMISSION_CODE = 100;

    // Local BroadcastReceiver that listens for badge update events sent by SmsReceiver.
    // When a new SMS invite arrives, this triggers a refresh of the badge number
    // while the user is on this screen.
    private BroadcastReceiver badgeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBadge();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The username arrives from LoginActivity via Intent extra.
        // Save it to SharedPreferences so every other screen in the app
        // can access "who is currently logged in" without passing it around manually.
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername != null) {
            getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("username", currentUsername)
                    .apply();
        }

        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvBadge = findViewById(R.id.tvBadge);

        requestSmsPermissions();

        // Navigation buttons to the 4 main sections of the app
        btn1.setOnClickListener(v -> startActivity(new Intent(this, SetListActivity.class)));
        btn2.setOnClickListener(v -> startActivity(new Intent(this, ListsOverviewActivity.class)));
        btn3.setOnClickListener(v -> startActivity(new Intent(this, InstructionsActivity.class)));
        btn4.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        // Bell icon - opens pending invites if there are any, otherwise shows a Toast
        btnNotifications.setOnClickListener(v -> {
            int count = getSharedPreferences("invites", MODE_PRIVATE)
                    .getInt("badge_count", 0);
            if (count > 0) {
                startActivity(new Intent(this, NotificationActivity.class));
            } else {
                Toast.makeText(this, "אין התראות חדשות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lifecycle method: called every time this screen becomes visible again
    // (including returning from another screen). Registers the badge receiver
    // and refreshes the badge count so it's always up to date.
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.myapplication.BADGE_UPDATE");
        registerReceiver(badgeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        updateBadge();
    }

    // Lifecycle method: called when this screen is no longer visible.
    // Unregisters the receiver to avoid memory leaks and duplicate registrations.
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(badgeReceiver);
    }

    // Reads the current pending-invite count from SharedPreferences and
    // shows/hides the red badge accordingly.
    private void updateBadge() {
        int count = getSharedPreferences("invites", MODE_PRIVATE)
                .getInt("badge_count", 0);
        if (count > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(String.valueOf(count));
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    // Checks if SMS permissions are already granted; if not, requests them from the user.
    // These are "dangerous" permissions in Android that require explicit runtime approval.
    private void requestSmsPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
        };
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, SMS_PERMISSION_CODE);
        }
    }

    // Callback triggered after the user responds to the permission request dialog.
    // Shows a warning Toast if any of the SMS permissions were denied.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permissions are required", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }
}