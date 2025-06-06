package com.example.studin.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.databinding.ActivityUserRegisterBinding // Asegúrate que el nombre coincida con tu layout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.example.studin.classes.User // Asegúrate que tu clase User tenga el campo profileImageUrl
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class RegisterActivity : AppCompatActivity() {

        private lateinit var binding: ActivityUserRegisterBinding
        private var selectedImageUri: Uri? = null

        private lateinit var auth: FirebaseAuth
        private lateinit var database: FirebaseDatabase
        private lateinit var userDatabaseReference: DatabaseReference
        private lateinit var storage: FirebaseStorage

        private lateinit var pickImageLauncher: ActivityResultLauncher<String>

        private val TAG = "RegisterActivity"

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                binding = ActivityUserRegisterBinding.inflate(layoutInflater)
                setContentView(binding.root)
                Log.d(TAG, "onCreate: Activity Creada")

                auth = Firebase.auth
                database = FirebaseDatabase.getInstance()
                userDatabaseReference = database.getReference("users")
                storage = Firebase.storage
                Log.d(TAG, "onCreate: Firebase inicializado")

                pickImageLauncher =
                    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        uri?.let {
                            selectedImageUri = it
                            binding.registroImagenPerfil.setImageURI(it)
                            Log.d(TAG, "Imagen seleccionada: $it")
                        }
                    }

                binding.botonSeleccionarFoto.setOnClickListener {
                    Log.d(TAG, "Botón seleccionar foto clickeado")
                    pickImageLauncher.launch("image/*")
                }

                binding.registroBoton.setOnClickListener {
                    Log.d(TAG, "Botón de registro clickeado")
                    crearUsuarioYGuardarPerfil()
                }

                binding.loginTextoRedirigido.setOnClickListener {
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            private fun validarCampos(): Boolean {
                Log.d(TAG, "validarCampos: Iniciando validación")
                val nombre = binding.registroNombre.text.toString().trim()
                val email = binding.registroEmail.text.toString().trim()
                val apellido = binding.registroApellido.text.toString().trim()
                val contrasena = binding.registroConstrasena.text.toString().trim()
                val telefono = binding.registroTelefono.text.toString().trim()
                var esValido = true

                if (nombre.isEmpty()) {
                    binding.registroNombre.error = "El nombre no puede estar vacío" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Nombre vacío")
                } else {
                    binding.registroNombre.error = null
                }

                if (apellido.isEmpty()) {
                    binding.registroApellido.error = "El apellido no puede estar vacío" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Apellido vacío")
                } else {
                    binding.registroApellido.error = null
                }

                if (email.isEmpty()) {
                    binding.registroEmail.error = "El correo electrónico no puede estar vacío" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Email vacío")
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.registroEmail.error = "Introduce un correo electrónico válido" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Email inválido")
                } else {
                    binding.registroEmail.error = null
                }

                if (telefono.isEmpty()) {
                    binding.registroTelefono.error = "El teléfono no puede estar vacío" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Teléfono vacío")
                } else if (telefono.length < 9) { // Validación muy básica, ajústala si es necesario
                    binding.registroTelefono.error = "El teléfono debe tener al menos 9 dígitos" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Teléfono corto")
                } else {
                    binding.registroTelefono.error = null
                }

                if (contrasena.isEmpty()) {
                    binding.registroConstrasena.error = "La contraseña no puede estar vacía" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Contraseña vacía")
                } else if (contrasena.length < 6) {
                    binding.registroConstrasena.error = "La contraseña debe tener al menos 6 caracteres" // Deberías usar R.string...
                    esValido = false
                    Log.w(TAG, "validarCampos: Contraseña corta")
                } else {
                    binding.registroConstrasena.error = null
                }

                Log.d(TAG, "validarCampos: Resultado de validación: $esValido")
                return esValido
            }

            private fun crearUsuarioYGuardarPerfil() {
                Log.d(TAG, "crearUsuarioYGuardarPerfil: Iniciando...")
                if (!validarCampos()) {
                    Log.w(TAG, "crearUsuarioYGuardarPerfil: Validación fallida, retornando.")
                    // No ocultamos ProgressBar aquí porque se muestra DESPUÉS de esta validación
                    return
                }

                Log.d(TAG, "crearUsuarioYGuardarPerfil: Validación exitosa.")
                showLoading(true) // Muestra ProgressBar AHORA

                val nombre = binding.registroNombre.text.toString().trim()
                val apellido = binding.registroApellido.text.toString().trim()
                val email = binding.registroEmail.text.toString().trim()
                val telefono = binding.registroTelefono.text.toString().trim()
                val contrasena = binding.registroConstrasena.text.toString().trim()

                Log.d(TAG, "crearUsuarioYGuardarPerfil: Intentando crear usuario en Firebase Auth...")
                auth.createUserWithEmailAndPassword(email, contrasena)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "createUserWithEmailAndPassword:success")
                            val firebaseUser = auth.currentUser
                            val uid = firebaseUser?.uid

                            if (uid != null) {
                                Log.d(TAG, "UID obtenido: $uid. Procediendo a guardar/subir datos.")
                                if (selectedImageUri != null) {
                                    Log.d(TAG, "Imagen seleccionada ($selectedImageUri), iniciando subida...")
                                    uploadProfileImageAndThenSaveUserDetails(uid, nombre, apellido, email, telefono, selectedImageUri!!)
                                } else {
                                    Log.d(TAG, "No hay imagen seleccionada, guardando detalles sin imagen...")
                                    saveUserDetailsToDatabase(uid, nombre, apellido, email, telefono, null)
                                }
                            } else {
                                showLoading(false)
                                Log.w(TAG, "createUserWithEmailAndPassword:success but UID is null")
                                Toast.makeText(baseContext, "Error inesperado al obtener ID de usuario.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            showLoading(false)
                            Log.w(TAG, "createUserWithEmailAndPassword:failure", task.exception)
                            val mensajeError = when (task.exception) {
                                is FirebaseAuthUserCollisionException -> "Ya existe un usuario con este correo electrónico."
                                else -> "Error de registro: ${task.exception?.message ?: "Error desconocido"}"
                            }
                            Toast.makeText(baseContext, mensajeError, Toast.LENGTH_LONG).show()
                        }
                    }
            }

    private fun uploadProfileImageAndThenSaveUserDetails(
        uid: String,
        nombre: String,
        apellido: String,
        email: String, // Añadido email
        telefono: String, // Añadido telefono
        imageUri: Uri
    ) {
        Log.d(TAG, "uploadProfileImage: Iniciando subida para UID: $uid")
        // Nombre del archivo en Storage (puedes usar "profile.jpg" o un ID único como uid + ".jpg")
        val imageFileName = "profile_${uid}.jpg" // Nombre de archivo único
        // Ruta en Firebase Storage: profile_images/users/{UID}/profile_{UID}.jpg
        val imageRef = storage.reference.child("profile_images/users/$uid/$imageFileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { uploadTask ->
                Log.d(TAG, "uploadProfileImage: Imagen subida con éxito. Obteniendo URL de descarga...")
                uploadTask.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.d(TAG, "uploadProfileImage: URL de descarga obtenida: $imageUrl")
                    // Guardar todos los detalles del usuario, incluyendo la URL de la imagen
                    saveUserDetailsToDatabase(uid, nombre, apellido, email, telefono, imageUrl)
                }.addOnFailureListener { exception ->
                    showLoading(false)
                    Log.w(TAG, "Error al obtener URL de descarga de la imagen: ", exception)
                    Toast.makeText(baseContext, "Error al obtener URL de imagen. Perfil guardado sin imagen.", Toast.LENGTH_LONG).show()
                    // Aún así, guardar el perfil sin la imagen si falla la obtención de URL
                    saveUserDetailsToDatabase(uid, nombre, apellido, email, telefono, null)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.w(TAG, "Error al subir imagen: ", exception)
                Toast.makeText(baseContext, "Error al subir imagen. Perfil guardado sin imagen.", Toast.LENGTH_LONG).show()
                // Aún así, guardar el perfil sin la imagen si falla la subida
                saveUserDetailsToDatabase(uid, nombre, apellido, email, telefono, null)
            }
            .addOnProgressListener { taskSnapshot -> // Opcional: para mostrar progreso
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                Log.d(TAG, "Subida de imagen: $progress% completado")
                // Aquí podrías actualizar un ProgressBar visual si lo deseas
                // binding.imageUploadProgressBar.progress = progress.toInt() // Si tuvieras uno específico
            }
    }

    private fun saveUserDetailsToDatabase(
        uid: String,
        nombre: String,
        apellido: String,
        email: String,      // Añadido email
        telefono: String,   // Añadido telefono
        profileImageUrl: String?
    ) {
        Log.d(TAG, "saveUserDetailsToDatabase: Preparando para guardar datos para UID: $uid")
        // Crear objeto User con todos los datos
        val userProfile = User(
            name = nombre,
            surName = apellido,
            email = email,
            phone = telefono,
            profileImageUrl = profileImageUrl
            // description y skills se inicializarán con sus valores por defecto (null y emptyList)
        )

        userDatabaseReference.child(uid).setValue(userProfile)
            .addOnSuccessListener {
                showLoading(false)
                Log.d(TAG, "Perfil de usuario guardado en RTDB con éxito para UID: $uid")
                Toast.makeText(baseContext, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()

                // Navegar a la actividad principal (por ejemplo, UserHomeActivity o la que corresponda)
                // Es importante limpiar el stack para que el usuario no pueda volver a RegisterActivity con el botón "atrás"
                val intent = Intent(this@RegisterActivity, UserHomeActivity::class.java) // Cambia UserHomeActivity si es otra
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Cierra RegisterActivity
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.w(TAG, "Error al guardar perfil en RTDB para UID: $uid", e)
                Toast.makeText(baseContext, "Usuario creado, pero falló al guardar datos del perfil. Intenta editar tu perfil más tarde.", Toast.LENGTH_LONG).show()
                // Aunque falle guardar en RTDB, el usuario de Auth ya está creado.
                // Decide el flujo. Por ahora, navegaremos igualmente.
                // Podrías considerar eliminar el usuario de Auth aquí si el guardado en DB es crítico
                // o redirigir a una pantalla donde pueda reintentar guardar el perfil.
                val intent = Intent(this@RegisterActivity, UserHomeActivity::class.java) // Cambia UserHomeActivity si es otra
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        Log.d(TAG, "showLoading: $isLoading")
        binding.progressBarRegistro.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registroBoton.isEnabled = !isLoading
        binding.botonSeleccionarFoto.isEnabled = !isLoading
        // También podrías deshabilitar los EditTexts si lo deseas
        binding.registroNombre.isEnabled = !isLoading
        binding.registroApellido.isEnabled = !isLoading
        binding.registroEmail.isEnabled = !isLoading
        binding.registroTelefono.isEnabled = !isLoading
        binding.registroConstrasena.isEnabled = !isLoading
    }
}
