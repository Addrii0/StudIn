package com.example.studin.activities// En UserProfileActivity.kt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studin.R // Asegúrate que tu R sea el correcto
import com.example.studin.classes.User // Tu clase User
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
    private var userIdToLoad: String? = null // Para cargar el perfil de un usuario específico o el actual

    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Determinar qué perfil de usuario cargar
        // Opción 1: Cargar el perfil del usuario actualmente logueado
        // userIdToLoad = auth.currentUser?.uid

        // Opción 2: Cargar el perfil de un UID pasado a través de un Intent
        // (Esto es útil si esta actividad puede mostrar perfiles de otros usuarios)
        userIdToLoad = intent.getStringExtra("USER_ID") // Si pasas "USER_ID" desde otra actividad
        if (userIdToLoad == null) {
            // Si no se pasa un USER_ID, por defecto carga el del usuario actual
            userIdToLoad = auth.currentUser?.uid
        }


        if (userIdToLoad == null) {
            Toast.makeText(this, "No se pudo identificar al usuario.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "USER_ID es nulo y no hay usuario logueado.")
            finish() // Cierra la actividad si no hay ID de usuario
            return
        }

        userReference = database.getReference("users").child(userIdToLoad!!) // El !! es seguro por la comprobación anterior

        // Ocultar/mostrar botón de editar perfil
        if (userIdToLoad == auth.currentUser?.uid) {
            binding.buttonEditProfile.visibility = View.VISIBLE
            binding.buttonEditProfile.setOnClickListener {
                // Lógica para ir a EditarPerfilActivity
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
        binding.progressBarUserProfile?.visibility = View.VISIBLE // Asume que tienes un ProgressBar con este ID

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarUserProfile?.visibility = View.GONE
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        // Poblar la UI con los datos del usuario
                        binding.textViewUserName.text = "${user.name ?: ""} ${user.surName ?: "N/A"}"
                        binding.textViewUserEmail.text = user.email ?: "Email no disponible"

                        // Cargar imagen de perfil usando Glide (o tu librería preferida)
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@UserProfileActivity)
                                .load(user.profileImageUrl)
                                .placeholder(R.drawable.ic_profile_person) // El placeholder que creamos
                                .error(R.drawable.ic_profile_person) // Imagen de error
                                .circleCrop() // Para hacerla redonda si no lo es ya por el ShapeableImageView
                                .into(binding.imageViewUserProfile)
                        } else {
                            // Si no hay URL de imagen, mostrar el placeholder por defecto
                            binding.imageViewUserProfile.setImageResource(R.drawable.ic_profile_person)
                        }

                        // Ejemplo de otros campos (asegúrate que existan en tu clase User y en el layout)
                        binding.textViewUserDescription.text = user.description ?: "Sin descripción."
                        //binding.textViewUserPhone.text = user.phone ?: "Teléfono no disponible"

                        // Para las habilidades, podrías tener un TextView o un ChipGroup
                        if (user.skills != null && user.skills.isNotEmpty()) {
                            // Si 'skills' es una List<String> en tu clase User
                            binding.textViewUserSkills.text = user.skills.joinToString(", ")
                            // Si usaras un ChipGroup, tendrías que inflar Chips aquí
                        } else {
                            binding.textViewUserSkills.text = "Habilidades no especificadas."
                        }

                        // Mostrar/ocultar campos opcionales
                        //binding.textViewUserPhone.visibility = if (user.phone.isNullOrEmpty()) View.GONE else View.VISIBLE


                    } else {
                        Log.e(TAG, "Error al deserializar el objeto User desde Firebase.")
                        Toast.makeText(this@UserProfileActivity, "No se pudo cargar el perfil.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "El perfil del usuario con ID $userIdToLoad no existe.")
                    Toast.makeText(this@UserProfileActivity, "Perfil no encontrado.", Toast.LENGTH_LONG).show()
                    // Podrías cerrar la actividad o mostrar un estado de "no encontrado" más explícito
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