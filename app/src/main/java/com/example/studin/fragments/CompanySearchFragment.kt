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
import com.example.studin.activities.CompanyProfileActivity
import com.example.studin.adapters.CompanySearchAdapter
import com.example.studin.classes.Company
import com.example.studin.databinding.FragmentCompanySearchBinding
import com.google.firebase.database.*

class CompanySearchFragment : Fragment() {

    private var _binding: FragmentCompanySearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var companyAdapter: CompanySearchAdapter
    private val allCompaniesList =
        mutableListOf<Company>()
    private var selectedIndustry: String = "Todas"

    private lateinit var database: FirebaseDatabase
    private lateinit var companiesRef: DatabaseReference

    private val TAG = "CompanySearchFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompanySearchBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        companiesRef = database.getReference("companies")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "View Created")

        setupRecyclerView()
        setupSearchView()
        setupCompanyFilters()

        fetchAllCompaniesOnce()
    }

    private fun setupRecyclerView() {
        companyAdapter = CompanySearchAdapter { company ->
            if (company.uid != null) {
                Log.d(TAG, "Empresa clicada: ${company.name}, UID: ${company.uid}")
                val intent = Intent(requireActivity(), CompanyProfileActivity::class.java)
                intent.putExtra("COMPANY_ID", company.uid) // Pasa el UID a la actividad de perfil
                startActivity(intent)
            } else {
                Log.w(TAG, "Empresa clicada sin UID: ${company.name}")
                Toast.makeText(requireContext(), "No se puede abrir el perfil, falta información.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.companiesRecyclerViewFragment.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = companyAdapter
        }
        Log.d(TAG, "RecyclerView Setup Completed")
    }

    private fun setupSearchView() {
        binding.searchViewCompaniesFragment.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCompanies(query ?: "", selectedIndustry)
                binding.searchViewCompaniesFragment.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCompanies(newText ?: "", selectedIndustry)
                return true
            }
        })
        Log.d(TAG, "SearchView Setup Completed")
    }

    private fun setupCompanyFilters() {
        val industries = arrayOf(
            "Todas",
            "Tecnología",
            "Educación",
            "Salud",
            "Finanzas",
            "Retail"
        )
        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, industries)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCompanyIndustry.adapter = spinnerAdapter

        binding.spinnerCompanyIndustry.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedIndustry = industries[position]
                    Log.d(TAG, "Industria seleccionada: $selectedIndustry")
                    filterCompanies(
                        binding.searchViewCompaniesFragment.query.toString(),
                        selectedIndustry
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        Log.d(TAG, "Spinner Setup Completed")
    }

    private fun fetchAllCompaniesOnce() {
        binding.progressBarCompaniesFragment.isVisible = true
        binding.textViewNoCompaniesFragment.isVisible = false
        binding.companiesRecyclerViewFragment.isVisible = false

        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allCompaniesList.clear()
                Log.d(TAG, "fetchAllCompaniesOnce - onDataChange. Snapshot exists: ${snapshot.exists()}")
                if (snapshot.exists()) {
                    for (companySnapshot in snapshot.children) {
                        Log.d(TAG, "fetchAllCompaniesOnce - Processing snapshot key: ${companySnapshot.key}")
                        val company = companySnapshot.getValue(Company::class.java)
                        if (company != null) {
                            Log.d(TAG, "fetchAllCompaniesOnce - Company object parsed: ${company.name}")
                            // Asignar el uid
                            company.uid = companySnapshot.key
                            Log.d(TAG, "fetchAllCompaniesOnce - Company UID set to: ${company.uid} for company: ${company.name}")
                            allCompaniesList.add(company)
                        } else {
                            Log.w(TAG, "fetchAllCompaniesOnce - Failed to parse company from snapshot key: ${companySnapshot.key}")
                        }
                    }
                    Log.d(TAG, "Todas las empresas cargadas: ${allCompaniesList.size}")
                    allCompaniesList.forEach { comp ->
                        Log.d(TAG, "Loaded company in allCompaniesList: Name=${comp.name}, UID=${comp.uid}")
                    }
                } else {
                    Log.d(TAG, "No existen empresas en la base de datos.")
                }
                filterCompanies(binding.searchViewCompaniesFragment.query.toString(), selectedIndustry)
                binding.progressBarCompaniesFragment.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarCompaniesFragment.isVisible = false
                binding.textViewNoCompaniesFragment.text =
                    "Error al cargar la lista de empresas."
                binding.textViewNoCompaniesFragment.isVisible = true
                binding.companiesRecyclerViewFragment.isVisible = false
                Log.e(
                    TAG,
                    "Error al cargar todas las empresas: ${error.message}",
                    error.toException()
                )
                Toast.makeText(
                    context,
                    "Error al cargar datos: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun filterCompanies(query: String, industry: String) {
        val normalizedQuery = query.trim().lowercase()
        Log.d(TAG, "Filtrando con query: '$normalizedQuery', industria: '$industry'")

        val filteredList = allCompaniesList.filter { company ->
            val nameMatches = company.name?.lowercase()?.contains(normalizedQuery) ?: false
            val industryMatches = if (industry == "Todas") {
                true
            } else {
                company.industry?.equals(industry, ignoreCase = true) ?: false
            }
            nameMatches && industryMatches
        }

        Log.d(TAG, "Empresas filtradas: ${filteredList.size}")
        if (::companyAdapter.isInitialized) { // Comprobar si el adaptador está inicializado
            companyAdapter.submitList(filteredList.toList())
        }

        // Definir estas variables después de actualizar adapatador
        val queryIsNotEmpty = query.trim().isNotEmpty()
        val industryIsNotDefault = industry != "Todas"

        // Lógica para la visibilidad
        if (filteredList.isNotEmpty()) {
            binding.companiesRecyclerViewFragment.isVisible = true
            binding.textViewNoCompaniesFragment.isVisible = false
        } else {
            binding.companiesRecyclerViewFragment.isVisible = false
            binding.textViewNoCompaniesFragment.isVisible = true

            if (allCompaniesList.isEmpty() && !queryIsNotEmpty && industry == "Todas") {
                binding.textViewNoCompaniesFragment.text = "Actualmente no hay empresas registradas." // TEXTO DIRECTO
            } else if (queryIsNotEmpty && industryIsNotDefault) {
                binding.textViewNoCompaniesFragment.text = "No se encontraron empresas para \"$query\" en la industria \"$industry\"." // TEXTO DIRECTO
            } else if (queryIsNotEmpty) {
                binding.textViewNoCompaniesFragment.text = "No se encontraron empresas para \"$query\"." // TEXTO DIRECTO
            } else if (industryIsNotDefault) {
                binding.textViewNoCompaniesFragment.text = "No se encontraron empresas en la industria \"$industry\"." // TEXTO DIRECTO
            } else {
                binding.textViewNoCompaniesFragment.text = "No hay empresas para mostrar con los filtros actuales." // TEXTO DIRECTO
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "View Destroyed, binding nulled")

    }
}