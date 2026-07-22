package com.example.samsung_alarm.service;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.ui.main.MainActivity;
import java.text.DateFormat;
import java.util.Date;

public final class UpcomingNotificationManager {
    private static final int NOTIFICATION_OFFSET=100_000;
    private static final int DISABLE_OFFSET=500_000;
    private UpcomingNotificationManager() {}

    public static void show(Context context, Alarm alarm) {
        if(alarm==null||!alarm.isActive)return;
        NotificationHelper.createChannels(context);
        PendingIntent open=PendingIntent.getActivity(context,alarm.id,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent disableIntent=new Intent(context,AlarmReceiver.class).setAction(AlarmReceiver.ACTION_DISABLE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID,alarm.id);
        PendingIntent disable=PendingIntent.getBroadcast(context,DISABLE_OFFSET+alarm.id,disableIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String time=DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(AlarmScheduler.nextTrigger(alarm)));
        NotificationCompat.Builder notification=new NotificationCompat.Builder(context,NotificationHelper.UPCOMING_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(context.getString(R.string.upcoming_persistent_title,time))
                .setContentText(alarm.label).setContentIntent(open).setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_DEFAULT).setSilent(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,context.getString(R.string.turn_off_alarm),disable);
        long remaining=AlarmScheduler.nextTrigger(alarm)-System.currentTimeMillis();
        if(remaining>0)notification.setTimeoutAfter(remaining);
        if(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
            context.getSystemService(NotificationManager.class).notify(NOTIFICATION_OFFSET+alarm.id,notification.build());
    }

    public static void cancel(Context context,int alarmId) {
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager!=null)manager.cancel(NOTIFICATION_OFFSET+alarmId);
    }
}
