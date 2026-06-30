package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BadgeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Badge count is already updated in SharedPreferences by SmsReceiver
        // MainActivity reads it in onResume automatically
    }
}