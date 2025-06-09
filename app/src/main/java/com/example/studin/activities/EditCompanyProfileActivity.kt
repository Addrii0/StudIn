package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company
import com.example.studin.databinding.ActivityCompanyEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class EditCompanyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private var currentCompany: Company? = null
    private var imageUri: Uri? = null
    private var companyId: String? = null
    private val TAG = "EditCompanyProfileAct"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data!!.data != null) {
                imageUri = result.data!!.data
                Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .placeholder(R.drawable.default_header_placeholder)
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
        val currentUser: FirebaseUser? = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "onCreate: Usuario no autenticado (currentUser es nulo). Finalizando actividad.")
            val intent = Intent(this, LoginActivity::class.java) // Asegúrate que LoginActivity exista
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        companyId = currentUser.uid
        Log.d(TAG, "onCreate: Usuario autenticado con companyId (UID): $companyId")

        // companyId no será nulo aquí debido a la verificación anterior.
        databaseReference = FirebaseDatabase.getInstance().getReference("companies").child(companyId!!)
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
        // companyId ya está verificado en onCreate
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
        binding.editTextCompanyName.setText(company.name ?: "")
        binding.editTextCompanyIndustry.setText(company.industry ?: "")
        binding.editTextCompanyDescription.setText(company.description ?: "")
        binding.editTextCompanyWebsite.setText(company.website ?: "")
        binding.editTextCompanyEmail.setText(company.email ?: "")
        binding.editTextCompanyPhone.setText(company.phone ?: "")


//        company.address?.let { addr ->
//            binding.editTextCompanyAddressStreet.setText(addr.street ?: "")
//            binding.editTextCompanyAddressCity.setText(addr.city ?: "")
//            binding.editTextCompanyAddressState.setText(addr.state ?: "")
//            binding.editTextCompanyAddressPostalCode.setText(addr.postalCode ?: "")
//            binding.editTextCompanyAddressCountry.setText(addr.country ?: "")
//        }
        // Si son campos directos en Company:
        // binding.editTextCompanyAddressStreet.setText(company.street ?: "")
        // binding.editTextCompanyAddressCity.setText(company.city ?: "")
        // ...y así sucesivamente...

//        binding.editTextCompanyLatitude.setText(company.latitude?.toString() ?: "")
//        binding.editTextCompanyLongitude.setText(company.longitude?.toString() ?: "")

        if (!company.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(company.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.default_header_placeholder)
                .error(R.drawable.ic_company_photo) // Asegúrate que este drawable existe
                .into(binding.imageViewEditCompanyProfilePicture)
        } else {
            Glide.with(this)
                .load(R.drawable.default_header_placeholder)
                .circleCrop()
                .into(binding.imageViewEditCompanyProfilePicture)
        }
    }

    private fun saveCompanyProfileChanges() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "saveCompanyProfileChanges: Usuario no autenticado. No se pueden guardar cambios.")
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            // Habilitar botón y ocultar progreso si se interrumpe
            binding.buttonSaveCompanyProfileChanges.isEnabled = true
            binding.progressBarEditCompanyProfile.visibility = View.GONE
            return
        }
        companyId = currentUser.uid // Re-asignar por si acaso, aunque no
        // --- RECOGER DATOS DE LOS EDITTEXT ---
        val companyName = binding.editTextCompanyName.text.toString().trim()
        val industry = binding.editTextCompanyIndustry.text.toString().trim()
        val description = binding.editTextCompanyDescription.text.toString().trim()
        val website = binding.editTextCompanyWebsite.text.toString().trim()
        val email = binding.editTextCompanyEmail.text.toString().trim()
        val phone = binding.editTextCompanyPhone.text.toString().trim()
//        val street = binding.editTextCompanyAddressStreet.text.toString().trim()
//        val city = binding.editTextCompanyAddressCity.text.toString().trim()
//        val state = binding.editTextCompanyAddressState.text.toString().trim()
//        val postalCode = binding.editTextCompanyAddressPostalCode.text.toString().trim()
//        val country = binding.editTextCompanyAddressCountry.text.toString().trim()
//        val latitudeStr = binding.editTextCompanyLatitude.text.toString().trim()
//        val longitudeStr = binding.editTextCompanyLongitude.text.toString().trim()

        var isValid = true
        if (companyName.isEmpty()) {
            binding.textInputLayoutCompanyName.error = "El nombre de la empresa es obligatorio."
            isValid = false
        } else {
            binding.textInputLayoutCompanyName.error = null
        }

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutCompanyEmail.error = "Formato de email inválido."
            isValid = false
        } else {
            binding.textInputLayoutCompanyEmail.error = null
        }

        if (website.isNotEmpty() && !Patterns.WEB_URL.matcher(website).matches()) {

            binding.textInputLayoutCompanyWebsite.error = "Formato de sitio web inválido."
            isValid = false
        } else {
            binding.textInputLayoutCompanyWebsite.error = null
        }

