package com.example.studin.activities

import android.app.Activity
import android.content.Intent
// import android.location.Address // Comentado si no se usa
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company // Asegúrate que esta clase solo tiene name, location, profileImageUrl
import com.example.studin.databinding.ActivityCompanyEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class EditCompanyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    private var currentCompany: Company? = null
    private var imageUri: Uri? = null
    private var companyId: String? = null

    private val TAG = "EditCompanyProfileAct"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data!!.data != null) {
                imageUri = result.data!!.data
                Glide.with(this).load(imageUri).circleCrop()
                    .placeholder(R.drawable.default_header_placeholder) // Cambia si tu placeholder tiene otro nombre
                    .into(binding.imageViewEditCompanyProfilePicture)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarEditCompanyProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Perfil de Empresa"

        auth = FirebaseAuth.getInstance()
        companyId = auth.currentUser?.uid

        if (companyId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "companyId (UID) es nulo.")
            finish()
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("companies").child(companyId!!)
        storageReference = FirebaseStorage.getInstance().getReference("company_profile_images")

        loadCompanyData()

        binding.buttonChangeCompanyProfilePicture.setOnClickListener {
            openGalleryForImage()
        }

        binding.buttonSaveCompanyProfileChanges.setOnClickListener {
            saveCompanyProfileChanges()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadCompanyData() {
        binding.progressBarEditCompanyProfile.visibility = View.VISIBLE
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                currentCompany = snapshot.getValue(Company::class.java)
                if (currentCompany != null) {
                    populateUI(currentCompany!!)
                } else {
                    Toast.makeText(this@EditCompanyProfileActivity, "No se encontraron datos del perfil.", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "No se encontró la empresa con ID: $companyId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                Toast.makeText(this@EditCompanyProfileActivity, "Error al cargar datos: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al cargar datos: ", error.toException())
            }
        })
    }

    private fun populateUI(company: Company) {
        binding.editTextCompanyName.setText(company.name)
        // binding.editTextCompanyLocation.setText(company.location) // Si tienes un campo location en el XML y clase Company

        // Las siguientes líneas están comentadas porque las comentaste en tu código
        // binding.editTextCompanyIndustry.setText(company.industry)
        // binding.editTextCompanyDescription.setText(company.description)
        // binding.editTextCompanyWebsite.setText(company.website)
        // binding.editTextCompanyEmail.setText(company.email)
        // binding.editTextCompanyPhone.setText(company.phone)

        // company.address?.let {
        //     binding.editTextCompanyAddressStreet.setText(it.street)
        //     // ... etc.
        // }

        // binding.editTextCompanyLatitude.setText(company.latitude?.toString() ?: "")
        // binding.editTextCompanyLongitude.setText(company.longitude?.toString() ?: "")

        if (!company.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(company.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.default_header_placeholder) // Cambia si es necesario
                .error(R.drawable.ic_company_photo) // Asegúrate de tener este drawable
                .into(binding.imageViewEditCompanyProfilePicture)
        }
    }

    private fun saveCompanyProfileChanges() {
        val companyName = binding.editTextCompanyName.text.toString().trim()
        // val location = binding.editTextCompanyLocation.text.toString().trim() // Si lo usas

        // --- VALIDACIONES ---
        if (companyName.isEmpty()) {
            binding.textInputLayoutCompanyName.error = "El nombre de la empresa es obligatorio."
            return
        } else {
            binding.textInputLayoutCompanyName.error = null
        }

// --- NO SE USAN LOS SIGUIENTES CAMPOS SEGÚN TUS COMENTARIOS ---
// val industry = binding.editTextCompanyIndustry.text.toString().trim()
// val description = binding.editTextCompanyDescription.text.toString().trim()
// val website = binding.editTextCompanyWebsite.text.toString().trim()
// val email = binding.editTextCompanyEmail.text.toString().trim()
// val phone = binding.editTextCompanyPhone.text.toString().trim()
// val street = binding.editTextCompanyAddressStreet.text.toString().trim()
// ... otros campos de dirección ...
// val latitudeStr = binding.editTextCompanyLatitude.text.toString().trim()
// val longitude// val latitudeStr = binding.editTextCompanyLatitude.text.toString().trim()
//        // val longitudeStr = binding.editTextCompanyLongitude.text.toString().trim()
//
//        // --- VALIDACIÓN DE EMAIL (EJEMPLO SI LO USARAS) ---
//        // if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//        //     binding.textInputLayoutCompanyEmail.error = "Formato de email inválido."
//        //     return
//        // } else {
//        //     binding.textInputLayoutCompanyEmail.error = null
//        // }
//
//        // --- VALIDACIÓN DE LATITUD/LONGITUD (COMENTADA PORQUE NO LA USAS) ---
//        // val latitude = latitudeStr.toDoubleOrNull()
//        // val longitude = longitudeStr.toDoubleOrNull()
//        //
//        // var latLongError = false
//        // if (latitudeStr.isNotEmpty() && latitude == null) {
//        //     binding.textInputLayoutCompanyLatitude.error = "Inválido"
//        //     latLongError = true
//        // } else {
//        //     binding.textInputLayoutCompanyLatitude.error = null
//        // }
//        //
//        // if (longitudeStr.isNotEmpty() && longitude == null) {
//        //     binding.textInputLayoutCompanyLongitude.error = "Inválido"
//        //     latLongError = true
//        // } else {
//        //     binding.textInputLayoutCompanyLongitude.error = null
//        // }
//        //
//        // if (latLongError) {
//        //     Toast.makeText(this, "Latitud o Longitud inválida.", Toast.LENGTH_SHORT).show()
//        //     return
//        // }
//
        binding.progressBarEditCompanyProfile.visibility = View.VISIBLE
        binding.buttonSaveCompanyProfileChanges.isEnabled = false

        // Si se seleccionó una nueva imagen, súbela primero
        if (imageUri != null) {
            val fileReference = storageReference.child("${companyId}_${System.currentTimeMillis()}.jpg")
            fileReference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // Ajusta la llamada a updateCompanyData según los campos que realmente uses
                        updateCompanyData(companyName, /* location, */ imageUrl)
                    }.addOnFailureListener { e ->
                        binding.progressBarEditCompanyProfile.visibility = View.GONE
                        binding.buttonSaveCompanyProfileChanges.isEnabled = true
                        Toast.makeText(this, "Error al obtener URL de descarga: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error al obtener URL de descarga: ", e)
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBarEditCompanyProfile.visibility = View.GONE
                    binding.buttonSaveCompanyProfileChanges.isEnabled = true
                    Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error al subir imagen: ", e)
                }
        } else {
            // Si no se cambió la imagen, usa la URL existente
            // Ajusta la llamada a updateCompanyData según los campos que realmente uses
            updateCompanyData(companyName, /* location, */ currentCompany?.profileImageUrl)
        }
    }

    // Modifica esta función para que solo acepte los parámetros que realmente vas a actualizar
    private fun updateCompanyData(
        name: String,
        // location: String?, // Si decides usar el campo location
        profileImageUrl: String?
    ) {
        val companyUpdates = mutableMapOf<String, Any?>()
        companyUpdates["name"] = name
        // companyUpdates["location"] = location // Si usas location
        companyUpdates["profileImageUrl"] = profileImageUrl
//
//        // Elimina las siguientes líneas si no usas estos campos en tu clase Company ni en Firebase
//        // companyUpdates["industry"] = industry // Ejemplo
//        // companyUpdates["description"] = description // Ejemplo
//        // companyUpdates["website"] = website // Ejemplo
//        // companyUpdates["email"] = email // Ejemplo
//        // companyUpdates["phone"] = phone // Ejemplo
//        // companyUpdates["address"] = null // O comenta si no tienes el campo address
//        // companyUpdates["latitude"] = null // O comenta si no tienes el campo latitude
//        // companyUpdates["longitude"] = null // O comenta si no tienes el campo longitude
//        // companyUpdates["lastUpdatedTimestamp"] = System.currentTimeMillis() // Útil para saber cuándo fue la última actualización
//
        databaseReference.updateChildren(companyUpdates)
            .addOnSuccessListener {
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                binding.buttonSaveCompanyProfileChanges.isEnabled = true
                Toast.makeText(this, "Perfil de empresa actualizado.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Perfil de empresa actualizado para $companyId")
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                binding.buttonSaveCompanyProfileChanges.isEnabled = true
                Toast.makeText(this, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al actualizar perfil: ", e)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}