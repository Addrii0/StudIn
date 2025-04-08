package com.example.studin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegistroActivity : AppCompatActivity() {

    private lateinit var nombreRegistro: EditText
    private lateinit var usuarioRegistro: EditText
    private lateinit var emailRegistro: EditText
    private lateinit var contrasenaRegistro: EditText
    private lateinit var textoRegistro: TextView
    private lateinit var botonRegistro: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        nombreRegistro = findViewById(R.id.registro_nombre)
        emailRegistro = findViewById(R.id.registro_email)
        usuarioRegistro = findViewById(R.id.registro_usuario)
        contrasenaRegistro = findViewById(R.id.registro_constrasena)
        textoRegistro = findViewById(R.id.loginTextoRedirigido)
        botonRegistro = findViewById(R.id.registro_Boton)

        botonRegistro.setOnClickListener {
            database = FirebaseDatabase.getInstance()
            reference = database.getReference("usuarios")

            val nombre = nombreRegistro.text.toString()
            val email = emailRegistro.text.toString()
            val usuario = usuarioRegistro.text.toString()
            val contrasena = contrasenaRegistro.text.toString()

            val helperClass = HelperClass(nombre, email, usuario, contrasena)
            reference.child(usuario).setValue(helperClass)

            Toast.makeText(this, "¡Te has registrado con éxito!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("usuario", usuario)
            startActivity(intent)
        }
        textoRegistro.setOnClickListener {
            val intent = Intent(this@RegistroActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}