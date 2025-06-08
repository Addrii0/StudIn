package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class User(
    val name: String? = null,
    val surName: String? = null,
    val description: String? = null,
    val skills: List<String> = emptyList(),
    val email: String? = null,
    val phone: String? = null,
    val experience: String? = null,
    var profileImageUrl: String? = null,
): Parcelable