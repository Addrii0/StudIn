package com.example.studin.activities // Mantén tu paquete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Importar View para View.GONE/VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.databinding.ActivityHomeBinding // Asegúrate que este es el nombre correcto
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.studin.classes.Offer // Importa tu clase Offer
import com.example.studin.ui.fragments.OffersOverlayFragment // Asegúrate de esta ruta
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.isGone
import com.bumptech.glide.Glide // Importa Glide
import com.example.studin.R // Importa R para acceder a los drawables
import com.example.studin.classes.User // Importa tu clase User

class UserHomeActivity : AppCompatActivity(), OffersOverlayFragment.OffersOverlayListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var offersReference: DatabaseReference
    private lateinit var userReference: DatabaseReference // Referencia para el usuario actual
    private lateinit var auth : FirebaseAuth

    private val TAG = "UserHomeActivity"

    private var loadedOffersList: MutableList<Offer> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        offersReference = database.getReference("offers")

        // Obtener el UID del usuario actual
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            userReference = database.getReference("users").child(currentUserUid)
            loadUserProfileImage() // Llama a la función para cargar la imagen
        } else {
            // Manejar el caso donde no hay usuario logueado (aunque no debería llegar aquí si UserHomeActivity requiere login)
            Log.w(TAG, "Usuario no logueado, no se puede cargar imagen de perfil.")
            // Puedes establecer una imagen por defecto si es necesario
            binding.imageView5.setImageResource(R.drawable.icono_persona) // Asume que tienes un drawable 'icono_persona'
        }


        binding.chat.setOnClickListener {
            val chatIntent = Intent(this, MainChatsActivity::class.java)
            startActivity(chatIntent)
        }
        binding.offerButtom.setOnClickListener {
            val offerIntent = Intent(this, UserOfferActivity::class.java)
            startActivity(offerIntent)
        }

        binding.buttonExample.setOnClickListener {
            logoutUser()
        }
        binding.imageView5.setOnClickListener {
            val profileIntent = Intent(this, UserProfileActivity::class.java)
            startActivity(profileIntent)
        }

        if (savedInstanceState == null) {
            loadOffersFromDatabase()
        }
    }

    private fun loadUserProfileImage() {
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null && !user.profileImageUrl.isNullOrEmpty()) {
                    Log.d(TAG, "URL de imagen de perfil obtenida: ${user.profileImageUrl}")
                    Glide.with(this@UserHomeActivity)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.icono_persona) // Opcional: imagen mientras carga
                        .error(R.drawable.icono_persona) // Opcional: imagen si hay error al cargar
                        .circleCrop() // Opcional: para hacer la imagen circular
                        .into(binding.imageView5)
                } else {
                    Log.w(TAG, "No se encontró URL de imagen de perfil o está vacía.")
                    // Establecer una imagen por defecto si no hay URL
                    binding.imageView5.setImageResource(R.drawable.icono_persona) // Asume que tienes un drawable 'icono_persona'
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Error al cargar datos del usuario para imagen: ", error.toException())
                // Establecer una imagen por defecto en caso de error
                binding.imageView5.setImageResource(R.drawable.ic_profile_person) // Asume que tienes un drawable 'icono_error_imagen'
            }
        })
    }

    private fun loadOffersFromDatabase() {
        offersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loadedOffersList.clear()
                for (offerSnapshot in snapshot.children) {
                    val offer = offerSnapshot.getValue(Offer::class.java)
                    if (offer != null) {
                        val offerWithId = offer.copy(id = offerSnapshot.key)
                        loadedOffersList.add(offerWithId)
                        Log.d(TAG, "Oferta cargada: ${offerWithId.title}")
                    } else {
                        Log.w(TAG, "Error al parsear un objeto Offer desde snapshot")
                    }
                }
                Log.d(TAG, "Carga de ofertas completa. ${loadedOffersList.size} encontradas.")
                if (loadedOffersList.isNotEmpty() && binding.offersFragmentContainer.isGone) {
                    showOffersOverlay(loadedOffersList)
                } else if (loadedOffersList.isEmpty()) {
                    Log.d(TAG, "No se encontraron ofertas para mostrar.")
                    Toast.makeText(baseContext, "No hay ofertas disponibles por el momento.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "loadOffers:onCancelled", error.toException())
                Toast.makeText(baseContext, "Error al cargar ofertas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showOffersOverlay(offers: List<Offer>) {
        val existingFragment = supportFragmentManager.findFragmentByTag("OffersOverlay")
        if (existingFragment != null && existingFragment.isVisible) {
            Log.d(TAG, "OffersOverlayFragment ya visible, no se añade de nuevo.")
            return
        }
        val offersOverlayFragment = OffersOverlayFragment.newInstance(ArrayList(offers))
        supportFragmentManager.beginTransaction()
            .add(binding.offersFragmentContainer.id, offersOverlayFragment, "OffersOverlay")
            .commit()
        binding.offersFragmentContainer.visibility = View.VISIBLE
        Log.d(TAG, "OffersOverlayFragment añadido y contenedor visible.")
    }

    override fun onOffersOverlayClose() {
        binding.offersFragmentContainer.visibility = View.GONE
        Log.d(TAG, "Contenedor de ofertas oculto.")
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this,"Has cerrado sesión",Toast.LENGTH_SHORT).show()
    }
}