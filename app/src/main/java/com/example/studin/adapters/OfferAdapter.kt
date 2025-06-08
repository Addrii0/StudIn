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
    private val onItemClicked: (Offer) -> Unit
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {


    inner class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewOfferTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewOfferDescription)


        fun bindData(offer: Offer) {
            titleTextView.text = offer.title
            descriptionTextView.text = offer.description
            // Actualiza aquí otros elementos con los datos de offer
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_offer, parent, false)
        return OfferViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val currentOffer = offerList[position]
        holder.bindData(currentOffer) // Llama a la función para establecer los datos

        holder.itemView.setOnClickListener {
            onItemClicked(currentOffer) // Llama directamente a la lambda del constructor del Adapter
        }
    }

    override fun getItemCount() = offerList.size
}