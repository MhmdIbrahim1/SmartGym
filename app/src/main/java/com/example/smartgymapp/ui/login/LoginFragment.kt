package com.example.smartgymapp.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.smartgymapp.databinding.FragmentLoginBinding
import com.example.smartgymapp.ui.doctor.DoctorActivity
import com.example.smartgymapp.ui.trainee.TraineeActivity
import com.example.smartgymapp.ui.trainer.TrainerActivity
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.example.smartgymapp.util.CommonActivity.showToast
import com.example.smartgymapp.util.Coroutines.ioSafe
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        performLogin()
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.textWebsiteLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartfit-web.vercel.app/"))
            startActivity(intent)
        }
    }

    private fun performLogin() {
        binding.loginBtn.setOnClickListener {
            val email = binding.edEmail.text.toString()
            val password = binding.edPassword.text.toString()

            ioSafe {
                loginViewModel.login(email, password)
            }
        }
        observeLogin()
    }

    private fun observeLogin() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            loginViewModel.login.collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        binding.loginBtn.isEnabled = false
                        showProgressBar()
                    }

                    is NetworkResult.Success -> {
                        binding.loginBtn.isEnabled = true
                        hideProgressBar()
                        val userType = result.data
                        if (userType != null) {
                            navigateToUserActivity(userType)
                        }
                    }

                    is NetworkResult.Error -> {
                        binding.loginBtn.isEnabled = true
                        hideProgressBar()
                        showToast(requireActivity(), result.message)
                    }

                    else -> {
                    }
                }
            }
        }
    }

    private fun navigateToUserActivity(userType: String) {
        val intent = when (userType) {
            TRAINING -> Intent(requireContext(), TraineeActivity::class.java)
            TRAINER -> Intent(requireContext(), TrainerActivity::class.java)
            DOCTOR -> Intent(requireContext(), DoctorActivity::class.java)
            else -> null
        }
        intent?.also {
            startActivity(it)
            requireActivity().finish()
        }
    }

    companion object {
        const val TRAINING = "Trainee"
        const val TRAINER = "Trainer"
        const val DOCTOR = "Doctor"
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }


}