package com.example.studin.ui.fragments // O el paquete donde lo tengas

import android.content.Context // Importar Context
import android.os.Bundle
import android.util.Log // Importar Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.R
import com.example.studin.adapters.OfferAdapter
import com.example.studin.classes.Offer // Importa tu clase Offer (¡Debe ser Parcelable!)

class OffersOverlayFragment : Fragment() {

    // **********************************************************
    // ¡ESTO ES LO QUE FALTABA! Define una interfaz para comunicar eventos a la actividad
    interface OffersOverlayListener {
        fun onOffersOverlayClose()
    }
    // **********************************************************

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonClose: Button
    private var offerList: List<Offer> = listOf() // Lista para almacenar las ofertas

    // **********************************************************
    // Variable para almacenar la referencia al listener de la actividad
    private var listener: OffersOverlayListener? = null
    // **********************************************************


    companion object {
        private const val ARG_OFFERS = "offers_list"
        private val TAG = "OffersOverlayFragment" // Tag para logging

        fun newInstance(offers: List<Offer>): OffersOverlayFragment {
            val fragment = OffersOverlayFragment()
            val args = Bundle()
            // Para pasar una lista de objetos personalizados, Offer debe ser Parcelable o Serializable
            // Parcelable es más eficiente en Android
            // Asegúrate de que tu clase Offer implemente Parcelable
            // Log.d(TAG, "Creando newInstance con ${offers.size} ofertas") // Opcional: Log
            args.putParcelableArrayList(ARG_OFFERS, ArrayList(offers))
            fragment.arguments = args
            return fragment
        }
    }

    // **********************************************************
    // onAttach se llama cuando el fragmento se adjunta a su actividad
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verifica si la actividad implementa la interfaz del listener
        if (context is OffersOverlayListener) {
            listener = context // Si la implementa, guarda la referencia
            Log.d(TAG, "Listener adjuntado.") // Opcional: Log
        } else {
            // Si la actividad no implementa la interfaz, lanza una excepción
            throw RuntimeException("$context must implement OffersOverlayListener")
        }
    }
    // **********************************************************


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Recupera la lista de ofertas del Bundle
            // Necesitas que Offer sea Parcelable
            offerList = it.getParcelableArrayList<Offer>(ARG_OFFERS) ?: listOf()
            // Log.d(TAG, "onCreate: Recuperadas ${offerList.size} ofertas del Bundle") // Opcional: Log
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout para este fragmento
        // **********************************************************
        // NOTA: En tu código anterior vi R.layout.fragment_offer_list (singular)
        // Asegúrate de que el nombre del layout XML coincida exactamente con R.layout.fragment_offers_list (plural)
        // O cambia la referencia aquí a R.layout.fragment_offer_list si ese es el nombre real.
        val view = inflater.inflate(R.layout.fragment_offers_list, container, false)
        // **********************************************************

        recyclerView = view.findViewById(R.id.recyclerViewOffers)
        buttonClose = view.findViewById(R.id.buttonCloseOffers) // Encuentra el botón de cerrar

        setupRecyclerView() // Configura el RecyclerView
        setupCloseButton() // Configura el botón de cerrar

        return view
    }

    private fun setupRecyclerView() {
        // Log.d(TAG, "Configurando RecyclerView con ${offerList.size} elementos.") // Opcional: Log
        recyclerView.layoutManager =
            LinearLayoutManager(context) // Un layout manager lineal vertical
        recyclerView.adapter = OfferAdapter(offerList) { offer ->
            // No hacer nada o un Log si quieres saber que se hizo clic
            Log.d(TAG, "Oferta clickeada en overlay: ${offer.title}, pero no se configuró acción.")
        }
    }
    private fun setupCloseButton() {
        buttonClose.setOnClickListener {
            Log.d(TAG, "Botón de cerrar clickeado.") // Opcional: Log
            // **********************************************************
            // Notifica a la actividad a través del listener
            listener?.onOffersOverlayClose()
            // **********************************************************

            // Cuando se haga clic en el botón de cerrar, le decimos a la actividad
            // que remueva este fragmento. (Este remove es opcional, podrías dejar que la actividad lo remueva
            // después de que se notifique, pero removerlo aquí es común)
            activity?.supportFragmentManager?.beginTransaction()
                ?.remove(this)
                ?.commit()
            Log.d(TAG, "Fragmento de overlay removido.") // Opcional: Log

            // Opcional: podrías cambiar la visibilidad del contenedor en la actividad en lugar de remover el fragmento
            // (activity as? HomeActivity)?.hideOffersOverlay() //
        }
    }

    // **********************************************************
    // onDetach se llama cuando el fragmento se desvincula de su actividad
    override fun onDetach() {
        super.onDetach()
        listener = null // Limpia la referencia al listener para evitar memory leaks
        Log.d(TAG, "Listener desvinculado.") // Opcional: Log
    }
    // **********************************************************
}
