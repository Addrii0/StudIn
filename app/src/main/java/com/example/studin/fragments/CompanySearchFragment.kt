package com.example.studin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment

import com.example.studin.databinding.FragmentCompanySearchBinding

class CompanySearchFragment : Fragment() {

    private var _binding: FragmentCompanySearchBinding? = null
    private val binding get() = _binding!!

    // private lateinit var companyAdapter: CompanyAdapter
    // private val companyList = mutableListOf<Company>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompanySearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setupRecyclerView()
        // setupSearchView()
        // setupCompanyFilters()
        // searchCompanies("")
        Log.d("CompanySearchFragment", "View Created") // Ejemplo de Log
        val industries = arrayOf("Todas", "Tecnología", "Educación", "Salud")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, industries)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCompanyIndustry.adapter = spinnerAdapter
    }

    // Implementa setupRecyclerView, setupSearchView, setupCompanyFilters y searchCompanies
    // igual q en offerSearchFragment

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}