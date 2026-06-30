package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// RecyclerView Adapter for the "all my lists" overview screen.
// Displays each shared list as a card with its name, the friend's username,
// and a delete button.
public class ListsAdapter extends RecyclerView.Adapter<ListsAdapter.ListViewHolder> {

    // Triggered when the user taps a list card - used to open that specific list
    public interface OnListClickListener {
        void onListClick(String[] listData);
    }

    // Triggered when the user taps the delete button on a list card
    public interface OnListDeleteListener {
        void onListDelete(String[] listData, int position);
    }

    // Each entry in "lists" is a String[4]: {list_id, list_name, first_user, second_user}
    private List<String[]> lists;
    private OnListClickListener clickListener;
    private OnListDeleteListener deleteListener;
    private String currentUsername;
    private Context context;

    public ListsAdapter(List<String[]> lists, String currentUsername,
                        Context context, OnListClickListener clickListener,
                        OnListDeleteListener deleteListener) {
        this.lists = lists;
        this.currentUsername = currentUsername;
        this.context = context;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_overview_row, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        String[] listData = lists.get(position);
        long listId = Long.parseLong(listData[0]);
        holder.tvListName.setText(listData[1]);

        // Prefer the friend's real username if we saved it after they accepted the
        // invite (see SmsReceiver ACCEPT case). Otherwise fall back to whatever
        // is stored in the database (which might still be a phone number placeholder).
        String storedFriendUsername = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("friend_username_" + listId, null);

        String friendDisplay;
        if (storedFriendUsername != null) {
            friendDisplay = storedFriendUsername;
        } else {
            String firstUser = listData[2];
            String secondUser = listData[3];
            // show whichever username is NOT mine
            friendDisplay = firstUser.equals(currentUsername) ? secondUser : firstUser;
        }

        holder.tvListDetails.setText("רשימה עם: " + friendDisplay);

        // Tapping the row opens the list; tapping the trash icon deletes it
        holder.itemView.setOnClickListener(v -> clickListener.onListClick(listData));
        holder.btnDeleteList.setOnClickListener(v -> deleteListener.onListDelete(listData, position));
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    // Holds references to the views inside a single list card row.
    public static class ListViewHolder extends RecyclerView.ViewHolder {
        TextView tvListName, tvListDetails;
        ImageButton btnDeleteList;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            tvListName = itemView.findViewById(R.id.tvListName);
            tvListDetails = itemView.findViewById(R.id.tvListDetails);
            btnDeleteList = itemView.findViewById(R.id.btnDeleteList);
        }
    }
}