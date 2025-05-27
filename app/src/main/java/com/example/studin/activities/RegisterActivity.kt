package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.example.studin.classes.UserProfile

class RegisterActivity : AppCompatActivity() {

    private lateinit var nombreRegistro: EditText
    private lateinit var emailRegistro: EditText
    private lateinit var usuarioRegistro: EditText // Este será el nombre de usuario, distinto del email
    private lateinit var contrasenaRegistro: EditText
    private lateinit var textoRegistro: TextView
    private lateinit var botonRegistro: Button

    private lateinit var auth: FirebaseAuth // Instancia de Firebase Auth
    private lateinit var database: FirebaseDatabase // Instancia de Realtime Database
    private lateinit var databaseReference: DatabaseReference // Referencia a la ubicación en la base de datos

    private val TAG = "RegisterActivity" // Constante para logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializa Firebase Auth y Realtime Database
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance() // Obtiene la instancia por defecto
        databaseReference = database.getReference("users") // Obtiene una referencia al nodo "usuarios"

        // Asocia las variables con los elementos de la UI
        nombreRegistro = findViewById(R.id.registro_nombre)
        emailRegistro = findViewById(R.id.registro_email)
        usuarioRegistro = findViewById(R.id.registro_usuario)
        contrasenaRegistro = findViewById(R.id.registro_constrasena)
        textoRegistro = findViewById(R.id.loginTextoRedirigido)
        botonRegistro = findViewById(R.id.registro_Boton)

        // Listener para el botón de registro
        botonRegistro.setOnClickListener {
            crearUsuarioYGuardarPerfil()
        }

        // Listener para el texto "Iniciar Sesión"
        textoRegistro.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Cierra RegisterActivity para que el usuario no pueda volver atrás
        }
    }

    private fun validarCampos(): Boolean {
        val nombre = nombreRegistro.text.toString().trim()
        val email = emailRegistro.text.toString().trim()
        val usuario = usuarioRegistro.text.toString().trim()
        val contrasena = contrasenaRegistro.text.toString().trim()

        var esValido = true

        if (nombre.isEmpty()) {
            nombreRegistro.error = "El nombre no puede estar vacío"
            esValido = false
        } else {
            nombreRegistro.error = null
        }

        if (email.isEmpty()) {
            emailRegistro.error = "El correo electrónico no puede estar vacío"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailRegistro.error = "Introduce un correo electrónico válido"
            esValido = false
        } else {
            emailRegistro.error = null
        }

        if (usuario.isEmpty()) {
            usuarioRegistro.error = "El nombre de usuario no puede estar vacío"
            esValido = false
        } else {
            usuarioRegistro.error = null
        }

        if (contrasena.isEmpty()) {
            contrasenaRegistro.error = "La contraseña no puede estar vacía"
            esValido = false
        } else if (contrasena.length < 6) { // Firebase Auth requiere un mínimo de 6 caracteres por defecto
            contrasenaRegistro.error = "La contraseña debe tener al menos 6 caracteres"
            esValido = false
        }
        else {
            contrasenaRegistro.error = null
        }

        return esValido
    }

    private fun crearUsuarioYGuardarPerfil() {
        if (!validarCampos()) {
            return // Si los campos no son válidos, no continuamos
        }

        val nombre = nombreRegistro.text.toString().trim()
        val email = emailRegistro.text.toString().trim()
        val usuario = usuarioRegistro.text.toString().trim() // Nombre de usuario para el perfil
        val contrasena = contrasenaRegistro.text.toString().trim()

        Toast.makeText(baseContext, "Registrando...", Toast.LENGTH_SHORT).show()

        // 1. Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmailAndPassword:success")
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid

                    if (uid != null) {
                        // 2. Si la cuenta de Auth se creó correctamente, guardar información adicional en Realtime Database
                        val userProfile = UserProfile(nombre, usuario)

                        databaseReference.child(uid).setValue(userProfile)
                            .addOnSuccessListener {
                                // 3. Información guardada exitosamente, navegar a la siguiente pantalla
                                Log.d(TAG, "Perfil de usuario guardado en RTDB bajo UID: $uid")
                                Toast.makeText(baseContext, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()

                                // Navegar a la actividad principal (por ejemplo, HomeActivity)
                                val intent = Intent(this@RegisterActivity, UserHomeActivity::class.java)
                                // Puedes pasar el UID si lo necesitas, aunque el SDK ya lo maneja
                                intent.putExtra("uid", uid)
                                startActivity(intent)
                                finish() // Cierra RegisterActivity
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar en la base de datos.
                                // Considera eliminar el usuario de Auth si falló el guardado en DB,
                                // para evitar cuentas de Auth sin datos de perfil asociados.
                                // Esto puede ser complejo, para este ejemplo simple lo dejamos así.
                                Log.w(TAG, "Error al guardar perfil en RTDB", e)
                                Toast.makeText(baseContext, "Registro exitoso en Auth, pero falló al guardar perfil.", Toast.LENGTH_LONG).show()
                                // Decide si navegas o no. Si no guardaste el perfil, quizás no deberías ir a HomeActivity.
                                // Para mantener la consistencia con el flujo de login, vamos a HomeActivity,
                                // pero la app debería manejar el caso de perfil incompleto.
                                val intent = Intent(this@RegisterActivity, UserHomeActivity::class.java)
                                intent.putExtra("uid", uid)
                                startActivity(intent)
                                finish() // Cierra RegisterActivity
                            }
                    } else {
                        // Esto no debería ocurrir si task.isSuccessful es true, pero por seguridad
                        Log.w(TAG, "createUserWithEmailAndPassword:success but UID is null")
                        Toast.makeText(baseContext, "Error inesperado al crear usuario.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Si falla la creación del usuario en Auth
                    Log.w(TAG, "createUserWithEmailAndPassword:failure", task.exception)

                    val mensajeError = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Ya existe un usuario con este correo electrónico."
                        // Otros posibles errores de Firebase Auth:
                        // FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
                        // FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es incorrecto."
                        else -> "Error de registro: ${task.exception?.message}"
                    }
                    Toast.makeText(baseContext, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }
}