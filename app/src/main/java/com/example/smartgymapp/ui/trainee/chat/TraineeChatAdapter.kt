package com.example.smartgymapp.ui.trainee.chat

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.UsersInChatItemBinding
import com.example.smartgymapp.ui.dochat.DoChatActivity
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity

class TraineeChatAdapter : RecyclerView.Adapter<TraineeChatAdapter.TraineeChatViewHolder>() {

    inner class TraineeChatViewHolder(val binding: UsersInChatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(userModel: UserModel) {
            binding.apply {
                userNameTv.text = "${userModel.firstName} ${userModel.lastName}"
                userModel.profile_picture.let { imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        userIv.load(imageUrl) {
                            crossfade(true)
                            crossfade(100)
                            transformations(CircleCropTransformation())
                        }
                    } else {
                        userIv.setImageResource(R.drawable.man_user)
                    }
                }

                when (userModel.userType) {
                    "Doctor" -> {
                        userTypeTv.text = root.context.getString(R.string.doctor)
                    }
                    "Trainer" -> {
                        userTypeTv.text = root.context.getString(R.string.trainer)
                    }
                    else -> {
                        userTypeTv.text = root.context.getString(R.string.trainee)
                    }
                }
            }
        }
    }

    private val diffUtil = object : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
    val differ = AsyncListDiffer(this, diffUtil)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraineeChatViewHolder {
        val binding =
            UsersInChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TraineeChatViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: TraineeChatViewHolder, position: Int) {
        val trainer = differ.currentList[position]
        holder.bind(trainer)

        holder.itemView.setOnClickListener {
            Intent(holder.itemView.context, DoChatActivity::class.java).also { intent ->
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                CommonActivity.passUserModelAsIntent(intent, trainer)
                holder.itemView.context.startActivity(intent)
            }

        }

    }
}