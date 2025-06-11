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
    // Empezamos con una lista vacía, se rellenará desde el Fragment
    private var chats: MutableList<Chat> = mutableListOf(),
    private val onItemClicked: (Chat) -> Unit // Lambda para manejar clics
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val avatarImageView: ImageView = itemView.findViewById(R.id.chat_avatar)
        val nameTextView: TextView = itemView.findViewById(R.id.chat_name)
        val messageTextView: TextView = itemView.findViewById(R.id.chat_last_message)

        fun bind(chat: Chat, onItemClicked: (Chat) -> Unit) {
            nameTextView.text = chat.otherUserName
            messageTextView.text = chat.lastMessage

            // Cargar imagen del avatar usando Glide
            Glide.with(itemView.context)
                .load(chat.otherUserAvatarUrl)
                .placeholder(R.drawable.icono_persona)
                .error(R.drawable.icono_persona)
                .circleCrop()
                .into(avatarImageView)

            itemView.setOnClickListener {
                onItemClicked(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentChat = chats[position]
        holder.bind(currentChat, onItemClicked)
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    fun submitList(newChats: List<Chat>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }

    fun addChat(chat: Chat) {
        chats.add(chat)
        notifyItemInserted(chats.size - 1)
    }


    fun updateChat(updatedChat: Chat) {
        val index = chats.indexOfFirst { it.chatRoomId == updatedChat.chatRoomId }
        if (index != -1) {
            chats[index] = updatedChat
            notifyItemChanged(index)
        }
    }

    fun clearChats() {
        chats.clear()
        notifyDataSetChanged()
    }
}