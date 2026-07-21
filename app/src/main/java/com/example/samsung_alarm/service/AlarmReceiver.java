package com.example.samsung_alarm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.repository.AlarmRepository;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_DISMISS = "com.example.samsung_alarm.DISMISS";
    public static final String ACTION_SNOOZE = "com.example.samsung_alarm.SNOOZE";

    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        if (ACTION_DISMISS.equals(intent.getAction()) || ACTION_SNOOZE.equals(intent.getAction())) {
            context.stopService(new Intent(context, AlarmRingingService.class));
            if (ACTION_SNOOZE.equals(intent.getAction())) {
                int minutes = intent.getIntExtra("minutes", 5);
                AlarmScheduler.scheduleSnooze(context, id, minutes);
            } else if (id > 0) {
                AppExecutors.DB.execute(() -> AlarmRepository.get(context).finishOneTimeSync(id));
            }
            return;
        }

        Intent service = new Intent(context, AlarmRingingService.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                .putExtra(AlarmScheduler.EXTRA_PREVIEW, intent.getStringExtra(AlarmScheduler.EXTRA_PREVIEW));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);

        if (id > 0) AppExecutors.DB.execute(() -> AlarmRepository.get(context).onTriggeredSync(id));
    }
}
