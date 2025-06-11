package com.example.studin.activities

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
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
    private lateinit var offersReference: DatabaseReference
    private lateinit var offerApplicationsRef: DatabaseReference
    private lateinit var usersReference: DatabaseReference

    private lateinit var applicantsAdapter: ApplicantsAdapter
    private val applicantProfilesList = mutableListOf<User>()
    private var currentApplicantUids = mutableListOf<String>()

    private val TAG = "CompanyOfferInfo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyOfferInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        offersReference = database.getReference("offers")
        usersReference = database.getReference("users")

        val offerId = intent.getStringExtra("SELECTED_OFFER_ID")
        if (offerId == null) {
            Log.e(TAG, "No se recibió el ID de la oferta.")
            Toast.makeText(this, "Error: No se pudo cargar la oferta.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        offerApplicationsRef = database.getReference("offerApplications").child(offerId)
        Log.d(TAG, "Cargando datos para la oferta ID: $offerId")

        setupRecyclerView()
        loadOfferDetails(offerId)
        loadOfferApplicantsUids()
    }

    private fun setupRecyclerView() {
        applicantsAdapter = ApplicantsAdapter(applicantProfilesList) { position ->
            if (position >= 0 && position < currentApplicantUids.size) {
                val selectedUserUid = currentApplicantUids[position]

                Log.d(TAG, "Ítem clickeado en posición: $position, UID correspondiente: $selectedUserUid")

                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("SELECTED_USER_ID", selectedUserUid)
                startActivity(intent)
                Toast.makeText(this, "Viendo perfil del aplicante...", Toast.LENGTH_SHORT).show()

            } else {
                Log.e(TAG, "Error: Posición inválida ($position) o UIDs no sincronizados.")
                Toast.makeText(this, "Error al seleccionar el usuario.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.offersRecyclerViewApplicants.apply {
            layoutManager = LinearLayoutManager(this@CompanyOfferInfoActivity)
            adapter = applicantsAdapter
            addItemDecoration(DividerItemDecoration(this@CompanyOfferInfoActivity, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadOfferDetails(offerId: String) {
        offersReference.child(offerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val offer = snapshot.getValue(Offer::class.java)
                    offer?.let { populateUIWithOfferData(it) }
                        ?: Log.e(TAG, "Error al deserializar la oferta desde Firebase.")
                } else {
                    Log.w(TAG, "La oferta con ID $offerId no existe.")
                    Toast.makeText(this@CompanyOfferInfoActivity, "Oferta no disponible.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de la oferta: ${error.message}")
            }
        })
    }

    private fun populateUIWithOfferData(offer: Offer) {
        binding.textViewOfferTitleInfo.text = offer.title ?: "N/A"
        binding.textViewOfferDescriptionInfo.text = offer.description ?: "No hay descripción disponible."
        binding.textViewOfferLocationInfo.text = offer.location ?: "Ubicación no especificada."
        binding.textViewOfferSkillsInfo.text = if (offer.skills.isNotEmpty()) offer.skills.joinToString(", ") else "No hay habilidades requeridas."
        binding.textViewOfferRequirementsInfo.text = if (offer.requirements.isNotEmpty()) offer.requirements.joinToString(", ") else "No hay requisitos adicionales."
        binding.textViewOfferTypeInfo.text = offer.type ?: "Tipo de oferta no especificado."
        offer.datePosted?.let {
            binding.textViewOfferDateInfo.text = "Publicado el: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
        } ?: run {
            binding.textViewOfferDateInfo.text = "Fecha de publicación no disponible."
        }
    }

    private fun loadOfferApplicantsUids() {
        currentApplicantUids.clear()
        applicantProfilesList.clear()
        applicantsAdapter.notifyDataSetChanged()
        binding.textViewNoApplicantsMessage.visibility = View.GONE

        offerApplicationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    Log.d(TAG, "No hay aplicantes para esta oferta.")
                    binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
                    return
                }

                val uidsFromDb = mutableListOf<String>()
                for (applicantSnapshot in snapshot.children) {
                    applicantSnapshot.key?.let { userId ->
                        uidsFromDb.add(userId)
                    }
                }

                if (uidsFromDb.isNotEmpty()) {
                    currentApplicantUids.addAll(uidsFromDb)
                    Log.d(TAG, "UIDs de aplicantes cargados: $currentApplicantUids")
                    loadApplicantProfilesByUids(currentApplicantUids)
                } else {
                    Log.d(TAG, "No se extrajeron UIDs válidos de la base de datos.")
                    binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar UIDs de aplicantes: ${error.message}")
                Toast.makeText(this@CompanyOfferInfoActivity, "Error al cargar solicitantes.", Toast.LENGTH_SHORT).show()
                binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
            }
        })
    }

    private fun loadApplicantProfilesByUids(uidsToLoad: List<String>) {
        applicantProfilesList.clear()

        if (uidsToLoad.isEmpty()) {
            Log.d(TAG, "loadApplicantProfilesByUids: La lista de UIDs para cargar está vacía.")
            applicantsAdapter.notifyDataSetChanged()
            updateNoApplicantsMessageVisibility()
            return
        }

        Log.d(TAG, "Iniciando carga de perfiles para ${uidsToLoad.size} UIDs.")
        val profilesMap = mutableMapOf<String, User?>() // Mapa para UID -> User, para luego ordenar
        var lookupsRemaining = uidsToLoad.size

        uidsToLoad.forEach { userId ->
            usersReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userProfile = snapshot.getValue(User::class.java)
                        if (userProfile != null) {
                            profilesMap[userId] = userProfile
                            Log.d(TAG, "Perfil cargado para UID: $userId, Nombre: ${userProfile.name}")
                        } else {
                            profilesMap[userId] = null
                            Log.w(TAG, "No se pudo deserializar perfil para UID: $userId, pero el nodo existe.")
                        }
                    } else {
                        profilesMap[userId] = null
                        Log.w(TAG, "No se encontró perfil en Firebase para UID: $userId")
                    }

                    lookupsRemaining--
                    if (lookupsRemaining == 0) {
                        applicantProfilesList.clear()
                        currentApplicantUids.forEach { uid ->
                            profilesMap[uid]?.let { user ->
                                applicantProfilesList.add(user)
                            }

                        }
                        Log.d(TAG, "Todos los perfiles procesados. Perfiles válidos cargados: ${applicantProfilesList.size}")
                        applicantsAdapter.notifyDataSetChanged()
                        updateNoApplicantsMessageVisibility()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al cargar perfil para UID $userId: ${error.message}")
                    profilesMap[userId] = null // Marcar como error
                    lookupsRemaining--
                    if (lookupsRemaining == 0) {
                        applicantProfilesList.clear()
                        currentApplicantUids.forEach { uid ->
                            profilesMap[uid]?.let { user ->
                                applicantProfilesList.add(user)
                            }
                        }
                        Log.d(TAG, "Todos los perfiles procesados (con posibles errores). Perfiles válidos cargados: ${applicantProfilesList.size}")
                        applicantsAdapter.notifyDataSetChanged()
                        updateNoApplicantsMessageVisibility()
                    }
                }
            })
        }
    }

    private fun updateNoApplicantsMessageVisibility() {
         if (applicantProfilesList.isEmpty()) {
            binding.textViewNoApplicantsMessage.visibility = View.VISIBLE
             if (currentApplicantUids.isNotEmpty()) {
                Log.d(TAG, "Hay UIDs de aplicantes pero no se cargaron perfiles válidos.")
            }
        } else {
            binding.textViewNoApplicantsMessage.visibility = View.GONE
        }
    }

}