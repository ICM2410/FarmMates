package mobile.mates.farmmates

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import mobile.mates.farmmates.adapter.UsersInfoAdapter
import mobile.mates.farmmates.databinding.ActivityChatListBinding
import mobile.mates.farmmates.models.User

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding

    // Firebase auth
    private lateinit var auth: FirebaseAuth
    private var users: ArrayList<User> = arrayListOf()

    // Firebase db
    private lateinit var database: FirebaseDatabase
    private lateinit var ref: DatabaseReference
    private val usersSelector = "users/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.availableChats.emptyView = binding.emptyListMessage

        database = Firebase.database
        getAllAvailableUsers()
    }

    private fun getAllAvailableUsers() {
        ref = database.getReference(usersSelector)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null)
                        if (child.key != auth.currentUser!!.uid)
                            users.add(user)
                }
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("Firebase", "Error retrieving all users")
            }
        })
    }

    private fun updateUI() {
        binding.availableChats.adapter = UsersInfoAdapter(users, baseContext)
    }
}