package com.example.studin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(fragment: MainChats) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChatsFragment() // <<---- Se ha añadido aquí.
            1 -> StatusFragment()
            2 -> CallsFragment()
            else -> ChatsFragment()
        }
    }
}