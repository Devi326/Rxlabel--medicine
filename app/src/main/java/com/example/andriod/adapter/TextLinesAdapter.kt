package com.example.andriod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.andriod.R

class TextLinesAdapter(
    private var lines: List<String>,
    private val onLineClicked: (String) -> Unit
) : RecyclerView.Adapter<TextLinesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLine: TextView = view.findViewById(R.id.tvLineText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_text_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = lines[position]
        holder.tvLine.text = text
        holder.itemView.setOnClickListener {
            onLineClicked(text)
        }
    }

    override fun getItemCount(): Int = lines.size

    fun updateData(newLines: List<String>) {
        lines = newLines
        notifyDataSetChanged()
    }
}
