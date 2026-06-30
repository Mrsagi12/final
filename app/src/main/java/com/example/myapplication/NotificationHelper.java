package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

// Utility class for showing system notifications (the ones that appear in the
// phone's notification shade). Used by SmsReceiver to alert the user about
// new invites, items, and deletions even when the app is in the background.
public class NotificationHelper {

    private static final String CHANNEL_ID = "app_channel";
    private static final String CHANNEL_NAME = "App Notifications";
    // Incrementing id so each notification is shown separately instead of overwriting the last one
    private static int notificationId = 0;

    // Builds and displays a single system notification with the given title and message.
    public static void showNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ (API 26+) requires every notification to belong to a "channel".
        // This creates the channel once if it doesn't already exist.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        // Build the actual notification content
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // dismiss automatically when tapped

        // notificationId++ ensures each call creates a new notification instead of
        // replacing a previous one with the same id
        manager.notify(notificationId++, builder.build());
    }
}