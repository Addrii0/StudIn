package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Chat (
    val chatRoomId: String = "", // ID de la sala de chat
    var otherUserId: String = "", // ID del otro usuario en el chat
    var otherUserName: String = "", // Nombre del otro usuario
    var otherUserAvatarUrl: String? = null, // URL del avatar del otro usuario
    var lastMessage: String = "",
    var lastMessageTimestamp: Long = 0,
    var lastMessageSenderId: String = "",
    var countUnreadMessages: Int = 0
    ): Parcelable