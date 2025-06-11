package com.example.studin.activities

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.News
import com.example.studin.databinding.ActivityNewsDetailBinding
//import com.google.android.material.chip.Chip
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsDetailBinding

    private var newsId: String? = null

    private lateinit var database: FirebaseDatabase
    private lateinit var newsRef: DatabaseReference
    private var newsDetailListener: ValueEventListener? = null

    private val TAG = "NewsDetailActivity"

    companion object {
        const val EXTRA_NEWS_ID = "news_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.database

        // Obtener el newsId del Intent
        newsId = intent.getStringExtra(EXTRA_NEWS_ID)

        if (newsId == null) {
            Log.e(TAG, "News ID es nulo. No se puede cargar la noticia.")
            showErrorState(getString(R.string.error_news_id_missing))

            return
        }


        newsRef = database.getReference("news").child(newsId!!)
        loadNewsDetail()
    }

    private fun loadNewsDetail() {

        newsDetailListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newsItem = snapshot.getValue(News::class.java)
                    newsItem?.let {
                        it.uid = snapshot.key // Asegurar que el UID esté presente
                        populateUi(it)
                    } ?: run {
                        Log.w(TAG, "No se pudo deserializar la noticia con ID: $newsId")
                        showErrorState()
                    }
                } else {
                    Log.w(TAG, "No se encontró la noticia con ID: $newsId")
                    showErrorState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar detalle de noticia: ${error.message}", error.toException())
                showErrorState(error.message)
            }
        }

        newsRef.addListenerForSingleValueEvent(newsDetailListener!!)

    }

    private fun populateUi(newsItem: News) {
        // Imagen principal
        if (!newsItem.imageUrl.isNullOrEmpty()) {
            binding.imageViewNewsDetailHeader.visibility = View.VISIBLE
            Glide.with(this)
                .load(newsItem.imageUrl)
                .into(binding.imageViewNewsDetailHeader)
        } else {
            binding.imageViewNewsDetailHeader.visibility = View.GONE
        }

        binding.textViewNewsDetailTitle.text = newsItem.title ?: getString(R.string.title_not_available)

        // Información de la empresa/autor
        val companyOrAuthorName = newsItem.companyName ?: newsItem.authorName
        if (!companyOrAuthorName.isNullOrEmpty()) {
            binding.textViewNewsDetailCompanyName.text = companyOrAuthorName
            binding.textViewNewsDetailCompanyName.visibility = View.VISIBLE
        } else {
            binding.textViewNewsDetailCompanyName.text = getString(R.string.error_charging)
        }

        if (!newsItem.companyLogoUrl.isNullOrEmpty()) {
            binding.imageViewNewsDetailCompanyLogo.visibility = View.VISIBLE
            Glide.with(this)
                .load(newsItem.companyLogoUrl)
                .circleCrop()
                .into(binding.imageViewNewsDetailCompanyLogo)
        } else {
            binding.imageViewNewsDetailCompanyLogo.visibility = View.GONE
        }

        // Timestamp
        newsItem.timestamp?.let {
            binding.textViewNewsDetailTimestamp.text = DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            binding.textViewNewsDetailTimestamp.visibility = View.VISIBLE
        } ?: run {
            binding.textViewNewsDetailTimestamp.visibility = View.GONE
        }

        // Contenido
        binding.textViewNewsDetailContent.text = newsItem.content?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT)
        } ?: getString(R.string.content_not_available)

        // Categoría
        if (!newsItem.category.isNullOrEmpty()) {
            binding.textViewNewsDetailCategoryLabel.visibility = View.VISIBLE
            binding.textViewNewsDetailCategoryValue.visibility = View.VISIBLE
            binding.textViewNewsDetailCategoryValue.text = newsItem.category
        } else {
            binding.textViewNewsDetailCategoryLabel.visibility = View.GONE
            binding.textViewNewsDetailCategoryValue.visibility = View.GONE
        }

        // Tags (Por implementar)
//        if (!newsItem.tags.isNullOrEmpty() && newsItem.tags!!.isNotEmpty()) {
//            binding.textViewNewsDetailTagsLabel.visibility = View.VISIBLE
//            binding.chipGroupNewsDetailTags.visibility = View.VISIBLE
//            binding.chipGroupNewsDetailTags.removeAllViews()
//            newsItem.tags?.forEach { tagText ->
//                if (tagText.isNotBlank()){
//                    val chip = Chip(this) // 'this' es el Contexto de la Activity
//                    chip.text = tagText
//                    binding.chipGroupNewsDetailTags.addView(chip)
//                }
//            }
//            if (binding.chipGroupNewsDetailTags.childCount == 0) {
//                binding.textViewNewsDetailTagsLabel.visibility = View.GONE
//                binding.chipGroupNewsDetailTags.visibility = View.GONE
//            }
//        } else {
//            binding.textViewNewsDetailTagsLabel.visibility = View.GONE
//            binding.chipGroupNewsDetailTags.visibility = View.GONE
//        }
    }

    private fun showErrorState(errorMessage: String? = null) {
        binding.textViewNewsDetailTitle.text = getString(R.string.error_loading_news_detail)
        binding.imageViewNewsDetailHeader.visibility = View.GONE
        binding.layoutNewsDetailMeta.visibility = View.GONE
        binding.dividerNewsDetail.visibility = View.GONE
        binding.textViewNewsDetailContent.text = errorMessage ?: getString(R.string.news_not_found_or_error)
        binding.layoutNewsDetailTagsAndCategory.visibility = View.GONE


    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        if (newsDetailListener != null && newsId != null) {

            if (::newsRef.isInitialized) { // Verifica si newsRef fue inicializada
                newsRef.removeEventListener(newsDetailListener!!)
            }
        }
        newsDetailListener = null // Ayuda al GC

        Log.d(TAG, "onDestroy: NewsDetailActivity destruida.")
    }
}