package com.example.studin.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityCompanyOfferFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

    private fun saveOfferToDatabase() {
        val offerTitle = binding.editTextOfferTitle.text.toString().trim()
        val offerDescription = binding.editTextOfferDescription.text.toString().trim()
        val offerLocation = binding.autoCompleteTextViewLocation.text.toString().trim()

        var isValid = true
        if (offerTitle.isEmpty()) {
            binding.editTextOfferTitle.error = "El título no puede estar vacío"
            isValid = false
        } else {
            binding.editTextOfferTitle.error = null
        }

        if (offerDescription.isEmpty()) {
            binding.editTextOfferDescription.error = "La descripción no puede estar vacía"
            isValid = false
        } else {
            binding.editTextOfferDescription.error = null
        }

        if (offerLocation.isEmpty()) {
            binding.autoCompleteTextViewLocation.error = "La ubicación no puede estar vacía"
            isValid = false
        } else {
            binding.autoCompleteTextViewLocation.error = null
        }

        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser == null) {
            Log.w(TAG, "Usuario no autenticado. No se puede guardar la oferta.")
            Toast.makeText(this, "Debes estar autenticado para crear una oferta.", Toast.LENGTH_LONG).show()
            return
        }

        if (!isValid) {
            Log.w(TAG, "Validación fallida. No se guardará la oferta.")
            Toast.makeText(this, "Por favor, completa todos los campos requeridos.", Toast.LENGTH_SHORT).show()
            return
        }

        //  Preparación de datos y guardado
        Log.d(TAG, "Validación exitosa. Obteniendo nombre de la compañía...")


        val offerCompanyId = currentFirebaseUser.uid
        val companyNameRef = database.getReference("companies").child(offerCompanyId).child("name")

        // Obtener el nombre de la compañía de forma asíncrona
        companyNameRef.addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offerCompanyName = dataSnapshot.getValue(String::class.java)

                if (offerCompanyName == null) {
                    Log.e(TAG, "No se pudo obtener el nombre de la compañía para ID: $offerCompanyId. Usando 'Nombre no disponible'.")
                    // Decide cómo manejar esto: ¿usar un nombre por defecto? ¿Mostrar un error?
                    // Aquí usamos un valor por defecto para continuar, pero podrías querer un manejo más robusto.
                    proceedWithSavingOffer(offerTitle, offerDescription, offerLocation, offerCompanyId, "Nombre no disponible")
                    return
                }

                Log.d(TAG, "Nombre de la compañía obtenido: $offerCompanyName. Procediendo a guardar la oferta...")
                proceedWithSavingOffer(offerTitle, offerDescription, offerLocation, offerCompanyId, offerCompanyName)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Error al obtener el nombre de la compañía: ${databaseError.message}")
                Toast.makeText(this@CreateOfferActivity, "Error al obtener datos de la compañía: ${databaseError.message}", Toast.LENGTH_LONG).show()

            }
        })
    }

    private fun proceedWithSavingOffer(
        offerTitle: String,
        offerDescription: String,
        offerLocation: String,
        offerCompanyId: String,
        offerCompanyName: String
    ) {
        val offerId = offersReference.push().key // Genera un ID único para la nueva oferta

        if (offerId == null) {
            Log.e(TAG, "No se pudo generar un ID para la oferta.")
            Toast.makeText(this, "Error al generar ID para la oferta. Intenta de nuevo.", Toast.LENGTH_SHORT).show()

            return
        }

        val offerDateTimestamp = System.currentTimeMillis()

        val newOffer = Offer(
            id = offerId,
            title = offerTitle,
            description = offerDescription,
            location = offerLocation,
            companyId = offerCompanyId,
            fechaPublicacion = offerDateTimestamp,
            companyName = offerCompanyName,
        )

        // Guardar la oferta en Firebase Realtime Database
        offersReference.child(offerId).setValue(newOffer)
            .addOnSuccessListener {
                Log.d(TAG, "Oferta guardada exitosamente con ID: $offerId")
                Toast.makeText(this, "Oferta guardada con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar la oferta en la base de datos", e)
                Toast.makeText(this, "Error al guardar la oferta: ${e.message}", Toast.LENGTH_LONG).show()

            }
    }
}