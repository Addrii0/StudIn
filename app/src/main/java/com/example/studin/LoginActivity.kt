package com.example.studin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var usuario: EditText
    private lateinit var contrasena: EditText
    private lateinit var botonLogin: Button
    private lateinit var clickableText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_login)

        usuario = findViewById(R.id.usuario)
        contrasena = findViewById(R.id.contrasena)
        botonLogin = findViewById(R.id.InicioBoton)
        clickableText = findViewById(R.id.RegistroTexto)

        botonLogin.setOnClickListener {
            if (validarUsuario() && validarContrasena()) {
                comprobarUsuario()
            }
        }

        clickableText.setOnClickListener {
            Toast.makeText(this, "cargando", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validarUsuario(): Boolean {
        val usuarioTexto = usuario.text.toString()
        return if (usuarioTexto.isEmpty()) {
            usuario.error = "Usuario no puede estar vacío"
            false
        } else {
            usuario.error = null
            true
        }
    }

    private fun validarContrasena(): Boolean {
        val contrasenaTexto = contrasena.text.toString()
        return if (contrasenaTexto.isEmpty()) {
            contrasena.error = "Contraseña no puede estar vacía"
            false
        } else {
            contrasena.error = null
            true
        }
    }

    private fun comprobarUsuario() {
        val nombreUsuario = usuario.text.toString().trim() // NombreUsuario == usuario, no es el nombre
        val contrasenaUsuario = contrasena.text.toString().trim()

        val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference("usuarios")
        val usuariosBD: Query = reference.orderByChild("usuario").equalTo(nombreUsuario)

        usuariosBD.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usuario.error = null
                    val contrasenaBD = snapshot.child(nombreUsuario).child("contrasena").getValue(String::class.java)

                    if (contrasenaBD == contrasenaUsuario) {
                        usuario.error = null

                        val nombreBD = snapshot.child(nombreUsuario).child("nombre").getValue(String::class.java)
                        val emailBD = snapshot.child(nombreUsuario).child("email").getValue(String::class.java)
                        val usuarioBD = snapshot.child(nombreUsuario).child("usuario").getValue(String::class.java)

                        val intent = Intent(this@LoginActivity, InicioActivity::class.java)
                        intent.putExtra("usuario", usuarioBD) // Se lleva el dato a la siguente pantalla(Inicio)
                        startActivity(intent)
                    } else {
                        contrasena.error = "Contraseña incorrecta"
                        contrasena.requestFocus()
                    }
                } else {
                    usuario.error = "Usuario no existe"
                    usuario.requestFocus()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                println("Database error: ${error.message}")
                // Toast.makeText(this@LoginActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}