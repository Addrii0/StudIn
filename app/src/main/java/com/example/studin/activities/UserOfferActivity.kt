package com.example.studin.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.activities.CompanyOffersActivity
import com.example.studin.adapters.OfferAdapter
import com.example.studin.adapters.OffersAdapter
import com.example.studin.classes.Offer
import com.example.studin.databinding.ActivityUserOffersBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class UserOfferActivity: AppCompatActivity() {

    private lateinit var binding: ActivityUserOffersBinding
    lateinit var offersAdapter: OffersAdapter // <--- CORREGIDO: Coincide con la instanciación
    private lateinit var offerReference: DatabaseReference
    private val offerList = mutableListOf<Offer>()
    private lateinit var valueEventListener: ValueEventListener

    // TAG ya estaba definido, lo mantengo. Si no lo usas, considera añadir Log.d(TAG, ...)
    private val TAG = "UserOfferActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        offerReference = FirebaseDatabase.getInstance().getReference("offers")
        setupRecyclerView()
        fetchOffers()
    }

    private fun setupRecyclerView() {
        // Inicializar el adaptador. La lambda es para manejar clics en los ítems.
        offersAdapter = OffersAdapter(offerList) { selectedOffer ->
            // Acción cuando se hace clic en una oferta
            Toast.makeText(this, "Oferta seleccionada: ${selectedOffer.title}", Toast.LENGTH_SHORT)
                .show()

            // Inicia una nueva Activity para mostrar los detalles de la oferta
            // Asegúrate de que tu clase Offer implementa Parcelable para pasarla a través de un Intent.
            // CAMBIO IMPORTANTE: Asumo que quieres ir a UserOfferInfoActivity desde UserOfferActivity
            // Si realmente quieres ir a CompanyOfferInfoActivity, déjalo como estaba.
            val intent = Intent(this, UserOfferInfoActivity::class.java).apply {
                putExtra("SELECTED_OFFER_ID", selectedOffer.id) // Pasa el ID
            }
            startActivity(intent)
        }

        // Configurar el RecyclerView
        binding.offersRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@UserOfferActivity) // CORREGIDO: Contexto actual
            adapter = offersAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@UserOfferActivity, // CORREGIDO: Contexto actual
                    LinearLayoutManager.VERTICAL
                )
            )
        }

        // Asegúrate que el ID 'addOffers' exista en tu layout activity_user_offers.xml
        // Este setOnClickListener parece correcto si el elemento existe.
        // Si 'addOffers' es para que el usuario cree ofertas, esta lógica es extraña en "UserOfferActivity"
        // que normalmente es para que el usuario VEA ofertas.
        // Si este botón es para un Admin/Empresa, quizás esta Activity tiene un doble propósito
        // o el botón debería estar en otra pantalla. Lo dejo como estaba en tu código.
        binding.addOffers.setOnClickListener {
            val intent = Intent(this, CreateOfferActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchOffers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewNoOffers.visibility = View.GONE
        binding.offersRecyclerView.visibility = View.GONE

        // Define la consulta a Firebase.
        // Usar offerReference que ya está inicializado.
        val query: Query = offerReference // <--- CORREGIDO: Usar la referencia correcta

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                offerList.clear()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        offer?.let {
                            // Asumiendo que tu clase Offer tiene un campo 'id' que quieres que sea la key de Firebase
                            // o que tu clase Offer ya tiene un constructor/metodo para asignar el id desde el key.
                            // Si 'Offer' no tiene campo 'id', el .copy(id = ...) es buena idea.
                            val offerWithId = it.copy(id = offerSnapshot.key)
                            offerList.add(offerWithId)
                        }
                    }
                }

                if (offerList.isEmpty()) {
                    binding.textViewNoOffers.visibility = View.VISIBLE
                    binding.offersRecyclerView.visibility = View.GONE
                } else {
                    binding.textViewNoOffers.visibility = View.GONE
                    binding.offersRecyclerView.visibility = View.VISIBLE
                }

                if(::offersAdapter.isInitialized) { // Buena práctica comprobar antes de usar
                    offersAdapter.updateOffers(offerList.toList())
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w(TAG, "loadOffers:onCancelled", error.toException())
                Toast.makeText(baseContext, "Failed to load offers: ${error.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.textViewNoOffers.text = "Error al cargar ofertas." // Informar al usuario
                binding.textViewNoOffers.visibility = View.VISIBLE
                binding.offersRecyclerView.visibility = View.GONE
            }
        }
        // ADJUNTAR EL LISTENER A LA CONSULTA
        query.addValueEventListener(valueEventListener) // <--- CORREGIDO: Listener adjuntado
    }

    override fun onDestroy() {
        super.onDestroy()
        // Buena práctica remover el listener para evitar fugas de memoria
        if (::valueEventListener.isInitialized && ::offerReference.isInitialized) {
            offerReference.removeEventListener(valueEventListener)
        }
    }
}