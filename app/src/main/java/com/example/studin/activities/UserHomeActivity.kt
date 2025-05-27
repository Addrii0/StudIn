package com.example.studin.activities // Mantén tu paquete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Importar View para View.GONE/VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Importa tu clase de ViewBinding generada
import com.example.studin.databinding.ActivityHomeBinding // Asegúrate que este es el nombre correcto
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.studin.classes.Offer // Importa tu clase Offer
import com.example.studin.ui.fragments.OffersOverlayFragment // Asegúrate de esta ruta
import com.google.firebase.auth.FirebaseAuth

class UserHomeActivity : AppCompatActivity(), OffersOverlayFragment.OffersOverlayListener {

    // Declara la variable de ViewBinding
    private lateinit var binding: ActivityHomeBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var offersReference: DatabaseReference
    private lateinit var auth : FirebaseAuth

    private val TAG = "UserHomeActivity" // Cambiado TAG para más claridad

    private var loadedOffersList: MutableList<Offer> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout usando ViewBinding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root) // Establece la vista raíz del binding

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        offersReference = database.getReference("offers") // O "ofertas" si lo renombraste

        binding.chat.setOnClickListener { // Acceso directo a la ImageView con ID "chat"
            val chatIntent = Intent(this, MainChatsActivity::class.java)
            startActivity(chatIntent)
        }

        binding.buttonExample.setOnClickListener {
          logoutUser()
        }


        if (savedInstanceState == null) {
            loadOffersFromDatabase()
        }
    }

    private fun loadOffersFromDatabase() {
        offersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loadedOffersList.clear()

                for (offerSnapshot in snapshot.children) {
                    val offer = offerSnapshot.getValue(Offer::class.java)
                    if (offer != null) {
                        // Asumiendo que tu clase Offer tiene un campo 'id' que quieres poblar con la clave del snapshot
                        val offerWithId = offer.copy(id = offerSnapshot.key) // Si Offer es data class y tiene 'id'
                        loadedOffersList.add(offerWithId)
                        // Si Offer no tiene 'id' o no lo necesitas de la key:
                        // loadedOffersList.add(offer)
                        Log.d(TAG, "Oferta cargada: ${offerWithId.title}")
                    } else {
                        Log.w(TAG, "Error al parsear un objeto Offer desde snapshot")
                    }
                }

                Log.d(TAG, "Carga de ofertas completa. ${loadedOffersList.size} encontradas.")

                if (loadedOffersList.isNotEmpty() && binding.offersFragmentContainer.visibility == View.GONE) {
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

        val offersOverlayFragment = OffersOverlayFragment.newInstance(ArrayList(offers)) // Pasar como ArrayList si Offer es Parcelable

        supportFragmentManager.beginTransaction()
            .add(binding.offersFragmentContainer.id, offersOverlayFragment, "OffersOverlay") // Usar el ID del contenedor desde el binding
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