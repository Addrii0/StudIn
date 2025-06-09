package com.example.studin.fragments

import OfferAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.activities.UserOfferInfoActivity
import com.example.studin.classes.Offer
import com.example.studin.databinding.FragmentOfferSearchBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OfferSearchFragment : Fragment() {

    private var _binding: FragmentOfferSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: FirebaseDatabase
     private lateinit var offerAdapter: OfferAdapter
     private val offerList = mutableListOf<Offer>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfferSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance()
        setupRecyclerView()
        setupSearchView()
        setupFilters()

         searchOffers("")
    }

    private fun setupRecyclerView() {
        offerAdapter = OfferAdapter(offerList) { clickedOffer ->
            // Esta es la lambda que se ejecuta cuando se hace clic en una oferta
            Log.d("OfferSearchFragment", "Oferta clickeada: ${clickedOffer.title} , ID: [${clickedOffer.id}]")

            // Lógica para abrir la pantalla de detalles de la oferta:
            val intent = Intent(requireContext(), UserOfferInfoActivity::class.java)
            intent.putExtra("SELECTED_OFFER_ID", clickedOffer.id)
            startActivity(intent)
        }

        binding.offersRecyclerViewFragment.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = offerAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchViewOffersFragment.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    // searchOffers(it)
                    Log.d("OfferSearchFragment", "Search submitted: $it")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    // Realizar búsqueda en tiempo real
                     searchOffers(it)
                    Log.d("OfferSearchFragment", "Search text changed: $it")
                }
                return true
            }
        })
    }

    private fun setupFilters() {
        // Filtro (todavia a probat)
        val offerTypes = arrayOf("Todos", "Remoto", "Presencial", "Híbrido")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, offerTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOfferType.adapter = spinnerAdapter

        binding.spinnerOfferType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = parent.getItemAtPosition(position).toString()
                Log.d("OfferSearchFragment", "Filter selected: $selectedType")
                // Se aplica filtro de la busqueda
                 searchOffers(binding.searchViewOffersFragment.query.toString(), selectedType)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    fun searchOffers(query: String, type: String = "Todos") {
        binding.progressBarOffersFragment.visibility = View.VISIBLE
        binding.textViewNoOffersFragment.visibility = View.GONE
        binding.offersRecyclerViewFragment.visibility = View.GONE
        offerList.clear() // Limpiar la lista antes de una nueva búsqueda
        offerAdapter.notifyDataSetChanged()

        val offersRef = database.getReference("offers")


        offersRef.addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                offerList.clear()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        if (offer != null) {

                            offer.id = offerSnapshot.key

                            val query = binding.searchViewOffersFragment.query.toString() // Obtener query actual para el filtro
                            val selectedTypeFromSpinner = binding.spinnerOfferType.selectedItem.toString()

                            val matchesQuery = query.isEmpty() ||
                                    offer.title?.contains(query, ignoreCase = true) == true ||
                                    offer.companyName?.contains(query, ignoreCase = true) == true

                            val matchesType = selectedTypeFromSpinner == "Todos" || offer.type == selectedTypeFromSpinner

                            if (matchesQuery && matchesType) {
                                offerList.add(offer)
                            }
                        }
                    }
                }

                // Actualizar UI después de obtener y filtrar los datos
                if (offerList.isEmpty()) {
                    binding.textViewNoOffersFragment.visibility = View.VISIBLE
                    binding.offersRecyclerViewFragment.visibility = View.GONE
                } else {
                    binding.textViewNoOffersFragment.visibility = View.GONE
                    binding.offersRecyclerViewFragment.visibility = View.VISIBLE
                }
                offerAdapter.notifyDataSetChanged()
                binding.progressBarOffersFragment.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OfferSearchFragment", "Error al cargar ofertas: ${error.message}")
                binding.progressBarOffersFragment.visibility = View.GONE
                binding.textViewNoOffersFragment.text = "Error al cargar ofertas."
                binding.textViewNoOffersFragment.visibility = View.VISIBLE
                binding.offersRecyclerViewFragment.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}