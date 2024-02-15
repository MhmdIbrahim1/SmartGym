package com.example.smartgymapp.ui.trainee.search

import android.content.Context
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.databinding.FragmentTrainersBinding
import com.example.smartgymapp.model.BookingStatus
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.log

@AndroidEntryPoint
class TrainersFragment : Fragment() {
    private lateinit var binding: FragmentTrainersBinding
    private lateinit var trainersAdapter: DoctorTrainerAdapter
    private val trainersViewModel by viewModels<TrainersViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentTrainersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        observeTrainers()
        observeBookingStatus()

        trainersAdapter.onBookClickListener = { trainer ->
            val selectedTrainerId = trainer.userId
            // Call the function to send the booking request
            trainersViewModel.sendTraineeToTrainerRequests(
                selectedTrainerId,
                FirebaseAuth.getInstance().currentUser!!.uid
            )
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                kotlinx.coroutines.delay(1000)
                trainersViewModel.getAllTrainers()
            }


            Log.d("TrainersViewModel", "onViewCreated:$selectedTrainerId")
            Log.d(
                "TrainersViewModel",
                "onViewCreated:${FirebaseAuth.getInstance().currentUser!!.uid}"
            )
        }


    }

    private fun setUpRecyclerView() {
        trainersAdapter = DoctorTrainerAdapter()
        binding.trainersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = trainersAdapter
            binding.trainersRecyclerView.addItemDecoration(CommonActivity.ItemSpacingDecoration(16))
        }
    }

    private fun observeTrainers() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                trainersViewModel.getAllTrainers.collectLatest {
                    when(it){
                        is CommonActivity.NetworkResult.Success -> {
                            hideProgressBar()
                            trainersAdapter.differ.submitList(it.data)
                        }
                        is CommonActivity.NetworkResult.Error -> {
                            hideProgressBar()
                        }
                        is CommonActivity.NetworkResult.Loading -> {
                            showProgressBar()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeBookingStatus() {
        trainersViewModel.sendTraineeToTrainerRequests.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CommonActivity.NetworkResult.Success -> handleSuccess(result.data)
                is CommonActivity.NetworkResult.Error -> handleError(result.message)
                is CommonActivity.NetworkResult.Loading -> handleLoading()
                else -> {} // Handle other states if needed
            }
        }
    }

    private fun handleSuccess(updatedUser: UserModel?) {
        updatedUser?.let { user ->
            val position = trainersAdapter.differ.currentList.indexOfFirst { it.userId == user.userId }
            if (position != -1) {
                val updatedList = trainersAdapter.differ.currentList.toMutableList()
                updatedList[position] = user
                trainersAdapter.differ.submitList(updatedList)
            }
        }
    }

    private fun handleError(errorMessage: String?) {
        // Handle error
    }

    private fun handleLoading() {
        // Handle loading state if needed
    }



    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }


}