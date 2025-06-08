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
    // private var otherUserAvatarUrl: String? = null // Si lo necesitas para algo más que el título

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
        // otherUserAvatarUrl = intent.getStringExtra("OTHER_USER_AVATAR_URL")

        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado, cerrando actividad de chat.")
            finish() // O redirigir al login
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
        messageAdapter = MessageAdapter(messageList, currentUser!!.uid) // Pasamos el UID del usuario actual
        val linearLayoutManager = LinearLayoutManager(this)
        // Para que el RecyclerView se desplace automáticamente al último mensaje
        linearLayoutManager.stackFromEnd = true
        binding.recyclerViewMessages.layoutManager = linearLayoutManager
        binding.recyclerViewMessages.adapter = messageAdapter
        // (Opcional) Para mejorar rendimiento si los items no cambian de tamaño
        // binding.recyclerViewMessages.setHasFixedSize(true)
    }

    private fun sendMessage() {
        val messageText = binding.editTextMessageInput.text.toString().trim()
        if (messageText.isEmpty()) {
            return // No enviar mensajes vacíos
        }

        val currentUid = currentUser?.uid ?: return // No debería ser nulo aquí

        // Crear el objeto mensaje
        val timestamp = System.currentTimeMillis()
        val messageId = database.getReference("chat_rooms").child(chatRoomId!!).child("messages").push().key ?: ""

        val message = Message(
            messageId = messageId,
            senderId = currentUid,
            text = messageText,
            timestamp = timestamp
            // receiverId no es estrictamente necesario si solo tenemos 2 participantes por chatRoom
        )

        // 1. Guardar el mensaje en la lista de mensajes del chat room
        database.getReference("chat_rooms").child(chatRoomId!!).child("messages").child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje enviado correctamente a messages/$messageId")
                binding.editTextMessageInput.setText("") // Limpiar el campo de texto
                // El RecyclerView se actualizará a través del ChildEventListener
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al enviar mensaje a messages/$messageId: ${e.message}")
                // Mostrar error al usuario si es necesario
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


        // 3. (Opcional pero recomendado) Actualizar los nodos userChats para ambos usuarios
        // Esto es útil si quieres que la lista de chats se ordene/actualice en tiempo real
        // para el otro usuario también, o si muestras el lastMessage directamente desde userChats.
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
        binding.recyclerViewMessages.visibility = View.GONE // Ocultar mientras carga
        // Podrías mostrar un ProgressBar aquí

        messagesRef = database.getReference("chat_rooms").child(chatRoomId!!).child("messages")

        // Limpiar listener anterior si existe
        messagesListener?.let { messagesRef.removeEventListener(it) }
        messageList.clear() // Limpiar lista antes de cargar/escuchar

        messagesListener = object : ChildEventListener {
            // ChatMessageActivity.kt
// ... (código anterior en loadMessages y ChildEventListener) ...

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        // (Opcional) Evitar añadir mensajes duplicados si el listener se dispara varias veces
                        // aunque ChildEventListener para "messages" debería ser bastante robusto.
                        if (!messageList.any { it.messageId == message.messageId }) {
                            messageList.add(message)
                            // Ordenar por timestamp por si acaso llegan desordenados inicialmente (poco probable con push keys)
                            // messageList.sortBy { it.timestamp }
                            // messageAdapter.submitList(ArrayList(messageList)) // Si usas DiffUtil en el Adapter
                            messageAdapter.notifyItemInserted(messageList.size - 1)
                            binding.recyclerViewMessages.scrollToPosition(messageList.size - 1) // Auto-scroll
                        }
                        Log.d(TAG, "Mensaje añadido: ${message.text}")
                    } else {
                        Log.w(TAG, "Mensaje nulo recibido de Firebase: ${snapshot.key}")
                    }
                } catch (e: DatabaseException) {
                    Log.e(TAG, "Error al deserializar mensaje: ${snapshot.key}", e)
                    // Puedes decidir si quieres mostrar un mensaje de error o ignorar el mensaje corrupto.
                }
                // Mostrar RecyclerView una vez que al menos un mensaje se ha procesado (o después de un tiempo)
                if (binding.recyclerViewMessages.visibility == View.GONE) {
                    binding.recyclerViewMessages.visibility = View.VISIBLE
                    // Ocultar ProgressBar si tenías uno
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Implementar si los mensajes pueden ser editados o su estado cambia (ej. 'leído')
                // y necesitas reflejarlo en la UI.
                Log.d(TAG, "Mensaje cambiado: ${snapshot.key}")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Implementar si los mensajes pueden ser eliminados.
                Log.d(TAG, "Mensaje eliminado: ${snapshot.key}")
                val messageKey = snapshot.key
                val indexToRemove = messageList.indexOfFirst { it.messageId == messageKey }
                if (indexToRemove != -1) {
                    messageList.removeAt(indexToRemove)
                    messageAdapter.notifyItemRemoved(indexToRemove)
                    // Podrías necesitar actualizar los rangos de ítems si no estás al final
                    // messageAdapter.notifyItemRangeChanged(indexToRemove, messageList.size);
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Generalmente no es relevante para una lista de chat ordenada por timestamp.
                Log.d(TAG, "Mensaje movido: ${snapshot.key}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar mensajes: ${error.message}")
                binding.recyclerViewMessages.visibility = View.VISIBLE // Aún mostrar lo que se haya cargado
                // Mostrar error al usuario si es necesario
            }
        }
        // Para asegurar que los mensajes se carguen en orden de creación (si usas push keys)
        // y para limitar la cantidad de mensajes iniciales si la conversación es muy larga (paginación)
        messagesRef.orderByChild("timestamp") //.limitToLast(50) // Ejemplo de paginación inicial
            .addChildEventListener(messagesListener!!)
    }

    // Manejar el botón de atrás en la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed() // Manera moderna de manejar el botón de atrás
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el listener de Firebase para evitar fugas de memoria
        messagesListener?.let {
            if (::messagesRef.isInitialized) { // Comprobar si messagesRef fue inicializado
                messagesRef.removeEventListener(it)
            }
        }
        messagesListener = null
        Log.d(TAG, "Listener de mensajes limpiado.")
    }
}