package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Chat (
    val chatRoomId: String = "",
    var otherUserId: String = "",
    var otherUserName: String = "",
    var otherUserAvatarUrl: String? = null,
    var lastMessage: String = "",
    var lastMessageTimestamp: Long = 0,
    var lastMessageSenderId: String = "",
    var countUnreadMessages: Int = 0
    ): Parcelable