// classes/CompanyProfile.kt
package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Asegúrate de tener el plugin kotlin-parcelize

@Parcelize
data class Company(
    val name: String? = null,
    val location: String? = null,
    val profileImageUrl: String? = null

    // Agrega aquí cualquier otro campo de perfil de empresa que necesites
    // Por ejemplo: val sitioWeb: String? = null, val sector: String? = null
) : Parcelable // Implementa Parcelable