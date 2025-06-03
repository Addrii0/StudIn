package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.Offer
import com.example.studin.R

class OfferAdapter(
    private val offerList: List<Offer>,
    private val onItemClicked: (Offer) -> Unit // Nuevo parámetro: lambda para el clic
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewOfferTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewOfferDescription)
        // Añade aquí referencias a otros elementos si los agregaste al layout list_item_offer.xml

        // Función para vincular el listener de clic
        fun bind(offer: Offer, onItemClicked: (Offer) -> Unit) {
            itemView.setOnClickListener { onItemClicked(offer) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_offer, parent, false)
        return OfferViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val currentOffer = offerList[position]
        holder.titleTextView.text = currentOffer.title
        holder.descriptionTextView.text = currentOffer.description
        // Actualiza aquí otros elementos con los datos de currentOffer

        // Llama a bind para configurar el listener de clic para este ítem
        holder.bind(currentOffer, onItemClicked)
    }

    override fun getItemCount() = offerList.size
}