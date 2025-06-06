package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.MainPagerAdapter
import com.example.studin.databinding.ActivityChatBinding // Importa la clase de binding generada
import com.google.android.material.tabs .TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainChatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding // Declara la variable de binding

    private val selectCompanyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedCompanyId = data?.getStringExtra("SELECTED_COMPANY_ID")
            val selectedCompanyName = data?.getStringExtra("SELECTED_COMPANY_NAME")
            val selectedCompanyAvatarUrl = data?.getStringExtra("SELECTED_COMPANY_AVATAR_URL")

            if (selectedCompanyId != null && selectedCompanyName != null) {
                getOrCreateChatRoomAndNavigate(selectedCompanyId, selectedCompanyName ,selectedCompanyAvatarUrl )
            } else {
                Log.w("MainChatsActivity", "No se recibió ID o nombre de empresa de SelectCompanyActivity")
                Toast.makeText(this, "No se seleccionó ninguna empresa.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("MainChatsActivity", "Selección de empresa cancelada o fallida.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando la clase de binding y establece el contenido de la vista
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Accede a las vistas a través del objeto binding
        setSupportActionBar(binding.toolbar)

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter // Usa binding.viewPager

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position -> // Usa binding.tabLayout y binding.viewPager
            when (position) {
                0 -> tab.text = "Chats"
                1 -> tab.text = "Status"
                2 -> tab.text = "Calls"
            }
        }.attach()

        binding.fab.setOnClickListener {
            val intent = Intent(this, SelectCompanyActivity::class.java)
            selectCompanyLauncher.launch(intent)
        }
        handleIntentToStartChat()
    }

    private fun getOrCreateChatRoomAndNavigate(otherUserId: String, otherUserName: String , otherUserAvatarUrl: String? ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión para chatear.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == otherUserId) {
            Toast.makeText(this, "No puedes chatear contigo mismo.", Toast.LENGTH_SHORT).show()
            return
        }

        // val progressDialog = ProgressDialog.show(this, "", "Iniciando chat...", true)

        val chatRoomId = if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }

        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chat_rooms").child(chatRoomId)

        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // progressDialog.dismiss()
                if (snapshot.exists()) {
                    Log.d("MainChatsActivity", "Chat room $chatRoomId ya existe. Navegando...")
                    updateUserChatEntries(currentUserId, otherUserId, otherUserName, chatRoomId, snapshot.child("lastMessage/timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),otherUserAvatarUrl)
                    navigateToChatMessageActivity(chatRoomId, otherUserId, otherUserName , otherUserAvatarUrl )
                } else {
                    Log.d("MainChatsActivity", "Chat room $chatRoomId no existe. Creando y navegando...")
                    createNewChatRoom(currentUserId, otherUserId, otherUserName, chatRoomId , otherUserAvatarUrl )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // progressDialog.dismiss()
                Log.e("MainChatsActivity", "Error al comprobar chat_room: ${error.message}")
                Toast.makeText(this@MainChatsActivity, "Error al iniciar chat: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNewChatRoom(currentUserId: String, otherUserId: String, otherUserName: String, chatRoomId: String , otherUserAvatarUrl: String? ) {
        val participants = mapOf(
            currentUserId to true,
            otherUserId to true
        )
        val createdAt = System.currentTimeMillis()
        var currentUserNameFromDB = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuario"
        var currentUserAvatarFromDB: String? = null

        val newChatRoomData = mapOf(
            "participants" to participants,
            "createdAt" to createdAt
        )

        val currentUserChatEntry = mapOf(
            "otherUserId" to otherUserId,
            "otherUserName" to otherUserName,
            "otherUserAvatarUrl" to otherUserAvatarUrl,
            "lastMessageTimestamp" to createdAt
        )

        val otherUserChatEntry = mapOf(
            "otherUserId" to currentUserId,
            "otherUserName" to currentUserNameFromDB,
            "otherUserAvatarUrl" to currentUserAvatarFromDB,
            "lastMessageTimestamp" to createdAt
        )

        val rootRef = FirebaseDatabase.getInstance().reference
        val updates = hashMapOf<String, Any?>(
            "/chat_rooms/$chatRoomId" to newChatRoomData,
            "/users/$currentUserId/userChats/$chatRoomId" to currentUserChatEntry,
            "/users/$otherUserId/userChats/$chatRoomId" to otherUserChatEntry
        )

        // val progressDialog = ProgressDialog.show(this, "", "Creando chat...", true)
        rootRef.updateChildren(updates)
            .addOnSuccessListener {
                // progressDialog.dismiss()
                Log.d("MainChatsActivity", "Chat room $chatRoomId y userChats creados.")
                navigateToChatMessageActivity(chatRoomId, otherUserId, otherUserName , otherUserAvatarUrl )
            }
            .addOnFailureListener { e ->
                // progressDialog.dismiss()
                Log.e("MainChatsActivity", "Error al crear chat room y userChats: ${e.message}")
                Toast.makeText(this, "Error al crear chat: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Función para actualizar las entradas de userChats si el chat ya existe (opcional pero bueno para mantener consistencia)
    private fun updateUserChatEntries(currentUserId: String, otherUserId: String, otherUserName: String, chatRoomId: String, lastTimestamp: Long , otherUserAvatarUrl: String? ) {
        // Obtener nombre y avatar del usuario actual (si los guardas en /users)
        // Esto es un ejemplo, necesitarás tu propia lógica para obtener estos datos si los quieres
        var currentUserNameFromDB = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuario"
        var currentUserAvatarFromDB: String? = null // Lógica para obtener tu avatar

        val currentUserChatData = mapOf(
            "otherUserId" to otherUserId,
            "otherUserName" to otherUserName, // Nombre de la empresa
            "otherUserAvatarUrl" to otherUserAvatarUrl,
            "lastMessageTimestamp" to lastTimestamp // Usa el timestamp existente o el nuevo si es más reciente
        )

        val otherUserChatData = mapOf(
            "otherUserId" to currentUserId,
            "otherUserName" to currentUserNameFromDB,
            "otherUserAvatarUrl" to currentUserAvatarFromDB,
            "lastMessageTimestamp" to lastTimestamp
        )

        val rootRef = FirebaseDatabase.getInstance().reference
        val updates = hashMapOf<String, Any?>(
            "/users/$currentUserId/userChats/$chatRoomId" to currentUserChatData,
            "/users/$otherUserId/userChats/$chatRoomId" to otherUserChatData
        )
        // Actualiza sin esperar un callback necesariamente, ya que es más una tarea de "mantenimiento"
        rootRef.updateChildren(updates)
            .addOnSuccessListener { Log.d("MainChatsActivity", "UserChats actualizados para $chatRoomId") }
            .addOnFailureListener { e -> Log.w("MainChatsActivity", "Fallo al actualizar userChats para $chatRoomId: ${e.message}")}
    }
    private fun navigateToChatMessageActivity(chatRoomId: String, otherUserId: String, otherUserName: String , otherUserAvatarUrl: String? ) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra("CHAT_ROOM_ID", chatRoomId)
            putExtra("OTHER_USER_ID", otherUserId)
            putExtra("OTHER_USER_NAME", otherUserName)
            putExtra("OTHER_USER_AVATAR_URL", otherUserAvatarUrl) // Si lo tienes y lo usas
        }
        startActivity(intent)
    }
    private fun handleIntentToStartChat() {
        val targetUserId = intent.getStringExtra("ACTION_START_CHAT_WITH_USER_ID")
        val targetUserName = intent.getStringExtra("ACTION_START_CHAT_WITH_USER_NAME")
        val targetUserAvatarUrl = intent.getStringExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL")

        if (targetUserId != null && targetUserName != null) {
            Log.d(
                "MainChatsActivity",
                "Intención recibida para iniciar chat con: $targetUserName ($targetUserId)"
            )
            // Asegúrate de que las claves aquí coincidan con las que usas para poner los extras
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_ID")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_NAME")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL")

            getOrCreateChatRoomAndNavigate(targetUserId, targetUserName, targetUserAvatarUrl)
        }
    }
}
