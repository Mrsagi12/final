package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Manages creation and version upgrades of the SQLite database.
// This class defines the structure (schema) of all tables used in the app.
public class DatabaseHelper extends SQLiteOpenHelper {

    // Name and version of the database file stored on the device
    private static final String DATABASE_NAME = "app4.db";
    private static final int DATABASE_VERSION = 3;

    // ---- Users table: stores registered accounts ----
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_VERIFIED = "verified"; // 0 = not verified, 1 = verified

    // ---- Lists table: stores shared shopping lists between two users ----
    public static final String TABLE_LISTS = "lists";
    public static final String COLUMN_LIST_ID = "list_id";
    public static final String COLUMN_LIST_NAME = "list_name";
    public static final String COLUMN_FIRST_USER = "first_user";   // username of list creator
    public static final String COLUMN_SECOND_USER = "second_user"; // username of invited friend

    // ---- List items table: stores items belonging to a specific list ----
    public static final String TABLE_LIST_ITEMS = "list_items";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_ITEM_TEXT = "item_text";
    public static final String COLUMN_ITEM_LIST_ID = "list_id"; // foreign key to lists table

    // SQL command that creates the users table
    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT UNIQUE, " +   // usernames must be unique
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_PHONE + " TEXT UNIQUE, " +      // phone numbers must be unique
                    COLUMN_VERIFIED + " INTEGER DEFAULT 0)";

    // SQL command that creates the lists table
    private static final String CREATE_LISTS_TABLE =
            "CREATE TABLE " + TABLE_LISTS + " (" +
                    COLUMN_LIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LIST_NAME + " TEXT, " +
                    COLUMN_FIRST_USER + " TEXT, " +
                    COLUMN_SECOND_USER + " TEXT)";

    // SQL command that creates the list_items table
    // Includes a FOREIGN KEY linking each item to the list it belongs to
    private static final String CREATE_LIST_ITEMS_TABLE =
            "CREATE TABLE " + TABLE_LIST_ITEMS + " (" +
                    COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ITEM_TEXT + " TEXT, " +
                    COLUMN_ITEM_LIST_ID + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_ITEM_LIST_ID + ") REFERENCES " +
                    TABLE_LISTS + "(" + COLUMN_LIST_ID + "))";

    // Constructor - passes database name and version to the parent SQLiteOpenHelper class
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called automatically the first time the database is created.
    // Creates all three tables.
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_LISTS_TABLE);
        db.execSQL(CREATE_LIST_ITEMS_TABLE);
    }

    // Called automatically when DATABASE_VERSION increases.
    // Drops all old tables and recreates them (data is lost on upgrade).
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIST_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}