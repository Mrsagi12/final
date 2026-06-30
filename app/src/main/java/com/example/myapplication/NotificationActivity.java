package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Screen shown when the user opens the notification bell and has pending list invites.
// Displays one invite at a time and lets the user accept or decline it.
// Supports multiple queued invites - after responding to one, the next is shown automatically.
public class NotificationActivity extends AppCompatActivity {

    TextView tvInviteMessage, tvInviteDetails;
    Button btnAccept, btnDecline;
    String listName, senderUsername, senderPhone;
    String currentUsername;
    JSONArray inviteList;   // all pending invites, stored as JSON in SharedPreferences
    int currentIndex = 0;   // which invite in the array is currently being shown

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        tvInviteMessage = findViewById(R.id.tvInviteMessage);
        tvInviteDetails = findViewById(R.id.tvInviteDetails);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        currentUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "");

        loadCurrentInvite();
    }

    // Reads the saved list of pending invites and displays the one at currentIndex.
    // Each invite was originally an SMS string like "LIST_INVITE:listName:senderUsername",
    // which gets parsed here to extract the list name and sender.
    private void loadCurrentInvite() {
        try {
            SharedPreferences prefs = getSharedPreferences("invites", MODE_PRIVATE);
            String inviteListStr = prefs.getString("invite_list", "[]");
            inviteList = new JSONArray(inviteListStr);

            if (inviteList.length() == 0) {
                Toast.makeText(this, "אין הזמנות ממתינות", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            JSONObject current = inviteList.getJSONObject(currentIndex);
            String inviteData = current.getString("invite");
            senderPhone = current.getString("phone");

            // Strip the "LIST_INVITE:" prefix and split the remaining "listName:senderUsername"
            String content = inviteData.substring(SmsReceiver.INVITE_PREFIX.length());
            String[] parts = content.split(":");
            listName = parts[0];
            senderUsername = parts[1];

            // Show "1/3" style counter when there are multiple invites queued
            tvInviteMessage.setText(senderUsername + " מזמין אותך לרשימה " +
                    (currentIndex + 1) + "/" + inviteList.length() + ":");
            tvInviteDetails.setText(listName);

            btnAccept.setOnClickListener(v -> handleResponse(true));
            btnDecline.setOnClickListener(v -> handleResponse(false));

        } catch (JSONException e) {
            e.printStackTrace();
            finish();
        }
    }

    // Sends an ACCEPT or DECLINE SMS back to whoever sent the invite,
    // and if accepted, creates the shared list locally too.
    private void handleResponse(boolean accepted) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String prefix = accepted ? SmsReceiver.ACCEPT_PREFIX : SmsReceiver.DECLINE_PREFIX;
            String message = prefix + listName + ":" + currentUsername;
            smsManager.sendTextMessage(senderPhone, null, message, null, null);

            if (accepted) {
                // Create the list on my own device too, so both sides have it
                ListDAO listDAO = new ListDAO(this);
                long listId = listDAO.createList(listName, senderUsername, currentUsername);
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("friend_phone_" + listId, senderPhone)
                        .apply();
                Toast.makeText(this, "קיבלת את ההזמנה!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "דחית את ההזמנה", Toast.LENGTH_SHORT).show();
            }

            // Remove this invite from the pending queue now that it's been handled
            removeCurrentInvite();

        } catch (Exception e) {
            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Rebuilds the invite list without the one that was just handled,
    // updates the badge count, and either shows the next invite or closes the screen.
    private void removeCurrentInvite() {
        try {
            JSONArray newList = new JSONArray();
            for (int i = 0; i < inviteList.length(); i++) {
                if (i != currentIndex) {
                    newList.put(inviteList.get(i));
                }
            }
            SharedPreferences prefs = getSharedPreferences("invites", MODE_PRIVATE);
            prefs.edit()
                    .putString("invite_list", newList.toString())
                    .putInt("badge_count", newList.length())
                    .apply();

            if (newList.length() > 0) {
                // more invites waiting - show the next one
                inviteList = newList;
                currentIndex = 0;
                loadCurrentInvite();
            } else {
                // no more invites - close the screen
                finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            finish();
        }
    }
}