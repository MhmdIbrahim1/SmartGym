package com.example.smartgymapp.ui.trainee.search

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
import com.example.smartgymapp.databinding.FragmentDoctorsBinding
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DoctorsFragment : Fragment() {
    private lateinit var binding: FragmentDoctorsBinding
    private val doctorsViewModel by viewModels<DoctorsViewModel>()
    private val doctorsAdapter = DoctorTrainerAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        observeDoctors()
        observeBookingStatus()


        doctorsAdapter.onBookClickListener = { doctor ->
            val selectedDoctorId = doctor.userId
            // Call the function to send the booking request
            doctorsViewModel.sendTraineeToTrainerRequests(
                selectedDoctorId,
                FirebaseAuth.getInstance().currentUser!!.uid
            )
        }
    }

    private fun observeBookingStatus() {
        doctorsViewModel.sendTraineeToTrainerRequests.observe(viewLifecycleOwner) { result ->
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
            val position = doctorsAdapter.differ.currentList.indexOfFirst { it.userId == user.userId }
            if (position != -1) {
                val updatedList = doctorsAdapter.differ.currentList.toMutableList()
                updatedList[position] = user
                doctorsAdapter.differ.submitList(updatedList)
            }
        }
    }

    private fun handleError(errorMessage: String?) {
        // Handle error
        Log.d("DoctorsFragment", "handleError: $errorMessage")
    }

    private fun handleLoading() {
        // Handle loading state if needed
    }

    private fun observeDoctors() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                doctorsViewModel.getAllDoctors.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Success -> {
                            hideProgressBar()
                            doctorsAdapter.differ.submitList(it.data)
                        }

                        is CommonActivity.NetworkResult.Error -> {
                            Log.d("DoctorsFragment", "observeDoctors: ${it.message}")
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

    private fun setUpRecyclerView() {
        binding.doctorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = doctorsAdapter
            binding.doctorsRecyclerView.addItemDecoration(CommonActivity.ItemSpacingDecoration(16))
        }
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

}