//        val latitude = if (latitudeStr.isNotEmpty()) latitudeStr.toDoubleOrNull() else null
//        val longitude = if (longitudeStr.isNotEmpty()) longitudeStr.toDoubleOrNull() else null
//
//        if (latitudeStr.isNotEmpty() && latitude == null) {
//            binding.textInputLayoutCompanyLatitude.error = "Latitud inválida"
//            isValid = false
//        } else {
//            binding.textInputLayoutCompanyLatitude.error = null
//        }
//
//        if (longitudeStr.isNotEmpty() && longitude == null) {
//            binding.textInputLayoutCompanyLongitude.error = "Longitud inválida"
//            isValid = false
//        } else {
//            binding.textInputLayoutCompanyLongitude.error = null
//        }


        if (!isValid) {
            Toast.makeText(this, "Por favor, corrige los errores.", Toast.LENGTH_SHORT).show()
            return // Detener si hay errores de validación
        }

        binding.progressBarEditCompanyProfile.visibility = View.VISIBLE
        binding.buttonSaveCompanyProfileChanges.isEnabled = false



//        val addressData = Company.Address( // Asumiendo que Company.Address es una data class
//            street = street.ifEmpty { null },
//            city = city.ifEmpty { null },
//            state = state.ifEmpty { null },
//            postalCode = postalCode.ifEmpty { null },
//            country = country.ifEmpty { null }
//        )


        if (imageUri != null) {
            // UID del company ya verificado y no nulo
            val currentUid = companyId!!
            val fileName = "profile_pic.jpg" // Nombre fijo para la imagen de perfil
            val storagePath = "profile_images/companies/$currentUid/$fileName"

            Log.d(TAG, "Intentando subir imagen a Storage en la ruta: $storagePath")
            val fileReference = FirebaseStorage.getInstance().getReference(storagePath)

            fileReference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Imagen subida exitosamente a: ${taskSnapshot.storage.path}")
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        Log.d(TAG, "URL de descarga obtenida: $imageUrl")
                        updateCompanyData(
                            name = companyName,
                            industry = industry.ifEmpty { null },
                            description = description.ifEmpty { null },
                            website = website.ifEmpty { null },
                            email = email.ifEmpty { null },
                            phone = phone.ifEmpty { null },
//                            address = addressData, // Pasa el objeto Address
//                            latitude = latitude,
//                            longitude = longitude,
                            profileImageUrl = imageUrl,
                        )
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
                    Log.e(TAG, "Error al subir imagen a Storage (${e.javaClass.simpleName}): ${e.message}", e)
                    Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_LONG).show()
                }
                .addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                    Log.d(TAG, "Progreso de subida: $progress%")
                }
        } else {
            // Si no se cambió la imagen, usa la URL existente
            updateCompanyData(
                name = companyName,
                industry = industry.ifEmpty { null },
                description = description.ifEmpty { null },
                website = website.ifEmpty { null },
                email = email.ifEmpty { null },
                phone = phone.ifEmpty { null },
//                address = addressData,
//                latitude = latitude,
//                longitude = longitude,
                profileImageUrl = currentCompany?.profileImageUrl // Usa la URL existente si no se cambió la imagen
            )
        }
    }

    // Modifica la firma de updateCompanyData para aceptar todos los campos
    private fun updateCompanyData(
        name: String,
        industry: String?,
        description: String?,
        website: String?,
        email: String?,
        phone: String?,
//        address: Company.Address?, // Asumiendo que Company.Address es tu clase para dirección
//        latitude: Double?,
//        longitude: Double?,
        profileImageUrl: String?
    ) {
        val currentUid = companyId ?: run {
            Log.e(TAG, "updateCompanyData: companyId es nulo. No se puede actualizar Realtime Database.")
            Toast.makeText(this, "Error de autenticación al guardar datos.", Toast.LENGTH_LONG).show()
            binding.progressBarEditCompanyProfile.visibility = View.GONE
            binding.buttonSaveCompanyProfileChanges.isEnabled = true
            return
        }

        val companyUpdates = mutableMapOf<String, Any?>()
        companyUpdates["name"] = name
        companyUpdates["industry"] = industry
        companyUpdates["description"] = description
        companyUpdates["website"] = website
        companyUpdates["email"] = email
        companyUpdates["phone"] = phone
        //companyUpdates["address"] = address // Guarda el objeto Address directamente
        //companyUpdates["latitude"] = latitude
        //companyUpdates["longitude"] = longitude
        // profileImageUrl ya se maneja (nueva o existente, o null si se borra)
        companyUpdates["profileImageUrl"] = profileImageUrl
        companyUpdates["lastUpdatedTimestamp"] = System.currentTimeMillis() // Útil


        Log.d(TAG, "Actualizando datos en Realtime Database para $currentUid con: $companyUpdates")

        databaseReference.updateChildren(companyUpdates)
            .addOnSuccessListener {
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                binding.buttonSaveCompanyProfileChanges.isEnabled = true
                Toast.makeText(this, "Perfil de empresa actualizado.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Perfil de empresa actualizado exitosamente para $currentUid")
                // Considera pasar datos actualizados a la actividad anterior si es necesario
                // setResult(Activity.RESULT_OK) // Si la actividad anterior espera un resultado
                finish()
            }
            .addOnFailureListener {
                binding.progressBarEditCompanyProfile.visibility = View.GONE
                binding.buttonSaveCompanyProfileChanges.isEnabled = true
                Toast.makeText(this, "Error al actualizar perfil en DB:", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al actualizar perfil en Realtime Database: ")
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