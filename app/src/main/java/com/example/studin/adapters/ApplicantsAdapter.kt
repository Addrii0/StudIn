package com.example.studin.adapters // O tu paquete de adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.User // Tu clase User
import com.example.studin.databinding.ListItemUserBinding

class ApplicantsAdapter(
    private var applicantsList: List<User>,
    private val onItemClicked: (User) -> Unit
) : RecyclerView.Adapter<ApplicantsAdapter.ApplicantViewHolder>() {

    inner class ApplicantViewHolder(val binding: ListItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textUserName.text = "${user.name ?: ""} ${user.surName ?: "N/A"}" // Combina nombre y apellido
            binding.textUserDescription.text = user.description ?: "Sin descripción"
            binding.textUserSkills.text = if (user.skills.isNotEmpty()) {
                "Habilidades: ${user.skills.joinToString(", ")}"
            } else {
                "Habilidades: No especificadas"
            }

            // Si añades un ImageView en item_applicant.xml con id "imageViewApplicantProfile":
            // user.profileImageUrl?.let { url ->
            //    Glide.with(itemView.context)
            //        .load(url)
            //        .placeholder(R.drawable.ic_profile_placeholder) // Necesitas un placeholder
            //        .error(R.drawable.ic_profile_placeholder)
            //        .circleCrop()
            //        .into(binding.imageViewApplicantProfile)
            // } ?: run {
            //    // Opcional: poner una imagen por defecto si no hay profileImageUrl
            //    binding.imageViewApplicantProfile.setImageResource(R.drawable.ic_profile_placeholder)
            // }

            itemView.setOnClickListener {
                onItemClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        // Asegúrate que el nombre del binding coincida con tu archivo XML
        // Si tu XML es item_applicant.xml, el binding generado será ItemApplicantBinding
        val binding = ListItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(applicantsList[position])
    }

    override fun getItemCount(): Int = applicantsList.size

    fun updateData(newApplicants: List<User>) {
        applicantsList = newApplicants
        notifyDataSetChanged() // Para simplicidad. Usa DiffUtil para mejor rendimiento.
    }
}