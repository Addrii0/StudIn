package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company
import com.example.studin.databinding.ListItemCompanySearchBinding

class CompanySearchAdapter(
    private val onItemClicked: (Company) -> Unit
) : ListAdapter<Company, CompanySearchAdapter.CompanyViewHolder>(CompanyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {

        val binding = ListItemCompanySearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompanyViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        val company = getItem(position)
        holder.bind(company)
    }


    class CompanyViewHolder(
        private val binding: ListItemCompanySearchBinding,
        private val onItemClicked: (Company) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(company: Company) {
            binding.textViewCompanyName.text = company.name ?: "Nombre no disponible"

            // Cargar avatar
            if (!company.profileImageUrl.isNullOrEmpty()) {
                Glide.with(binding.imageViewCompanyLogo.context)
                    .load(company.profileImageUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .circleCrop()
                    .into(binding.imageViewCompanyLogo)
            } else {
                binding.imageViewCompanyLogo.setImageResource(R.mipmap.ic_launcher_round) // Placeholder
            }

            binding.root.setOnClickListener {
                onItemClicked(company)
            }
        }
    }

    class CompanyDiffCallback : DiffUtil.ItemCallback<Company>() {
        override fun areItemsTheSame(oldItem: Company, newItem: Company): Boolean {
            return oldItem.uid == newItem.uid // compara que el UID es único
        }

        override fun areContentsTheSame(oldItem: Company, newItem: Company): Boolean {
            return oldItem == newItem // Compara todos los campos
        }
    }
}