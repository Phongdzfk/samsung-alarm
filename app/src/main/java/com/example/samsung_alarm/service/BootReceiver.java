package com.example.samsung_alarm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.data.model.Alarm;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) return;
        PendingResult pending = goAsync();
        AppExecutors.DB.execute(() -> {
            try {
                for (Alarm alarm : AlarmRepository.get(context).getActiveSync()) {
                    AlarmScheduler.schedule(context, alarm);
                }
            } finally { pending.finish(); }
        });
    }
}
