package com.example.studin.ui.fragments // O el paquete donde lo tengas

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.adapters.OfferAdapter
import com.example.studin.classes.Offer // Importa tu clase Offer (¡Debe ser Parcelable!)
import com.example.studin.databinding.FragmentOffersListBinding // Asegúrate que el nombre coincida con tu XML

class OffersOverlayFragment : Fragment() {

    interface OffersOverlayListener {
        fun onOffersOverlayClose()
        fun onOfferSelected(offer: Offer)
    }

    // Declara la variable de binding. Será nullable.
    private var _binding: FragmentOffersListBinding? = null
    // Esta propiedad no es nullable y solo se puede acceder entre onCreateView y onDestroyView.
    private val binding get() = _binding!!

    private var offerList: List<Offer> = listOf()
    private var listener: OffersOverlayListener? = null

    companion object {
        private const val ARG_OFFERS = "offers_list"
        private val TAG = "OffersOverlayFragment"

        fun newInstance(offers: List<Offer>): OffersOverlayFragment {
            val fragment = OffersOverlayFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_OFFERS, ArrayList(offers))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OffersOverlayListener) {
            listener = context
            Log.d(TAG, "Listener adjuntado.")
        } else {
            throw RuntimeException("$context must implement OffersOverlayListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            offerList = it.getParcelableArrayList<Offer>(ARG_OFFERS) ?: listOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // El tipo de retorno ahora es View, no View? ya que binding.root no será null
        // Infla el layout usando la clase de binding generada
        _binding = FragmentOffersListBinding.inflate(inflater, container, false)
        // Ya no necesitas: val view = inflater.inflate(R.layout.fragment_offers_list, container, false)
        return binding.root // Retorna la vista raíz del binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         setupRecyclerView()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView con ${offerList.size} elementos.")
        // Accede a las vistas a través del objeto binding
        binding.recyclerViewOffers.layoutManager = LinearLayoutManager(context)
        val offerAdapter = OfferAdapter(offerList) { clickedOffer ->
            // Esta lambda se ejecuta cuando el OfferAdapter notifica un clic en un ítem.
            Log.d(TAG, "Oferta clickeada en overlay: ${clickedOffer.title}. Notificando al listener.")
            listener?.onOfferSelected(clickedOffer) // <--- ¡AQUÍ ESTÁ LA PARTE CLAVE!
        }
        binding.recyclerViewOffers.adapter = offerAdapter
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        Log.d(TAG, "Listener desvinculado.")
    }

    // Es crucial limpiar la referencia al binding en onDestroyView para evitar fugas de memoria.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Libera la referencia al binding
        Log.d(TAG, "Binding de OffersOverlayFragment limpiado en onDestroyView.")
    }
}