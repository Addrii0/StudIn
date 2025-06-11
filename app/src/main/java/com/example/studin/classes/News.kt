package com.example.studin.classes

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class News (
    var uid: String? = null,
    var title: String? = null,
    var content: String? = null,
    var imageUrl: String? = null,
    var authorName: String? = null,
    var authorId: String? = null,
    var timestamp: Long? = System.currentTimeMillis(),
    var category: String? = null,
    var companyId: String? = null,
    var companyName: String? = null,
    var companyLogoUrl: String? = null ,
    var tags: List<String>? = null
    ) : Parcelable
