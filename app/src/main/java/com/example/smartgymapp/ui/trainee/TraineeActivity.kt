package com.example.smartgymapp.ui.trainee

import android.content.res.ColorStateList
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.example.smartgymapp.databinding.ActivityTraineeBinding
import com.example.smartgymapp.util.CommonActivity.getResourceColor
import com.example.smartgymapp.util.CommonActivity.isLtr
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TraineeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraineeBinding
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTraineeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getNFCToken()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.trainee_fragment_container_view) as NavHostFragment
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

//        val userId = intent.extras?.getString("userId")
//        FirebaseFirestore.getInstance().collection(USER_COLLECTION).document(userId!!).get()
//            .addOnCompleteListener {task ->
//                if (task.isSuccessful){
//                    val model = task.result.toObject(UserModel::class.java)
//                    if (model != null){
//                        val intent = Intent(this, DoChatActivity::class.java)
//                        CommonActivity.passUserModelAsIntent(intent, model)
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        startActivity(intent)
//                    }
//                }
//
//            }

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
            supportFragmentManager.findFragmentById(R.id.trainee_fragment_container_view) as NavHostFragment
        navHostFragment.navController.currentDestination?.let { updateNavBar(it) }
    }

    private fun updateNavBar(destination: NavDestination) {
        //   this.hideKeyboard()

        val dontPush = listOf(
            R.id.mainFragment,
            R.id.chatFragment,
            R.id.profileFragment,
            R.id.searchFragment
        ).contains(destination.id)

        binding.traineeFragmentContainerView.apply {
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

    private fun getNFCToken(){
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if(it.isSuccessful){
                val token = it.result
                FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .update("fcmToken", token)
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            Log.d("FCM", "Token: $token")
                        }
                    }
            }
        }
    }
}
