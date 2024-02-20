package com.example.smartgymapp.ui.trainer.tMain

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
import com.example.smartgymapp.databinding.FragmentTrainerMainBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class TrainerMainFragment : Fragment() {
    private lateinit var binding: FragmentTrainerMainBinding
    private lateinit var mainTraineeRequestsAdapter: MainTraineeRequestsAdapter
    private val mainTraineeViewModel by viewModels<MainTrainerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentTrainerMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setUpRecyclerView()
        observeGetTraineeRequests()
        observeAcceptedTraineeRequest()

        mainTraineeRequestsAdapter.onAcceptClick = {trainee ->
            val traineeId = trainee.userId
            val trainerId  = FirebaseAuth.getInstance().currentUser!!.uid
            mainTraineeViewModel.acceptAndSendTrainees(traineeId, trainerId)
        }
        mainTraineeRequestsAdapter.onRejectClick = {
            val traineeId = it.userId
            val trainerId  = FirebaseAuth.getInstance().currentUser!!.uid
            mainTraineeViewModel.rejectTraineeRequest(traineeId, trainerId)
        }
    }

    private fun observeAcceptedTraineeRequest() {
        mainTraineeViewModel.acceptedTraineeRequestLiveData.observe(viewLifecycleOwner){result ->
            when(result){
                is CommonActivity.NetworkResult.Loading ->{
                    showProgressBar()
                }
                is CommonActivity.NetworkResult.Error ->{
                    hideProgressBar()
                }
                is CommonActivity.NetworkResult.Success ->{
                    hideProgressBar()
                }
                else ->{}
            }
        }
    }

    private fun observeGetTraineeRequests() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainTraineeViewModel.getAllTraineesRequest.collectLatest {result ->
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
                                if(result.data.isNullOrEmpty()){
                                    showNoTraineeRequests()
                                }else{
                                    hideNoTraineeRequests()
                                }
                         }
                        else ->{}
                    }
                }
            }
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showNoTraineeRequests() {
        binding.noTraineeRequests.visibility = View.VISIBLE
        binding.traineeBookings.visibility = View.GONE
    }

    private fun hideNoTraineeRequests() {
        binding.noTraineeRequests.visibility = View.GONE
        binding.traineeBookings.visibility = View.VISIBLE
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


}