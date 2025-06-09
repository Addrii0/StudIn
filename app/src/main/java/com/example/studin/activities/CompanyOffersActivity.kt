package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.R
import com.example.studin.adapters.OffersAdapter
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityCompanyOffersBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CompanyOffersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyOffersBinding

    private lateinit var offersAdapter: OffersAdapter
    private val offerList = mutableListOf<Offer>()

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        databaseReference = FirebaseDatabase.getInstance().getReference("offers")

        setupRecyclerView()
        fetchOffers()
    }

    private fun setupRecyclerView() {
        // Inicializar el adaptador. La lambda es para manejar clics en los ítems.
        offersAdapter = OffersAdapter(offerList) { selectedOffer ->
            // Acción cuando se hace clic en una oferta
            Toast.makeText(this, "Oferta seleccionada: ${selectedOffer.title}", Toast.LENGTH_SHORT).show()

            // Inicia una nueva Activity para mostrar los detalles de la oferta
             val intent = Intent(this, CompanyOfferInfoActivity::class.java).apply {
                 putExtra("SELECTED_OFFER_ID", selectedOffer.id) // Pasa el ID
             }
             startActivity(intent)
        }

        // Configurar el RecyclerView
        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyOffersActivity)
            adapter = offersAdapter
            // Decoración entre lineas
            addItemDecoration(DividerItemDecoration(this@CompanyOffersActivity, LinearLayoutManager.VERTICAL))
        }

        binding.addOffers.setOnClickListener {
            val intent = Intent(this, CreateOfferActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchOffers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewNoOffers.visibility = View.GONE
        binding.offersRecyclerView.visibility = View.GONE


        val companyIdToFilterBy = auth.currentUser?.uid
        val query = databaseReference.orderByChild("companyId").equalTo(companyIdToFilterBy)


        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                offerList.clear() // Limpiar la lista antes de añadir los nuevos datos
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        offer?.let {
//
                             offerList.add(it)
                        }
                    }
                }

                // Actualizar la UI según si hay ofertas o no
                if (offerList.isEmpty()) {
                    binding.textViewNoOffers.visibility = View.VISIBLE
                    binding.offersRecyclerView.visibility = View.GONE
                } else {
                    binding.textViewNoOffers.visibility = View.GONE
                    binding.offersRecyclerView.visibility = View.VISIBLE
                }
                offersAdapter.updateOffers(offerList.toList()) // Actualizar el adaptador con la nueva lista
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de Firebase
                binding.progressBar.visibility = View.GONE
                binding.textViewNoOffers.visibility = View.VISIBLE
                binding.textViewNoOffers.text = getString(R.string.error_loading_offers) // Usar un string resource
                Log.e("CompanyOffersActivity", "Error al cargar ofertas desde Firebase", error.toException())
                Toast.makeText(this@CompanyOffersActivity, getString(R.string.error_loading_offers_toast, error.message), Toast.LENGTH_LONG).show()
            }
        }
        // Añadir el listener a la consulta
        query.addValueEventListener(valueEventListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let {
            databaseReference.removeEventListener(it)
        }

    }
}