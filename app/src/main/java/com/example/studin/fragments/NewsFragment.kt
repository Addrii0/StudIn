package com.example.studin.fragments // Asegúrate que el package sea correcto

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.studin.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    // private lateinit var newsAdapter: NewsAdapter
    // private val newsList = mutableListOf<News>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setupRecyclerView()
        // setupSearchView()
        // setupNewsFilters()
        // loadNews()
        Log.d("NewsFragment", "View Created") // Ejemplo de Log
        val categories = arrayOf("Todas", "Tecnología", "Startups", "Eventos")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNewsCategory.adapter = spinnerAdapter

    }

    // Implementa setupRecyclerView, setupSearchView, setupNewsFilters y loadNews
    // y logica respectiva

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}