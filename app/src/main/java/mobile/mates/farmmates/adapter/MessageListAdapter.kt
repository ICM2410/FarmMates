package mobile.mates.farmmates.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import mobile.mates.farmmates.databinding.ItemChatMeBinding
import mobile.mates.farmmates.databinding.ItemChatOtherBinding
import mobile.mates.farmmates.models.chat.Message


class MessageListAdapter(
    private val mContext: Context,
    private val mMessageList: ArrayList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val auth = Firebase.auth

    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2

    override fun getItemCount(): Int {
        return mMessageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val message: Message = mMessageList[position]
        return if (message.senderId == (auth.currentUser?.uid ?: ""))
            VIEW_TYPE_MESSAGE_SENT
        else
            VIEW_TYPE_MESSAGE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val view = ItemChatMeBinding.inflate(LayoutInflater.from(parent.context)).root
                SentMessageHolder(view)
            }

            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val view = ItemChatOtherBinding.inflate(LayoutInflater.from(parent.context)).root
                ReceivedMessageHolder(view)
            }

            else -> {
                throw IllegalArgumentException("Unknown viewType: $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message : Message = mMessageList[position]
        when(holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder).bind(message)
        }
    }
}
