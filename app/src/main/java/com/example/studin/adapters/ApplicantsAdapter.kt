package com.example.studin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.classes.User
import com.example.studin.databinding.ListItemUserBinding

class ApplicantsAdapter(
    private var applicantsList: List<User>,

    private val onItemClicked: (position: Int) -> Unit
) : RecyclerView.Adapter<ApplicantsAdapter.ApplicantViewHolder>() {

    inner class ApplicantViewHolder(val binding: ListItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textUserName.text = "${user.name ?: ""} ${user.surName ?: "N/A"}"
            binding.textUserDescription.text = user.description ?: "Sin descripción"
            binding.textUserSkills.text = if (user.skills.isNotEmpty()) {
                "Habilidades: ${user.skills.joinToString(", ")}"
            } else {
                "Habilidades: No especificadas"
            }

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(position)
                }
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