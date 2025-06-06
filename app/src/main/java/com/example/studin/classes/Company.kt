package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Company(
    var uid: String? = null,
    val name: String? = null,
    val location: String? = null,
    val profileImageUrl: String? = null,
    val description: String? = null,
    val industry: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null

) : Parcelable
