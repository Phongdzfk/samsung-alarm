package com.example.samsung_alarm.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import com.example.samsung_alarm.R;

public final class NotificationHelper {
    public static final String ALARM_CHANNEL = "ringing_alarm";
    public static final String UPCOMING_CHANNEL = "upcoming_alarm";
    private NotificationHelper() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel alarm = new NotificationChannel(ALARM_CHANNEL, context.getString(R.string.channel_alarm),
                NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription(context.getString(R.string.channel_alarm_description));
        alarm.setSound(null, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        alarm.enableVibration(true);
        alarm.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(alarm);

        NotificationChannel upcoming = new NotificationChannel(UPCOMING_CHANNEL, context.getString(R.string.channel_upcoming),
                NotificationManager.IMPORTANCE_DEFAULT);
        upcoming.setDescription(context.getString(R.string.channel_upcoming_description));
        manager.createNotificationChannel(upcoming);
    }
}
