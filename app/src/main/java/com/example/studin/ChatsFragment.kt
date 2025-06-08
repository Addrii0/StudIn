package com.example.studin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.activities.ChatMessageActivity
import com.example.studin.adapters.ChatAdapter
import com.example.studin.classes.Chat
import com.example.studin.databinding.FragmentChatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class ChatsFragment : Fragment() {

    // Declarar una variable para el binding. Debe ser nullable y gestionada
    // cuidadosamente durante el ciclo de vida del fragment.
    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!! // !! es seguro aquí debido al ciclo de vida

    private lateinit var chatAdapter: ChatAdapter


    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var database: FirebaseDatabase

    private var userChatRoomsListener: ValueEventListener? = null
    private lateinit var userChatRoomsRef: DatabaseReference
    private val chatList = mutableListOf<Chat>()
    private val chatRoomListeners = mutableMapOf<String, ValueEventListener>()
    private val chatRoomRefs = mutableMapOf<String, DatabaseReference>()

    companion object {
        private const val TAG = "ChatsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUser = auth.currentUser

        setupRecyclerView()

        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado.")
            binding.textViewNoChats.text = "Por favor, inicia sesión para ver tus chats."
            binding.textViewNoChats.visibility = View.VISIBLE
            binding.progressBarChats.visibility = View.GONE
            return
        }
        loadUserChatRoomIds()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList) { clickedChat ->
            val intent = Intent(requireContext(), ChatMessageActivity::class.java).apply {
                putExtra("CHAT_ROOM_ID", clickedChat.chatRoomId)
                putExtra("OTHER_USER_ID", clickedChat.otherUserId)
                putExtra("OTHER_USER_NAME", clickedChat.otherUserName)
                putExtra("OTHER_USER_AVATAR_URL", clickedChat.otherUserAvatarUrl)
            }
            startActivity(intent)
        }
        binding.chatsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatsRecyclerView.adapter = chatAdapter
    }

    private fun loadUserChatRoomIds() {
        binding.progressBarChats.visibility = View.VISIBLE
        binding.textViewNoChats.visibility = View.GONE
        chatList.clear()

        val currentUid = currentUser?.uid ?: return

        userChatRoomsRef = database.getReference("users").child(currentUid).child("userChats")
        userChatRoomsListener?.let { userChatRoomsRef.removeEventListener(it) }

        userChatRoomsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clearChatRoomListeners()
                chatList.clear()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d(TAG, "El usuario $currentUid no tiene chats en userChats.")
                    updateUIBasedOnChatList()
                    return
                }

                Log.d(TAG, "Se encontraron ${snapshot.childrenCount} chatRoomIds para $currentUid")
                snapshot.children.forEach { chatRoomIdSnapshot ->
                    chatRoomIdSnapshot.key?.let { chatRoomId ->
                        Log.d(TAG, "Procesando chatRoomId: $chatRoomId")
                        fetchChatRoomDetails(chatRoomId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar los IDs de las salas de chat del usuario: ${error.message}")
                binding.progressBarChats.visibility = View.GONE
                binding.textViewNoChats.text = "Error al cargar chats."
                binding.textViewNoChats.visibility = View.VISIBLE
            }
        }
        userChatRoomsRef.addValueEventListener(userChatRoomsListener!!)
    }

    private fun fetchChatRoomDetails(chatRoomId: String) {
        val chatRoomRef = database.getReference("chat_rooms").child(chatRoomId)
        chatRoomRefs[chatRoomId] = chatRoomRef
        chatRoomListeners[chatRoomId]?.let { chatRoomRef.removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Detalles no encontrados para chatRoomId: $chatRoomId.")
                    removeChatFromList(chatRoomId)
                    updateUIBasedOnChatList()
                    return
                }
                val lastMessageText = snapshot.child("lastMessage/text").getValue(String::class.java) ?: ""
                val lastMessageTimestamp = snapshot.child("lastMessage/timestamp").getValue(Long::class.java) ?: 0L
                val lastMessageSenderId = snapshot.child("lastMessage/senderId").getValue(String::class.java) ?: ""
                var otherUserId: String? = null
                snapshot.child("participants").children.forEach { participantSnapshot ->
                    val participantId = participantSnapshot.key
                    if (participantId != null && participantId != currentUser?.uid) {
                        otherUserId = participantId
                        return@forEach // Sale del bucle forEach
                    }
                }

                if (otherUserId == null) {
                    Log.e(TAG, "No se pudo determinar el otherUserId para chatRoomId: $chatRoomId")
                    removeChatFromList(chatRoomId)
                    updateUIBasedOnChatList()
                    return
                }

                fetchUserDetails(otherUserId) { otherUserName, otherUserAvatarUrl ->
                    val chat = Chat(
                        chatRoomId = chatRoomId,
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        otherUserAvatarUrl = otherUserAvatarUrl,
                        lastMessage = lastMessageText,
                        lastMessageTimestamp = lastMessageTimestamp,
                        lastMessageSenderId = lastMessageSenderId
                    )
                    updateChatInList(chat)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar detalles de chatRoom $chatRoomId: ${error.message}")
                removeChatFromList(chatRoomId)
                updateUIBasedOnChatList()
            }
        }
        chatRoomRef.addValueEventListener(listener)
        chatRoomListeners[chatRoomId] = listener
    }

    private fun fetchUserDetails(userId: String, onResult: (name: String, avatarUrl: String?) -> Unit) {
        val userRef = database.getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Usuario"
                    val avatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    onResult(name, avatarUrl)
                } else {
                    Log.w(TAG, "Detalles no encontrados para el usuario: $userId")
                    onResult("Usuario Desconocido", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar detalles del usuario $userId: ${error.message}")
                onResult("Error Usuario", null)
            }
        })
    }

    private fun updateChatInList(chat: Chat) {
        val existingChatIndex = chatList.indexOfFirst { it.chatRoomId == chat.chatRoomId }
        if (existingChatIndex != -1) {
            chatList[existingChatIndex] = chat
        } else {
            chatList.add(chat)
        }
        chatList.sortByDescending { it.lastMessageTimestamp }
        updateUIBasedOnChatList()
    }

    private fun removeChatFromList(chatRoomId: String) {
        val removed = chatList.removeAll { it.chatRoomId == chatRoomId }
        if (removed) {
            updateUIBasedOnChatList()
        }
    }

    private fun updateUIBasedOnChatList() {
        if (chatList.isEmpty()) {
            binding.textViewNoChats.text = "No tienes chats."
            binding.textViewNoChats.visibility = View.VISIBLE
            binding.chatsRecyclerView.visibility = View.GONE
        } else {
            binding.textViewNoChats.visibility = View.GONE
            binding.chatsRecyclerView.visibility = View.VISIBLE
            // Es buena práctica pasar una nueva lista al adaptador para que DiffUtil (si lo usaras) funcione correctamente
            // y para evitar modificaciones concurrentes si el adapter trabaja en otro hilo (aunque aquí no es el caso).
            chatAdapter.submitList(ArrayList(chatList))
        }
        binding.progressBarChats.visibility = View.GONE
    }

    private fun clearChatRoomListeners() {
        chatRoomListeners.forEach { (chatRoomId, listener) ->
            chatRoomRefs[chatRoomId]?.removeEventListener(listener)
        }
        chatRoomListeners.clear()
        chatRoomRefs.clear()
        Log.d(TAG, "Listeners de salas de chat individuales limpiados.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar todos los listeners de Firebase para evitar fugas de memoria
        userChatRoomsListener?.let {
            if (::userChatRoomsRef.isInitialized) { // Comprobar si userChatRoomsRef fue inicializado
                userChatRoomsRef.removeEventListener(it)
            }
        }
        userChatRoomsListener = null
        clearChatRoomListeners()
        Log.d(TAG, "Todos los listeners de Firebase en ChatsFragment limpiados.")

        _binding = null // Muy importante: Limpiar la referencia al binding para evitar memory leaks
    }
}