package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

// Shows the items of one specific shared list, and lets the user add/delete items.
// Every change is synced to the friend's device automatically via SMS.
public class ViewListActivity extends AppCompatActivity {

    // Custom action name used for a LOCAL broadcast - this lets SmsReceiver tell
    // this screen "data changed, please refresh" while it's open.
    public static final String ACTION_LIST_UPDATED = "com.example.myapplication.LIST_UPDATED";

    RecyclerView recyclerView;
    ItemAdapter adapter;
    EditText etNewItem;
    Button btnAddItem;
    TextView tvListName;
    ListDAO listDAO;
    String currentUsername;
    long listId = -1;
    String friendPhone = null; // where to send SMS sync messages for this list
    List<String> currentItems = new ArrayList<>();

    // Receiver that listens for the ACTION_LIST_UPDATED broadcast sent by SmsReceiver.
    // When a friend adds/removes an item remotely, this triggers a live refresh
    // of the RecyclerView without the user needing to reopen the screen.
    private BroadcastReceiver listUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_list);

        currentUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "");

        listDAO = new ListDAO(this);

        tvListName = findViewById(R.id.tvListName);
        recyclerView = findViewById(R.id.recyclerView);
        etNewItem = findViewById(R.id.etNewItem);
        btnAddItem = findViewById(R.id.btnAddItem);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // This screen is opened with the specific list's id and name passed via Intent
        // (from ListsOverviewActivity, when the user taps a list)
        listId = getIntent().getLongExtra("list_id", -1);
        String listName = getIntent().getStringExtra("list_name");

        if (listId == -1) {
            Toast.makeText(this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvListName.setText(listName != null ? listName : "הרשימה שלי");

        // Retrieve the friend's phone number that was saved for this specific list
        friendPhone = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("friend_phone_" + listId, null);

        // Load existing items and set up the RecyclerView with a delete callback
        currentItems = listDAO.getItems(listId);
        adapter = new ItemAdapter(currentItems, position -> {
            String itemToDelete = currentItems.get(position);

            // Delete the item from my own local database
            deleteItemByText(listId, itemToDelete);

            // Notify the friend's device by sending a "delete" SMS so their
            // copy of the list stays in sync
            if (friendPhone != null) {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    String message = SmsReceiver.DELETE_PREFIX + listId + ":" + itemToDelete;
                    smsManager.sendTextMessage(friendPhone, null, message, null, null);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not sync delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            refreshList();
        });
        recyclerView.setAdapter(adapter);

        // "Add" button - saves the new item locally and sends it via SMS to the friend
        btnAddItem.setOnClickListener(v -> {
            String itemText = etNewItem.getText().toString().trim();
            if (itemText.isEmpty()) {
                Toast.makeText(this, "הכנס פריט", Toast.LENGTH_SHORT).show();
                return;
            }

            listDAO.addItem(listId, itemText);

            if (friendPhone != null) {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    String message = SmsReceiver.ITEM_PREFIX + listId + ":" + itemText;
                    smsManager.sendTextMessage(friendPhone, null, message, null, null);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not sync: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            refreshList();
            etNewItem.setText(""); // clear input field after adding
        });
    }

    // Re-reads all items for this list from the database and updates the RecyclerView.
    // Called after any local change and also after a remote sync broadcast.
    private void refreshList() {
        currentItems = listDAO.getItems(listId);
        adapter.updateItems(currentItems);
    }

    // Removes an item from the database by matching its text (simple approach
    // since item text is treated as unique enough for this app's scope).
    private void deleteItemByText(long listId, String itemText) {
        SQLiteDatabase db = new DatabaseHelper(this).getWritableDatabase();
        db.delete("list_items", "list_id=? AND item_text=?",
                new String[]{String.valueOf(listId), itemText});
        db.close();
    }

    // Lifecycle method: register the local broadcast receiver only while this
    // screen is visible, and refresh the list in case anything changed while away.
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_LIST_UPDATED);
        registerReceiver(listUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        refreshList();
    }

    // Lifecycle method: unregister the receiver when leaving the screen,
    // to avoid leaks and receiving updates for a screen that's no longer shown.
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(listUpdateReceiver);
    }
}