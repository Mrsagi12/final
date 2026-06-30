package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

// Data Access Object for "lists" and "list_items" tables.
// Handles creating/deleting shared lists and adding/removing/reading items inside them.
public class ListDAO {

    private DatabaseHelper dbHelper;

    public ListDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // Creates a new shared list between two users and returns its generated list_id.
    public long createList(String listName, String firstUser, String secondUser) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LIST_NAME, listName);
        values.put(DatabaseHelper.COLUMN_FIRST_USER, firstUser);
        values.put(DatabaseHelper.COLUMN_SECOND_USER, secondUser);
        long id = db.insert(DatabaseHelper.TABLE_LISTS, null, values);
        db.close();
        return id;
    }

    // Updates the second_user field of a list - used once a friend accepts an invite
    // and their real username replaces the placeholder phone number.
    public void updateSecondUser(long listId, String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SECOND_USER, username);
        db.update(DatabaseHelper.TABLE_LISTS, values,
                DatabaseHelper.COLUMN_LIST_ID + "=?",
                new String[]{String.valueOf(listId)});
        db.close();
    }

    // Finds the list_id of a list given its name and one of its participating usernames.
    // Used by SmsReceiver when an ACCEPT message comes in, to locate which list it refers to.
    public long getListIdByNameAndUser(String listName, String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LISTS,
                new String[]{DatabaseHelper.COLUMN_LIST_ID},
                DatabaseHelper.COLUMN_LIST_NAME + "=? AND (" +
                        DatabaseHelper.COLUMN_FIRST_USER + "=? OR " +
                        DatabaseHelper.COLUMN_SECOND_USER + "=?)",
                new String[]{listName, username, username},
                null, null, null
        );
        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return id;
    }

    // Adds a new item to a specific list.
    public void addItem(long listId, String itemText) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ITEM_TEXT, itemText);
        values.put(DatabaseHelper.COLUMN_ITEM_LIST_ID, listId);
        db.insert(DatabaseHelper.TABLE_LIST_ITEMS, null, values);
        db.close();
    }

    // Deletes a single item by its item_id (currently unused - deletion by text is used instead).
    public void deleteItem(long itemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_LIST_ITEMS,
                DatabaseHelper.COLUMN_ITEM_ID + "=?",
                new String[]{String.valueOf(itemId)});
        db.close();
    }

    // Returns all item texts belonging to a given list, used to populate the RecyclerView.
    public List<String> getItems(long listId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> items = new ArrayList<>();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LIST_ITEMS,
                new String[]{DatabaseHelper.COLUMN_ITEM_TEXT},
                DatabaseHelper.COLUMN_ITEM_LIST_ID + "=?",
                new String[]{String.valueOf(listId)},
                null, null, null
        );
        while (cursor.moveToNext()) {
            items.add(cursor.getString(0));
        }
        cursor.close();
        db.close();
        return items;
    }

    // Returns all lists that a given username participates in (as first_user or second_user).
    // Each result row is a String[4]: {list_id, list_name, first_user, second_user}.
    // Used to populate the "all my lists" overview screen.
    public List<String[]> getAllListsForUser(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String[]> lists = new ArrayList<>();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LISTS,
                new String[]{
                        DatabaseHelper.COLUMN_LIST_ID,
                        DatabaseHelper.COLUMN_LIST_NAME,
                        DatabaseHelper.COLUMN_FIRST_USER,
                        DatabaseHelper.COLUMN_SECOND_USER
                },
                DatabaseHelper.COLUMN_FIRST_USER + "=? OR " +
                        DatabaseHelper.COLUMN_SECOND_USER + "=?",
                new String[]{username, username},
                null, null, null
        );
        while (cursor.moveToNext()) {
            lists.add(new String[]{
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            });
        }
        cursor.close();
        db.close();
        return lists;
    }

    // Returns the first list_id found for a given username (legacy helper from
    // before multiple lists were supported - kept for compatibility).
    public long getListIdForUser(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LISTS,
                new String[]{DatabaseHelper.COLUMN_LIST_ID},
                DatabaseHelper.COLUMN_FIRST_USER + "=? OR " +
                        DatabaseHelper.COLUMN_SECOND_USER + "=?",
                new String[]{username, username},
                null, null, null
        );
        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return id;
    }

    // Returns the display name of a list given its list_id.
    public String getListName(long listId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LISTS,
                new String[]{DatabaseHelper.COLUMN_LIST_NAME},
                DatabaseHelper.COLUMN_LIST_ID + "=?",
                new String[]{String.valueOf(listId)},
                null, null, null
        );
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return name;
    }

    // Given a list and "my" username, returns the OTHER participant's username.
    // Used to display "List with: [friend]" in the lists overview screen.
    public String getFriendUsername(long listId, String currentUsername) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LISTS,
                new String[]{
                        DatabaseHelper.COLUMN_FIRST_USER,
                        DatabaseHelper.COLUMN_SECOND_USER
                },
                DatabaseHelper.COLUMN_LIST_ID + "=?",
                new String[]{String.valueOf(listId)},
                null, null, null
        );
        String friendUsername = null;
        if (cursor.moveToFirst()) {
            String firstUser = cursor.getString(0);
            String secondUser = cursor.getString(1);
            // whichever username is NOT mine is the friend's
            friendUsername = firstUser.equals(currentUsername) ? secondUser : firstUser;
        }
        cursor.close();
        db.close();
        return friendUsername;
    }

    // Deletes an entire list: first removes all its items (to avoid orphaned rows),
    // then removes the list itself.
    public void deleteList(long listId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_LIST_ITEMS,
                DatabaseHelper.COLUMN_ITEM_LIST_ID + "=?",
                new String[]{String.valueOf(listId)});
        db.delete(DatabaseHelper.TABLE_LISTS,
                DatabaseHelper.COLUMN_LIST_ID + "=?",
                new String[]{String.valueOf(listId)});
        db.close();
    }
}