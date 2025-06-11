package com.example.studin

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Message(
    var messageId: String? = null,
    var senderId: String? = null,
    var text: String? = null,
    var timestamp: Long = 0L,

) : Parcelable {
    constructor() : this(null, null, null, 0L)
}