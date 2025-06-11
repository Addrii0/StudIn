package com.example.studin.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studin.Message
import com.example.studin.adapters.MessageAdapter
import com.example.studin.databinding.ActivityChatMessageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.util.*

class ChatMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatMessageBinding
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = ArrayList<Message>()
    private val TAG = "ChatMessageActivity"

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var database: FirebaseDatabase

    private var chatRoomId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null

    private lateinit var messagesRef: DatabaseReference
    private var messagesListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        database = FirebaseDatabase.getInstance()

        // Recibir datos del Intent
        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID")
        otherUserId = intent.getStringExtra("OTHER_USER_ID")
        otherUserName = intent.getStringExtra("OTHER_USER_NAME")

        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado, cerrando actividad de chat.")
            finish()
            return
        }

        if (chatRoomId == null || otherUserId == null) {
            Log.e(TAG, "CHAT_ROOM_ID u OTHER_USER_ID es nulo, cerrando actividad.")
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()

        binding.buttonSendMessage.setOnClickListener {
            sendMessage()
        }

        loadMessages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarChatMessage)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = otherUserName ?: "Chat"
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, currentUser!!.uid) // Se pasa el UID del usuario actual
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recyclerViewMessages.layoutManager = linearLayoutManager
        binding.recyclerViewMessages.adapter = messageAdapter

    }

    private fun sendMessage() {
        val messageText = binding.editTextMessageInput.text.toString().trim()
        if (messageText.isEmpty()) {
            return
        }

        val currentUid = currentUser?.uid ?: return

        // Crear mensaje
        val timestamp = System.currentTimeMillis()
        val messageId = database.getReference("chat_rooms").child(chatRoomId!!).child("messages").push().key ?: ""

        val message = Message(
            messageId = messageId,
            senderId = currentUid,
            text = messageText,
            timestamp = timestamp
        )

        // Guardar el mensaje en la lista de mensajes del chat room
        database.getReference("chat_rooms").child(chatRoomId!!).child("messages").child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje enviado correctamente a messages/$messageId")
                binding.editTextMessageInput.setText("") // Limpiar el campo de texto
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al enviar mensaje a messages/$messageId: ${e.message}")
            }

        // 2. Actualizar el lastMessage en el chat room
        val lastMessageMap = mapOf(
            "text" to messageText,
            "timestamp" to timestamp,
            "senderId" to currentUid
        )
        database.getReference("chat_rooms").child(chatRoomId!!).child("lastMessage").setValue(lastMessageMap)
            .addOnSuccessListener { Log.d(TAG, "lastMessage actualizado para $chatRoomId") }
            .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar lastMessage: ${e.message}") }

        val userChatUpdate = mapOf(
            "lastMessageText" to messageText,
            "lastMessageTimestamp" to timestamp
        )
        // Para el usuario actual
        database.getReference("users").child(currentUid).child("userChats").child(chatRoomId!!).updateChildren(userChatUpdate)
        // Para el otro usuario
        database.getReference("users").child(otherUserId!!).child("userChats").child(chatRoomId!!).updateChildren(userChatUpdate)

    }

    private fun loadMessages() {
        binding.recyclerViewMessages.visibility = View.GONE

        messagesRef = database.getReference("chat_rooms").child(chatRoomId!!).child("messages")

        messagesListener?.let { messagesRef.removeEventListener(it) }
        messageList.clear()

        messagesListener = object : ChildEventListener {


            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        if (!messageList.any { it.messageId == message.messageId }) {
                            messageList.add(message)

                            messageAdapter.notifyItemInserted(messageList.size - 1)
                            binding.recyclerViewMessages.scrollToPosition(messageList.size - 1)
                        }
                        Log.d(TAG, "Mensaje añadido: ${message.text}")
                    } else {
                        Log.w(TAG, "Mensaje nulo recibido de Firebase: ${snapshot.key}")
                    }
                } catch (e: DatabaseException) {
                    Log.e(TAG, "Error al deserializar mensaje: ${snapshot.key}", e)
                    //
                }

                if (binding.recyclerViewMessages.visibility == View.GONE) {
                    binding.recyclerViewMessages.visibility = View.VISIBLE
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                Log.d(TAG, "Mensaje cambiado: ${snapshot.key}")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d(TAG, "Mensaje eliminado: ${snapshot.key}")
                val messageKey = snapshot.key
                val indexToRemove = messageList.indexOfFirst { it.messageId == messageKey }
                if (indexToRemove != -1) {
                    messageList.removeAt(indexToRemove)
                    messageAdapter.notifyItemRemoved(indexToRemove)

                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "Mensaje movido: ${snapshot.key}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar mensajes: ${error.message}")
                binding.recyclerViewMessages.visibility = View.VISIBLE
            }
        }

        messagesRef.orderByChild("timestamp")
            .addChildEventListener(messagesListener!!)
    }

    // Manejar el botón de atrás en la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()

        messagesListener?.let {
            if (::messagesRef.isInitialized) {
                messagesRef.removeEventListener(it)
            }
        }
        messagesListener = null
        Log.d(TAG, "Listener de mensajes limpiado.")
    }
}