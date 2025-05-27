package com.example.studin.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.studin.MainPagerAdapter
import com.example.studin.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainChatsActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var fab: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 1. Encuentra las vistas en el layout:
        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        fab = findViewById(R.id.fab)
        // 2. Configura la Toolbar:
        setSupportActionBar(toolbar)
        // 3. Configura el ViewPager2:
        val adapter = MainPagerAdapter(this) // Crea el adaptador
        viewPager.adapter = adapter // Asigna el adaptador al ViewPager2

        // 4. Conecta el ViewPager2 con el TabLayout:
        val tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Chats"
                1 -> tab.text = "Status"
                2 -> tab.text = "Calls"
            }
        }
        tabLayoutMediator.attach() // Este es el que une el viewpager con el tabLayout.
        // 5. Funcionalidad del FAB:
        fab.setOnClickListener {
            //Aquí puedes poner lo que quieras hacer al pulsar el boton
        }
    }
}