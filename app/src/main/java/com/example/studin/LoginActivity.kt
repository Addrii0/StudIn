package com.example.studin

import android.content.Intent
import android.os.Bundle
import android.util.Log // Importar Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth // Importar FirebaseAuth
import com.google.firebase.auth.ktx.auth // Importar la extensión ktx para 'Firebase.auth'
import com.google.firebase.ktx.Firebase // Importar Firebase principal

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var contrasenaEditText: EditText
    private lateinit var botonLogin: Button
    private lateinit var clickableTextRegistro: TextView

    private lateinit var auth: FirebaseAuth

    private val TAG = "LoginActivity" // Constante para logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa Firebase Auth
        auth = Firebase.auth

        // Asocia las variables con los elementos de la UI
        emailEditText = findViewById(R.id.usuario) // Asumiendo que el ID en pantalla_login.xml para el email es 'usuario'
        contrasenaEditText = findViewById(R.id.contrasena) // Asumiendo que el ID en pantalla_login.xml para la contraseña es 'contrasena'
        botonLogin = findViewById(R.id.InicioBoton) // Asumiendo que el ID en pantalla_login.xml para el botón es 'InicioBoton'
        clickableTextRegistro = findViewById(R.id.RegistroTexto) // Asumiendo que el ID en pantalla_login.xml para el texto de registro es 'RegistroTexto'

        // Listener para el botón de login
        botonLogin.setOnClickListener {
            if (validarEmail() && validarContrasena()) {
                iniciarSesionConFirebase()
            }
        }

        // Listener para el texto "Registrarse"
        clickableTextRegistro.setOnClickListener {
            Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show()
            // Asegúrate de que tienes una actividad llamada RegistroActivity y su layout correspondiente
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Opcional: Verificar si el usuario ya está logueado al iniciar la actividad.
    // Descomenta este bloque si quieres que la app salte el login si ya hay una sesión activa.
    /*
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya logueado, navegar directamente a InicioActivity
            Log.d(TAG, "Usuario ya logueado: ${currentUser.uid}. Navegando a Inicio.")
            val intent = Intent(this, InicioActivity::class.java)
            // Puedes pasar el UID si lo necesitas, aunque el SDK ya lo maneja
            intent.putExtra("uid", currentUser.uid)
            startActivity(intent)
            finish() // Cierra LoginActivity para que el usuario no pueda volver atrás
        } else {
            Log.d(TAG, "Ningún usuario logueado.")
        }
    }
    */

    private fun validarEmail(): Boolean {
        val emailTexto = emailEditText.text.toString().trim()
        return if (emailTexto.isEmpty()) {
            emailEditText.error = "El correo electrónico no puede estar vacío"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTexto).matches()) { // Validación básica de formato de email
            emailEditText.error = "Introduce un correo electrónico válido"
            false
        } else {
            emailEditText.error = null
            true
        }
    }

    private fun validarContrasena(): Boolean {
        val contrasenaTexto = contrasenaEditText.text.toString().trim()
        return if (contrasenaTexto.isEmpty()) {
            contrasenaEditText.error = "La contraseña no puede estar vacía"
            false
        } else {
            contrasenaEditText.error = null
            true
        }
    }

    private fun iniciarSesionConFirebase() {
        val email = emailEditText.text.toString().trim()
        val password = contrasenaEditText.text.toString().trim()

        Toast.makeText(baseContext, "Iniciando sesión...", Toast.LENGTH_SHORT).show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmailAndPassword:success")
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Inicio de sesión exitoso.",
                        Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    intent.putExtra("uid", user?.uid) // Opcional, pero puede ser útil
                    startActivity(intent)
                    finish() // Cierra LoginActivity para que el usuario no pueda volver atrás
                } else {
                    Log.w(TAG, "signInWithEmailAndPassword:failure", task.exception)
                    // Proporciona un feedback más específico del error si es posible
                    val mensajeError = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "El usuario no existe o ha sido deshabilitado."
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "La contraseña es incorrecta."
                        else -> "Error de inicio de sesión: ${task.exception?.message}"
                    }
                    Toast.makeText(baseContext, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }
}