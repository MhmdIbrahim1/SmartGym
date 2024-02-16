package com.example.smartgymapp.ui.trainee.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentMainBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.example.smartgymapp.util.UiHelper.navigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var viewpager2Adapter: Viewpager2Adapter
    private lateinit var exercisesAdapter: ExercisesAdapter
    private lateinit var exercisesItem: List<ExercisesAdapter.ExerciseItem>
    private val viewModel: MainViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        observeViewModel()
        setupExercisesRv()

        binding.searchIv.setOnClickListener {
            if (Navigation.findNavController(binding.root).currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(R.id.action_mainFragment_to_searchFragment)
            }
        }
        binding.searchTv.setOnClickListener {
            if (Navigation.findNavController(binding.root).currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(R.id.action_mainFragment_to_searchFragment)
            }
        }
    }

    private fun setupExercisesRv() {
        exercisesItem = listOf(
            ExercisesAdapter.ExerciseItem(R.drawable.cycling,"CYCLING"),
            ExercisesAdapter.ExerciseItem(R.drawable.running,"RUNNING"),
            ExercisesAdapter.ExerciseItem(R.drawable.daily,"DAILY HEALTH"),
            ExercisesAdapter.ExerciseItem(R.drawable.flex,"FLEXIBILITY"),
            ExercisesAdapter.ExerciseItem(R.drawable.jump,"JUMP ROPE"),
            ExercisesAdapter.ExerciseItem(R.drawable.squat,"SQUAT"),
        )
        exercisesAdapter = ExercisesAdapter(exercisesItem)
        binding.exerciseRv.apply {
            val layoutManager = GridLayoutManager(
                requireContext(),
                2,
                GridLayoutManager.VERTICAL,
                false
            )
            binding.exerciseRv.layoutManager = layoutManager
            binding.exerciseRv.addItemDecoration(CommonActivity.ItemSpacingDecoration(20))
            adapter = exercisesAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quotes.collectLatest { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            Log.d("MainFragment", "Loading")
                        }

                        is NetworkResult.Success -> {
                            result.data?.let { viewpager2Adapter.submitList(it) }
                        }

                        is NetworkResult.Error -> {
                            Log.d("MainFragment", "Error: ${result.message}")
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setupViewPager() {
        viewpager2Adapter = Viewpager2Adapter(requireContext())
        binding.viewPager2.adapter = viewpager2Adapter
        binding.viewPager2.setPageTransformer(MarginPageTransformer(40))
        binding.dotsIndicator.attachTo(binding.viewPager2)
    }

    override fun onResume() {
        super.onResume()
        //show bottom navigation
        CommonActivity.showBottomNav(requireActivity())
    }
}
