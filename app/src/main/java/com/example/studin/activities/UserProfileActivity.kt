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
    private var userIdToLoad: String? = null
    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        userIdToLoad = intent.getStringExtra("USER_ID")
        if (userIdToLoad == null) {
            userIdToLoad = auth.currentUser?.uid
        }

        if (userIdToLoad == null) {
            Toast.makeText(this, "No se pudo identificar al usuario.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "USER_ID es nulo y no hay usuario logueado.")
            finish()
            return
        }

        userReference = database.getReference("users").child(userIdToLoad!!)

        // Ocultar/mostrar botón de editar perfil
        if (userIdToLoad == auth.currentUser?.uid) {
            binding.buttonEditProfile.visibility = View.VISIBLE
            binding.buttonEditProfile.setOnClickListener {
                val intent = Intent(this, EditUserProfileActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Ir a editar perfil", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.buttonEditProfile.visibility = View.GONE
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        binding.progressBarUserProfile.visibility = View.VISIBLE

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarUserProfile.visibility = View.GONE
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        binding.textViewUserName.text = "${user.name ?: ""} ${user.surName ?: "N/A"}"
                        binding.textViewUserEmail.text = user.email ?: "Email no disponible"
                        binding.textViewUserDescription.text = user.description ?: "Sin descripción."
                        binding.textViewUserSkills.text = user.skills?.joinToString(", ") ?: "Habilidades no especificadas."
                        binding.textViewUserExperience.text = user.experience ?: "Experiencia no especificada."
                        binding.textViewUserPhone.text = user.phone ?: "Teléfono no disponible"
                        // Cargar imagen de perfil usando Glide
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@UserProfileActivity)
                                .load(user.profileImageUrl)
                                .placeholder(R.drawable.ic_profile_person)
                                .error(R.drawable.ic_profile_person)
                                .circleCrop() // Para hacerla redonda
                                .into(binding.imageViewUserProfile)
                        } else {
                            binding.imageViewUserProfile.setImageResource(R.drawable.ic_profile_person)
                        }

                        if (user.skills != null && user.skills.isNotEmpty()) {
                            binding.textViewUserSkills.text = user.skills.joinToString(", ")
                        } else {
                            binding.textViewUserSkills.text = "Habilidades no especificadas."
                        }

                    } else {
                        Log.e(TAG, "Error al deserializar el objeto User desde Firebase.")
                        Toast.makeText(this@UserProfileActivity, "No se pudo cargar el perfil.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "El perfil del usuario con ID $userIdToLoad no existe.")
                    Toast.makeText(this@UserProfileActivity, "Perfil no encontrado.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarUserProfile?.visibility = View.GONE
                Log.e(TAG, "Error al leer datos del perfil: ${error.message}")
                Toast.makeText(this@UserProfileActivity, "Error al cargar perfil: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}