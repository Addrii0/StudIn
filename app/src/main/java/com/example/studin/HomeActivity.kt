package com.example.studin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var imagenChat: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ELIMINA ESTA LÍNEA: startActivity(intent)

        imagenChat = findViewById(R.id.chat)
        imagenChat.setOnClickListener {
            // Crea un NUEVO Intent para ir a MainChats
            val chatIntent = Intent(this, MainChats::class.java)
            startActivity(chatIntent)
        }
    }
}