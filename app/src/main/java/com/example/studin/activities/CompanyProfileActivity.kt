package com.example.studin.activities

import OfferAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityCompanyProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CompanyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var companyReference: DatabaseReference
    private lateinit var offersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var offerAdapter: OfferAdapter
    private val companyOffersList = mutableListOf<Offer>()

    private var companyId: String? = null
    private var companyProfileImageUrl: String? = null
    private val TAG = "CompanyProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbarCompanyProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        // Obtener el ID de la empresa del Intent si entra desde busqueda de empresas
        companyId = intent.getStringExtra("COMPANY_ID")
        companyProfileImageUrl = intent.getStringExtra("COMPANY_PROFILE_IMAGE_URL")

        if(companyId == null){
            companyId = auth.currentUser?.uid
            Log.e(TAG, "No se recibió companyId en el Intent.")
        }

        if (companyId == null) {
                Toast.makeText(this, "Error: No se identificar a la empresa.", Toast.LENGTH_LONG)
                .show()
                Log.e(TAG, "No se recibió companyId en el Intent.")
                finish()
                return
        }
        companyReference = database.getReference("companies").child(companyId!!)
        offersReference = database.getReference("offers")
        val currentCompanyId = companyReference.key
        if( currentCompanyId == auth.currentUser?.uid){
            binding.buttonEditCompanyProfile.visibility = android.view.View.VISIBLE
            Log.e(TAG, "Es la empresa actual.")
        }else{
            binding.buttonEditCompanyProfile.visibility = android.view.View.GONE
            Log.e(TAG, "No es la empresa actual.")
        }

        setupRecyclerView()
        loadCompanyData()
        loadCompanyOffers()


        binding.buttonSendMessageToCompany.setOnClickListener {
            Toast.makeText(this, "Iniciando chat con la empresa...", Toast.LENGTH_SHORT).show()

            val companyIdFromProfile = companyId // ID de la empresa actual
            val companyNameFromProfile = binding.collapsingToolbarCompany.title.toString()

            if (companyIdFromProfile == null || companyIdFromProfile.isEmpty()) {
                Toast.makeText(this, "ID de empresa no disponible.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "companyId es nulo o vacío al intentar iniciar chat.")
                return@setOnClickListener
            }

            // Iniciar MainChatsActivity para que maneje la creación del chat
            val intent = Intent(this, MainChatsActivity::class.java)
            intent.putExtra("ACTION_START_CHAT_WITH_USER_ID", companyIdFromProfile)
            intent.putExtra("ACTION_START_CHAT_WITH_USER_NAME", companyNameFromProfile)
            intent.putExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL", companyProfileImageUrl)
            startActivity(intent)
        }

        binding.buttonEditCompanyProfile.setOnClickListener {
            val intent = Intent(this, EditCompanyProfileActivity::class.java)
            startActivity(intent)
        }

        //Implementación de mapa(futuro desarrollo)
//        // Placeholder para la sección del mapa
//        binding.mapViewContainer.setOnClickListener {
//            val companyAddress = binding.textViewCompanyAddress.text.toString()
//            if (companyAddress.isNotEmpty()) {
//                // Intenta abrir la dirección en Google Maps
//                // Necesito lat/lon para una integración de mapa en la app
//                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(companyAddress)}")
//                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
//                mapIntent.setPackage("com.google.android.apps.maps")
//                if (mapIntent.resolveActivity(packageManager) != null) {
//                    startActivity(mapIntent)
//                } else {
//                    Toast.makeText(
//                        this,
//                        "No se encontró una aplicación de mapas.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
    }

    private fun setupRecyclerView() {
        offerAdapter = OfferAdapter(companyOffersList) { offer -> // Ahora esto es válido
            val intent = Intent(this, UserOfferInfoActivity::class.java)
            if (offer.id.isNullOrEmpty()) {
                Toast.makeText(this, "ID de la oferta no disponible.", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Intento de abrir oferta sin ID. Objeto oferta completo: $offer")
                return@OfferAdapter // Sale de la lambda del adaptador
            }
            intent.putExtra(UserOfferInfoActivity.EXTRA_OFFER_ID, offer.id)
            startActivity(intent)
        }
        binding.recyclerViewCompanyOffers.apply {
            layoutManager = LinearLayoutManager(this@CompanyProfileActivity)
            adapter = offerAdapter
            isNestedScrollingEnabled = false
        }
    }
    private fun loadCompanyData() {
        companyReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val company = snapshot.getValue(Company::class.java)
                if (company != null) {
                    displayCompanyData(company)
                } else {
                    Toast.makeText(
                        this@CompanyProfileActivity,
                        "No se encontró la empresa.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.w(TAG, "Empresa con ID $companyId no encontrada.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@CompanyProfileActivity,
                    "Error al cargar datos de la empresa: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error al cargar datos de la empresa: ", error.toException())
            }
        })
    }


    private fun displayCompanyData(company: Company) {
        binding.collapsingToolbarCompany.title = company.name ?: "Nombre no disponible"

        if (!company.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(company.profileImageUrl)
                .centerCrop()
                .placeholder(R.drawable.default_header_placeholder)
                .error(R.drawable.ic_company_photo)
                .into(binding.imageViewCompanyProfileHeader)
        } else {
            // Imagen por defecto si no hay URL
            binding.imageViewCompanyProfileHeader.setImageResource(R.drawable.default_header_placeholder)
        }

        binding.textViewCompanyIndustry.text = if (!company.industry.isNullOrEmpty()) "Industria: ${company.industry}" else "Industria no especificada"
        binding.textViewCompanyDescription.text = company.description ?: "Descripción no disponible."

        binding.textViewCompanyWebsite.text = company.website ?: "Sitio web no disponible"
        binding.textViewCompanyEmail.text = company.email ?: "Email no disponible"
        binding.textViewCompanyPhone.text = company.phone ?: "Teléfono no disponible"

        Log.d(TAG, "Datos de la empresa '${company.name}' mostrados.")
    }


    private fun loadCompanyOffers() {
        val query = offersReference.orderByChild("companyId").equalTo(companyId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                companyOffersList.clear()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        if (offer != null) {
                            offer.id = offerSnapshot.key
                            companyOffersList.add(offer)
                            Log.d(TAG, "Oferta añadida: ${offer.title}, ID asignado: ${offer.id}") // Log para confirmar
                        } else {
                            Log.w(TAG, "No se pudo deserializar la oferta desde snapshot con clave: ${offerSnapshot.key}")
                        }
                    }
                    Log.d(TAG, "Se encontraron ${companyOffersList.size} ofertas para la empresa $companyId.")
                } else {
                    Log.d(TAG, "No se encontraron ofertas para la empresa $companyId.")
                }
                offerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CompanyProfileActivity, "Error al cargar ofertas: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al cargar ofertas de la empresa: ", error.toException())
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar el botón de atrás en la Toolbar
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}