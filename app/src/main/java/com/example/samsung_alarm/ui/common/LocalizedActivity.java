package com.example.samsung_alarm.ui.common;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.example.samsung_alarm.settings.AppPreferences;

/** Applies the saved language before Android inflates any Activity resources. **/
public abstract class LocalizedActivity extends AppCompatActivity {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppPreferences.localizedContext(base));
    }
}
