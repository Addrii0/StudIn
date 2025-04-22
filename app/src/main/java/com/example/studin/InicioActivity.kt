package com.example.studin
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.LoginActivity

class InicioActivity: AppCompatActivity() {

    private lateinit var imagenChat: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_principal)

        startActivity(intent)


        imagenChat = findViewById(R.id.chat)
        imagenChat.setOnClickListener {
            val intent = Intent(this, MainChats::class.java)
            startActivity(intent)
        }

    }

}