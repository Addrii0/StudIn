package com.example.studin.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.R
import com.example.studin.classes.Offer
import com.example.studin.databinding.ListItemOfferManagementBinding

class OfferManagementAdapter(
    private val onOfferActiveChanged: (Offer, Boolean) -> Unit,
    private val onEditClicked: (Offer) -> Unit,
    private val onDeleteClicked: (Offer) -> Unit,
    private val onItemClicked: (Offer) -> Unit
) : ListAdapter<Offer, OfferManagementAdapter.OfferManagementViewHolder>(OfferDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferManagementViewHolder {
        val binding = ListItemOfferManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return OfferManagementViewHolder(binding, onOfferActiveChanged, onEditClicked, onDeleteClicked, onItemClicked)
    }

    override fun onBindViewHolder(holder: OfferManagementViewHolder, position: Int) {
        val offer = getItem(position)
        holder.bind(offer)
    }

    class OfferManagementViewHolder(
        private val binding: ListItemOfferManagementBinding,

        private val onOfferActiveChangedCallback: (Offer, Boolean) -> Unit,
        private val onEditClickedCallback: (Offer) -> Unit,
        private val onDeleteClickedCallback: (Offer) -> Unit,
        private val onItemClickedCallback: (Offer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentOffer: Offer? = null

        init {

            binding.root.setOnClickListener {
                currentOffer?.let { offer ->
                    onItemClickedCallback(offer)
                }
            }
        }

        fun bind(offer: Offer) {
            currentOffer = offer
            binding.textViewOfferTitleManagement.text = offer.title ?: itemView.context.getString(R.string.default_offer_title)

            offer.datePosted?.let {
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    it,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                binding.textViewOfferDateManagement.text = itemView.context.getString(R.string.offer_posted_date, timeAgo)
            } ?: run {
                binding.textViewOfferDateManagement.text = itemView.context.getString(R.string.offer_date_unknown)
            }

            binding.switchOfferActive.setOnCheckedChangeListener(null)
            binding.switchOfferActive.isChecked = offer.active
            binding.switchOfferActive.text = if (offer.active) itemView.context.getString(R.string.offer_status_active) else itemView.context.getString(R.string.offer_status_inactive)

            binding.switchOfferActive.setOnCheckedChangeListener { _, isChecked ->

                onOfferActiveChangedCallback(offer, isChecked)
            }

            binding.buttonEditOffer.setOnClickListener {

                onEditClickedCallback(offer)
            }

            binding.buttonDeleteOffer.setOnClickListener {

                onDeleteClickedCallback(offer)
            }
        }
    }

    class OfferDiffCallback : DiffUtil.ItemCallback<Offer>() {
        override fun areItemsTheSame(oldItem: Offer, newItem: Offer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Offer, newItem: Offer): Boolean {
            return oldItem == newItem
        }
    }
}