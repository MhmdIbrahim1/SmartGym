package com.example.smartgymapp.ui.trainer.tProfile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.FragmentTrainerProfileBinding
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.ui.login.LoginActivity
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint

class TrainerProfileFragment : Fragment() {
    private lateinit var binding: FragmentTrainerProfileBinding
    private val userViewModel by activityViewModels<ProfileViewModel>()

    private lateinit var imageActivityResultLauncher: ActivityResultLauncher<Intent>

    // Coroutine jobs for observing data
    private var updateUserInfoJob: Job? = null
    private var getUserInfoJob: Job? = null

    private var imageUri: Uri? = null

    private lateinit var fcmtoken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                imageUri = it.data?.data
                Glide.with(this).load(imageUri).into(binding.imageUser)
            }
        getNFCToken()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrainerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeGetTrainerUser()
        observeUpdateUser()

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.imageEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            imageActivityResultLauncher.launch(intent)
        }
    }

    private fun observeGetTrainerUser() {
        getUserInfoJob?.cancel()
        getUserInfoJob = lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.getUser.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Loading -> {
                            showUserLoading()
                        }

                        is CommonActivity.NetworkResult.Success -> {
                            hideUserLoading()
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

    private fun observeUpdateUser() {
        updateUserInfoJob?.cancel()
        updateUserInfoJob = lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.updateInfo.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Loading -> {
                            binding.btnEdit.startAnimation()
                        }

                        is CommonActivity.NetworkResult.Success -> {
                            binding.btnEdit.revertAnimation()
                            findNavController().navigateUp()
                            Toast.makeText(
                                requireContext(),
                                "User Information Updated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is CommonActivity.NetworkResult.Error -> {
                            binding.btnEdit.revertAnimation()
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> Unit
                    }
                }
            }
        }

        blockUserInputForUpdate()
        // Set click listener for Edit/Update button
        binding.btnEdit.setOnClickListener {
            if (binding.btnEdit.text == "Edit" || binding.btnEdit.text == "تعديل") {
                unblockUserInput()
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnEdit.text = resources.getString(R.string.update)
            } else {
                // Handle update action
                binding.apply {
                    val firstName = edFirstName.text.toString().trim()
                    val lastName = edLastName.text.toString().trim()
                    val email = edEmail.text.toString().trim()
                    val uId = FirebaseAuth.getInstance().currentUser!!.uid
                    val userBookedIdsAccepted =
                        userViewModel.getUser.value.data!!.userBookedIdsAccepted
                    val userBookedIdsPending =
                        userViewModel.getUser.value.data!!.userBookedIdsPending
                    val user = UserModel(
                        uId,
                        firstName,
                        lastName,
                        email,
                        "Trainer",
                        userViewModel.getUser.value.data!!.profile_picture,
                        userBookedIdsAccepted,
                        userBookedIdsPending,
                        fcmtoken
                    )
                    userViewModel.updateUser(user, imageUri)
                }
                // Revert button text and hide cancel button
                blockUserInputForUpdate()
                binding.btnCancel.visibility = View.GONE
                binding.btnEdit.text = resources.getString(R.string.edit)
            }
        }

        // Set click listener for Cancel button
        binding.btnCancel.setOnClickListener {
            blockUserInputForUpdate()
            binding.btnCancel.visibility = View.GONE
            binding.btnEdit.text = resources.getString(R.string.edit)
        }
    }


    private fun hideUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.GONE
            imageUser.visibility = View.VISIBLE
            edFirstName.visibility = View.VISIBLE
            edLastName.visibility = View.VISIBLE
            edEmail.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE
        }
    }

    private fun showUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.VISIBLE
            imageUser.visibility = View.INVISIBLE
            edFirstName.visibility = View.INVISIBLE
            edLastName.visibility = View.INVISIBLE
            edEmail.visibility = View.INVISIBLE
            btnEdit.visibility = View.INVISIBLE
        }
    }

    private fun showUserInformation(data: UserModel) {
        binding.apply {
            Glide.with(requireContext())
                .load(data.profile_picture)
                .error(ResourcesCompat.getDrawable(resources, R.drawable.man_user, null))
                .into(imageUser)
            edFirstName.setText(data.firstName)
            edLastName.setText(data.lastName)
            edEmail.setText(data.email)
            edUserType.setText(data.userType)
        }
    }

    private fun blockUserInputForUpdate() {
        binding.apply {
            edFirstName.isEnabled = false
            edLastName.isEnabled = false
            edEmail.isEnabled = false
            edUserType.isEnabled = false
            imageEdit.visibility = View.INVISIBLE
        }
    }

    private fun unblockUserInput() {
        binding.apply {
            edFirstName.isEnabled = true
            edLastName.isEnabled = true
            imageEdit.visibility = View.VISIBLE
        }
    }
    private fun getNFCToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .update("fcmToken", token)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d("FCM", "Token: $token")
                            fcmtoken = token
                        }
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Hide the bottom navigation bar
        val bottomNav = requireActivity().findViewById<View>(R.id.trainer_nav_view)
        bottomNav.visibility = View.GONE
    }
    override fun onDestroyView() {
        super.onDestroyView()

        // Clear the updateInfo StateFlow when the view is destroyed
        userViewModel.clearUpdateInfo()
    }

}