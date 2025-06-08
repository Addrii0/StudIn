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

    private lateinit var binding: ActivityChatBinding
    private val TAG = "MainChatsActivity"

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
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

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

        val chatRoomId = if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }

        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chat_rooms").child(chatRoomId)

        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                Log.e("MainChatsActivity", "Error al comprobar chat_room: ${error.message}")
                Toast.makeText(this@MainChatsActivity, "Error al iniciar chat: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNewChatRoom(
        currentUserId: String,
        otherUserId: String,
        otherUserName: String,
        chatRoomId: String,
        otherUserAvatarUrl: String?
    ) {
        val currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)

        currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val currentUserNameFromDB = userSnapshot.child("name").getValue(String::class.java) ?: // Ojo aquí, el nombre de tu user
                FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuario"
                val currentUserAvatarFromDB = userSnapshot.child("profileImageUrl").getValue(String::class.java) // Avatar del usuario actual

                Log.d(TAG, "Datos del usuario actual para chat: Nombre='$currentUserNameFromDB', AvatarURL='$currentUserAvatarFromDB'")

                val participants = mapOf(
                    currentUserId to true,
                    otherUserId to true
                )
                val createdAt = System.currentTimeMillis()

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

                rootRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Chat room $chatRoomId y userChats creados.")
                        navigateToChatMessageActivity(chatRoomId, otherUserId, otherUserName, otherUserAvatarUrl)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al crear chat room y userChats: ${e.message}")
                        Toast.makeText(this@MainChatsActivity, "Error al crear chat: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener datos del usuario actual ($currentUserId) para crear chat: ${error.message}")
                Toast.makeText(this@MainChatsActivity, "Error al obtener tus datos para el chat.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Función para actualizar las entradas de userChats si el chat ya existe
    private fun updateUserChatEntries(currentUserId: String, otherUserId: String, otherUserName: String, chatRoomId: String, lastTimestamp: Long , otherUserAvatarUrl: String? ) {
         var currentUserNameFromDB = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuario"
        var currentUserAvatarFromDB: String? = null // Lógica para obtener tu avatar

        val currentUserChatData = mapOf(
            "otherUserId" to otherUserId,
            "otherUserName" to otherUserName,
            "otherUserAvatarUrl" to otherUserAvatarUrl,
            "lastMessageTimestamp" to lastTimestamp
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
        rootRef.updateChildren(updates)
            .addOnSuccessListener { Log.d("MainChatsActivity", "UserChats actualizados para $chatRoomId") }
            .addOnFailureListener { e -> Log.w("MainChatsActivity", "Fallo al actualizar userChats para $chatRoomId: ${e.message}")}
    }
    private fun navigateToChatMessageActivity(chatRoomId: String, otherUserId: String, otherUserName: String , otherUserAvatarUrl: String? ) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra("CHAT_ROOM_ID", chatRoomId)
            putExtra("OTHER_USER_ID", otherUserId)
            putExtra("OTHER_USER_NAME", otherUserName)
            putExtra("OTHER_USER_AVATAR_URL", otherUserAvatarUrl)
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
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_ID")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_NAME")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL")

            getOrCreateChatRoomAndNavigate(targetUserId, targetUserName, targetUserAvatarUrl)
        }

    }
}
