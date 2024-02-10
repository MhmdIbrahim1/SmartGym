package com.example.smartgymapp.ui.trainee.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.databinding.ExercisesItemBinding

class ExercisesAdapter(private val exerciseItem: List<ExerciseItem>) : RecyclerView.Adapter<ExercisesAdapter.ExerciseViewHolder>(){

    inner class ExerciseViewHolder(val binding: ExercisesItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExercisesAdapter.ExerciseViewHolder {
        val binding = ExercisesItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExercisesAdapter.ExerciseViewHolder, position: Int) {
        val exerciseItem = exerciseItem[position]
        holder.binding.apply {
            exerciseImage.setImageResource(exerciseItem.exerciseImage)
            exerciseName.text = exerciseItem.exerciseName
        }

        if (onExerciseClickListener != null) {
            holder.itemView.setOnClickListener {
                onExerciseClickListener!!.onExerciseClick(position, exerciseItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return exerciseItem.size
    }

    data class ExerciseItem(val exerciseImage: Int ,val exerciseName: String)

    var onExerciseClickListener: OnExerciseClickListener? = null

    interface OnExerciseClickListener {
        fun onExerciseClick(position: Int, exerciseItem: ExerciseItem)
    }
}