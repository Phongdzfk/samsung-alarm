package com.example.samsung_alarm.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.samsung_alarm.data.model.Alarm;

@Database(entities = {Alarm.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    public abstract AlarmDao alarmDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE alarm_table ADD COLUMN skipUntilMillis INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE alarm_table ADD COLUMN gradualVolume INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE alarm_table ADD COLUMN vibrate INTEGER NOT NULL DEFAULT 1");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "alarm_database")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigrationOnDowngrade().build();
                }
            }
        }
        return instance;
    }
}
