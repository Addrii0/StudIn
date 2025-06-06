package com.example.studin // o com.example.studin.models

import android.os.Parcelable // Importar Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize // Importar la anotación Parcelize

@IgnoreExtraProperties
@Parcelize // <--- AÑADIR ESTA ANOTACIÓN
data class Message(
    var messageId: String? = null,
    var senderId: String? = null,
    var text: String? = null,
    var timestamp: Long = 0L,
    // var imageUrl: String? = null,
    // var status: String? = null
) : Parcelable { // <--- IMPLEMENTAR LA INTERFAZ Parcelable
    // Constructor vacío requerido por Firebase
    constructor() : this(null, null, null, 0L)

    // El plugin kotlin-parcelize genera automáticamente el código de writeToParcel y el CREATOR.
    // No necesitas escribirlos manualmente.
}