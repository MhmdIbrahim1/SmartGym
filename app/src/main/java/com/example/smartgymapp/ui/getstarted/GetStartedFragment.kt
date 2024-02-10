package com.example.smartgymapp.ui.getstarted

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieDrawable
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentGetStartedBinding
import com.example.smartgymapp.databinding.FragmentLoginBinding
import com.example.smartgymapp.util.UiHelper.navigate


class GetStartedFragment : Fragment() {
    private lateinit var binding: FragmentGetStartedBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetStartedBinding.inflate(layoutInflater,container,false)
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
}