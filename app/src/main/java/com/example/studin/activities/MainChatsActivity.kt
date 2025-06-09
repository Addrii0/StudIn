package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.studin.databinding.ActivityChatBinding
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
                getOrCreateChatRoomAndNavigate(
                    selectedCompanyId,
                    selectedCompanyName,
                    selectedCompanyAvatarUrl
                )
            } else {
                Log.w(TAG, "No se recibió ID o nombre de empresa de SelectCompanyActivity")
                Toast.makeText(this, "No se seleccionó ninguna empresa.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Selección de empresa cancelada o fallida.")
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

    private fun getOrCreateChatRoomAndNavigate(
        otherUserId: String,
        otherUserName: String,
        otherUserAvatarUrl: String?
    ) {
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

        val chatRoomRef =
            FirebaseDatabase.getInstance().getReference("chat_rooms").child(chatRoomId)

        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Chat room $chatRoomId ya existe. Navegando...")
                    updateUserChatEntries(
                        currentUserId,
                        otherUserId,
                        otherUserName,
                        chatRoomId,
                        snapshot.child("lastMessage/timestamp").getValue(Long::class.java)
                            ?: System.currentTimeMillis(),
                        otherUserAvatarUrl
                    )
                    navigateToChatMessageActivity(
                        chatRoomId,
                        otherUserId,
                        otherUserName,
                        otherUserAvatarUrl
                    )
                } else {
                    Log.d(TAG, "Chat room $chatRoomId no existe. Creando y navegando...")
                    createNewChatRoom(
                        currentUserId,
                        otherUserId,
                        otherUserName,
                        chatRoomId,
                        otherUserAvatarUrl
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al comprobar chat_room $chatRoomId: ${error.message}")
                Toast.makeText(
                    this@MainChatsActivity,
                    "Error al iniciar chat: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
        val currentUserRef =
            FirebaseDatabase.getInstance().getReference("users").child(currentUserId)

        currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val currentUserNameFromDB = userSnapshot.child("name").getValue(String::class.java)
                    ?: FirebaseAuth.getInstance().currentUser?.displayName // Fallback al display name de Auth
                    ?: "Usuario" // Fallback final
                val currentUserAvatarFromDB =
                    userSnapshot.child("profileImageUrl").getValue(String::class.java)

                Log.d(
                    TAG,
                    "Datos del usuario actual para chat: Nombre='$currentUserNameFromDB', AvatarURL='$currentUserAvatarFromDB'"
                )

                val participants = mapOf(
                    currentUserId to true,
                    otherUserId to true
                )
                val createdAt = System.currentTimeMillis()

                val newChatRoomData = mapOf(
                    "participants" to participants,
                    "createdAt" to createdAt,
                    "lastMessageTimestamp" to createdAt
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
                        navigateToChatMessageActivity(
                            chatRoomId,
                            otherUserId,
                            otherUserName,
                            otherUserAvatarUrl
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al crear chat room $chatRoomId y userChats: ${e.message}")
                        Toast.makeText(
                            this@MainChatsActivity,
                            "Error al crear chat: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Error al obtener datos del usuario actual ($currentUserId) para crear chat: ${error.message}"
                )
                Toast.makeText(
                    this@MainChatsActivity,
                    "Error al obtener tus datos para el chat.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateUserChatEntries(
        currentUserId: String,
        otherUserId: String,
        otherUserName: String,
        chatRoomId: String,
        lastKnownTimestamp: Long,
        otherUserAvatarUrl: String?
    ) {

        val currentUserRef =
            FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
        currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val currentUserNameFromDB = userSnapshot.child("name").getValue(String::class.java)
                    ?: FirebaseAuth.getInstance().currentUser?.displayName
                    ?: "Usuario"
                val currentUserAvatarFromDB =
                    userSnapshot.child("profileImageUrl").getValue(String::class.java)

                Log.d(
                    TAG,
                    "Actualizando UserChats. Datos actuales del usuario para el otro: Nombre='$currentUserNameFromDB', Avatar='$currentUserAvatarFromDB'"
                )

                val currentUserChatData = mapOf(
                    "otherUserId" to otherUserId,
                    "otherUserName" to otherUserName,
                    "otherUserAvatarUrl" to otherUserAvatarUrl,
                    "lastMessageTimestamp" to lastKnownTimestamp
                )

                val otherUserChatData = mapOf(
                    "otherUserId" to currentUserId,
                    "otherUserName" to currentUserNameFromDB,
                    "otherUserAvatarUrl" to currentUserAvatarFromDB,
                    "lastMessageTimestamp" to lastKnownTimestamp
                )

                val rootRef = FirebaseDatabase.getInstance().reference
                val updates = hashMapOf<String, Any?>(
                    "/users/$currentUserId/userChats/$chatRoomId" to currentUserChatData,
                    "/users/$otherUserId/userChats/$chatRoomId" to otherUserChatData
                )
                rootRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "UserChats actualizados exitosamente para $chatRoomId")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Fallo al actualizar userChats para $chatRoomId: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Error al obtener datos del usuario actual para actualizar UserChats: ${error.message}"
                )
            }
        })
    }

    private fun navigateToChatMessageActivity(
        chatRoomId: String,
        otherUserId: String,
        otherUserName: String,
        otherUserAvatarUrl: String?
    ) {
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
            Log.d(TAG, "Intención recibida para iniciar chat con: $targetUserName ($targetUserId)")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_ID")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_NAME")
            intent.removeExtra("ACTION_START_CHAT_WITH_USER_AVATAR_URL")

            getOrCreateChatRoomAndNavigate(targetUserId, targetUserName, targetUserAvatarUrl)
        }
    }
}