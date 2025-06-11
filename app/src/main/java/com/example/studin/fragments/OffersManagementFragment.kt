package com.example.studin.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.R
import com.example.studin.activities.CompanyOfferInfoActivity
import com.example.studin.activities.CreateOfferActivity
import com.example.studin.adapters.OfferManagementAdapter
import com.example.studin.classes.Offer
import com.example.studin.databinding.FragmentOffersManagementBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class OffersManagementFragment : Fragment() {

    private var _binding: FragmentOffersManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var offerAdapter: OfferManagementAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var offersRootRef: DatabaseReference
    private var companyOffersListener: ValueEventListener? = null
    private var companyId: String? = null

    private val TAG = "OffersManagementFrag"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOffersManagementBinding.inflate(inflater, container, false)
        database = Firebase.database
        offersRootRef = database.getReference("offers")

        companyId = FirebaseAuth.getInstance().currentUser?.uid

        if (companyId == null) {
            Log.e(TAG, "Company ID es nulo. No se pueden cargar ofertas.")
            Toast.makeText(requireContext(), "Error: No se pudo identificar la empresa.", Toast.LENGTH_LONG).show()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (companyId != null) {
            setupRecyclerView()
            loadCompanyOffers()
        } else {
            binding.progressBarOffersManagement.isVisible = false
            binding.textViewNoOffersManagement.isVisible = true
            binding.textViewNoOffersManagement.text = getString(R.string.error_company_id) // "ID de empresa no encontrado."
        }
        binding.createOffers.setOnClickListener {
            val intent = Intent(activity, CreateOfferActivity::class.java)
            startActivity(intent)
             }

    }

    private fun setupRecyclerView() {
        offerAdapter = OfferManagementAdapter(
            onOfferActiveChanged = { offer, isActive ->
                updateOfferActiveStatus(offer, isActive)
            },
            onEditClicked = { offer ->
                // Futura implementación  de editar ofertas
            },
            onDeleteClicked = { offer ->
                showDeleteConfirmationDialog(offer)
            },
            onItemClicked = { offer ->
                Log.d("OfferClick", "Oferta clickeada: ${offer.title}")
                val intent = Intent(activity, CompanyOfferInfoActivity::class.java)
                intent.putExtra("SELECTED_OFFER_ID", offer.id)
                startActivity(intent)
            }
        )
        binding.recyclerViewOffersManagement.adapter = offerAdapter

        binding.recyclerViewOffersManagement.apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadCompanyOffers() {
        binding.progressBarOffersManagement.isVisible = true
        binding.textViewNoOffersManagement.isVisible = false
        binding.recyclerViewOffersManagement.isVisible = false

        // Query para obtener solo ofertas de la empresa actual, ordenadas por fecha
        val companyOffersQuery = offersRootRef.orderByChild("companyId").equalTo(companyId)

        companyOffersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offersList = mutableListOf<Offer>()
                if (snapshot.exists()) {
                    for (offerSnapshot in snapshot.children) {
                        val offer = offerSnapshot.getValue(Offer::class.java)
                        offer?.let {
                            it.id = offerSnapshot.key // Asegurarse que el UID existe
                            offersList.add(it)
                        }
                    }

                    Log.d(TAG, "Ofertas cargadas para ${companyId}: ${offersList.size}")
                } else {
                    Log.d(TAG, "No existen ofertas para la empresa: $companyId")
                }
                offerAdapter.submitList(offersList)
                updateUIBasedOnList(offersList)
                binding.progressBarOffersManagement.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar ofertas: ${error.message}", error.toException())
                binding.progressBarOffersManagement.isVisible = false
                binding.textViewNoOffersManagement.text = getString(R.string.error_loading_offers)
                binding.textViewNoOffersManagement.isVisible = true
                binding.recyclerViewOffersManagement.isVisible = false
            }
        }
        // El addValueEvent permite actualizar los datos en tiempo real
        companyOffersQuery.addValueEventListener(companyOffersListener!!)
    }

    private fun updateUIBasedOnList(offersList: List<Offer>) {
        if (offersList.isEmpty()) {
            binding.textViewNoOffersManagement.text = getString(R.string.no_offers_posted)
            binding.textViewNoOffersManagement.isVisible = true
            binding.recyclerViewOffersManagement.isVisible = false
        } else {
            binding.textViewNoOffersManagement.isVisible = false
            binding.recyclerViewOffersManagement.isVisible = true
        }
    }

    private fun updateOfferActiveStatus(offer: Offer, isActive: Boolean) {
        offer.id?.let { offerId ->
            offersRootRef.child(offerId).child("active").setValue(isActive)
                .addOnSuccessListener {
                    Log.d(TAG, "Estado de oferta '${offer.title}' actualizado a $isActive")
                    Toast.makeText(requireContext(),
                        getString(if (isActive) R.string.offer_activated else R.string.offer_deactivated),
                        Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar estado de oferta: ${e.message}", e)
                    Toast.makeText(requireContext(), getString(R.string.error_updating_offer_status), Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e(TAG, "Intento de actualizar oferta con UID nulo: ${offer.title}")
    }

    private fun showDeleteConfirmationDialog(offer: Offer) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_offer_dialog_title))
            .setMessage(getString(R.string.delete_offer_dialog_message, offer.title))
            .setPositiveButton(getString(R.string.delete_action)) { dialog, _ ->
                deleteOfferFromFirebase(offer)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel_action)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteOfferFromFirebase(offer: Offer) {
        offer.id?.let { offerId ->
            offersRootRef.child(offerId).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Oferta '${offer.title}' eliminada exitosamente.")
                    Toast.makeText(requireContext(), getString(R.string.offer_deleted_successfully), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al eliminar oferta '${offer.title}': ${e.message}", e)
                    Toast.makeText(requireContext(), getString(R.string.error_deleting_offer), Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e(TAG, "Intento de eliminar oferta con UID nulo: ${offer.title}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el listener para evitar fugas de memoria
        companyOffersListener?.let {
            offersRootRef.orderByChild("companyId").equalTo(companyId).removeEventListener(it)
        }
        companyOffersListener = null
        _binding = null // Limpiar el ViewBinding
        Log.d(TAG, "onDestroyView: Listener de ofertas removido y binding limpiado.")
    }
}