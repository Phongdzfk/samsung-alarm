package com.example.samsung_alarm.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "alarm_table")
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int hour;
    public int minute;
    public boolean isActive = true;
    public boolean mon, tue, wed, thu, fri, sat, sun;
    public String label = "Báo thức";
    public String ringtoneUri;
    public int volume = 80;
    public boolean isMathDismiss;
    public int mathDifficulty = 1;
    public int snoozeMinutes = 5;
    public int autoAction = 0; // 0: không, 1: tự hoãn, 2: tự tắt
    public int autoAfterMinutes = 5;
    public boolean keepAfterDismiss;
    @ColumnInfo(defaultValue = "0") public long skipUntilMillis;
    @ColumnInfo(defaultValue = "0") public boolean gradualVolume;
    @ColumnInfo(defaultValue = "1") public boolean vibrate = true;

    public Alarm() {}

    public boolean repeats() {
        return mon || tue || wed || thu || fri || sat || sun;
    }
}
