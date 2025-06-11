package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.Offer
import com.example.studin.databinding.ItemOfferBinding

class OffersAdapter(
    private var offerList: List<Offer>,
    private val onItemClicked: (Offer) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    inner class OfferViewHolder(val binding: ItemOfferBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(offer: Offer) {
            binding.textViewOfferTitle.text = offer.title ?: "N/A"
            binding.textViewCompanyName.text = offer.companyName ?: "N/A"
            binding.textViewOfferLocation.text = offer.location ?: "N/A"
            binding.textViewOfferDescription.text = offer.description ?: "Sin descripción"


            itemView.setOnClickListener {
                onItemClicked(offer)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val binding = ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(offerList[position])
    }

    override fun getItemCount(): Int = offerList.size


}