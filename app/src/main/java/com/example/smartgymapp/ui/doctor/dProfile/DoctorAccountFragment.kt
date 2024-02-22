package com.example.smartgymapp.ui.doctor.dProfile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentDoctorAccountBinding
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.ui.login.LoginActivity
import com.example.smartgymapp.ui.trainer.tProfile.ProfileViewModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DoctorAccountFragment : Fragment() {
    private lateinit var binding: FragmentDoctorAccountBinding

    private val viewModel by activityViewModels<ProfileViewModel>()
    private var getUserInfoJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentDoctorAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        observeGetTrainerUser()

        binding.cardUserDetails.setOnClickListener {
            if (Navigation.findNavController(view).currentDestination?.id == R.id.doctorAccountFragment) {
                Navigation.findNavController(view)
                    .navigate(R.id.action_doctorAccountFragment_to_doctorProfileFragment)
            }
        }

        binding.cardTraineesBookings.setOnClickListener {
            CommonActivity.showToast(requireActivity(), "Soon to be implemented")
        }



        binding.tvLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { _, _ ->
            FirebaseMessaging.getInstance().deleteToken()
            FirebaseAuth.getInstance().signOut()
            Intent(requireActivity(), LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        }
        builder.setNegativeButton("No") { _, _ -> }
        builder.show()
    }

    private fun showUserInformation(data: UserModel) {
        binding.apply {
            tvName.text = resources.getString(R.string.ahlan).plus(" ").plus(data.firstName)
            tvEmail.text = data.email
        }

    }
    private fun observeGetTrainerUser() {
        getUserInfoJob?.cancel()
        getUserInfoJob = lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getUser.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Loading -> {
                        }

                        is CommonActivity.NetworkResult.Success -> {
                            showUserInformation(it.data!!)
                            Log.d("UserAccountFragment", it.data.toString())
                        }

                        is CommonActivity.NetworkResult.Error -> {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        // Show the bottom navigation bar
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        getUserInfoJob?.cancel()
    }
}
