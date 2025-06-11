package com.example.studin.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.News
import com.example.studin.databinding.ListItemNewsManagementBinding

class NewsManagementAdapter(
    private val currentCompanyId: String?,
    private val onNewsClicked: (News) -> Unit,
    private val onEditNewsClicked: (News) -> Unit,
    private val onDeleteNewsClicked: (News) -> Unit
) : ListAdapter<News, NewsManagementAdapter.NewsManagementViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsManagementViewHolder {
        val binding = ListItemNewsManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsManagementViewHolder(binding, currentCompanyId, onNewsClicked, onEditNewsClicked, onDeleteNewsClicked)
    }

    override fun onBindViewHolder(holder: NewsManagementViewHolder, position: Int) {
        val newsItem = getItem(position)
        holder.bind(newsItem)
    }

    class NewsManagementViewHolder(
        private val binding: ListItemNewsManagementBinding,
        private val currentCompanyId: String?,
        private val onNewsClicked: (News) -> Unit,
        private val onEditNewsClicked: (News) -> Unit,
        private val onDeleteNewsClicked: (News) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: News) {
            binding.textViewNewsTitleManagement.text = news.title ?: itemView.context.getString(R.string.default_news_title)
            binding.textViewNewsContentSnippetManagement.text = news.content ?: ""
            binding.textViewNewsContentSnippetManagement.isVisible = !news.content.isNullOrEmpty()

            if (!news.authorName.isNullOrEmpty()) {
                binding.textViewNewsAuthorManagement.text = itemView.context.getString(R.string.news_author_prefix, news.authorName)
                binding.textViewNewsAuthorManagement.isVisible = true
            } else {
                binding.textViewNewsAuthorManagement.isVisible = false
            }

            news.timestamp?.let {
                val timeAgo = DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                binding.textViewNewsTimestampManagement.text = timeAgo
                binding.textViewNewsTimestampManagement.isVisible = true
            } ?: run {
                binding.textViewNewsTimestampManagement.isVisible = false
            }

            if (!news.imageUrl.isNullOrEmpty()) {
                binding.imageViewNewsManagement.isVisible = true
                Glide.with(itemView.context)
                    .load(news.imageUrl)
                    .placeholder(R.drawable.default_header_placeholder)
                    .error(R.drawable.default_header_placeholder)
                    .centerCrop()
                    .into(binding.imageViewNewsManagement)
            } else {
                binding.imageViewNewsManagement.isVisible = false
            }

            // Lógica para mostrar/ocultar botones de acción
            if (news.authorId != null && news.authorId == currentCompanyId) {
                binding.layoutNewsActionsManagement.isVisible = true
                binding.buttonEditNews.setOnClickListener { onEditNewsClicked(news) }
                binding.buttonDeleteNews.setOnClickListener { onDeleteNewsClicked(news) }
            } else {
                binding.layoutNewsActionsManagement.isVisible = false
            }

            itemView.setOnClickListener {
                onNewsClicked(news)
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }
}