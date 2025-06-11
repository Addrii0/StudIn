package com.example.studin.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.R
import com.example.studin.adapters.CompanyPagerAdapter // Importa tu nuevo PagerAdapter
import com.example.studin.databinding.ActivityCompanyOffersBinding // O como hayas llamado al layout
import com.google.android.material.tabs.TabLayoutMediator

// Considera renombrar esta Activity a algo como CompanyMainActivity o CompanyDashboardActivity
class CompanyOffersActivity : AppCompatActivity() {

    // Cambia el tipo de ViewBinding al del nuevo layout de la Activity
    private lateinit var binding: ActivityCompanyOffersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el nuevo layout
        binding = ActivityCompanyOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la Toolbar (opcional, pero común con TabLayout)
        setSupportActionBar(binding.toolbarCompany)
        // supportActionBar?.title = "Panel de Empresa" // O el título que prefieras

        // Configurar el ViewPager2 y el TabLayout
        val pagerAdapter = CompanyPagerAdapter(this)
        binding.viewPagerCompany.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutCompany, binding.viewPagerCompany) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_offers) // "Ofertas"
                1 -> getString(R.string.title_news)   // "Noticias"
                else -> null
            }
        }.attach()
    }

}