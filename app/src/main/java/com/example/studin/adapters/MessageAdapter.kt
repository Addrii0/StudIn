package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.Message
import com.example.studin.R // Asegúrate que es tu R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String // UID del usuario actual para determinar si el mensaje es enviado o recibido
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // Usamos RecyclerView.ViewHolder genérico

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    // ViewHolder para mensajes enviados
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageBody: TextView = itemView.findViewById(R.id.textView_message_body_sent)
        val messageTimestamp: TextView = itemView.findViewById(R.id.textView_message_timestamp_sent) // Opcional

        fun bind(message: Message) {
            messageBody.text = message.text
            if (message.timestamp > 0) { // Mostrar timestamp si está disponible
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
        val messageTimestamp: TextView = itemView.findViewById(R.id.textView_message_timestamp_received) // Opcional
        // val userAvatar: ImageView = itemView.findViewById(R.id.imageView_avatar_received) // Si lo tienes

        fun bind(message: Message) {
            messageBody.text = message.text
            if (message.timestamp > 0) {
                messageTimestamp.text = formatTimestamp(message.timestamp)
                messageTimestamp.visibility = View.VISIBLE
            } else {
                messageTimestamp.visibility = View.GONE
            }
            // Aquí podrías cargar el avatar del remitente si lo incluyes en el layout
            // Glide.with(itemView.context).load(senderAvatarUrl).into(userAvatar)
        }
    }

    /**
     * Determina qué tipo de vista usar para un ítem en una posición dada.
     */
    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    /**
     * Crea un nuevo ViewHolder (invocado por el layout manager).
     */
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

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageViewHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    /**
     * Devuelve el tamaño de tu dataset (invocado por el layout manager).
     */
    override fun getItemCount(): Int {
        return messageList.size
    }

    /**
     * Formatea el timestamp (Long) a una cadena de tiempo legible (ej. "10:30 AM").
     */
    private fun formatTimestamp(timestamp: Long): String {
        // Puedes personalizar el formato según tus necesidades
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()) // ej. 10:30 AM
        // Si quieres incluir la fecha para mensajes más antiguos:
        // val messageDate = Calendar.getInstance()
        // messageDate.timeInMillis = timestamp
        // val today = Calendar.getInstance()
        // if (messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        //     messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
        //     return sdf.format(Date(timestamp)) // Solo hora si es de hoy
        // } else {
        //     val sdfDate = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) // ej. 23 Oct, 10:30 AM
        //     return sdfDate.format(Date(timestamp))
        // }
        return sdf.format(Date(timestamp))
    }

    // (Opcional) Si necesitas actualizar la lista desde fuera de forma más compleja que solo
    // reconstruyendo el adaptador, puedes añadir métodos como submitList, addMessage, etc.
    // Por ahora, como la lista se pasa en el constructor y se actualiza con notifyItemInserted
    // desde ChatMessageActivity, no es estrictamente necesario aquí.
}