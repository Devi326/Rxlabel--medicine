package com.example.andriod.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val hour: Int,
    val minute: Int,
    val days: String,
    val isEnabled: Boolean = true
)
