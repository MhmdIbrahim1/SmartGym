package com.example.smartgymapp.ui.doctor.dProfile.booking

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartgymapp.databinding.FragmentPatinetsBookedBinding
import com.example.smartgymapp.databinding.FragmentTraineeBookedBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.trainee.chat.ChatViewModel
import com.example.smartgymapp.ui.trainee.profile.booking.TrainersBookedAdapter
import com.example.smartgymapp.util.CommonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class PatinetsBookedFragment : Fragment() {
    private lateinit var binding: FragmentPatinetsBookedBinding
    private val chatViewModel by activityViewModels<ChatViewModel>()
    private lateinit var traineeChatAdapter: TrainersBookedAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentPatinetsBookedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        observeTrainers()

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setUpRecyclerView() {
        traineeChatAdapter = TrainersBookedAdapter()
        binding.traineeChatRv.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = traineeChatAdapter

            addItemDecoration(CommonActivity.ItemSpacingDecoration(16))
        }
    }

    private fun observeTrainers() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                chatViewModel.getTrainersFromBookedList.collectLatest {result ->
                    when(result){
                        is CommonActivity.NetworkResult.Loading ->{
                            showProgressBar()
                        }

                        is CommonActivity.NetworkResult.Success ->{
                            hideProgressBar()
                            traineeChatAdapter.differ.submitList(result.data)
                        }

                        is CommonActivity.NetworkResult.Error -> {
                            hideProgressBar()
                            Log.d("ChatFragment", "observeGetTrainee: ${result.message}")
                        }

                        else ->{}
                    }
                }
            }
        }
        chatViewModel.getTrainersFromBookedAccepted("Trainee")
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

}