package com.example.studin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    var usuario: EditText? = null
    var contrasena: EditText? = null
    var botonLogin: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_login)

        usuario = findViewById(R.id.usuario)
        contrasena = findViewById(R.id.contrasena)
        botonLogin = findViewById(R.id.InicioBoton)

        botonLogin?.let { button -> // Ejecuta el bloque solo si botonLogin no es nulo
            button.setOnClickListener(View.OnClickListener {
                usuario?.let { user ->  // Ejecuta el bloque interno solo si usuario no es nulo
                    contrasena?.let { pass -> // Ejecuta el bloque interno solo si contrasena no es nulo
                        val userText = user.text.toString()
                        val passText = pass.text.toString()
                        if (userText == "admin" && passText == "admin") {
                            Toast.makeText(
                                this@LoginActivity,
                                "Inicio correcto",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Inicio incorrecto",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }
}