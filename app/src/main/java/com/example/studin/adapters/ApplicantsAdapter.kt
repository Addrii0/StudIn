package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.User
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

            // Por si añado un ImageView en item_applicant.xml con id "imageViewApplicantProfile":
            // user.profileImageUrl?.let { url ->
            //    Glide.with(itemView.context)
            //        .load(url)
            //        .placeholder(R.drawable.ic_profile_placeholder) // Necesitas un placeholder
            //        .error(R.drawable.ic_profile_placeholder)
            //        .circleCrop()
            //        .into(binding.imageViewApplicantProfile)
            // } ?: run {
            //    binding.imageViewApplicantProfile.setImageResource(R.drawable.ic_profile_placeholder)
            // }

            itemView.setOnClickListener {
                onItemClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ListItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(applicantsList[position])
    }

    override fun getItemCount(): Int = applicantsList.size

    fun updateData(newApplicants: List<User>) {
        applicantsList = newApplicants
        notifyDataSetChanged()
    }
}