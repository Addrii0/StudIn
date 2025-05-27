package com.example.studin.activities // Mantén tu paquete actual

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns // Importar Patterns para validación de email
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.R // Asegúrate de importar tu archivo R
import com.example.studin.classes.CompanyProfile // Importa la nueva clase CompanyProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException // Importar esta excepción específica
import com.google.firebase.auth.ktx.auth // Importar la extensión ktx para 'Firebase.auth'
import com.google.firebase.database.DatabaseReference // Importar DatabaseReference
import com.google.firebase.database.FirebaseDatabase // Importar FirebaseDatabase
import com.google.firebase.ktx.Firebase // Importar Firebase principal


class CompanyRegisterActivity : AppCompatActivity() {

    private lateinit var nombreEmpresaRegistro: EditText
    private lateinit var localizacionRegistro: EditText
    private lateinit var emailRegistro: EditText
    private lateinit var contrasenaRegistro: EditText
    private lateinit var botonRegistro: Button
    private lateinit var textoLoginRedirigido: TextView
    private lateinit var repetirContrasena: EditText

    private lateinit var auth: FirebaseAuth // Instancia de Firebase Auth
    private lateinit var database: FirebaseDatabase // Instancia de Realtime Database
    private lateinit var companiesReference: DatabaseReference // Referencia al nodo "companies"

    private val TAG = "CompanyRegisterActivity" // Constante para logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_register) // Usamos el nuevo layout de empresa

        // Inicializa Firebase Auth y Realtime Database
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        companiesReference = database.getReference("companies") // ¡Referencia al nodo "companies"!

        // Asocia las variables con los elementos de la UI
        nombreEmpresaRegistro = findViewById(R.id.company_registro_nombreEmpresa)
        localizacionRegistro = findViewById(R.id.company_registro_localizacion)
        emailRegistro = findViewById(R.id.company_registro_email)
        contrasenaRegistro = findViewById(R.id.company_registro_contrasena)
        repetirContrasena = findViewById(R.id.company_repetir_contrasena)
        botonRegistro = findViewById(R.id.company_registro_Boton)
        textoLoginRedirigido = findViewById(R.id.company_loginTextoRedirigido)


        // Listener para el botón de registro de empresa
        botonRegistro.setOnClickListener {
            if (validarCampos()) {
                crearEmpresaYGuardarPerfil()
            }
        }

        // Listener para el texto "Iniciar Sesión"
        textoLoginRedirigido.setOnClickListener {
            val intent = Intent(this@CompanyRegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Cierra CompanyRegisterActivity para que el usuario no pueda volver atrás
        }
    }

    private fun validarCampos(): Boolean {
        val nombreEmpresa = nombreEmpresaRegistro.text.toString().trim()
        val localizacion = localizacionRegistro.text.toString().trim()
        val email = emailRegistro.text.toString().trim()
        val contrasena = contrasenaRegistro.text.toString().trim()
        val repetir = repetirContrasena.text.toString().trim()
        var esValido = true

        if (nombreEmpresa.isEmpty()) {
            nombreEmpresaRegistro.error = "El nombre de la empresa no puede estar vacío"
            esValido = false
        } else {
            nombreEmpresaRegistro.error = null
        }

        if (localizacion.isEmpty()) {
            localizacionRegistro.error = "La ubicación no puede estar vacía"
            esValido = false
        } else {
            localizacionRegistro.error = null
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
        if(repetir.isEmpty()){
            repetirContrasena.error = "La contraseña no puede estar vacía"
            esValido = false
        }else if(repetir != contrasena){
            repetirContrasena.error = "Las contraseñas no coinciden"
            esValido = false
        }
        // Agrega validaciones para otros campos si los tienes

        return esValido
    }

    private fun crearEmpresaYGuardarPerfil() {
        if (!validarCampos()) {
            return // Si los campos no son válidos, no continuamos
        }

        val nombreEmpresa = nombreEmpresaRegistro.text.toString().trim()
        val localizacion = localizacionRegistro.text.toString().trim()
        val email = emailRegistro.text.toString().trim()
        val contrasena = contrasenaRegistro.text.toString().trim()

        Toast.makeText(baseContext, "Registrando empresa...", Toast.LENGTH_SHORT).show()

        // 1. Crear usuario en Firebase Authentication con Email y Contraseña
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmailAndPassword:success")
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid

                    if (uid != null) {
                        // 2. Si la cuenta de Auth se creó correctamente, guardar información adicional en Realtime Database
                        val companyProfile = CompanyProfile(nombreEmpresa, localizacion)

                        // Guardar en el nodo 'companies' usando el UID como clave
                        companiesReference.child(uid).setValue(companyProfile)
                            .addOnSuccessListener {
                                // 3. Información de empresa guardada exitosamente en RTDB, navegar a la pantalla de empresa
                                Log.d(TAG, "Perfil de empresa guardado en RTDB bajo UID: $uid")
                                Toast.makeText(baseContext, "¡Empresa registrada con éxito!", Toast.LENGTH_SHORT).show()

                                // Navegar a la actividad principal de la empresa (CompanyHomeActivity)
                                val intent = Intent(this@CompanyRegisterActivity, CompanyHomeActivity::class.java)
                                intent.putExtra("uid", uid) // Pasar el UID puede ser útil
                                startActivity(intent)
                                finish() // Cierra CompanyRegisterActivity
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar en la base de datos.
                                // Esto es un escenario delicado: la cuenta de Auth se creó, pero el perfil en RTDB no.
                                // Idealmente, deberías eliminar la cuenta de Auth aquí para mantener la consistencia,
                                // ya que tienes una cuenta de Auth sin datos de perfil completos.
                                // La eliminación de la cuenta de Auth es asíncrona y también puede fallar.
                                Log.w(TAG, "Error al guardar perfil de empresa en RTDB", e)

                                auth.currentUser?.delete()?.addOnCompleteListener { authDeleteTask ->
                                    if (authDeleteTask.isSuccessful) {
                                        Log.d(TAG, "Cuenta de Auth eliminada después de fallo en DB.")
                                    } else {
                                        Log.w(TAG, "Fallo al eliminar cuenta de Auth después de fallo en DB.", authDeleteTask.exception)
                                    }
                                }

                                // Informa al usuario que hubo un problema.
                                Toast.makeText(baseContext, "Registro fallido. Error al guardar datos de empresa.", Toast.LENGTH_LONG).show()
                                // Nos quedamos en la pantalla de registro para que pueda intentarlo de nuevo o contactar soporte.
                            }
                    } else {
                        // Esto no debería ocurrir si task.isSuccessful es true, pero por seguridad
                        Log.w(TAG, "createUserWithEmailAndPassword:success but UID is null")
                        Toast.makeText(baseContext, "Error inesperado al crear usuario de empresa.", Toast.LENGTH_LONG).show()
                        // No navegamos, nos quedamos en la pantalla de registro.
                    }
                } else {
                    // Si falla la creación del usuario en Auth
                    Log.w(TAG, "createUserWithEmailAndPassword:failure", task.exception)

                    val mensajeError = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Ya existe una empresa registrada con este correo electrónico."
                        // Otros posibles errores de Firebase Auth:
                        // com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
                        // com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es incorrecto."
                        else -> "Error de registro de empresa: ${task.exception?.message}"
                    }
                    Toast.makeText(baseContext, mensajeError, Toast.LENGTH_LONG).show()
                    // Nos quedamos en la pantalla de registro.
                }
            }
    }
}
