package com.example.andriod.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    LiveData<List<ReminderEntity>> getAllReminders();

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    List<ReminderEntity> getActiveReminders();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertReminder(ReminderEntity reminder);

    @Update
    void updateReminder(ReminderEntity reminder);

    @Delete
    void deleteReminder(ReminderEntity reminder);
}
