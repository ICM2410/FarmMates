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
import mobile.mates.farmmates.models.chat.ChatRoom
import mobile.mates.farmmates.models.chat.Message
import java.util.Date


class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private var messageList = arrayListOf<Message>()

    private var otherUserId: String? = null

    // Firebase auth
    private lateinit var auth: FirebaseAuth

    // Firebase realtime database
    private lateinit var database: FirebaseDatabase
    private lateinit var ref: DatabaseReference
    private val chatSelector = "chats/"
    private val messageSelector =  "messages/"
    private var chatRoom : ChatRoom? = null
    private var roomKey : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database

        findChatRoom()

        otherUserId = intent.getStringExtra("userToChat")

        binding.buttonGchatSend.setOnClickListener { sendMessage() }
    }

    private fun findChatRoom() {
        val roomRef = database.getReference(chatSelector)
        roomRef.addListenerForSingleValueEvent(filterChatRoom)
    }

    private val filterChatRoom = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            for(child in snapshot.children) {
                val room = child.getValue(ChatRoom::class.java)
                if(room != null) {
                    val users = arrayListOf(room.firstUser, room.secondUser)
                    if (users.contains(auth.currentUser!!.uid) && users.contains(otherUserId)) {
                        chatRoom = room
                        roomKey = child.key!!
                        loadMessages()
                        break
                    }
                }
            }
            if(chatRoom == null)
                createChatRoom()
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Chat", "Error loading chat room")
        }
    }

    private fun createChatRoom() {
        val ref = database.getReference(chatSelector).push()
        val users = arrayListOf(
            auth.currentUser!!.uid,
            otherUserId!!
        )
        val newChatRoom = ChatRoom(users[0], users[1])
        ref.setValue(newChatRoom).addOnSuccessListener {
            chatRoom = newChatRoom
            roomKey = ref.key!!
            loadMessages()
        }
    }

    private fun sendMessage() {
        val content = binding.editGchatMessage.text.toString()
        if (content.isEmpty())
            return

        val newMessage = Message(roomKey, content, auth.currentUser!!.uid, Date())

        val ref = database.getReference(messageSelector).push()
        ref.setValue(newMessage)
        binding.editGchatMessage.text.clear()
    }

    private fun loadMessages() {
        if (otherUserId == null) {
            Toast.makeText(baseContext, "Error loading user to chat", Toast.LENGTH_SHORT).show()
            finish()
        }
        database.getReference(messageSelector)
            .orderByChild("room")
            .equalTo(roomKey)
            .addValueEventListener(chatMessagesListener)
    }

    private val chatMessagesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            messageList.clear()
            for(child in snapshot.children) {
                val message = child.getValue(Message::class.java)
                if(message != null) {
                    messageList.add(message)
                    updateMessages()
                }
            }
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