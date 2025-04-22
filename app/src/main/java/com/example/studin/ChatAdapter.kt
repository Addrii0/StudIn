package com.example.studin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chats: MutableList<Chat>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // 1. ViewHolder: Mantiene las referencias a las vistas de cada elemento
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.chat_name)
        val messageTextView: TextView = itemView.findViewById(R.id.chat_last_message)
        //val imageView:ImageView = itemView.findViewById(R.id.chat_avatar) // añadir mas vistas si se necesitan
    }

    // 2. onCreateViewHolder: Crea el ViewHolder (se llama cuando se necesita un nuevo elemento)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(itemView)
    }

    // 3. onBindViewHolder: Conecta los datos con las vistas (se llama para cada elemento)
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentChat = chats[position]
        holder.nameTextView.text = currentChat.name
        holder.messageTextView.text = currentChat.lastMessage
        //añade más lineas para mostrar más datos, como podria ser una foto.
    }

    // 4. getItemCount: Devuelve la cantidad de elementos (tamaño de la lista)
    override fun getItemCount(): Int {
        return chats.size
    }

    // 5. Añadir items a la lista:
    fun addItems(newItems: MutableList<Chat>){
        chats.addAll(newItems)
        notifyItemRangeInserted(chats.size-1, newItems.size)
    }
}