package com.example.studin.classes
import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Importa la anotación @Parcelize
@Parcelize
data class Offer(
    var id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val datePosted: Long? = null,
    val companyId: String? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
    var requirements: List<String> = emptyList(),
    val type: String? = null,
    val companyName: String? = null,
    val active: Boolean = true,
) : Parcelable