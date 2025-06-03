package com.example.studin.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studin.R // Asegúrate de importar tu clase R
import com.example.studin.classes.User // Tu clase User
import com.example.studin.databinding.ActivityUserProfileEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class EditUserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileEditBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userDatabaseReference: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var currentProfileImageUrl: String? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private var currentUserUid: String? = null

    private val TAG = "EditUserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbarEditProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Perfil"

        // Inicializar Firebase
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        storage = Firebase.storage

        currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "Error: Usuario no encontrado.", Toast.LENGTH_LONG).show()
            finish() // Cierra la actividad si no hay usuario
            return
        }
        userDatabaseReference = database.getReference("users").child(currentUserUid!!)

        // Inicializar el lanzador para seleccionar imágenes
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    selectedImageUri = it
                    Glide.with(this)
                        .load(it)
                        .circleCrop() // Para que la vista previa sea circular si quieres
                        .placeholder(R.drawable.icono_persona) // Reemplaza con tu placeholder
                        .into(binding.imageViewEditProfilePicture)
                    Log.d(TAG, "Nueva imagen seleccionada: $it")
                }
            }

        // Cargar datos actuales del usuario
        loadCurrentUserData()

        // Configurar listeners de botones
        binding.buttonChangeProfilePicture.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonSaveProfileChanges.setOnClickListener {
            validateAndSaveChanges()
        }
    }

    private fun loadCurrentUserData() {
        showLoading(true)
        userDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    binding.editTextProfileName.setText(user.name)
                    binding.editTextProfileSurname.setText(user.surName)
                    binding.editTextProfileEmail.setText(user.email) // Email generalmente no editable
                    binding.editTextProfilePhone.setText(user.phone ?: "")
                    binding.editTextProfileDescription.setText(user.description ?: "")
                    binding.editTextProfileSkills.setText(user.skills.joinToString(", "))

                    currentProfileImageUrl = user.profileImageUrl
                    if (!currentProfileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@EditUserProfileActivity)
                            .load(currentProfileImageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.default_header_placeholder) // Reemplaza con tu placeholder
                            .error(R.drawable.ic_profile_person)   // Reemplaza con tu imagen de error
                            .into(binding.imageViewEditProfilePicture)
                    } else {
                        binding.imageViewEditProfilePicture.setImageResource(R.drawable.icono_persona) // Placeholder si no hay imagen
                    }
                    Log.d(TAG, "Datos del usuario cargados.")
                } else {
                    Toast.makeText(this@EditUserProfileActivity, "No se pudieron cargar los datos del perfil.", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Datos del usuario no encontrados en la base de datos.")
                }
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Toast.makeText(this@EditUserProfileActivity, "Error al cargar datos: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al cargar datos del usuario: ", error.toException())
            }
        })
    }

    private fun validateAndSaveChanges() {
        val name = binding.editTextProfileName.text.toString().trim()
        val surname = binding.editTextProfileSurname.text.toString().trim()
        // val email = binding.editTextProfileEmail.text.toString().trim() // Email no se suele editar
        val phone = binding.editTextProfilePhone.text.toString().trim()
        val description = binding.editTextProfileDescription.text.toString().trim()
        val skillsString = binding.editTextProfileSkills.text.toString().trim()
        val skillsList = skillsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (name.isEmpty()) {
            binding.textInputLayoutProfileName.error = "El nombre no puede estar vacío"
            return
        } else {
            binding.textInputLayoutProfileName.error = null
        }

        if (surname.isEmpty()) {
            binding.textInputLayoutProfileSurname.error = "El apellido no puede estar vacío"
            return
        } else {
            binding.textInputLayoutProfileSurname.error = null
        }

        // Añade más validaciones si son necesarias (teléfono, etc.)

        showLoading(true)

        if (selectedImageUri != null) {
            // Si se seleccionó una nueva imagen, subirla primero
            uploadNewProfileImageAndThenUpdateUserDetails(name, surname, phone, description, skillsList)
        } else {
            // Si no se cambió la imagen, solo actualizar los detalles de texto
            updateUserDetailsInDatabase(name, surname, phone, description, skillsList, currentProfileImageUrl)
        }
    }

    private fun uploadNewProfileImageAndThenUpdateUserDetails(
        name: String,
        surname: String,
        phone: String,
        description: String,
        skills: List<String>
    ) {
        val imageFileName = "profile_${currentUserUid}.jpg" // Nombre de archivo consistente
        val imageRef = storage.reference.child("profile_images/users/$currentUserUid/$imageFileName")

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener { uploadTask ->
                    uploadTask.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        val newImageUrl = downloadUri.toString()
                        Log.d(TAG, "Nueva imagen subida, URL: $newImageUrl")
                        updateUserDetailsInDatabase(name, surname, phone, description, skills, newImageUrl)
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Log.w(TAG, "Error al obtener URL de descarga de la nueva imagen: ", e)
                        Toast.makeText(baseContext, "Error al obtener URL de imagen. Se guardaron otros datos.", Toast.LENGTH_LONG).show()
                        // Aún así, guardar los otros datos con la imagen anterior (o sin imagen si no había)
                        // ... (código anterior de addOnFailureListener para obtener downloadUrl)
                        updateUserDetailsInDatabase(name, surname, phone, description, skills, currentProfileImageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.w(TAG, "Error al subir nueva imagen de perfil: ", e)
                    Toast.makeText(baseContext, "Error al subir imagen: ${e.message}", Toast.LENGTH_LONG).show()
                    // Opcional: podrías decidir no guardar ningún cambio si la subida de imagen falla,
                    // o guardar solo los datos de texto como en el caso anterior.
                    // Por ahora, no haremos nada más aquí, el usuario deberá intentarlo de nuevo.
                }
        }
    }

    private fun updateUserDetailsInDatabase(
        name: String,
        surname: String,
        phone: String,
        description: String,
        skills: List<String>,
        imageUrl: String? // Puede ser la nueva URL o la anterior
    ) {
        val userUpdates = mutableMapOf<String, Any>()
        userUpdates["name"] = name
        userUpdates["surName"] = surname // Asumo que el campo en tu clase User es surName
        userUpdates["telefono"] = phone
        userUpdates["description"] = description
        userUpdates["skills"] = skills
        imageUrl?.let {
            userUpdates["profileImageUrl"] = it
        }

        userDatabaseReference.updateChildren(userUpdates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Datos del usuario actualizados en la base de datos.")
                // Opcional: setResult(Activity.RESULT_OK) si iniciaste esta actividad con startActivityForResult
                finish() // Cierra la actividad de edición
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al actualizar datos del usuario: ", e)
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarEditProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSaveProfileChanges.isEnabled = !isLoading
        binding.buttonChangeProfilePicture.isEnabled = !isLoading
        // Puedes deshabilitar otros campos de entrada también si lo deseas
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar el botón de atrás en la Toolbar
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed() // o finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}