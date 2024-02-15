package com.example.smartgymapp.ui.trainer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.TraineeRequestItemBinding
import com.example.smartgymapp.model.UserModel

class MainTraineeRequestsAdapter: RecyclerView.Adapter<MainTraineeRequestsAdapter.MainTraineeRequestsViewHolder>(){
    inner class MainTraineeRequestsViewHolder( val binding: TraineeRequestItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(userModel: UserModel){
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
            }
        }

    }

    private val diffUtil = object : DiffUtil.ItemCallback<UserModel>(){
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainTraineeRequestsViewHolder {
        val itemBinding = TraineeRequestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MainTraineeRequestsViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: MainTraineeRequestsViewHolder, position: Int) {
        val userModel = differ.currentList[position]
        holder.bind(userModel)

        holder.binding.acceptIv.setOnClickListener {
            onAcceptClick?.invoke(userModel)
        }

        holder.binding.rejectIv.setOnClickListener {
            onRejectClick?.invoke(userModel)
        }

    }

    val onAcceptClick: ((UserModel) -> Unit)? = null
    val onRejectClick: ((UserModel) -> Unit)? = null
}