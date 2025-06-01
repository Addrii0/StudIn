package com.example.studin.activities

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.adapters.ApplicantsAdapter
import com.example.studin.classes.Offer
import com.example.studin.classes.User
import com.example.studin.databinding.ActivityCompanyOfferInfoBinding
import com.google.firebase.database.*
import java.util.Date
import java.util.Locale

class CompanyOfferInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompanyOfferInfoBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var offersReference: DatabaseReference // Para la oferta
    private lateinit var offerApplicationsRef: DatabaseReference // Para los aplicantes de ESTA oferta
    private lateinit var usersReference: DatabaseReference // Para obtener perfiles de usuario

    private lateinit var applicantsAdapter: ApplicantsAdapter
    private val applicantProfilesList = mutableListOf<User>()
    // Para mapear userId a detalles de aplicación si es necesario (ej. estado)
    // private val applicationDetailsMap = mutableMapOf<String, ApplicationDetails>()

    private val TAG = "CompanyOfferInfo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyOfferInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        offersReference = database.getReference("offers")
        usersReference = database.getReference("users")

        setupRecyclerView()

        val offerId = intent.getStringExtra("SELECTED_OFFER_ID")
        if (offerId == null) {
            Log.e(TAG, "No se recibió el ID de la oferta.")
            Toast.makeText(this, "Error: No se pudo cargar la oferta.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        offerApplicationsRef = database.getReference("offerApplications").child(offerId)

        Log.d(TAG, "Cargando datos para la oferta ID: $offerId")
        // Asume que tienes un ProgressBar para los detalles de la oferta
        // binding.progressBarOfferDetails.visibility = View.VISIBLE
        // Asume que tienes un ProgressBar para la lista de aplicantes
        // binding.progressBarApplicants.visibility = View.VISIBLE


        loadOfferDetails(offerId)
        loadOfferApplicantsUids() // Cambiado para primero obtener UIDs
    }

    private fun setupRecyclerView() {
        applicantsAdapter = ApplicantsAdapter(applicantProfilesList) { selectedUser ->
            Log.d(TAG, "Usuario seleccionado: ${selectedUser.name}")
            val intent = Intent(this, UserProfileActivity::class.java) // Crea UserProfileActivity
            intent.putExtra("SELECTED_USER_PROFILE", selectedUser)
            startActivity(intent)
            Toast.makeText(this, "Perfil de: ${selectedUser.name}", Toast.LENGTH_SHORT).show() // Temporal
        }
        binding.offersRecyclerViewApplicants.apply { // Usa el ID de tu RecyclerView en el XML
            layoutManager = LinearLayoutManager(this@CompanyOfferInfoActivity)
            adapter = applicantsAdapter
            // Opcional: añadir un divisor
            // addItemDecoration(DividerItemDecoration(this@CompanyOfferInfoActivity, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadOfferDetails(offerId: String) {
        offersReference.child(offerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // binding.progressBarOfferDetails.visibility = View.GONE

                if (snapshot.exists()) {
                    val offer = snapshot.getValue(Offer::class.java)
                    if (offer != null) {
                        populateUIWithOfferData(offer)
                    } else {
                        Log.e(TAG, "Error al deserializar la oferta desde Firebase.")
                        Toast.makeText(this@CompanyOfferInfoActivity, "No se pudieron cargar los detalles.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "La oferta con ID $offerId no existe.")
                    Toast.makeText(this@CompanyOfferInfoActivity, "Oferta no disponible.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // binding.progressBarOfferDetails.visibility = View.GONE
                Log.e(TAG, "Error al leer datos de la oferta: ${error.message}")
                Toast.makeText(this@CompanyOfferInfoActivity, "Error al cargar oferta: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun populateUIWithOfferData(offer: Offer) {
        Log.d(TAG, "Rellenando UI con datos de la oferta: ${offer.title}")
        binding.textViewOfferTitleInfo.text = offer.title ?: "N/A"
        binding.textViewOfferDescriptionInfo.text = offer.description ?: "No hay descripción disponible."
        binding.textViewOfferLocationInfo.text = offer.location ?: "Ubicación no especificada."
        binding.textViewOfferSkillsInfo.text = offer.skills ?: "No hay habilidades requeridas." // Asumiendo que Offer tiene 'skills'
        // binding.textViewOfferRequirementsInfo.text = offer.requirements ?: "No especificados" // Si tienes este campo

        offer.fechaPublicacion?.let { timestamp -> // Asegúrate que tu clase Offer tenga 'fechaPublicacion'
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textViewOfferDateInfo.text = "Publicado el: " + sdf.format(Date(timestamp))
        } ?: run {
            binding.textViewOfferDateInfo.text = "Fecha de publicación no disponible."
        }
    }

    private fun loadOfferApplicantsUids() {
        // binding.progressBarApplicants.visibility = View.VISIBLE
        applicantProfilesList.clear() // Limpiar lista antes de cargar nuevos datos
        // applicationDetailsMap.clear() // Limpiar si lo usas

        offerApplicationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val applicantUids = mutableListOf<String>()
                if (!snapshot.exists()) {
                    Log.d(TAG, "No hay aplicantes para esta oferta.")
                    // binding.progressBarApplicants.visibility = View.GONE
                    binding.textViewNoApplicantsMessage.visibility = View.VISIBLE // Añade un TextView para este mensaje
                    applicantsAdapter.updateData(emptyList()) // Actualizar adaptador con lista vacía
                    return
                }
                binding.textViewNoApplicantsMessage.visibility = View.GONE

                for (applicantSnapshot in snapshot.children) {
                    applicantSnapshot.key?.let { userId ->
                        applicantUids.add(userId)
                        // Si guardas detalles de la aplicación como 'status' o 'appliedAt':
                        // val details = applicantSnapshot.getValue(ApplicationDetails::class.java)
                        // details?.let { applicationDetailsMap[userId] = it }
                    }
                }
                if (applicantUids.isNotEmpty()) {
                    loadApplicantProfiles(applicantUids)
                } else {
                    // binding.progressBarApplicants.visibility = View.GONE
                    applicantsAdapter.updateData(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // binding.progressBarApplicants.visibility = View.GONE
                Log.e(TAG, "Error al cargar UIDs de aplicantes: ${error.message}")
                Toast.makeText(this@CompanyOfferInfoActivity, "Error al cargar solicitantes.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ... (código anterior de CompanyOfferInfoActivity.kt) ...

    private fun loadApplicantProfiles(uids: List<String>) {
        // Esta función se llama después de obtener todos los UIDs de los aplicantes.
        // Limpiamos la lista actual del adaptador antes de llenarla con nuevos perfiles.
        val tempList = mutableListOf<User>() // Lista temporal para acumular perfiles
        var lookupsRemaining = uids.size

        // Si no hay UIDs, no hay nada que cargar.
        if (lookupsRemaining == 0) {
            // binding.progressBarApplicants.visibility = View.GONE
            applicantsAdapter.updateData(tempList) // Actualiza con la lista vacía
            // Considera mostrar un mensaje si la lista está vacía, aunque ya se maneja en loadOfferApplicantsUids
            return
        }

        uids.forEach { userId ->
            usersReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userProfile = snapshot.getValue(User::class.java)
                    if (userProfile != null) {
                        // Importante: Firebase no almacena el UID dentro del objeto User por defecto.
                        // Si necesitas el UID dentro del objeto User (por ejemplo, para pasarlo al hacer clic),
                        // y no lo estás guardando explícitamente al crear el usuario, debes asignarlo aquí.
                        // Sin embargo, tu clase User ya tiene un campo 'uid' (si la definición que me pasaste antes es la actual)
                        // Si tu clase User no tiene `uid` y lo necesitas: userProfile.uid = userId (necesitarías `var uid` en User)

                        // Si guardaste el uid al crear el usuario y tu clase User tiene un campo uid:
                        // userProfile.uid = snapshot.key // Esto es redundante si userId es el key y ya tienes un campo uid en User que se llena al deserializar.
                        // Asegúrate que tu clase User tenga un campo uid y que Firebase lo esté poblando.
                        // Si `User` no tiene `uid`, y necesitas pasarlo, puedes hacer un Pair(userId, userProfile)
                        // o modificar tu clase `User` para incluir un `var uid: String? = null`.
                        // Por ahora, asumiré que tu clase `User` SÍ tiene un campo `uid` que Firebase puede poblar,
                        // o que el `userId` que tenemos es suficiente para la acción de clic.

                        tempList.add(userProfile)
                    } else {
                        Log.w(TAG, "No se encontró perfil para el UID: $userId")
                        // Podrías añadir un usuario placeholder o simplemente omitirlo
                    }

                    lookupsRemaining--
                    if (lookupsRemaining == 0) {
                        // Todas las búsquedas de perfiles han terminado
                        // binding.progressBarApplicants.visibility = View.GONE
                        applicantProfilesList.clear()
                        applicantProfilesList.addAll(tempList)
                        applicantsAdapter.notifyDataSetChanged() // O usar applicantsAdapter.updateData(tempList)

                        if (applicantProfilesList.isEmpty() && uids.isNotEmpty()) {
                            Log.d(TAG, "No se cargaron perfiles aunque se encontraron UIDs.")
                            binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
                        } else if (applicantProfilesList.isEmpty()) {
                            binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
                        }
                        else {
                            binding.textViewNoApplicantsMessage.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al cargar perfil para UID $userId: ${error.message}")
                    lookupsRemaining--
                    if (lookupsRemaining == 0) {
                        // binding.progressBarApplicants.visibility = View.GONE
                        applicantProfilesList.clear()
                        applicantProfilesList.addAll(tempList) // Añadir los que sí se cargaron
                        applicantsAdapter.notifyDataSetChanged()

                        if (applicantProfilesList.isEmpty()) {
                            binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
                        } else {
                            binding.textViewNoApplicantsMessage.visibility = View.GONE
                        }
                    }
                    // Considera mostrar un mensaje de error parcial
                }
            })
        }
    }
} // Fin de la clase CompanyOfferInfoActivity