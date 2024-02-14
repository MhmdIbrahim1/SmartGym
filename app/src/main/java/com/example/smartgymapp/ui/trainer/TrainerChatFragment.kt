package com.example.smartgymapp.ui.trainer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartgymapp.databinding.FragmentTrainerChatBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.trainee.chat.ChatViewModel
import com.example.smartgymapp.ui.trainee.chat.TraineeChatAdapter
import com.example.smartgymapp.util.CommonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class TrainerChatFragment : Fragment() {
    private lateinit var binding: FragmentTrainerChatBinding
    private lateinit var traineeChatAdapter: TraineeChatAdapter
    private val chatViewModel by viewModels<TrainerChatViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrainerChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeGetTrainee()
        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        traineeChatAdapter = TraineeChatAdapter()
        binding.traineeChatRv.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = traineeChatAdapter
            addItemDecoration(CommonActivity.ItemSpacingDecoration(16))
        }
    }

    private fun observeGetTrainee() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                chatViewModel.getTraineesFromUsersCollection.collectLatest { result ->
                    when(result){
                        is CommonActivity.NetworkResult.Loading ->{
                            showProgressBar()
                        }

                        is CommonActivity.NetworkResult.Success ->{
                            hideProgressBar()
                            traineeChatAdapter.differ.submitList(result.data)
                            Log.d("ChatFragment", "observeGetTrainee: ${result.data}")
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
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }


}