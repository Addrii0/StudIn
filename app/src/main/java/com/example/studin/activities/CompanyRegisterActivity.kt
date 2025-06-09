package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.classes.Company
import com.example.studin.databinding.ActivityCompanyRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase


class CompanyRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyRegisterBinding


    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var companiesReference: DatabaseReference

    private val TAG = "CompanyRegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        companiesReference = database.getReference("companies")


        binding.companyRegistroBoton.setOnClickListener {
            if (validarCampos()) {
                crearEmpresaYGuardarPerfil()
            }
        }

          binding.companyLoginTextoRedirigido.setOnClickListener {
              val intent = Intent(this@CompanyRegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validarCampos(): Boolean {
        val nombreEmpresa = binding.companyRegistroNombreEmpresa.text.toString().trim()
        val localizacion = binding.companyRegistroLocalizacion.text.toString().trim()
        val email = binding.companyRegistroEmail.text.toString().trim()
        val contrasena = binding.companyRegistroContrasena.text.toString().trim()
        val repetir = binding.companyRepetirContrasena.text.toString().trim()
        var esValido = true

        if (nombreEmpresa.isEmpty()) {
            binding.companyRegistroNombreEmpresa.error = "El nombre de la empresa no puede estar vacío"
            esValido = false
        } else {
            binding.companyRegistroNombreEmpresa.error = null
        }

        if (localizacion.isEmpty()) {
            binding.companyRegistroLocalizacion.error = "La ubicación no puede estar vacía"
            esValido = false
        } else {
            binding.companyRegistroLocalizacion.error = null
        }

        if (email.isEmpty()) {
            binding.companyRegistroEmail.error = "El correo electrónico no puede estar vacío"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.companyRegistroEmail.error = "Introduce un correo electrónico válido"
            esValido = false
        } else {
            binding.companyRegistroEmail.error = null
        }

        if (contrasena.isEmpty()) {
            binding.companyRegistroContrasena.error = "La contraseña no puede estar vacía"
            esValido = false
        } else if (contrasena.length < 6) { // Firebase Auth requiere un mínimo de 6 caracteres por defecto
            binding.companyRegistroContrasena.error = "La contraseña debe tener al menos 6 caracteres"
            esValido = false
        } else {
            binding.companyRegistroContrasena.error = null
        }

        if (repetir.isEmpty()) {
            binding.companyRepetirContrasena.error = "La contraseña no puede estar vacía"
            esValido = false
        } else if (repetir != contrasena) {
            binding.companyRepetirContrasena.error = "Las contraseñas no coinciden"
            esValido = false
        } else {
            binding.companyRepetirContrasena.error = null
        }

        return esValido
    }

    private fun crearEmpresaYGuardarPerfil() {
        if (!validarCampos()) {
            return
        }

        val nombreEmpresa = binding.companyRegistroNombreEmpresa.text.toString().trim()
        val localizacion = binding.companyRegistroLocalizacion.text.toString().trim()
        val email = binding.companyRegistroEmail.text.toString().trim()
        val contrasena = binding.companyRegistroContrasena.text.toString().trim()

        Toast.makeText(baseContext, "Registrando empresa...", Toast.LENGTH_SHORT).show()

        //  Crear usuario en Firebase Authentication con Email y Contraseña
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmailAndPassword:success")
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid

                    if (uid != null) {
                        // Si la cuenta de Auth se creó correctamente, guarda información adicional en Realtime Database
                        val company = Company(nombreEmpresa, localizacion)

                        // Guardar en el nodo 'companies' usando el UID como clave
                        companiesReference.child(uid).setValue(company)
                            .addOnSuccessListener {
                                //  Información de empresa guardada exitosamente en bbdd
                                Log.d(TAG, "Perfil de empresa guardado en RTDB bajo UID: $uid")
                                Toast.makeText(baseContext, "¡Empresa registrada con éxito!", Toast.LENGTH_SHORT).show()

                                // Navegar a la actividad principal de la empresa (CompanyHomeActivity)
                                val intent = Intent(this@CompanyRegisterActivity, CompanyHomeActivity::class.java)
                                intent.putExtra("COMPANY_UID", uid)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar en la base de datos.
                                Log.w(TAG, "Error al guardar perfil de empresa en RTDB", e)

                                // Intenta eliminar la cuenta de Auth para mantener la consistencia
                                firebaseUser?.delete()?.addOnCompleteListener { authDeleteTask ->
                                    if (authDeleteTask.isSuccessful) {
                                        Log.d(TAG, "Cuenta de Auth eliminada después de fallo en DB.")
                                    } else {
                                        Log.w(TAG, "Fallo al eliminar cuenta de Auth después de fallo en DB.", authDeleteTask.exception)
                                    }
                                }
                                Toast.makeText(baseContext, "Registro fallido. Error al guardar datos de empresa.", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Log.w(TAG, "createUserWithEmailAndPassword:success but UID is null")
                        Toast.makeText(baseContext, "Error inesperado al crear usuario de empresa.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Si falla la creación del usuario en Auth
                    Log.w(TAG, "createUserWithEmailAndPassword:failure", task.exception)

                    val mensajeError = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Ya existe una empresa registrada con este correo electrónico."
                        else -> "Error de registro de empresa: ${task.exception?.message}"
                    }
                    Toast.makeText(baseContext, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }
}