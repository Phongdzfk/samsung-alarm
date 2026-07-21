package com.example.samsung_alarm.service;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.samsung_alarm.ui.main.MainActivity;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.data.model.Alarm;
import java.util.Locale;

public class UpcomingReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        PendingResult result = goAsync();
        AppExecutors.DB.execute(() -> {
            try {
                Alarm alarm = AlarmRepository.get(context).getByIdSync(id);
                if (alarm == null || !alarm.isActive) return;
                NotificationHelper.createChannels(context);
                PendingIntent open = PendingIntent.getActivity(context, id, new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                String time = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute);
                NotificationCompat.Builder n = new NotificationCompat.Builder(context, NotificationHelper.UPCOMING_CHANNEL)
                        .setSmallIcon(R.drawable.ic_alarm)
                        .setContentTitle(context.getString(R.string.upcoming_notification_title))
                        .setContentText(time + " · " + alarm.label)
                        .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT);
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    context.getSystemService(NotificationManager.class).notify(100000 + id, n.build());
                }
            } finally { result.finish(); }
        });
    }
}
