package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.adapters.CompanySearchAdapter
import com.example.studin.classes.Company
import com.example.studin.databinding.ActivitySelectCompanyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SelectCompanyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectCompanyBinding
    private lateinit var companyAdapter: CompanySearchAdapter
    private val companyList = ArrayList<Company>()
    private lateinit var databaseRef: DatabaseReference
    private var companiesListener: ValueEventListener? = null
    private var currentUserId: String? = null

    companion object {
        private const val TAG = "SelectCompanyActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        setupToolbar()
        setupRecyclerView()

        databaseRef = FirebaseDatabase.getInstance().getReference("companies")
        loadCompanies() // Carga los datos y los envía al adaptador
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarSelectCompany)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        companyAdapter = CompanySearchAdapter { selectedCompany ->
            // Cuando se hace clic en una empresa
            if (selectedCompany.uid != null && selectedCompany.name != null) {
                val resultIntent = Intent()
                resultIntent.putExtra("SELECTED_COMPANY_ID", selectedCompany.uid)
                resultIntent.putExtra("SELECTED_COMPANY_NAME", selectedCompany.name)
                resultIntent.putExtra("SELECTED_COMPANY_AVATAR_URL", selectedCompany.profileImageUrl) // Opcional
                Log.d(TAG, "Empresa seleccionada: ${selectedCompany.name}, UID: ${selectedCompany.uid}, Avatar URL: ${selectedCompany.profileImageUrl}")
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Cierra esta actividad y vuelve a MainChatsActivity
            } else {
                Log.w(TAG, "Empresa seleccionada con datos nulos: $selectedCompany")
            }
        }

        binding.recyclerViewCompanies.apply {
            layoutManager = LinearLayoutManager(this@SelectCompanyActivity)
            adapter = companyAdapter
        }
    }

    private fun loadCompanies() {
        binding.progressBarSelectCompany.visibility = View.VISIBLE
        binding.textViewNoCompanies.visibility = View.GONE
        binding.recyclerViewCompanies.visibility = View.GONE

        companiesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<Company>() // Lista temporal para construir datos nuevos
                if (snapshot.exists()) {
                    for (companySnapshot in snapshot.children) {
                        val companyUid = companySnapshot.key
                        val companyData = companySnapshot.getValue(Company::class.java)

                        if (companyData != null && companyUid != null) {
                            companyData.uid = companyUid // Asignar el UID desde la clave del snapshot
                            if (companyUid != currentUserId) { // Evitar que el usuario se seleccione a sí mismo
                                tempList.add(companyData)
                            }
                        }
                    }
                }

                companyList.clear()
                companyList.addAll(tempList)
                companyAdapter.submitList(companyList.toList())

                // Actualizar visibilidad de UI
                if (companyList.isEmpty()) {
                    binding.textViewNoCompanies.visibility = View.VISIBLE
                    binding.recyclerViewCompanies.visibility = View.GONE
                } else {
                    binding.textViewNoCompanies.visibility = View.GONE
                    binding.recyclerViewCompanies.visibility = View.VISIBLE
                }
                binding.progressBarSelectCompany.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar empresas: ${error.message}")
                binding.progressBarSelectCompany.visibility = View.GONE
                binding.textViewNoCompanies.text = "Error al cargar empresas."
                binding.textViewNoCompanies.visibility = View.VISIBLE
                binding.recyclerViewCompanies.visibility = View.GONE
            }
        }
        databaseRef.addValueEventListener(companiesListener!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed() // Forma moderna de manejar el botón de atrás
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        companiesListener?.let {
            databaseRef.removeEventListener(it)
        }
    }
}