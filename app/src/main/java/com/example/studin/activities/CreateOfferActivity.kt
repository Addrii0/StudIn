package com.example.studin.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityCompanyOfferFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CreateOfferActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCompanyOfferFormBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var offersReference: DatabaseReference

    private val TAG = "CreateOfferActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyOfferFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        offersReference = database.getReference("offers")

        binding.buttonSaveOffer.setOnClickListener {
        saveOfferToDatabase()

        }



    }
    // En CreateOfferActivity.kt
// Asegúrate de tener las importaciones necesarias, incluyendo tu clase Offer
// import com.example.studin.classes.Offer // Si está en otro paquete
// import android.widget.Toast // Para mostrar mensajes

    private fun saveOfferToDatabase() {
        val offerTitle = binding.editTextOfferTitle.text.toString().trim()
        val offerDescription = binding.editTextOfferDescription.text.toString().trim() // También hacer trim
        val offerLocation = binding.autoCompleteTextViewLocation.text.toString().trim() // También hacer trim

        // --- Validación ---
        var isValid = true
        if (offerTitle.isEmpty()) {
            binding.editTextOfferTitle.error = "El título no puede estar vacío"
            isValid = false
        } else {
            binding.editTextOfferTitle.error = null // Limpiar error si es válido
        }

        if (offerDescription.isEmpty()) {
            binding.editTextOfferDescription.error = "La descripción no puede estar vacía"
            isValid = false
        } else {
            binding.editTextOfferDescription.error = null
        }

        if (offerLocation.isEmpty()) {
            binding.autoCompleteTextViewLocation.error = "La ubicación no puede estar vacía" // Asumiendo que es un EditText o similar
            isValid = false
        } else {
            binding.autoCompleteTextViewLocation.error = null
        }

        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser == null) {
            Log.w(TAG, "Usuario no autenticado. No se puede guardar la oferta.")
            Toast.makeText(this, "Debes estar autenticado para crear una oferta.", Toast.LENGTH_LONG).show()
            // Aquí podrías redirigir al login si es necesario
            return // Salir de la función
        }

        if (!isValid) {
            Log.w(TAG, "Validación fallida. No se guardará la oferta.")
            Toast.makeText(this, "Por favor, completa todos los campos requeridos.", Toast.LENGTH_SHORT).show()
            return // Salir de la función si la validación falla
        }

        // --- Preparación de datos y guardado ---
        Log.d(TAG, "Validación exitosa. Guardando oferta en la base de datos...")
        // Opcional: Mostrar un ProgressBar mientras se guarda
        // binding.progressBarSaveOffer.visibility = View.VISIBLE
        // binding.buttonSaveOffer.isEnabled = false

        val offerCompanyId = currentFirebaseUser.uid
        val offerId = offersReference.push().key // Genera un ID único para la nueva oferta

        if (offerId == null) {
            Log.e(TAG, "No se pudo generar un ID para la oferta.")
            Toast.makeText(this, "Error al generar ID para la oferta. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            // Opcional: Ocultar ProgressBar
            // binding.progressBarSaveOffer.visibility = View.GONE
            // binding.buttonSaveOffer.isEnabled = true
            return
        }

        val offerDateTimestamp = System.currentTimeMillis()

        val newOffer = Offer(
            id = offerId,
            title = offerTitle,
            description = offerDescription,
            location = offerLocation,
            companyId = offerCompanyId,
            fechaPublicacion = offerDateTimestamp
        )

        // Guardar la oferta en Firebase Realtime Database bajo el nodo "offers" y el offerId generado
        offersReference.child(offerId).setValue(newOffer)
            .addOnSuccessListener {
                Log.d(TAG, "Oferta guardada exitosamente con ID: $offerId")
                Toast.makeText(this, "Oferta guardada con éxito", Toast.LENGTH_SHORT).show()

                // Opcional: Ocultar ProgressBar
                // binding.progressBarSaveOffer.visibility = View.GONE
                // binding.buttonSaveOffer.isEnabled = true

                // Aquí puedes, por ejemplo, finalizar la actividad o limpiar los campos
                finish() // Cierra CreateOfferActivity y vuelve a la anterior

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar la oferta en la base de datos", e)
                Toast.makeText(this, "Error al guardar la oferta: ${e.message}", Toast.LENGTH_LONG).show()

                // Opcional: Ocultar ProgressBar
                // binding.progressBarSaveOffer.visibility = View.GONE
                // binding.buttonSaveOffer.isEnabled = true
            }
    }
}