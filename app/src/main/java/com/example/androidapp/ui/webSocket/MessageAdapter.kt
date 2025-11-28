package com.example.androidapp.ui.webSocket

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidapp.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<MessageItem>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)

        fun bind(item: MessageItem) {
            messageText.text = item.text
            timeText.text = timeFormat.format(Date(item.timestamp))

            when (item.type) {
                MessageType.SENT -> {
                    messageContainer.gravity = Gravity.END
                    messageText.setBackgroundColor(Color.parseColor("#2196F3"))
                    messageText.setTextColor(Color.WHITE)
                }
                MessageType.RECEIVED -> {
                    messageContainer.gravity = Gravity.START
                    messageText.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    messageText.setTextColor(Color.BLACK)
                }
                MessageType.SYSTEM -> {
                    messageContainer.gravity = Gravity.CENTER
                    messageText.setBackgroundColor(Color.parseColor("#F5F5F5"))
                    messageText.setTextColor(Color.parseColor("#757575"))
                }
            }
        }
    }
}