package com.example.studin.classes
import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Importa la anotación @Parcelize
@Parcelize
data class Offer(
    val id: String? = null, // Un ID único para la oferta (útil si necesitas referenciarla)
    val title: String? = null,
    val description: String? = null,
    val fechaPublicacion: Long? = null, // Podrías usar un timestamp
    val companyId: String? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
) : Parcelable