package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.Offer
import com.example.studin.R

class OfferAdapter(private val offerList: List<Offer>) :
    RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    // Esta clase interna mantiene referencias a las vistas de un solo elemento de la lista
    class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewOfferTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewOfferDescription)
        // Añade aquí referencias a otros elementos si los agregaste al layout list_item_offer.xml
    }

    // Crea nuevas vistas (invocado por el layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        // Crea una nueva vista, que define la interfaz de usuario del elemento de la lista
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_offer, parent, false)
        return OfferViewHolder(itemView)
    }

    // Reemplaza el contenido de una vista (invocado por el layout manager)
    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        // Obtén el elemento de datos en esta posición
        val currentOffer = offerList[position]

        // Reemplaza el contenido de las vistas con los datos de ese elemento
        holder.titleTextView.text = currentOffer.title
        holder.descriptionTextView.text = currentOffer.description
        // Actualiza aquí otros elementos con los datos de currentOffer
    }

    // Retorna el tamaño de tu dataset (invocado por el layout manager)
    override fun getItemCount() = offerList.size
}
