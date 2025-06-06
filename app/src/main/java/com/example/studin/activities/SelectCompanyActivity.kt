package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.R
import com.example.studin.adapters.SelectCompanyAdapter
import com.example.studin.classes.Company
import com.example.studin.databinding.ActivitySelectCompanyBinding // Importar ViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SelectCompanyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectCompanyBinding
    private lateinit var companyAdapter: SelectCompanyAdapter
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

        databaseRef = FirebaseDatabase.getInstance().getReference("companies") // MODIFICA ESTA RUTA SEGÚN TU ESTRUCTURA

        loadCompanies()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarSelectCompany)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        companyAdapter = SelectCompanyAdapter(companyList) { selectedCompany ->
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
                // Manejar el error, aunque no debería ocurrir si los datos se cargan correctamente.
            }
        }
        binding.recyclerViewCompanies.apply {
            layoutManager = LinearLayoutManager(this@SelectCompanyActivity)
            adapter = companyAdapter
            // addItemDecoration(...) // Opcional: para divisores
        }
    }

    private fun loadCompanies() {
        binding.progressBarSelectCompany.visibility = View.VISIBLE
        binding.textViewNoCompanies.visibility = View.GONE
        binding.recyclerViewCompanies.visibility = View.GONE

        companiesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                companyList.clear()
                if (snapshot.exists()) {
                    for (companySnapshot in snapshot.children) {
                        // Asumimos que la clave del snapshot es el UID de la empresa
                        val companyUid = companySnapshot.key
                        val companyData = companySnapshot.getValue(Company::class.java)

                        if (companyData != null && companyUid != null) {
                            companyData.uid = companyUid // Asignar el UID desde la clave del snapshot

                            // IMPORTANTE: Evitar que el usuario se seleccione a sí mismo si las "empresas" son también usuarios.
                            if (companyUid != currentUserId) {
                                companyList.add(companyData)
                            }
                        }
                    }
                }

                companyAdapter.notifyDataSetChanged() // Notificar al adaptador sobre los nuevos datos

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
                // Podrías mostrar un Toast o un Snackbar con el error
            }
        }
        // Usar addValueEventListener para carga inicial y actualizaciones en tiempo real.
        // Si solo necesitas una carga única, usa addListenerForSingleValueEvent.
        databaseRef.addValueEventListener(companiesListener!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar el clic en el botón de atrás de la Toolbar
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el listener de Firebase para evitar fugas de memoria
        companiesListener?.let {
            databaseRef.removeEventListener(it)
        }
    }
}