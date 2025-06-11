package com.example.studin.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.studin.fragments.NewsManagementFragment
import com.example.studin.fragments.OffersManagementFragment

class CompanyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2 // Número de pestañas totates

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OffersManagementFragment() // Primer fragment
            1 -> NewsManagementFragment()   // Segundo fragment
            else -> throw IllegalStateException("Posición de fragment inválida: $position")
        }
    }
}