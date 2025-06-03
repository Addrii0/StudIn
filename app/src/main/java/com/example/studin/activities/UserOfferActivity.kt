package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // Importar SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.adapters.OffersAdapter
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityUserOffersBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale // Para búsqueda insensible a mayúsculas

class UserOfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOffersBinding
    lateinit var offersAdapter: OffersAdapter
    private lateinit var offerReference: DatabaseReference
    private var originalOfferList = mutableListOf<Offer>() // Lista original obtenida de Firebase
    private var filteredOfferList = mutableListOf<Offer>() // Lista para mostrar en el RecyclerView
    private lateinit var valueEventListener: ValueEventListener

    private val TAG = "UserOfferActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar referencia a Firebase
        offerReference = FirebaseDatabase.getInstance().getReference("offers")

        setupRecyclerView()
        setupSearchView() // Configurar el SearchView
        fetchOffers()     // Cargar las ofertas iniciales
    }

    private fun setupRecyclerView() {
        // Inicializa el adaptador con la lista que se va a filtrar (filteredOfferList)
        offersAdapter = OffersAdapter(filteredOfferList) { selectedOffer ->
            // Acción cuando se hace clic en una oferta
            Toast.makeText(this, "Oferta seleccionada: ${selectedOffer.title}", Toast.LENGTH_SHORT).show()

            // Inicia UserOfferInfoActivity para mostrar los detalles de la oferta
            val intent = Intent(this, UserOfferInfoActivity::class.java).apply {
                putExtra("SELECTED_OFFER_ID", selectedOffer.id) // Pasa el ID
            }
            startActivity(intent)
        }

        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserOfferActivity)
            adapter = offersAdapter
            addItemDecoration(DividerItemDecoration(this@UserOfferActivity, LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupSearchView() {
        binding.searchViewOffers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // El usuario presionó buscar en el teclado.
                // La lógica de onQueryTextChange ya debería haber filtrado.
                // Opcionalmente, puedes ocultar el teclado aquí.
                binding.searchViewOffers.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Esto se llama cada vez que el texto cambia en el SearchView
                filterOffers(newText)
                return true
            }
        })

        // Opcional: Manejar cuando el SearchView se cierra (botón 'x')
        binding.searchViewOffers.setOnCloseListener {
            filterOffers("") // Muestra todas las ofertas de nuevo (limpia el filtro)
            // No es necesario llamar a clearFocus() aquí, el SearchView lo maneja.
            false // Devuelve false para permitir que el SearchView se cierre por defecto
        }

        // Opcional: Para expandir el SearchView al hacer clic en cualquier parte de él,
        // si está configurado como iconifiedByDefault="true"
        binding.searchViewOffers.setOnClickListener {
            binding.searchViewOffers.isIconified = false
        }
    }

    private fun fetchOffers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewNoOffers.visibility = View.GONE
        binding.offersRecyclerView.visibility = View.GONE

        // Remover listener anterior para evitar duplicados si esta función se llama múltiples veces
        // (por ejemplo, en un onResume o si hay un "refresh")
        if (::valueEventListener.isInitialized) {
            offerReference.removeEventListener(valueEventListener)
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalOfferList.clear()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        offer?.let {
                            // Asegurarse de que el ID de la oferta se asigna desde la clave del snapshot
                            val offerWithId = it.copy(id = offerSnapshot.key ?: it.id)
                            originalOfferList.add(offerWithId)
                        }
                    }
                }
                Log.d(TAG, "Fetched ${originalOfferList.size} offers from Firebase.")
                // Después de obtener los datos, aplicar cualquier filtro de búsqueda actual
                // (por ejemplo, si el usuario ya había escrito algo antes de que los datos cargaran,
                // o si la pantalla se recreó y el query se restauró).
                filterOffers(binding.searchViewOffers.query.toString())
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "loadOffers:onCancelled", error.toException())
                Toast.makeText(baseContext, "Fallo al cargar ofertas: ${error.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.textViewNoOffers.text = "Error al cargar ofertas."
                binding.textViewNoOffers.visibility = View.VISIBLE
                binding.offersRecyclerView.visibility = View.GONE
                originalOfferList.clear() // Limpiar la lista en caso de error
                filterOffers(null)      // Actualizar UI para mostrar "sin ofertas" o estado de error
            }
        }
        // Adjuntar el listener a la referencia de Firebase
        offerReference.addValueEventListener(valueEventListener)
    }

    private fun filterOffers(query: String?) {
        filteredOfferList.clear() // Limpiar la lista filtrada actual

        val searchQuery = query?.trim()?.lowercase(Locale.getDefault()) ?: ""

        if (searchQuery.isEmpty()) {
            // Si la búsqueda está vacía, mostrar todas las ofertas originales
            filteredOfferList.addAll(originalOfferList)
        } else {
            // Filtrar la lista original basada en el query
            for (offer in originalOfferList) {
                // Buscar en el título. Puedes extender esto a otros campos (descripción, empresa, etc.)
                if (offer.title?.lowercase(Locale.getDefault())?.contains(searchQuery) == true) {
                    filteredOfferList.add(offer)
                }
                // Ejemplo para buscar también en la descripción (si el campo existe y es String):
                // else if (offer.description?.lowercase(Locale.getDefault())?.contains(searchQuery) == true) {
                //     filteredOfferList.add(offer)
                // }
            }
        }

        Log.d(TAG, "Filtering with query '$searchQuery', ${filteredOfferList.size} results.")

        // Actualizar el adaptador
        if (::offersAdapter.isInitialized) {
            offersAdapter.updateOffers(filteredOfferList) // Notifica al adaptador de los cambios
        }

        // Actualizar la visibilidad de los elementos de la UI
        if (filteredOfferList.isEmpty()) {
            if (originalOfferList.isNotEmpty() && searchQuery.isNotEmpty()){
                binding.textViewNoOffers.text = "No hay ofertas que coincidan con '$query'"
            } else if (originalOfferList.isEmpty() && searchQuery.isEmpty()){
                binding.textViewNoOffers.text = "No hay ofertas disponibles en este momento."
            }
            binding.textViewNoOffers.visibility = View.VISIBLE
            binding.offersRecyclerView.visibility = View.GONE
        } else {
            binding.textViewNoOffers.visibility = View.GONE
            binding.offersRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Buena práctica remover el listener para evitar fugas de memoria y llamadas innecesarias
        if (::valueEventListener.isInitialized && ::offerReference.isInitialized) {
            offerReference.removeEventListener(valueEventListener)
        }
    }
}