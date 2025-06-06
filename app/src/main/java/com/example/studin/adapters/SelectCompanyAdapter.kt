package com.example.studin.adapters // o donde tengas tus adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company

class SelectCompanyAdapter(
    private val companies: List<Company>,
    private val onItemClick: (Company) -> Unit
) : RecyclerView.Adapter<SelectCompanyAdapter.CompanyViewHolder>() {

    inner class CompanyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val companyNameTextView: TextView = itemView.findViewById(R.id.textView_company_name_selectable)
        private val companyAvatarImageView: ImageView = itemView.findViewById(R.id.imageView_company_avatar_selectable) // Opcional

        fun bind(company: Company) {
            companyNameTextView.text = company.name
            // Cargar avatar si existe (usando Glide como ejemplo)
            if (!company.profileImageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(company.profileImageUrl)
                    .placeholder(R.mipmap.ic_launcher_round) // Placeholder mientras carga
                    .error(R.mipmap.ic_launcher_round) // Imagen de error si falla la carga
                    .circleCrop() // Opcional: para avatares redondos
                    .into(companyAvatarImageView)
                companyAvatarImageView.visibility = View.VISIBLE
            } else {
                // Puedes poner un placeholder genérico o esconder el ImageView
                companyAvatarImageView.setImageResource(R.mipmap.ic_launcher_round) // O un ícono genérico
                // companyAvatarImageView.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(company)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_company_selectable, parent, false)
        return CompanyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(companies[position])
    }

    override fun getItemCount(): Int = companies.size
}