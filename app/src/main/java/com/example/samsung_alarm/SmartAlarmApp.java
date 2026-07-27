package com.example.samsung_alarm;

import android.app.Application;
import android.content.Context;
import com.example.samsung_alarm.service.NotificationHelper;
import com.example.samsung_alarm.settings.AppPreferences;

/** Application-level initialization for locale, theme and notification channels. */
public class SmartAlarmApp extends Application {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppPreferences.localizedContext(base));
    }
    @Override public void onCreate() {
        super.onCreate();
        AppPreferences.apply(this);
        NotificationHelper.createChannels(this);
    }
}
