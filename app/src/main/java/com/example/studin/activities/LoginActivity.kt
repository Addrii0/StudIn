package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth // o FirebaseAuth.getInstance()

        binding.InicioBoton.setOnClickListener {
            if (validarEmail() && validarContrasena()) {
                iniciarSesionConFirebase()
            }
        }
        binding.RegistroTexto.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.RegistroEmpresaTexto.setOnClickListener {
            val intent = Intent(this, CompanyRegisterActivity::class.java)
            startActivity(intent)
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya logueado: ${currentUser.uid}. Verificando tipo de usuario.")
            checkUserTypeAndNavigate(currentUser.uid)
        } else {
            Log.d(TAG, "Ningún usuario logueado. Mostrando pantalla de login.")
        }
    }

    private fun validarEmail(): Boolean {
        val emailTexto = binding.usuario.text.toString().trim()
        return if (emailTexto.isEmpty()) {
            binding.usuario.error = "El correo electrónico no puede estar vacío"
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailTexto).matches()) {
            binding.usuario.error = "Introduce un correo electrónico válido"
            false
        } else {
            binding.usuario.error = null
            true
        }
    }

    private fun validarContrasena(): Boolean {
        val contrasenaTexto = binding.contrasena.text.toString().trim()
        return if (contrasenaTexto.isEmpty()) {
            binding.contrasena.error = "La contraseña no puede estar vacía"
            false
        } else {
            binding.contrasena.error = null
            true
        }
    }

    private fun iniciarSesionConFirebase() {
        val email = binding.usuario.text.toString().trim()
        val password = binding.contrasena.text.toString().trim()

        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmailAndPassword:success")
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        Toast.makeText(this, "Autenticación exitosa. Verificando perfil...", Toast.LENGTH_SHORT).show()
                        checkUserTypeAndNavigate(uid)
                    } else {
                        Log.w(TAG, "signInWithEmailAndPassword:success but UID is null")
                        Toast.makeText(this, "Error inesperado al iniciar sesión.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "signInWithEmailAndPassword:failure", task.exception)
                    val mensajeError = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "El usuario no existe o ha sido deshabilitado."
                        is FirebaseAuthInvalidCredentialsException -> "La contraseña es incorrecta."
                        else -> "Error de inicio de sesión: ${task.exception?.message ?: "Error desconocido"}"
                    }
                    Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserTypeAndNavigate(uid: String) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        val companiesRef = database.getReference("companies")

        companiesRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Usuario $uid es una empresa.")
                    Toast.makeText(this@LoginActivity, "Bienvenido Empresa.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, CompanyHomeActivity::class.java)
                    intent.putExtra("uid", uid)
                    startActivity(intent)
                    finish()
                } else {
                    // No encontrado en users, busca en companies
                    usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(companySnapshot: DataSnapshot) { // Renombrado snapshot para claridad
                            if (companySnapshot.exists()) {
                                Log.d(TAG, "Usuario $uid es un usuario normal.")
                                Toast.makeText(this@LoginActivity, "Bienvenida Usuario.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, UserHomeActivity::class.java)
                                intent.putExtra("uid", uid)
                                startActivity(intent)
                                finish()
                            } else {
                                // El UID existe en Auth pero no en nuestros nodos de perfil
                                Log.w(TAG, "UID $uid autenticado pero no encontrado en nodos 'users' o 'companies'.")
                                Toast.makeText(this@LoginActivity, "Error: Perfil de usuario no encontrado. Contacta soporte.", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w(TAG, "checkUserTypeAndNavigate:companies:onCancelled", error.toException())
                            Toast.makeText(this@LoginActivity, "Error al verificar tipo de empresa: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "checkUserTypeAndNavigate:users:onCancelled", error.toException())
                Toast.makeText(this@LoginActivity, "Error al verificar tipo de usuario: ${error.message}", Toast.LENGTH_LONG).show()// Decide qué hacer si hay un error en la DB. Quizás mostrar un error y no navegar.
            }
        })
    }
}
