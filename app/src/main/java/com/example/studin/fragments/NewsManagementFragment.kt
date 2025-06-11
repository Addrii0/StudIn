package com.example.studin.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.R
import com.example.studin.activities.CreateEditNewsActivity
import com.example.studin.activities.NewsDetailActivity
import com.example.studin.adapters.NewsManagementAdapter
import com.example.studin.classes.News
import com.example.studin.databinding.FragmentNewsManagementBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Locale

class NewsManagementFragment : Fragment() {

    private var _binding: FragmentNewsManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var newsAdapter: NewsManagementAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var newsRootRef: DatabaseReference
    private var newsListener: ValueEventListener? = null
    private var currentCompanyId: String? = null
    private val allNewsList = mutableListOf<News>()
    private val filteredNewsList = mutableListOf<News>()

    private val TAG = "NewsManagementFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsManagementBinding.inflate(inflater, container, false)
        database = Firebase.database
        newsRootRef = database.getReference("news")
        currentCompanyId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentCompanyId == null) {
            Log.w(TAG, "ID de empresa actual es nulo. Las funciones de gestión (crear, editar, eliminar) no estarán disponibles.")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        loadAllNews()

        binding.fabCreateNews.setOnClickListener {
            if (currentCompanyId != null) {
                val intent = Intent(requireActivity(), CreateEditNewsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_login_to_create_news), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearchView() {
        binding.searchViewNewsManagement.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterNews(query)
                // Cierra el teclado si el usuario presiona "buscar" en el teclado
                binding.searchViewNewsManagement.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterNews(newText)
                return true
            }
        })
        binding.searchViewNewsManagement.setOnQueryTextFocusChangeListener { _, hasFocus ->
        }
    }

    private fun filterNews(query: String?) {
        val currentListToShow = if (query.isNullOrEmpty()) {
            allNewsList // Mostrar todas si la query está vacía
        } else {
            val searchQuery = query.lowercase(Locale.getDefault())
            allNewsList.filter { news ->
                news.title?.lowercase(Locale.getDefault())?.contains(searchQuery) == true ||
                        news.content?.lowercase(Locale.getDefault())?.contains(searchQuery) == true ||
                        news.authorName?.lowercase(Locale.getDefault())?.contains(searchQuery) == true
            }
        }
        filteredNewsList.clear()
        filteredNewsList.addAll(currentListToShow)
        newsAdapter.submitList(filteredNewsList.toList())
        updateUINewsBasedOnList(filteredNewsList)
    }


    private fun setupRecyclerView() {
        newsAdapter = NewsManagementAdapter(
            currentCompanyId = currentCompanyId,
            onNewsClicked = { news ->
                val intent = Intent(requireActivity(), NewsDetailActivity::class.java)
                intent.putExtra(NewsDetailActivity.EXTRA_NEWS_ID, news.uid)
                startActivity(intent)
                            },
            onEditNewsClicked = { news ->
                val intent = Intent(requireActivity(), CreateEditNewsActivity::class.java).apply {
                    putExtra("NEWS_ID", news.uid)
                }
                startActivity(intent)
            },
            onDeleteNewsClicked = { news ->
                showDeleteNewsConfirmationDialog(news)
            }
        )
        binding.recyclerViewNewsManagement.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
        }
    }

    private fun loadAllNews() {
        binding.progressBarNewsManagement.isVisible = true
        binding.textViewNoNewsManagement.isVisible = false
        binding.recyclerViewNewsManagement.isVisible = false

        val newsQuery = newsRootRef.orderByChild("timestamp")

        newsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allNewsList.clear()
                if (snapshot.exists()) {
                    for (newsSnapshot in snapshot.children) {
                        val newsItem = newsSnapshot.getValue(News::class.java)
                        newsItem?.let {
                            it.uid = newsSnapshot.key
                            allNewsList.add(it)
                        }
                    }
                    allNewsList.reverse()
                    Log.d(TAG, "Noticias cargadas: ${allNewsList.size}")
                } else {
                    Log.d(TAG, "No existen noticias en la base de datos.")
                }
                // Al cargar datos nuevos, aplicar el filtro actual
                filterNews(binding.searchViewNewsManagement.query?.toString())
                binding.progressBarNewsManagement.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar noticias: ${error.message}", error.toException())
                binding.progressBarNewsManagement.isVisible = false
                updateUINewsBasedOnList(emptyList())
                binding.textViewNoNewsManagement.text = getString(R.string.error_loading_news)
            }
        }
        newsQuery.addValueEventListener(newsListener!!)
    }

    private fun updateUINewsBasedOnList(newsListToDisplay: List<News>) {
        if (newsListToDisplay.isEmpty()) {
            // Si la búsqueda no encuentra resultados pero sí hay noticias en allNewsList
            if (binding.searchViewNewsManagement.query.isNotEmpty() && allNewsList.isNotEmpty()) {
                binding.textViewNoNewsManagement.text = getString(R.string.no_news_found_for_search) // "No se encontraron noticias para tu búsqueda."
            } else { // Si no hay noticias o la lista filtrada está vacía sin búsqueda
                binding.textViewNoNewsManagement.text = getString(R.string.no_news_available)
            }
            binding.textViewNoNewsManagement.isVisible = true
            binding.recyclerViewNewsManagement.isVisible = false
        } else {
            binding.textViewNoNewsManagement.isVisible = false
            binding.recyclerViewNewsManagement.isVisible = true
        }
    }

    private fun showDeleteNewsConfirmationDialog(news: News) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_news_dialog_title))
            .setMessage(getString(R.string.delete_news_dialog_message, news.title))
            .setPositiveButton(getString(R.string.delete_action)) { dialog, _ ->
                deleteNewsFromFirebase(news)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel_action)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteNewsFromFirebase(news: News) {
        news.uid?.let { newsId ->
            newsRootRef.child(newsId).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Noticia '${news.title}' eliminada exitosamente.")
                    Toast.makeText(requireContext(), getString(R.string.news_deleted_successfully), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al eliminar noticia '${news.title}': ${e.message}", e)
                    Toast.makeText(requireContext(), getString(R.string.error_deleting_news), Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e(TAG, "Intento de eliminar noticia con UID nulo: ${news.title}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        newsListener?.let {
            newsRootRef.orderByChild("timestamp").removeEventListener(it)
        }
        newsListener = null
        _binding = null
    }
}