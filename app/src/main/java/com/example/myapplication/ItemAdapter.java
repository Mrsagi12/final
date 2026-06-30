package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// RecyclerView Adapter responsible for displaying the list of shopping items.
// Connects the data (a List<String> of item texts) to the actual rows shown on screen,
// and handles clicks on each row's delete button.
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    // Interface used to notify the Activity when the user taps a delete button,
    // since the adapter itself shouldn't directly handle database/SMS logic.
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    private List<String> items;
    private OnDeleteClickListener deleteListener;

    public ItemAdapter(List<String> items, OnDeleteClickListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    // Replaces the current item list with a new one and refreshes the RecyclerView display.
    // Called whenever the underlying data changes (item added, deleted, or synced from SMS).
    public void updateItems(List<String> newItems) {
        this.items = newItems;
        notifyDataSetChanged(); // tells RecyclerView to redraw all visible rows
    }

    // Called by RecyclerView when it needs to create a new row view (inflates the XML layout).
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_row, parent, false);
        return new ItemViewHolder(view);
    }

    // Called by RecyclerView to fill a row with data at a specific position.
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Display item with a running number, e.g. "1. Milk"
        holder.tvItem.setText((position + 1) + ". " + items.get(position));

        // When the delete icon on this row is tapped, notify the Activity via the listener
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(position);
            }
        });
    }

    // Tells RecyclerView how many rows to display in total.
    @Override
    public int getItemCount() {
        return items.size();
    }

    // Holds references to the views inside a single row, so they don't need
    // to be looked up with findViewById() every time the row is reused/scrolled.
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem;
        ImageButton btnDelete;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tvItem);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}