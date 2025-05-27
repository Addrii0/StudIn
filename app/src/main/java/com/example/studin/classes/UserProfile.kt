package com.example.studin.classes

// Clase de datos para guardar la información del perfil
data class UserProfile(
    val nombre: String? = null,
    val usuario: String? = null
    // Puedes añadir más campos aquí según necesites, por ejemplo:
    // val email: String? = null,
    // val fotoUrl: String? = null,
    // val uid: String? = null // Si quieres almacenar el ID de Firebase Auth
)