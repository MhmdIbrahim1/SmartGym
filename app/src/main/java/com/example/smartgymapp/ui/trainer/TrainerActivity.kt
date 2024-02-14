package com.example.smartgymapp.ui.trainer

import android.content.res.ColorStateList
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ActivityTrainerBinding
import com.example.smartgymapp.util.CommonActivity.getResourceColor
import com.example.smartgymapp.util.CommonActivity.isLtr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrainerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainerBinding
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.trainer_nav_host_fragment) as NavHostFragment
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
                inclusive = false,
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
            supportFragmentManager.findFragmentById(R.id.trainer_nav_host_fragment) as NavHostFragment
        navHostFragment.navController.currentDestination?.let { updateNavBar(it) }
    }

    private fun updateNavBar(destination: NavDestination) {
        //   this.hideKeyboard()

        val dontPush = listOf(
            R.id.trainerMainFragment,
            R.id.trainerChatFragment,
            R.id.trainerProfileFragment,
        ).contains(destination.id)

        binding.trainerNavHostFragment.apply {
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
}