package mobile.mates.farmmates.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import mobile.mates.farmmates.ChatActivity
import mobile.mates.farmmates.R
import mobile.mates.farmmates.models.User

class UsersInfoAdapter(data: List<User>, mContext: Context) :
    ArrayAdapter<User?>(mContext, R.layout.chat_item_adapter, data),
    View.OnClickListener {
    private class ViewHolder {
        var name: TextView? = null
        var profilePic: ImageView? = null
        var goToTrackButton: ImageButton? = null
    }

    override fun onClick(v: View) {}

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mConvertView = convertView
        val user = getItem(position)
        val viewHolder: ViewHolder

        if (mConvertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            mConvertView = inflater.inflate(R.layout.chat_item_adapter, parent, false)

            viewHolder.name = mConvertView.findViewById(R.id.userName)
            viewHolder.profilePic = mConvertView.findViewById(R.id.userProfilePic)
            viewHolder.goToTrackButton = mConvertView.findViewById(R.id.chatButton)

            if (viewHolder.goToTrackButton != null)
                viewHolder.goToTrackButton!!.setOnClickListener { goToChat(context, user) }

            mConvertView.tag = viewHolder
        } else {
            viewHolder = mConvertView.tag as ViewHolder
        }

        viewHolder.name?.text = buildString {
            append(user?.name)
            append(" ")
            append(user?.lastName)
        }
        val url = user?.profilePicUrl
        if (!url.isNullOrEmpty()) {
            Picasso
                .get()
                .load(url)
                .into(viewHolder.profilePic)
        } else {
            viewHolder.profilePic?.setImageResource(R.drawable.profile_pic_placeholder)
        }

        return mConvertView!!
    }

    private fun goToChat(context: Context, user: User?) {
        if (user != null) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("userToChat", user.id)
            context.startActivity(intent)
        }
    }
}