package com.example.smartgymapp.ui.trainer

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.example.smartgymapp.databinding.ActivityTrainerBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.trainer.tMain.MainTrainerViewModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.getResourceColor
import com.example.smartgymapp.util.CommonActivity.isLtr
import com.example.smartgymapp.util.MyContextWrapper
import com.example.smartgymapp.util.MyPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class TrainerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainerBinding
    private lateinit var navController: NavController
    private val viewModel by viewModels<MainTrainerViewModel>()

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
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required, Please allow notification permission from setting")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionRationale() {

        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
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
        binding = ActivityTrainerBinding.inflate(layoutInflater)
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
        binding.trainerNavView.setOnItemSelectedListener { item ->
            onNavDestinationSelected(item, navController)
        }

        val rippleColor = ColorStateList.valueOf(getResourceColor(R.attr.orange, 0.1f))

        binding.trainerNavView.apply {
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navHostFragment.navController.currentDestination?.let { updateNavBar(it) }
    }

    private fun updateNavBar(destination: NavDestination) {
        //   this.hideKeyboard()

        val dontPush = listOf(
            R.id.trainerMainFragment,
            R.id.trainerChatFragment,
            R.id.trainerProfileFragment,
        ).contains(destination.id)

        binding.trainerNavView.apply {
            val params = layoutParams as ConstraintLayout.LayoutParams
            val push =
                if (!dontPush) resources.getDimensionPixelSize(R.dimen.navbar_width) else 0
            if (!this.isLtr()) {
                params.setMargins(
                    params.leftMargin,
                    params.topMargin,
                    push,
                    params.bottomMargin
                )
            } else {
                params.setMargins(
                    push,
                    params.topMargin,
                    params.rightMargin,
                    params.bottomMargin
                )
            }
            layoutParams = params
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
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a dialog to prompt the user to grant notification permission
            AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage("Please grant permission to receive notifications.")
                .setPositiveButton("Allow") { dialog, _ ->
                    // Handle allow button click
                    // You can proceed with enabling notifications here
                    dialog.dismiss()
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    // Handle deny button click
                    // You can show a message or take any other action here
                    dialog.dismiss()
                }
                .setCancelable(false) // Prevent dismissing dialog by clicking outside of it
                .show()
        }
    }

    private fun observeBookingNumber() {
        lifecycleScope.launchSafe {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAllTraineesRequest.collectLatest {
                    when (it) {
                        is CommonActivity.NetworkResult.Success -> {
                            val count = it.data?.size ?: 0
                            val bottomNavigation = binding.trainerNavView
                            bottomNavigation.getOrCreateBadge(R.id.trainerMainFragment).apply{
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