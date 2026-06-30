package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Screen that shows all shared lists the current user participates in.
// Tapping a list opens it (ViewListActivity); tapping delete removes it entirely.
public class ListsOverviewActivity extends AppCompatActivity {

    RecyclerView recyclerViewLists;
    ListsAdapter adapter;
    ListDAO listDAO;
    String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists_overview);

        currentUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "");

        listDAO = new ListDAO(this);
        recyclerViewLists = findViewById(R.id.recyclerViewLists);
        recyclerViewLists.setLayoutManager(new LinearLayoutManager(this));

        loadLists();
    }

    // Lifecycle method: refresh the lists every time this screen becomes visible again,
    // so newly created or accepted lists show up without needing to restart the app.
    @Override
    protected void onResume() {
        super.onResume();
        loadLists();
    }

    // Loads all lists belonging to the current user from the database and
    // builds the adapter with click handlers for opening and deleting lists.
    private void loadLists() {
        List<String[]> lists = listDAO.getAllListsForUser(currentUsername);

        if (lists.isEmpty()) {
            Toast.makeText(this, "אין רשימות עדיין — צור רשימה חדשה", Toast.LENGTH_SHORT).show();
        }

        adapter = new ListsAdapter(lists, currentUsername, this,
                // onListClick: open the tapped list, passing its id and name
                listData -> {
                    Intent intent = new Intent(this, ViewListActivity.class);
                    intent.putExtra("list_id", Long.parseLong(listData[0]));
                    intent.putExtra("list_name", listData[1]);
                    startActivity(intent);
                },
                // onListDelete: remove the list from the database and update the UI
                (listData, position) -> {
                    long listId = Long.parseLong(listData[0]);
                    listDAO.deleteList(listId);
                    lists.remove(position);
                    adapter.notifyItemRemoved(position); // animates row removal
                    Toast.makeText(this, "הרשימה נמחקה", Toast.LENGTH_SHORT).show();
                });

        recyclerViewLists.setAdapter(adapter);
    }
}