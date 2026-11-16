package com.example.andriod.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.andriod.MainActivity
import com.example.andriod.R
import com.example.andriod.util.AlarmScheduler

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "medicine_reminders_channel"
        private const val CHANNEL_NAME = "Medicine Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_NAME) ?: "Medicine"
        val reminderId = intent.getIntExtra(AlarmScheduler.EXTRA_REMINDER_ID, 0)

        showNotification(context, reminderId, medicineName)
    }

    private fun showNotification(context: Context, notificationId: Int, medicineName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled medicine reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take your medication: $medicineName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
