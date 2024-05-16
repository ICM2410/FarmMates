package mobile.mates.farmmates.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mobile.mates.farmmates.databinding.ItemChatOtherBinding
import mobile.mates.farmmates.models.chat.Message
import java.text.SimpleDateFormat
import java.util.Locale

class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding: ItemChatOtherBinding = ItemChatOtherBinding.bind(itemView)

    fun bind(message: Message) {
        binding.textGchatMessageOther.text = message.content

        val formatter = SimpleDateFormat("EEE MMM dd - HH:mm", Locale.ENGLISH)
        binding.textGchatTimestampOther.text = formatter.format(message.createdAt).toString()

        binding.textGchatUserOther.text = message.sender.name
    }
}