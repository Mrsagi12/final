package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// BroadcastReceiver that listens for incoming SMS messages.
// This is the core of the app's sync mechanism: it intercepts SMS messages with
// special prefixes (invite, accept, decline, item, delete) and updates the local
// SQLite database accordingly, without the user needing to open the SMS app.
public class SmsReceiver extends BroadcastReceiver {

    // Message prefixes used to identify the type/purpose of an incoming SMS.
    // Format examples:
    // "LIST_INVITE:listName:senderUsername"
    // "LIST_ACCEPT:listName:acceptingUsername"
    // "LIST_DECLINE:listName:decliningUsername"
    // "LIST_ITEM:listId:itemText"
    // "LIST_DELETE:listId:itemText"
    public static final String INVITE_PREFIX = "LIST_INVITE:";
    public static final String ACCEPT_PREFIX = "LIST_ACCEPT:";
    public static final String DECLINE_PREFIX = "LIST_DECLINE:";
    public static final String ITEM_PREFIX = "LIST_ITEM:";
    public static final String DELETE_PREFIX = "LIST_DELETE:";

    // Called automatically by the system whenever an SMS is received,
    // because this receiver is registered in AndroidManifest.xml for SMS_RECEIVED.
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        // SMS data arrives packed as "pdus" (protocol data units) - need to decode each one
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            String sender = sms.getOriginatingAddress(); // phone number that sent the SMS
            String body = sms.getMessageBody();           // text content of the SMS

            if (body == null) continue;

            // ---- Case 1: someone invited me to a shared list ----
            if (body.startsWith(INVITE_PREFIX)) {
                String content = body.substring(INVITE_PREFIX.length());
                String[] parts = content.split(":");
                if (parts.length < 2) continue;
                String listName = parts[0];
                String senderUsername = parts[1];

                // Save the invite so it can be shown later in NotificationActivity
                addPendingInvite(context, body, sender);

                // Show a system notification immediately
                NotificationHelper.showNotification(context,
                        "הזמנה לרשימה חדשה",
                        senderUsername + " מזמין אותך לרשימה: " + listName);

                // Update the red badge count on the main screen's bell icon
                updateBadgeCount(context);

                // ---- Case 2: a friend accepted my invite ----
            } else if (body.startsWith(ACCEPT_PREFIX)) {
                String[] parts = body.substring(ACCEPT_PREFIX.length()).split(":");
                if (parts.length < 2) continue;
                String listName = parts[0];
                String acceptingUsername = parts[1];

                String currentUsername = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .getString("username", "");

                // Find the list I created earlier (it was stored with the friend's phone
                // number as a placeholder) and update it with their real username
                ListDAO listDAO = new ListDAO(context);
                long listId = listDAO.getListIdByNameAndUser(listName, currentUsername);

                if (listId != -1) {
                    listDAO.updateSecondUser(listId, acceptingUsername);
                    context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("friend_username_" + listId, acceptingUsername)
                            .apply();
                }

                NotificationHelper.showNotification(context,
                        "הרשימה אושרה!",
                        acceptingUsername + " קיבל את ההזמנה לרשימה: " + listName);

                // ---- Case 3: a friend declined my invite ----
            } else if (body.startsWith(DECLINE_PREFIX)) {
                String[] parts = body.substring(DECLINE_PREFIX.length()).split(":");
                if (parts.length < 2) continue;
                String listName = parts[0];
                String decliningUsername = parts[1];

                NotificationHelper.showNotification(context,
                        "הרשימה נדחתה",
                        decliningUsername + " דחה את ההזמנה לרשימה: " + listName);

                // ---- Case 4: a friend added an item to our shared list ----
            } else if (body.startsWith(ITEM_PREFIX)) {
                String[] parts = body.substring(ITEM_PREFIX.length()).split(":", 2);
                if (parts.length < 2) continue;
                long listId = Long.parseLong(parts[0]);
                String itemText = parts[1];

                // Save the new item directly into the local SQLite database
                new ListDAO(context).addItem(listId, itemText);

                NotificationHelper.showNotification(context,
                        "פריט חדש ברשימה",
                        "נוסף: " + itemText);

                // Tell ViewListActivity (if it's currently open) to refresh its RecyclerView
                Intent updateIntent = new Intent(ViewListActivity.ACTION_LIST_UPDATED);
                context.sendBroadcast(updateIntent);

                // ---- Case 5: a friend deleted an item from our shared list ----
            } else if (body.startsWith(DELETE_PREFIX)) {
                String[] parts = body.substring(DELETE_PREFIX.length()).split(":", 2);
                if (parts.length < 2) continue;
                long listId = Long.parseLong(parts[0]);
                String itemText = parts[1];

                // Remove the matching item from the local database
                android.database.sqlite.SQLiteDatabase db =
                        new DatabaseHelper(context).getWritableDatabase();
                db.delete("list_items", "list_id=? AND item_text=?",
                        new String[]{String.valueOf(listId), itemText});
                db.close();

                NotificationHelper.showNotification(context,
                        "פריט הוסר מהרשימה",
                        "הוסר: " + itemText);

                // Refresh the open list screen if it's visible
                Intent updateIntent = new Intent(ViewListActivity.ACTION_LIST_UPDATED);
                context.sendBroadcast(updateIntent);
            }
        }
    }

    // Saves a pending list invite into SharedPreferences as a JSON array,
    // so multiple invites can queue up and be shown one at a time later.
    private void addPendingInvite(Context context, String inviteData, String senderPhone) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("invites", Context.MODE_PRIVATE);
            String existing = prefs.getString("invite_list", "[]");
            JSONArray arr = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("invite", inviteData);
            obj.put("phone", senderPhone);
            arr.put(obj);
            prefs.edit().putString("invite_list", arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Recalculates how many pending invites exist and saves the count,
    // then broadcasts an update so MainActivity can refresh its red badge number.
    private void updateBadgeCount(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("invites", Context.MODE_PRIVATE);
            String existing = prefs.getString("invite_list", "[]");
            JSONArray arr = new JSONArray(existing);
            prefs.edit().putInt("badge_count", arr.length()).apply();

            Intent badgeIntent = new Intent("com.example.myapplication.BADGE_UPDATE");
            context.sendBroadcast(badgeIntent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}