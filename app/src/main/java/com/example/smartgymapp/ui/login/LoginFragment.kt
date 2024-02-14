package com.example.smartgymapp.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentLoginBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.doctor.DoctorActivity
import com.example.smartgymapp.ui.trainee.TraineeActivity
import com.example.smartgymapp.ui.trainer.TrainerActivity
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.example.smartgymapp.util.CommonActivity.showToast
import com.example.smartgymapp.util.Coroutines.ioSafe
import com.example.smartgymapp.util.Coroutines.main
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val loginViewModel by viewModels<LoginViewModel>()
    private var selectedUserType: String? = null
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
    }

    private fun performLogin() {
        binding.loginBtn.setOnClickListener {
            val email = binding.edEmail.text.toString()
            val password = binding.edPassword.text.toString()
            selectedUserType = when (binding.whoWillLoginRadioGroup.checkedRadioButtonId) {
                R.id.userRadioButton -> TRAINING
                R.id.trainerRadioButton -> TRAINER
                R.id.doctorRadioButton -> DOCTOR
                else -> null
            }
            if (selectedUserType != null) {
                ioSafe {
                    loginViewModel.login(email, password, selectedUserType!!)
                }
            } else {
                main {
                    showToast(requireActivity(), "Please select user type")
                }
            }
        }
        observeLogin()
    }

    private fun observeLogin() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.login.collectLatest { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            binding.loginBtn.isEnabled = false
                            showProgressBar()
                        }

                        is NetworkResult.Success -> {
                            binding.loginBtn.isEnabled = true
                            hideProgressBar()
                            val intent = when (selectedUserType) {
                                TRAINING-> Intent(requireContext(), TraineeActivity::class.java)
                                TRAINER -> Intent(requireContext(), TrainerActivity::class.java)
                                DOCTOR -> Intent(requireContext(), DoctorActivity::class.java)
                                else -> null
                            }
                            intent?.also {
                                startActivity(it)
                                requireActivity().finish()
                            }
                        }

                        is NetworkResult.Error -> {
                            main { showToast(requireActivity(), result.message) }
                            binding.loginBtn.isEnabled = true
                            hideProgressBar()
                        }

                        else -> {
                        }
                    }
                }
            }
        }
    }
    companion object{
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