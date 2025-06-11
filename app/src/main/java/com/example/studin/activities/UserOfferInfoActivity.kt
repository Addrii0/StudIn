package com.example.studin.activities

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityUserOfferInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.Date
import java.util.Locale

class UserOfferInfoActivity: AppCompatActivity() {

    private lateinit var binding: ActivityUserOfferInfoBinding
    private lateinit var offerId: String
    private lateinit var database: FirebaseDatabase
    private lateinit var offerAplicantReference: DatabaseReference
    private lateinit var userAplicantReference: DatabaseReference
    private lateinit var offersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val TAG = "UserOfferInfoActivity"
    companion object {
        const val EXTRA_OFFER_ID = "SELECTED_OFFER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOfferInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        offersReference = database.getReference("offers")
        offerAplicantReference = database.getReference("offerApplications")
        userAplicantReference = database.getReference("userAppliedOffers")

        // Obtener el ID de la oferta del Intent
        offerId = intent.getStringExtra("SELECTED_OFFER_ID") ?: run {
            Toast.makeText(this, "Error: ID de oferta no encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadOfferDetails(offerId)

        binding.buttonApplyToOffer.setOnClickListener {
            applyToOffer()
        }
        checkIfUserAlreadyApplied()
    }

    private fun applyToOffer() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para aplicar.", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        // Deshabilitar el botón para evitar múltiples clics
        binding.buttonApplyToOffer.isEnabled = false
        binding.buttonApplyToOffer.text = "Aplicando..."

        // Crear el objeto de la solicitud
        val applicationData = hashMapOf(
            "appliedAt" to ServerValue.TIMESTAMP,
            "status" to "pending" // Estado inicial
        )

        val childUpdates = hashMapOf<String, Any?>()
        val applicationPath = "$offerId/$userId" // Ruta en offerApplications
        val userAppliedPath = "$userId/$offerId"  // Ruta en userAppliedOffers

        childUpdates["/offerApplications/$applicationPath"] = applicationData
        childUpdates["/userAppliedOffers/$userAppliedPath"] = true

        FirebaseDatabase.getInstance().reference.updateChildren(childUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Has aplicado a la oferta!", Toast.LENGTH_SHORT).show()
                binding.buttonApplyToOffer.text = "Aplicado" // Cambiar texto del botón
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al aplicar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.buttonApplyToOffer.isEnabled = true // Si hay error, vuelve a mostrar botón
                binding.buttonApplyToOffer.text = "Aplicar a Oferta"
                Log.e("ApplyToOffer", "Error al escribir en Firebase", e)
            }
    }
    private fun checkIfUserAlreadyApplied() {
        val currentUser = auth.currentUser
        if (currentUser == null) {

            binding.buttonApplyToOffer.isEnabled = true
            return
        }
        val userId = currentUser.uid

        // Comprueba en el nodo "offerApplications"
        offerAplicantReference.child(offerId).child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // El usuario ya aplicó
                    binding.buttonApplyToOffer.isEnabled = false
                    binding.buttonApplyToOffer.text = "Ya has aplicado"
                } else {
                    // El usuario no ha aplicado
                    binding.buttonApplyToOffer.isEnabled = true
                    binding.buttonApplyToOffer.text = "Aplicar a Oferta"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CheckApplied", "Error al verificar si ya aplicó: ${error.message}")
                // En caso de error, es seguro permitir que intente aplicar,
                binding.buttonApplyToOffer.isEnabled = true
                binding.buttonApplyToOffer.text = "Aplicar a Oferta"
            }
        })
    }
    private fun loadOfferDetails(offerId: String) {
        offersReference.child(offerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    val offer = snapshot.getValue(Offer::class.java)
                    if (offer != null) {
                        populateUIWithOfferData(offer)
                    } else {
                        Log.e(TAG, "Error al deserializar la oferta desde Firebase.")
                        Toast.makeText(this@UserOfferInfoActivity, "No se pudieron cargar los detalles.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "La oferta con ID $offerId no existe.")
                    Toast.makeText(this@UserOfferInfoActivity, "Oferta no disponible.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de la oferta: ${error.message}")
                Toast.makeText(this@UserOfferInfoActivity, "Error al cargar oferta: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun populateUIWithOfferData(offer: Offer) {
        Log.d(TAG, "Rellenando UI con datos de la oferta: ${offer.title}")
        binding.textViewOfferTitleInfo.text = offer.title ?: "N/A"
        binding.textViewOfferDescriptionInfo.text = offer.description ?: "No hay descripción disponible."
        binding.textViewOfferLocationInfo.text = offer.location ?: "Ubicación no especificada."
        binding.textViewOfferTypeInfo.text = offer.type ?: "Tipo de oferta no especificado."
        if (offer.skills.isNotEmpty()) {
            binding.textViewOfferSkillsInfo.text = offer.skills.joinToString(separator = ", ")
        } else {
            binding.textViewOfferSkillsInfo.text = "No hay habilidades requeridas."
        }
         binding.textViewOfferRequirementsInfo.text = offer.requirements.joinToString(separator = ", ")

        offer.datePosted?.let { timestamp ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textViewOfferDateInfo.text = "Publicado el: " + sdf.format(Date(timestamp))
        } ?: run {
            binding.textViewOfferDateInfo.text = "Fecha de publicación no disponible."
        }
    }
}