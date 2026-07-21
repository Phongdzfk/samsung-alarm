package com.example.samsung_alarm.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.samsung_alarm.data.model.Alarm;
import java.util.List;

@Dao
public interface AlarmDao {
    @Insert long insert(Alarm alarm);
    @Update void update(Alarm alarm);
    @Delete void delete(Alarm alarm);
    @Query("SELECT * FROM alarm_table ORDER BY hour ASC, minute ASC")
    LiveData<List<Alarm>> getAllAlarms();
    @Query("SELECT * FROM alarm_table WHERE isActive = 1")
    List<Alarm> getActiveAlarms();
    @Query("SELECT * FROM alarm_table WHERE id = :id LIMIT 1")
    Alarm getById(int id);
    @Query("UPDATE alarm_table SET isActive = :active WHERE id = :id")
    void setActive(int id, boolean active);
}
