package mobile.mates.farmmates

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import mobile.mates.farmmates.adapter.MessageListAdapter
import mobile.mates.farmmates.databinding.ActivityChatBinding
import mobile.mates.farmmates.models.chat.Message
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageList: ArrayList<Message>

    private var otherUserId: String? = null

    // Firebase auth
    private lateinit var auth: FirebaseAuth

    // Firebase realtime database
    private lateinit var database: FirebaseDatabase
    private lateinit var ref: DatabaseReference
    private val chatSelector = "chats/"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database

        otherUserId = intent.getStringExtra("userToChat")
        loadMessages()

        binding.buttonGchatSend.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val content = binding.editGchatMessage.text.toString()
        if (content.isEmpty())
            return

        val newMessage = Message(content, auth.currentUser!!.uid, Date())

        ref = database.getReference(chatSelector + auth.currentUser!!.uid + otherUserId)
        ref.orderByChild("createdAt").get().addOnSuccessListener {
            ref.setValue(newMessage)
        }
    }

    private fun loadMessages() {
        if (otherUserId == null) {
            Toast.makeText(baseContext, "Error loading user to chat", Toast.LENGTH_SHORT).show()
            finish()
        }

        ref = database.getReference(chatSelector + auth.currentUser!!.uid + otherUserId)
        ref.addValueEventListener(chatEventListener)
    }

    private val chatEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            messageList.clear()
            for (child in snapshot.children) {
                val message = child.getValue(Message::class.java)
                if (message != null)
                    messageList.add(message)
            }
            updateMessages()
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error loading messages")
        }
    }

    private fun updateMessages() {
        val recycler = binding.recyclerGchat
        val adapter = MessageListAdapter(baseContext, messageList)
        recycler.layoutManager = LinearLayoutManager(baseContext)
        recycler.adapter = adapter
    }
}