package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Chat

class ChatAdapter(
    // Empezamos con una lista vacía, se poblará desde el Fragment
    private var chats: MutableList<Chat> = mutableListOf(),
    private val onItemClicked: (Chat) -> Unit // Lambda para manejar clics
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Asumiendo que los IDs en tu item_chat.xml son:
        // chat_avatar para ImageView
        // chat_name para TextView del nombre
        // chat_last_message para TextView del último mensaje
        val avatarImageView: ImageView = itemView.findViewById(R.id.chat_avatar)
        val nameTextView: TextView = itemView.findViewById(R.id.chat_name)
        val messageTextView: TextView = itemView.findViewById(R.id.chat_last_message)
        // Podrías añadir un TextView para el timestamp del último mensaje si lo deseas

        fun bind(chat: Chat, onItemClicked: (Chat) -> Unit) {
            nameTextView.text = chat.otherUserName
            messageTextView.text = chat.lastMessage

            // Cargar imagen del avatar usando Glide
            Glide.with(itemView.context)
                .load(chat.otherUserAvatarUrl)
                .placeholder(R.drawable.icono_persona) // Placeholder por defecto
                .error(R.drawable.icono_persona)       // Imagen de error si falla la carga o URL es nula
                .circleCrop() // Para avatares redondos
                .into(avatarImageView)

            itemView.setOnClickListener {
                onItemClicked(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false) // Asegúrate que tu item_chat.xml esté correcto
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentChat = chats[position]
        holder.bind(currentChat, onItemClicked)
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    /**
     * Actualiza la lista de chats en el adaptador.
     * Limpia la lista actual y añade todos los nuevos elementos.
     * Notifica al RecyclerView que los datos han cambiado.
     */
    fun submitList(newChats: List<Chat>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged() // Notifica al RecyclerView que todo el conjunto de datos ha cambiado.
        // Para listas muy grandes o actualizaciones frecuentes, considera usar DiffUtil.
    }

    /**
     * (Opcional) Método para añadir un solo chat, útil si los chats llegan uno por uno.
     * Asegúrate de que no existan duplicados si lo usas en combinación con submitList.
     */
    fun addChat(chat: Chat) {
        chats.add(chat)
        notifyItemInserted(chats.size - 1)
    }

    /**
     * (Opcional) Método para actualizar un chat existente si su contenido cambia.
     * Necesitarás una forma de encontrar el índice del chat a actualizar.
     */
    fun updateChat(updatedChat: Chat) {
        val index = chats.indexOfFirst { it.chatRoomId == updatedChat.chatRoomId }
        if (index != -1) {
            chats[index] = updatedChat
            notifyItemChanged(index)
        }
    }

    /**
     * (Opcional) Método para limpiar todos los chats.
     */
    fun clearChats() {
        chats.clear()
        notifyDataSetChanged()
    }
}