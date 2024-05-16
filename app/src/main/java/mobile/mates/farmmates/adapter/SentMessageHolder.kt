package mobile.mates.farmmates.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mobile.mates.farmmates.databinding.ItemChatMeBinding
import mobile.mates.farmmates.models.chat.Message
import java.text.SimpleDateFormat
import java.util.Locale

class SentMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding: ItemChatMeBinding = ItemChatMeBinding.bind(itemView)

    fun bind(message: Message) {
        binding.textGchatDateMe.text = message.content

        val formatter = SimpleDateFormat("EEE MMM dd - HH:mm", Locale.ENGLISH)
        binding.textGchatTimestampMe.text = formatter.format(message.createdAt).toString()
    }
}