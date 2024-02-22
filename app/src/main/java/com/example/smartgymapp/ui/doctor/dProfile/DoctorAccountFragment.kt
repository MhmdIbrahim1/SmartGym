package com.example.smartgymapp.ui.doctor.dProfile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.example.smartgymapp.mvvm.logError
import com.example.smartgymapp.ui.login.LoginActivity
import com.example.smartgymapp.ui.trainer.tProfile.ProfileViewModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.MyPreference
import com.example.smartgymapp.util.SingeSelectionHelper.showDialog
import com.example.smartgymapp.util.SubtitleHelper
import com.example.smartgymapp.util.UiHelper.navigate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale


@AndroidEntryPoint
class DoctorAccountFragment : Fragment() {
    private lateinit var binding: FragmentDoctorAccountBinding

    private val viewModel by activityViewModels<ProfileViewModel>()
    private var getUserInfoJob: Job? = null

    private lateinit var myPreference: MyPreference
    private var currentLanguage: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDoctorAccountBinding.inflate(inflater, container, false)
        myPreference = MyPreference(requireContext())

        currentLanguage = myPreference.getLanguage()

        CommonActivity.setSubArrowImageBasedOnLayoutDirection(resources, binding.ivArrow2)
        CommonActivity.setSubArrowImageBasedOnLayoutDirection(resources, binding.ivArrow3)
        CommonActivity.setSubArrowImageBasedOnLayoutDirection(resources, binding.ivArrow4)

        observeGetTrainerUser()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun navigate(id: Int) {
            activity?.navigate(id, Bundle())
        }
        observeGetTrainerUser()

        binding.apply {
            listOf(
                cardUserDetails to R.id.action_navigation_global_to_navigation_my_profile,
                cardTraineesBookings to R.id.action_navigation_global_to_navigation_my_patients,
            ).forEach{(view, navigationId) ->
                view.apply {
                    setOnClickListener {
                        navigate(navigationId)
                    }
                }
            }
        }
        binding.cardRegional.setOnClickListener {
            val tempLangs = CommonActivity.appLanguages.toMutableList()
            val current = CommonActivity.getCurrentLocale(requireContext())
            val languageCodes = tempLangs.map { (_, _, iso) -> iso }
            val languageNames = tempLangs.map { (emoji, name, iso) ->
                val flag = emoji.ifBlank { SubtitleHelper.getFlagFromIso(iso) ?: "ERROR" }
                "$flag $name"
            }
            val index = languageCodes.indexOf(current)
            activity?.showDialog(
                languageNames, index, getString(R.string.app_language), true, { }
            ) { languageIndex ->
                try {
                    val code = languageCodes[languageIndex]
                    myPreference.setLanguage(code)
                    val locale = Locale(code)
                    Locale.setDefault(locale)
                    val config = requireContext().resources.configuration
                    config.setLocale(locale)
                    requireContext().resources.updateConfiguration(
                        config,
                        requireContext().resources.displayMetrics
                    )
                    activity?.recreate()
                } catch (e: Exception) {
                    logError(e)
                }
            }
            return@setOnClickListener
        }

        binding.cardUserNotifications.setOnClickListener{
            openNotificationSettings()
        }


        binding.tvLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

    }

    private fun openNotificationSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Handle the case where the intent cannot be resolved
            CommonActivity.showToast(
                requireActivity(),
                "Notification settings are not available right now. Please try again later."
            )
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(resources.getString(R.string.log_out))
        builder.setMessage(resources.getString(R.string.are_you_sure_you_want_to_logout))
        builder.setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
            FirebaseMessaging.getInstance().deleteToken()
            FirebaseAuth.getInstance().signOut()
            Intent(requireActivity(), LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        }
        builder.setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
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
