package com.example.smartgymapp.ui.trainee.chat

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
import com.example.smartgymapp.databinding.FragmentChatBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class ChatFragment : Fragment() {
    private lateinit var binding: FragmentChatBinding
    private val chatViewModel by activityViewModels<ChatViewModel>()
    private lateinit var traineeChatAdapter: TraineeChatAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
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
                chatViewModel.getTrainersFromUserCollection.collectLatest {result ->
                    when(result){
                         is NetworkResult.Loading ->{
                             showProgressBar()
                        }

                        is NetworkResult.Success ->{
                            hideProgressBar()
                            traineeChatAdapter.differ.submitList(result.data)
                        }

                        is NetworkResult.Error -> {
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