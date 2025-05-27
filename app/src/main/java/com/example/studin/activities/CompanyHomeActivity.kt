package com.example.studin.activities // O el paquete de tu actividad

import android.content.Intent
import android.os.Bundle
import android.widget.TextView // Importa TextView si no usas ViewBinding directamente para el tipo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.R // Asegúrate de importar tu clase R
import com.example.studin.databinding.ActivityCompanyHomeBinding // Asume que este es tu binding
// O el binding de la actividad donde esté tu TextView de logout
import com.google.firebase.auth.FirebaseAuth

class CompanyHomeActivity : AppCompatActivity() { // O tu Activity correspondiente

    private lateinit var binding: ActivityCompanyHomeBinding // Reemplaza con tu clase de ViewBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configurar el listener para el TextView de cerrar sesión
        // Si usas ViewBinding, el ID del TextView se convierte en una propiedad:
        binding.textViewLogout.setOnClickListener {
            logoutUser()
        }
        binding.buttonExample.setOnClickListener {
            val intent = Intent(this, CompanyOffersActivity::class.java)
            startActivity(intent)
        }

        binding.chat.setOnClickListener {
            val intent = Intent(this, MainChatsActivity::class.java)
            startActivity(intent)
        }
        // ... resto de tu código de onCreate ...
    }

    private fun logoutUser() {
        auth.signOut() // Cierra la sesión del usuario actual de Firebase

        // Muestra un mensaje al usuario (opcional)
        Toast.makeText(this, "Has cerrado sesión.", Toast.LENGTH_SHORT).show()

        // Redirige al usuario a la pantalla de Login
        // Es importante limpiar el stack de actividades para que el usuario
        // no pueda volver a esta actividad presionando el botón "Atrás".
        val intent = Intent(this, LoginActivity::class.java) // Reemplaza LoginActivity con tu actividad de inicio de sesión
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra la actividad actual
    }

    // ... resto de tus métodos de la Activity ...
}