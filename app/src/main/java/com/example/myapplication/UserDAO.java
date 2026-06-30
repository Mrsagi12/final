package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

// Data Access Object for the "users" table.
// Handles all database operations related to user accounts:
// registration, login, checking duplicates, and phone verification.
public class UserDAO {

    private DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // Registers a new user if the username and phone are not already taken.
    // Returns true on success, false if username/phone already exist or insert failed.
    public boolean registerUser(String username, String password, String phone) {
        if (usernameExists(username)) return false;
        if (phoneExists(phone)) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);
        values.put(DatabaseHelper.COLUMN_PHONE, phone);
        values.put(DatabaseHelper.COLUMN_VERIFIED, 0); // new accounts start unverified

        long result = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        db.close();
        return result != -1; // insert() returns -1 if it failed
    }

    // Checks if a username + password combination exists AND the account is verified.
    // Used by LoginActivity to authenticate the user.
    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_USERNAME + "=? AND " +
                        DatabaseHelper.COLUMN_PASSWORD + "=? AND " +
                        DatabaseHelper.COLUMN_VERIFIED + "=1", // only verified accounts can log in
                new String[]{username, password},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Checks if a given username is already taken - used to prevent duplicate accounts.
    public boolean usernameExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Checks if a given phone number is already registered - used to prevent duplicate accounts.
    public boolean phoneExists(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_PHONE + "=?",
                new String[]{phone},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Marks a user's account as verified (called after the SMS verification code is correct).
    public void setVerified(String phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_VERIFIED, 1);
        db.update(DatabaseHelper.TABLE_USERS, values,
                DatabaseHelper.COLUMN_PHONE + "=?", new String[]{phone});
        db.close();
    }

    // Looks up a user's phone number given their username.
    // Used when sending SMS invites/items to a friend by username.
    public String getPhoneByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_PHONE},
                DatabaseHelper.COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null
        );
        String phone = null;
        if (cursor.moveToFirst()) {
            phone = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return phone;
    }

    // Looks up a user's username given their phone number.
    // Used when an incoming SMS needs to be matched back to a known contact.
    public String getUsernameByPhone(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME},
                DatabaseHelper.COLUMN_PHONE + "=?",
                new String[]{phone},
                null, null, null
        );
        String username = null;
        if (cursor.moveToFirst()) {
            username = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return username;
    }
}