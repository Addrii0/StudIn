package com.example.studin


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatsFragment : Fragment() {

    private lateinit var chatAdapter: ChatAdapter // Aquí declaramos el adaptador

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.chats_fragmentados, container, false) // Aqui va el nuevo layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Encuentra la referencia al RecyclerView:
        val recyclerView = view.findViewById<RecyclerView>(R.id.chats_recycler_view)

        // 2. Establece el LayoutManager:
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 3. Crea el adaptador
        chatAdapter = ChatAdapter(mutableListOf())//Se ha añadido mutableListOf. Cuando añadas los datos, tendras que crear la lista y modificarla con addAll()
        // 4. Asigna el adaptador al RecyclerView:
        recyclerView.adapter = chatAdapter

        // Añade datos de ejemplo al adaptador.
        addExampleData() // Crea esta función para añadir datos de ejemplo
    }
    private fun addExampleData(){
        val exampleData: MutableList<Chat> = mutableListOf() // aqui crea una nueva lista
        exampleData.add(Chat("Persona 1","ultimo mensaje 1",/*Aquí la foto*/)) // ejemplo de como añadir chats a la lista, tienes que modificar el parametro con lo que quieras mostrar
        exampleData.add(Chat("Persona 2","ultimo mensaje 2",/*Aquí la foto*/))
        exampleData.add(Chat("Persona 3","ultimo mensaje 3",/*Aquí la foto*/))
        chatAdapter.addItems(exampleData) // añadimos los datos de ejemplo al adapter con esta funcion, recuerda que tienes que crearla.
    }
}
