package com.example.samsung_alarm;

import android.app.Application;
import com.example.samsung_alarm.service.NotificationHelper;
import com.example.samsung_alarm.settings.AppPreferences;

/** Application-level initialization for locale, theme and notification channels. */
public class SmartAlarmApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppPreferences.apply(this);
        NotificationHelper.createChannels(this);
    }
}
