package com.example.smartgymapp.ui.trainee.main

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.databinding.ExercisesItemBinding

class ExercisesAdapter(private val exerciseItem: List<ExerciseItem>) :
    RecyclerView.Adapter<ExercisesAdapter.ExerciseViewHolder>() {

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

        holder.itemView.setOnClickListener {
            val url = "https://smartfit-web.vercel.app/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = android.net.Uri.parse(url)
            holder.itemView.context.startActivity(i)

        }
    }

    override fun getItemCount(): Int {
        return exerciseItem.size
    }

    data class ExerciseItem(val exerciseImage: Int, val exerciseName: String)

    var onExerciseClickListener: OnExerciseClickListener? = null

    interface OnExerciseClickListener {
        fun onExerciseClick(position: Int, exerciseItem: ExerciseItem)
    }
}