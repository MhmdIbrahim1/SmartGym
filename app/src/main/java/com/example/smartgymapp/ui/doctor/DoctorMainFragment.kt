package com.example.smartgymapp.ui.doctor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentDoctorMainBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.trainer.MainTraineeRequestsAdapter
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DoctorMainFragment : Fragment() {
    private lateinit var binding: FragmentDoctorMainBinding
    private lateinit var mainTraineeRequestsAdapter: MainTraineeRequestsAdapter
    private val mainDoctorViewModel by viewModels<MainDoctorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentDoctorMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        observeGetTraineeRequests()

        mainTraineeRequestsAdapter.onAcceptClick = {trainee ->
            val traineeId = trainee.userId
            val doctorId  = FirebaseAuth.getInstance().currentUser!!.uid
            mainDoctorViewModel.acceptAndSendTrainees(traineeId, doctorId)
        }

        mainTraineeRequestsAdapter.onRejectClick = {
            val traineeId = it.userId
            val doctorId  = FirebaseAuth.getInstance().currentUser!!.uid
            mainDoctorViewModel.rejectTraineeRequest(traineeId, doctorId)
        }
    }

    private fun observeGetTraineeRequests() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainDoctorViewModel.getAllTraineesRequest.collectLatest {result ->
                    when(result){
                        is CommonActivity.NetworkResult.Loading ->{
                            showProgressBar()
                        }
                        is CommonActivity.NetworkResult.Error ->{
                            hideProgressBar()

                        }
                        is CommonActivity.NetworkResult.Success ->{
                            hideProgressBar()
                            mainTraineeRequestsAdapter.differ.submitList(result.data)
                        }
                        else ->{}
                    }
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        mainTraineeRequestsAdapter = MainTraineeRequestsAdapter()
        binding.trainersRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = mainTraineeRequestsAdapter
            addItemDecoration(CommonActivity.ItemSpacingDecoration(16))
        }
    }
    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }
}