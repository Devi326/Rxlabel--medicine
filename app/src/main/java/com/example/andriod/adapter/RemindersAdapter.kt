package com.example.andriod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.andriod.R
import com.example.andriod.data.db.ReminderEntity
import java.util.Locale

class RemindersAdapter(
    private var reminders: List<ReminderEntity>,
    private val onToggleReminder: (ReminderEntity, Boolean) -> Unit,
    private val onDeleteReminder: (ReminderEntity) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvReminderName)
        val tvTime: TextView = view.findViewById(R.id.tvReminderTime)
        val tvDays: TextView = view.findViewById(R.id.tvReminderDays)
        val switchEnabled: SwitchCompat = view.findViewById(R.id.switchReminderEnabled)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteReminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.tvName.text = reminder.name
        holder.tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute)
        holder.tvDays.text = if (reminder.days.isNotBlank()) reminder.days else "Everyday"
        
        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = reminder.isEnabled
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggleReminder(reminder, isChecked)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteReminder(reminder)
        }
    }

    override fun getItemCount(): Int = reminders.size

    fun updateData(newReminders: List<ReminderEntity>) {
        reminders = newReminders
        notifyDataSetChanged()
    }
}
