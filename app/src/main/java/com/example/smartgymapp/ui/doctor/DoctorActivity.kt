package com.example.smartgymapp.ui.doctor

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ActivityDoctorBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.doctor.dChat.DoctorChatViewModel
import com.example.smartgymapp.ui.doctor.dMain.MainDoctorViewModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.colorFromAttribute
import com.example.smartgymapp.util.CommonActivity.getResourceColor
import com.example.smartgymapp.util.MyContextWrapper
import com.example.smartgymapp.util.MyPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DoctorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorBinding
    private lateinit var navController: NavController
    private val mainVewModel by viewModels<MainDoctorViewModel>()

    private lateinit var myPreference: MyPreference

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotificationPermissionGranted = isGranted
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                            showNotificationPermissionRationale()
                        } else {
                            showSettingDialog()
                        }
                    }
                }
            }
        }

    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required, Please allow notification permission from setting")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionRationale() {

        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("Alert")
            .setMessage("Notification permission is required, to show notification")
            .setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private var hasNotificationPermissionGranted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getNFCToken()
        observeBookingNumber()

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermissionGranted = true
        }


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        binding.navView.setOnItemSelectedListener { item ->
            onNavDestinationSelected(item, navController)
        }

        val rippleColor = ColorStateList.valueOf(getResourceColor(R.attr.orange, 0.1f))

        binding.navView.apply {
            itemRippleColor = rippleColor
            itemActiveIndicatorColor = rippleColor
            setupWithNavController(navController)
            setOnItemSelectedListener { item ->
                onNavDestinationSelected(
                    item,
                    navController
                )
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    window?.navigationBarColor = colorFromAttribute(R.attr.primaryGrayBackground)

                    // If we don't disable we end up in a loop with default behavior calling
                    // this callback as well, so we disable it, run default behavior,
                    // then re-enable this callback so it can be used for next back press.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        )
    }


    private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
        hierarchy.any { it.id == destId }

    private fun onNavDestinationSelected(item: MenuItem, navController: NavController): Boolean {
        val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
            .setEnterAnim(R.anim.enter_anim)
            .setExitAnim(R.anim.exit_anim)
            .setPopEnterAnim(R.anim.pop_enter)
            .setPopExitAnim(R.anim.pop_exit)
        if (item.order and Menu.CATEGORY_SECONDARY == 0) {
            builder.setPopUpTo(
                navController.graph.findStartDestination().id,
                inclusive = true,
                saveState = true
            )
        }
        val options = builder.build()
        return try {
            navController.navigate(item.itemId, null, options)
            navController.currentDestination?.matchDestination(item.itemId) == true
        } catch (e: IllegalArgumentException) {
            false
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
                        }
                    }
            }
        }
    }

    private fun observeBookingNumber() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainVewModel.getAllTraineesRequest.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Success -> {
                            val count = it.data?.size ?: 0
                            val bottomNavigation = binding.navView
                            bottomNavigation.getOrCreateBadge(R.id.doctorMainFragment).apply{
                                number = count
                                backgroundColor = resources.getColor(R.color.colorPrimaryRed, null)
                                badgeTextColor = resources.getColor(R.color.white, null)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        myPreference = MyPreference(newBase!!)
        val lang = myPreference.getLanguage()
        super.attachBaseContext(MyContextWrapper.wrap(newBase,lang))
    }

}