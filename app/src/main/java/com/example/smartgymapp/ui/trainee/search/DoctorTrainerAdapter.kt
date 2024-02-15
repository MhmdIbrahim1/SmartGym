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

class DoctorTrainerAdapter: RecyclerView.Adapter<DoctorTrainerAdapter.DoctorTrainerViewHolder>() {
    inner class DoctorTrainerViewHolder(val binding: DoctorItemBinding):
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

    // Inside onBindViewHolder method of adapter
// Inside onBindViewHolder method of adapter
    override fun onBindViewHolder(holder: DoctorTrainerViewHolder, position: Int) {
        val userModel = differ.currentList[position]
        holder.bind(userModel)

        // Set the click listener for the book now button
        holder.binding.bookNowBtn.apply {

            //update the booking status based on the user's booking status
            when (userModel.bookingStatus) {
                BookingStatus.NOT_BOOKED -> {
                    text = "Book Now"
                    setOnClickListener {
                        onBookClickListener?.invoke(userModel)
                    }
                }
                BookingStatus.BOOKED -> {
                    text = "Booked"
                    isEnabled = false
                }
                BookingStatus.ACCEPTED -> {
                    text = "Booked"
                    // change button color to green
                    setBackgroundColor(context.getResourceColor(R.attr.colorPrimary))
                    isEnabled = false
                }
                BookingStatus.REJECTED -> {
                    text = "Rejected"
                    isEnabled = false
                }
                BookingStatus.REQUESTED -> {
                    text = "Requested"
                    isEnabled = false
                }
            }

            setOnClickListener {
                onBookClickListener?.invoke(userModel)
            }
        }

    }



    var onBookClickListener: ((UserModel) -> Unit)? = null
}