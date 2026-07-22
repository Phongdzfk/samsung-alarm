package com.example.samsung_alarm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.data.model.Alarm;

public class UpcomingReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        PendingResult result = goAsync();
        AppExecutors.DB.execute(() -> {
            try {
                Alarm alarm = AlarmRepository.get(context).getByIdSync(id);
                if (alarm == null || !alarm.isActive) return;
                UpcomingNotificationManager.show(context,alarm);
            } finally { result.finish(); }
        });
    }
}
