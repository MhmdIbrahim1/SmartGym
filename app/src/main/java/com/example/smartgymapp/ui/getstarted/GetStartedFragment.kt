package com.example.smartgymapp.ui.getstarted

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentGetStartedBinding
import com.example.smartgymapp.ui.doctor.DoctorActivity
import com.example.smartgymapp.ui.trainee.TraineeActivity
import com.example.smartgymapp.ui.trainer.TrainerActivity
import com.example.smartgymapp.util.UiHelper.navigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class GetStartedFragment : Fragment() {
    private lateinit var binding: FragmentGetStartedBinding
    //private val viewModel by activityViewModels<GetStartedViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetStartedBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lottieAnimation()
        binding.getStartedButton.setOnClickListener {
           activity?.navigate(R.id.action_getStartedFragment_to_loginFragment)
        }
    }

    private fun lottieAnimation() {
        val lottieAnimationView = binding.imageOrderConfirm
        lottieAnimationView.setAnimation(R.raw.get_started)
        lottieAnimationView.repeatCount = LottieDrawable.INFINITE
        lottieAnimationView.playAnimation()
    }

    private fun navigateToActivity(activityClass: Class<*>) {
     Intent(requireActivity(), activityClass).also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

}