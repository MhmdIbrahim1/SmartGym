package com.example.smartgymapp.ui.dochat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ChatMessageRecyclerRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.text.SimpleDateFormat
import java.util.Locale

class ChatRecyclerAdapter(options: FirestoreRecyclerOptions<ChatMessageModel>, context: Context):
    FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder>(options) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatRecyclerAdapter.ChatModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_message_recycler_row, parent, false)
        return ChatModelViewHolder(view)
    }

    class ChatModelViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val binding = ChatMessageRecyclerRowBinding.bind(itemView)
    }

    override fun onBindViewHolder(
        holder: ChatRecyclerAdapter.ChatModelViewHolder,
        position: Int,
        model: ChatMessageModel
    ) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = sdf.format(model.timestamp.toDate())
        if (model.senderId == FirebaseAuth.getInstance().currentUser!!.uid) {
            holder.binding.leftChatLayout.visibility = View.GONE;
            holder.binding.rightChatLayout.visibility = View.VISIBLE;
            holder.binding.rightChatTextview.text = model.message;
            holder.binding.rightChatTime.text = formattedTime;
        } else {
            holder.binding.leftChatLayout.visibility = View.VISIBLE;
            holder.binding.rightChatLayout.visibility = View.GONE;
            holder.binding.leftChatTextview.text = model.message;
            holder.binding.leftChatTime.text = formattedTime;
        }
    }

}