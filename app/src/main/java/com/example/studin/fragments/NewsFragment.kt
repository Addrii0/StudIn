package com.example.studin.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.activities.NewsDetailActivity
import com.example.studin.adapters.NewsAdapter
import com.example.studin.classes.News
import com.example.studin.databinding.FragmentNewsBinding
import com.google.firebase.database.*

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var newsAdapter: NewsAdapter
    private val allNewsList = mutableListOf<News>()
    private var selectedCategory: String = "Todas"

    private lateinit var database: FirebaseDatabase
    private lateinit var newsRef: DatabaseReference

    private val TAG = "NewsFragment"
    private val newsCategories = arrayOf("Todas", "Tecnología", "Startups", "Eventos", "Anuncios")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        newsRef = database.getReference("news")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "View Created")

        setupRecyclerView()
        setupSearchView()
        setupNewsFiltersSpinner()
        fetchNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter { newsItem ->
            val intent = Intent(requireActivity(), NewsDetailActivity::class.java)
             intent.putExtra(NewsDetailActivity.EXTRA_NEWS_ID, newsItem.uid)
            startActivity(intent)
        }

        binding.newsRecyclerViewFragment.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
        }
        Log.d(TAG, "RecyclerView Setup Completed")
    }

    private fun setupSearchView() {
        binding.searchViewNewsFragment.setOnQueryTextListener(object : SearchView.OnQueryTextListener { // ID CORREGIDO
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterNewsList(query ?: "")
                binding.searchViewNewsFragment.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterNewsList(newText ?: "")
                return true
            }
        })
        Log.d(TAG, "SearchView Configurado")
    }


    private fun setupNewsFiltersSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, newsCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNewsCategory.adapter = spinnerAdapter

        binding.spinnerNewsCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = newsCategories[position]
                Log.d(TAG, "Categoría seleccionada: $selectedCategory")
                filterNewsList(binding.searchViewNewsFragment.query.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action
            }
        }
        Log.d(TAG, "Spinner de Categorías Configurado")
    }

    private fun fetchNews() {
        binding.progressBarNewsFragment.isVisible = true
        binding.textViewNoNewsFragment.isVisible = false
        binding.newsRecyclerViewFragment.isVisible = false

        newsRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allNewsList.clear()
                    if (snapshot.exists()) {
                        for (newsSnapshot in snapshot.children.reversed()) {
                            val newsItem = newsSnapshot.getValue(News::class.java)
                            newsItem?.let {
                                it.uid = newsSnapshot.key
                                allNewsList.add(it)
                            }
                        }
                        Log.d(TAG, "Noticias cargadas: ${allNewsList.size}")
                    } else {
                        Log.d(TAG, "No existen noticias en la base de datos.")
                    }
                    // Filtra con la query y la categoría seleccionada
                    filterNewsList(binding.searchViewNewsFragment.query.toString())
                    binding.progressBarNewsFragment.isVisible = false
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBarNewsFragment.isVisible = false
                    binding.textViewNoNewsFragment.text = "Error al cargar las noticias."
                    binding.textViewNoNewsFragment.isVisible = true
                    binding.newsRecyclerViewFragment.isVisible = false
                    Log.e(TAG, "Error al cargar noticias: ${error.message}", error.toException())
                    Toast.makeText(context, "Error al cargar noticias: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterNewsList(searchQuery: String = "") {
        val normalizedQuery = searchQuery.trim().lowercase()
        Log.d(TAG, "Filtrando noticias con query: '$normalizedQuery', categoría: '$selectedCategory'")

        val filteredList = allNewsList.filter { news ->
            val categoryMatches = if (selectedCategory == "Todas") {
                true // Si la categoría es "Todas", se muestran todas
            } else {
                news.category.equals(selectedCategory, ignoreCase = true)
            }

            // Lógica de búsqueda por texto (si searchQuery no está vacío)
            val textMatches = if (normalizedQuery.isEmpty()) {
                true
            } else {
                (news.title?.lowercase()?.contains(normalizedQuery) ?: false) ||
                        (news.content?.lowercase()?.contains(normalizedQuery) ?: false) ||
                        (news.authorName?.lowercase()?.contains(normalizedQuery) ?: false) ||
                        (news.tags?.any { tag -> tag.lowercase().contains(normalizedQuery) } ?: false)
            }

            categoryMatches && textMatches // Ambas se cumplen
        }

        Log.d(TAG, "Noticias filtradas: ${filteredList.size}")
        if (::newsAdapter.isInitialized) {
            newsAdapter.submitList(filteredList.toList())
        }


        val queryIsNotEmpty = normalizedQuery.isNotEmpty()
        val categoryIsNotDefault = selectedCategory != "Todas"

        if (filteredList.isNotEmpty()) {
            binding.newsRecyclerViewFragment.isVisible = true
            binding.textViewNoNewsFragment.isVisible = false
        } else {
            binding.newsRecyclerViewFragment.isVisible = false
            binding.textViewNoNewsFragment.isVisible = true

            if (allNewsList.isEmpty() && !queryIsNotEmpty && !categoryIsNotDefault) {
                binding.textViewNoNewsFragment.text = "Actualmente no hay noticias publicadas."
            } else if (queryIsNotEmpty && categoryIsNotDefault) {
                binding.textViewNoNewsFragment.text = "No se encontraron noticias para \"$normalizedQuery\" en la categoría \"$selectedCategory\"."
            } else if (queryIsNotEmpty) {
                binding.textViewNoNewsFragment.text = "No se encontraron noticias para \"$normalizedQuery\"."
            } else if (categoryIsNotDefault) {
                binding.textViewNoNewsFragment.text = "No se encontraron noticias en la categoría \"$selectedCategory\"."
            } else {
                binding.textViewNoNewsFragment.text = "No hay noticias para mostrar con los filtros actuales."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "View Destroyed, binding nulled")
    }
}