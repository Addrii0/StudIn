package com.example.studin.activities

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
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
import com.example.studin.R
class CreateOfferActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCompanyOfferFormBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var offersReference: DatabaseReference
    private var selectedType: String? = null
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

        val offerTypes = resources.getStringArray(R.array.offer_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, offerTypes)
        val autoCompleteTextView = binding.autoCompleteTextViewOfferType
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            selectedType = parent.getItemAtPosition(position).toString()

        }
    }

    private fun saveOfferToDatabase() {
        val offerTitle = binding.editTextOfferTitle.text.toString().trim()
        val offerDescription = binding.editTextOfferDescription.text.toString().trim()
        val offerLocation = binding.autoCompleteTextViewLocation.text.toString().trim()
        val offerSkills = binding.editTextOfferSkills.text.toString().trim()
        val offerRequirements = binding.editTextOfferRequirements.text.toString().trim()
        val offerType = selectedType

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
        if (offerSkills.isEmpty()) {
            binding.editTextOfferSkills.error = "Las habilidades no pueden estar vacías"
            isValid = false
        } else {
            binding.editTextOfferSkills.error = null
        }
        if (offerRequirements.isEmpty()) {
            binding.editTextOfferRequirements.error = "Los requisitos no pueden estar vacíos"
            isValid = false
        } else {
            binding.editTextOfferRequirements.error = null
            }
        if (offerType!!.isEmpty()) {
            binding.autoCompleteTextViewOfferType.error = "El tipo de oferta no puede estar vacío"
            isValid = false
        } else {
            binding.autoCompleteTextViewOfferType.error = null
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

        // Obtener el nombre de la compañía
        companyNameRef.addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offerCompanyName = dataSnapshot.getValue(String::class.java)

                if (offerCompanyName == null) {
                    Log.e(TAG, "No se pudo obtener el nombre de la compañía para ID: $offerCompanyId. Usando 'Nombre no disponible'.")
                      proceedWithSavingOffer(offerTitle, offerDescription, offerLocation, offerCompanyId, "Nombre no disponible", offerSkills, offerRequirements, offerType)
                    return
                }

                Log.d(TAG, "Nombre de la compañía obtenido: $offerCompanyName. Procediendo a guardar la oferta...")
                proceedWithSavingOffer(offerTitle, offerDescription, offerLocation, offerCompanyId, offerCompanyName, offerSkills, offerRequirements, offerType)
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
        offerCompanyName: String,
        offerSkills: String,
        offerRequirements: String,
        offerType: String
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
            datePosted = offerDateTimestamp,
            companyName = offerCompanyName,
            skills = offerSkills.split(",").map { it.trim() },
            requirements = offerRequirements.split(",").map { it.trim() },
            type = offerType
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