package com.example.studin.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.R
import com.example.studin.adapters.UserOfferPagerAdapter
import com.example.studin.databinding.ActivityUserOffersBinding
import com.google.android.material.tabs.TabLayoutMediator

class UserOfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOffersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarUserOffer)

        // Configurar el ViewPager2 y el TabLayout
        val adapter = UserOfferPagerAdapter(this)
        binding.viewPagerUserOffer.adapter = adapter

        TabLayoutMediator(binding.tabLayoutUserOffer, binding.viewPagerUserOffer) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_offers)
                1 -> getString(R.string.tab_companies)
                2 -> getString(R.string.tab_news)
                else -> null
            }
        }.attach()
    }
}