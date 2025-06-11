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
import com.example.studin.databinding.ListItemNewsBinding

class NewsAdapter(
    private val onItemClicked: (News) -> Unit
) : ListAdapter<News, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ListItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = getItem(position)
        holder.bind(newsItem)
    }

    class NewsViewHolder(
        private val binding: ListItemNewsBinding,
        private val onItemClicked: (News) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: News) {
            binding.textViewNewsTitle.text = news.title ?: itemView.context.getString(R.string.default_news_title)
            // Contenido del snippet
            binding.textViewNewsContentSnippet.text = news.content ?: ""
            binding.textViewNewsContentSnippet.isVisible = !news.content.isNullOrEmpty()


            // Autor
            if (!news.authorName.isNullOrEmpty()) {
                binding.textViewNewsAuthor.text = itemView.context.getString(R.string.news_author_prefix, news.authorName)
                binding.textViewNewsAuthor.isVisible = true
            } else {
                binding.textViewNewsAuthor.isVisible = false
            }

            // Timestamp
            news.timestamp?.let {
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    it,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                binding.textViewNewsTimestamp.text = timeAgo
                binding.textViewNewsTimestamp.isVisible = true
            } ?: run {
                binding.textViewNewsTimestamp.isVisible = false
            }

            // Imagen
            if (!news.imageUrl.isNullOrEmpty()) {
                binding.imageViewNews.isVisible = true
                Glide.with(itemView.context)
                    .load(news.imageUrl)
                    .placeholder(R.drawable.default_header_placeholder)
                    .error(R.drawable.default_header_placeholder)
                    .centerCrop()
                    .into(binding.imageViewNews)
            } else {
                binding.imageViewNews.isVisible = false
            }

            itemView.setOnClickListener {
                onItemClicked(news)

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