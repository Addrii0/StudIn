package com.example.studin.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.studin.R // Importa tu clase R
import com.example.studin.adapters.OfferAdapter // Necesitarás un adaptador para las ofertas
import com.example.studin.classes.Company // Tu clase Company
import com.example.studin.classes.Offer // Tu clase Offer
import com.example.studin.databinding.ActivityCompanyProfileBinding // ViewBinding
import com.google.firebase.database.*

class CompanyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var companyReference: DatabaseReference
    private lateinit var offersReference: DatabaseReference

    private lateinit var offerAdapter: OfferAdapter // Adaptador para el RecyclerView de ofertas
    private val companyOffersList = mutableListOf<Offer>()

    private var companyId: String? = null

    private val TAG = "CompanyProfileActivity"

    companion object {
        const val EXTRA_COMPANY_ID = "company_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbarCompanyProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // El título se establecerá dinámicamente con el nombre de la empresa

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance()

        // Obtener el ID de la empresa del Intent
        companyId = intent.getStringExtra(EXTRA_COMPANY_ID)

        if (companyId == null) {
            Toast.makeText(this, "Error: No se proporcionó ID de la empresa.", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "No se recibió companyId en el Intent.")
            finish()
            return
        }

        companyReference = database.getReference("companies").child(companyId!!)
        offersReference = database.getReference("offers") // Referencia a todas las ofertas

        setupRecyclerView()
        loadCompanyData()
        loadCompanyOffers()

        binding.buttonSendMessageToCompany.setOnClickListener {
            //
            // Por ahora, un placeholder:
            Toast.makeText(this, "Funcionalidad de chat próximamente.", Toast.LENGTH_SHORT).show()
            // Podrías pasar el companyId y companyName a una futura ChatActivity
            // val intent = Intent(this, ChatActivity::class.java)
            // intent.putExtra(ChatActivity.EXTRA_RECIPIENT_ID, companyId)
            // intent.putExtra(ChatActivity.EXTRA_RECIPIENT_NAME, binding.collapsingToolbarCompany.title.toString())
            // intent.putExtra(ChatActivity.EXTRA_RECIPIENT_TYPE, "company") // Para diferenciar
            // startActivity(intent)
        }
        binding.buttonEditCompanyProfile.setOnClickListener {
            val intent = Intent(this, EditCompanyProfileActivity::class.java)
            startActivity(intent)
        }

        // Placeholder para la sección del mapa
        binding.mapViewContainer.setOnClickListener {
            val companyAddress = binding.textViewCompanyAddress.text.toString()
            if (companyAddress.isNotEmpty()) {
                // Intenta abrir la dirección en Google Maps (o cualquier app de mapas)
                // Esto es un ejemplo simple, necesitarías lat/lon para una integración de mapa en la app
                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(companyAddress)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(
                        this,
                        "No se encontró una aplicación de mapas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        offerAdapter = OfferAdapter(companyOffersList) { offer -> // Ahora esto es válido
            val intent = Intent(this, UserOfferInfoActivity::class.java)
            if (offer.id.isNullOrEmpty()) {
                Toast.makeText(this, "ID de la oferta no disponible.", Toast.LENGTH_SHORT).show()
                return@OfferAdapter // Sale de la lambda del adaptador
            }
            intent.putExtra(UserOfferInfoActivity.EXTRA_OFFER_ID, offer.id)
            startActivity(intent)
            // El Toast aquí es opcional, ya que estás navegando
            // Toast.makeText(this, "Clic en oferta: ${offer.title}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewCompanyOffers.apply {
            layoutManager = LinearLayoutManager(this@CompanyProfileActivity)
            adapter = offerAdapter
            isNestedScrollingEnabled = false
        }
    }
    private fun loadCompanyData() {
        // Opcional: Mostrar un ProgressBar mientras carga
        // binding.progressBarCompany.visibility = View.VISIBLE

        companyReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // binding.progressBarCompany.visibility = View.GONE
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
                    // Podrías cerrar la actividad o mostrar un estado de error más elaborado
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // binding.progressBarCompany.visibility = View.GONE
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
        // También puedes poner el nombre en un TextView dentro del LinearLayout si lo prefieres

        if (!company.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(company.profileImageUrl)
                .centerCrop()
                .placeholder(R.drawable.default_header_placeholder) // Debes tener este drawable
                .error(R.drawable.ic_company_photo) // Debes tener este drawable
                .into(binding.imageViewCompanyProfileHeader)
        } else {
            // Imagen por defecto si no hay URL
            binding.imageViewCompanyProfileHeader.setImageResource(R.drawable.default_header_placeholder)
        }

//        binding.textViewCompanyIndustry.text = if (!company.industry.isNullOrEmpty()) "Industria: ${company.industry}" else "Industria no especificada"
//        binding.textViewCompanyDescription.text = company.description ?: "Descripción no disponible."
//
//        binding.textViewCompanyWebsite.text = company.website ?: "Sitio web no disponible"
//        binding.textViewCompanyEmail.text = company.email ?: "Email no disponible"
//        binding.textViewCompanyPhone.text = company.phone ?: "Teléfono no disponible"
//
//        val address = company.address
//        if (address != null) {
//            val fullAddress = listOfNotNull(address.street, address.city, address.state, address.postalCode, address.country)
//                .joinToString(", ")
//                .ifEmpty { "Dirección no disponible" }
//            binding.textViewCompanyAddress.text = fullAddress
//        } else {
//            binding.textViewCompanyAddress.text = "Dirección no disponible"
//        }
        Log.d(TAG, "Datos de la empresa '${company.name}' mostrados.")
    }


    private fun loadCompanyOffers() {
        // Aquí asumimos que cada 'Offer' tiene un campo 'companyId' que coincide con el ID de esta empresa.
        // También, si la empresa tuviera una lista de 'offerIds' en su objeto Company,
        // podrías iterar sobre esos IDs y hacer N consultas individuales a la referencia "offers".
        // Sin embargo, filtrar por 'companyId' en la colección "offers" suele ser más eficiente
        // si tienes muchos offers por empresa.

        // Muestra un indicador de carga si lo deseas
        // binding.progressBarOffers.visibility = View.VISIBLE

        val query = offersReference.orderByChild("companyId").equalTo(companyId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                companyOffersList.clear()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        if (offer != null) {
                            // Opcional: podrías querer añadir el ID de la oferta al objeto si no está ya
                            // offer.id = offerSnapshot.key
                            companyOffersList.add(offer)
                        }
                    }
                    Log.d(TAG, "Se encontraron ${companyOffersList.size} ofertas para la empresa $companyId.")
                } else {
                    Log.d(TAG, "No se encontraron ofertas para la empresa $companyId.")
                    // Puedes mostrar un mensaje de "No hay ofertas publicadas" en la UI
                    // binding.textViewNoOffers.visibility = View.VISIBLE
                }
                offerAdapter.notifyDataSetChanged()
                // Oculta el indicador de carga
                // binding.progressBarOffers.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Oculta el indicador de carga
                // binding.progressBarOffers.visibility = View.GONE
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