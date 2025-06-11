package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.User
import com.example.studin.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var userReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var userIdToLoad: String? = null // El UID del perfil que se va a mostrar
    private var currentUserUID: String? = null // El UID del usuario actualmente logueado
    private val TAG = "UserProfileActivity"
    private var userProfileImageUrl: String? = null
    private var userNameFromProfile: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUserUID = auth.currentUser?.uid

        // Determinar qué perfil cargar
        val selectedUserIdFromIntent = intent.getStringExtra("SELECTED_USER_ID")
        if (selectedUserIdFromIntent != null) {
            userIdToLoad = selectedUserIdFromIntent
            Log.d(TAG, "Cargando perfil del usuario seleccionado: $userIdToLoad")
        } else {
            // Si no se pasa un ID, por defecto se carga el perfil del usuario actual
            userIdToLoad = currentUserUID
            Log.d(TAG, "No se recibió SELECTED_USER_ID, cargando perfil del usuario actual: $userIdToLoad")
        }

        if (userIdToLoad == null) {
            Toast.makeText(this, "No se pudo identificar al usuario.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "userIdToLoad es nulo. No se puede cargar el perfil.")
            finish()
            return
        }

        userReference = database.getReference("users").child(userIdToLoad!!)
        userNameFromProfile = database.getReference("users").child(userIdToLoad!!).child("name").toString()
        userProfileImageUrl = database.getReference("users").child(userIdToLoad!!).child("profileImageUrl").toString()

        setupButtons()
        loadUserProfile()
    }

    private fun setupButtons() {
        if (userIdToLoad == currentUserUID) {
            // Es el perfil del usuario actual
            binding.buttonEditProfile.visibility = View.VISIBLE
            binding.buttonSendMessage.visibility = View.GONE // Ocultar "Enviar Mensaje" para el propio usuario

            binding.buttonEditProfile.setOnClickListener {
                val intent = Intent(this, EditUserProfileActivity::class.java)

                startActivity(intent)
            }
        } else {
            // Es el perfil de otro usuario
            binding.buttonEditProfile.visibility = View.GONE
            binding.buttonSendMessage.visibility = View.VISIBLE

            binding.buttonSendMessage.setOnClickListener {

                Log.d(TAG, "Botón 'Enviar Mensaje' clickeado para el usuario: $userIdToLoad")

                val intent = Intent(this, MainChatsActivity::class.java)
                intent.putExtra("ACTION_START_CHAT_WITH_USER_ID", userIdToLoad)
                intent.putExtra("ACTION_START_CHAT_WITH_USER_NAME", userNameFromProfile)
                intent.putExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL", userProfileImageUrl)
                startActivity(intent)
                Toast.makeText(this, "Iniciando chat con el usuario...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile() {
        binding.progressBarUserProfile.visibility = View.VISIBLE

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarUserProfile.visibility = View.GONE
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        // Rellenar los datos de la UI
                        binding.textViewUserName.text = "${user.name ?: ""} ${user.surName ?: "N/A"}"
                        binding.textViewUserEmail.text = user.email ?: "Email no disponible"
                        binding.textViewUserPhone.text = user.phone ?: "Teléfono no disponible"
                        binding.textViewUserDescription.text = user.description ?: "Sin descripción."
                        binding.textViewUserExperience.text = user.experience ?: "Experiencia no especificada."
                        binding.textViewUserEducation.text = user.education ?: "Educación no especificada." // Campo añadido

                        // Habilidades
                        if (user.skills.isNotEmpty()) {
                            binding.textViewUserSkills.text = user.skills.joinToString(", ")
                        } else {
                            binding.textViewUserSkills.text = "Habilidades no especificadas."
                        }

                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@UserProfileActivity)
                                .load(user.profileImageUrl)
                                .placeholder(R.drawable.ic_profile_person)
                                .error(R.drawable.ic_profile_person)
                                .circleCrop()
                                .into(binding.imageViewUserProfile)
                        } else {
                            binding.imageViewUserProfile.setImageResource(R.drawable.ic_profile_person)
                        }

                    } else {
                        Log.e(TAG, "Error al deserializar el objeto User desde Firebase para ID: $userIdToLoad")
                        Toast.makeText(this@UserProfileActivity, "No se pudo cargar el perfil.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "El perfil del usuario con ID $userIdToLoad no existe.")
                    Toast.makeText(this@UserProfileActivity, "Perfil no encontrado.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarUserProfile.visibility = View.GONE // Asegúrate de ocultar el ProgressBar también en caso de error
                Log.e(TAG, "Error al leer datos del perfil para ID $userIdToLoad: ${error.message}")
                Toast.makeText(this@UserProfileActivity, "Error al cargar perfil: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}