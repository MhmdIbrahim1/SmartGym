package com.example.smartgymapp.ui.trainee.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ItemQuoteBinding

class Viewpager2Adapter(private val context: Context) :
    ListAdapter<String, Viewpager2Adapter.ViewPager2ViewHolder>(QuoteDiffCallback()) {

        private val usedColors = HashSet<Int>()

    inner class ViewPager2ViewHolder(private val binding: ItemQuoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(quote: String) {
            binding.apply {
                headlineTextView.text = "Quote of the day"
                descriptionTextView.text = quote
                val colors = context.resources.getIntArray(R.array.random_colors)

                // Remove already used colors from the available colors
                val availableColors = colors.filter { !usedColors.contains(it) }

                // If all colors are used, reset the used colors set
                if (availableColors.isEmpty()) {
                    usedColors.clear()
                }

                // Get a random color from the available colors
                val randomColor = availableColors.random()

                usedColors.add(randomColor)

                itemView.setBackgroundColor(randomColor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPager2ViewHolder {
        return ViewPager2ViewHolder(
            ItemQuoteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewPager2ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil callback for calculating item differences
    class QuoteDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
