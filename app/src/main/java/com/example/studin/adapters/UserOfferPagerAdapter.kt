package com.example.studin.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.studin.fragments.CompanySearchFragment
import com.example.studin.fragments.NewsFragment
import com.example.studin.fragments.OfferSearchFragment

class UserOfferPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = listOf(
        OfferSearchFragment(),
        CompanySearchFragment(),
        NewsFragment()
    )

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}