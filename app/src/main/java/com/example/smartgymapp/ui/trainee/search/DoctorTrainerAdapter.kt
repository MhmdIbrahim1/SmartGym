package com.example.smartgymapp.ui.trainee.search

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
import com.example.smartgymapp.util.CommonActivity.getResourceColor
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
                    text = "Pending"
                    isEnabled = false
                }
                userModel.userBookedIdsAccepted.contains(userId) -> {
                    text = "Booked"
                    isEnabled = false
                    setBackgroundColor(context.getResourceColor(R.attr.colorAccent))
                }
                else -> {
                    text = "Book Now"
                    isEnabled = true
                    setOnClickListener {
                        onBookClickListener?.invoke(userModel)
                    }
                }
            }
        }
    }

    var onBookClickListener: ((UserModel) -> Unit)? = null
}