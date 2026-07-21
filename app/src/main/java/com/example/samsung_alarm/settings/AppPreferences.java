package com.example.samsung_alarm.settings;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class AppPreferences {
    private static final String FILE = "smart_alarm_preferences";
    private static final String THEME = "theme_mode";
    private static final String LANGUAGE = "language";
    public static final int THEME_SYSTEM = 0, THEME_LIGHT = 1, THEME_DARK = 2;
    private AppPreferences() {}

    public static int theme(Context context) { return context.getSharedPreferences(FILE, 0).getInt(THEME, THEME_SYSTEM); }
    public static String language(Context context) { return context.getSharedPreferences(FILE, 0).getString(LANGUAGE, "vi"); }
    public static void save(Context context, int theme, String language) {
        context.getSharedPreferences(FILE, 0).edit().putInt(THEME, theme).putString(LANGUAGE, language).apply();
        apply(context);
    }
    public static void apply(Context context) {
        int mode = theme(context);
        AppCompatDelegate.setDefaultNightMode(mode == THEME_LIGHT ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == THEME_DARK ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language(context)));
    }
}
