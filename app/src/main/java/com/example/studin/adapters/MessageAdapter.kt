package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.Message
import com.example.studin.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String // UID del usuario actual para determinar si el mensaje es enviado o recibido
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    // ViewHolder para mensajes enviados
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageBody: TextView = itemView.findViewById(R.id.textView_message_body_sent)
        val messageTimestamp: TextView = itemView.findViewById(R.id.textView_message_timestamp_sent)

        fun bind(message: Message) {
            messageBody.text = message.text
            if (message.timestamp > 0) {
                messageTimestamp.text = formatTimestamp(message.timestamp)
                messageTimestamp.visibility = View.VISIBLE
            } else {
                messageTimestamp.visibility = View.GONE
            }
        }
    }

    // ViewHolder para mensajes recibidos
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageBody: TextView = itemView.findViewById(R.id.textView_message_body_received)
        val messageTimestamp: TextView = itemView.findViewById(R.id.textView_message_timestamp_received)
        // val userAvatar: ImageView = itemView.findViewById(R.id.imageView_avatar_received)

        fun bind(message: Message) {
            messageBody.text = message.text
            if (message.timestamp > 0) {
                messageTimestamp.text = formatTimestamp(message.timestamp)
                messageTimestamp.visibility = View.VISIBLE
            } else {
                messageTimestamp.visibility = View.GONE
            }
            //  Cargar el avatar del remitente si lo incluyes en el layout
            // Glide.with(itemView.context).load(senderAvatarUrl).into(userAvatar)
        }
    }


    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else { // VIEW_TYPE_MESSAGE_RECEIVED
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageViewHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageViewHolder).bind(message)
        }
    }


    override fun getItemCount(): Int {
        return messageList.size
    }


    private fun formatTimestamp(timestamp: Long): String {
        // Se puede personalizar el formato según necesidades
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()) // ejemplo: 10:30 AM
         // incluir la fecha para mensajes más antiguos:
         val messageDate = Calendar.getInstance()
         messageDate.timeInMillis = timestamp
         val today = Calendar.getInstance()
         if (messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
             messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
             return sdf.format(Date(timestamp)) // Solo hora si es de hoy
         } else {
             val sdfDate = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) // ejemplo : 23 Oct, 10:30 AM
             return sdfDate.format(Date(timestamp))
         }
        return sdf.format(Date(timestamp))
    }


}