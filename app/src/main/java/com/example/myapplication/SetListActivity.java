package com.example.myapplication;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Screen for creating a new shared shopping list and inviting a friend to it.
// Sends the invite as an SMS containing a special LIST_INVITE prefix that
// SmsReceiver will recognize on the friend's device.
public class SetListActivity extends AppCompatActivity {

    EditText etListName, etFriendPhone;
    Button btnCreateList;
    String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_list);

        // Get the logged-in username saved earlier in MainActivity
        currentUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "");

        etListName = findViewById(R.id.etListName);
        etFriendPhone = findViewById(R.id.etFriendPhone);
        btnCreateList = findViewById(R.id.btnCreateList);

        btnCreateList.setOnClickListener(v -> {
            String listName = etListName.getText().toString().trim();
            String friendPhone = etFriendPhone.getText().toString().trim();

            if (listName.isEmpty()) {
                Toast.makeText(this, "Please enter a list name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (friendPhone.isEmpty()) {
                Toast.makeText(this, "Please enter friend's phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                SmsManager smsManager = SmsManager.getDefault();
                // Build the SMS message: "LIST_INVITE:listName:myUsername"
                // The friend's SmsReceiver will parse this and know who invited them and to what list
                String message = SmsReceiver.INVITE_PREFIX + listName + ":" + currentUsername;
                smsManager.sendTextMessage(friendPhone, null, message, null, null);

                // Create the list locally right away. Since we don't know the friend's
                // username yet (only their phone number), we temporarily store the phone
                // number as a placeholder in the second_user field. It gets replaced with
                // their real username later, once they accept the invite (see SmsReceiver ACCEPT case).
                long listId = new ListDAO(this).createList(listName, currentUsername, friendPhone);

                // Remember the friend's phone number for this specific list, so future
                // item additions/deletions know where to send the sync SMS
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("friend_phone_" + listId, friendPhone)
                        .putString("friend_username_" + listId, friendPhone) // placeholder until accepted
                        .apply();

                Toast.makeText(this, "Invite sent!", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send invite: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}