// classes/CompanyProfile.kt
package com.example.studin.classes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Asegúrate de tener el plugin kotlin-parcelize

@Parcelize
data class CompanyProfile(
    val nombreEmpresa: String? = null,
    val localizacion: String? = null,
    // Agrega aquí cualquier otro campo de perfil de empresa que necesites
    // Por ejemplo: val sitioWeb: String? = null, val sector: String? = null
) : Parcelable // Implementa Parcelable