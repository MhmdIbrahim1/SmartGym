package com.example.smartgymapp.ui.trainee.search

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.DoctorItemBinding
import com.example.smartgymapp.model.BookingStatus
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.ui.GeneralProfileDetailsActivity
import com.example.smartgymapp.ui.dochat.DoChatActivity
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.getResourceColor
import com.example.smartgymapp.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorTrainerAdapter : RecyclerView.Adapter<DoctorTrainerAdapter.DoctorTrainerViewHolder>() {
    inner class DoctorTrainerViewHolder(val binding: DoctorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(userModel: UserModel) {
            binding.apply {
                userNameTv.text = userModel.firstName + " " + userModel.lastName
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorTrainerViewHolder {
        val binding = DoctorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorTrainerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }


    override fun onBindViewHolder(holder: DoctorTrainerViewHolder, position: Int) {
        val userModel = differ.currentList[position]
        holder.bind(userModel)

        holder.binding.bookNowBtn.apply {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            when {
                userModel.userBookedIdsPending.contains(userId) -> {
                    text = rootView.context.getString(R.string.pending)
                    isEnabled = false
                }
                userModel.userBookedIdsAccepted.contains(userId) -> {
                    text = rootView.context.getString(R.string.accepted)
                    isEnabled = false
                    setBackgroundColor(context.getResourceColor(R.attr.colorAccent))
                }
                else -> {
                    text = rootView.context.getString(R.string.book_now)
                    isEnabled = true
                    setOnClickListener {
                        onBookClickListener?.invoke(userModel)
                    }
                }
            }
        }

        holder.itemView.setOnClickListener {

            val chatRoomId = FirebaseUtil().getCharRoomId(
                FirebaseAuth.getInstance().currentUser!!.uid,
                userModel.userId
            )
            Intent(holder.itemView.context, GeneralProfileDetailsActivity::class.java).also { intent ->
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                CommonActivity.passUserModelAsIntent(intent, userModel)
                intent.putExtra("chatRoomId", chatRoomId) // Pass the chatRoomId
                holder.itemView.context.startActivity(intent)
            }

        }
    }

    var onBookClickListener: ((UserModel) -> Unit)? = null
}