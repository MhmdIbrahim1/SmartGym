package com.example.smartgymapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.smartgymapp.R
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.ui.dochat.DoChatActivity
import com.example.smartgymapp.ui.doctor.DoctorActivity
import com.example.smartgymapp.ui.login.LoginActivity
import com.example.smartgymapp.ui.trainee.TraineeActivity
import com.example.smartgymapp.ui.trainer.TrainerActivity
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.Coroutines.main
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.CoroutineContext

class StartedScreen : AppCompatActivity(), CoroutineScope {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private  var currentUserType = ""
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_started_screen)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if the activity was started from a notification
        if (intent.extras != null) {
            val userId = intent.extras?.getString("userId")
            FirebaseFirestore.getInstance().collection("users").document(userId!!)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userModel = task.result?.toObject(UserModel::class.java)
                        if (userModel != null) {
                            // Fetch the user type from Firestore
                            main {
                                currentUserType = getUserTypeFromFirestore(auth.currentUser!!.uid)
                                Log.d("Test", "UserType: $currentUserType")
                                navigateToChatActivity(userModel, currentUserType)
                            }
                        } else {
                            // If userModel is null, navigate to login screen
                            navigateToLoginScreen()
                        }
                    } else {
                        // If the task is not successful, navigate to login screen
                        navigateToLoginScreen()
                    }
                }
        } else {
            // If not started from notification, check login state
            Handler(Looper.myLooper()!!).postDelayed({
                checkLoginState()
            }, SPLASH_DELAY)
        }
    }

    private fun checkLoginState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            launch {
                val userType = getUserTypeFromFirestore(currentUser.uid)
                Log.d("Test", "UserType: $userType")
                navigateToActivity(userType)
            }
        } else {
            // If user not logged in, navigate to login screen
            navigateToLoginScreen()
        }
    }

    private suspend fun getUserTypeFromFirestore(uid: String): String {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("userType") ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    private fun navigateToActivity(userType: String) {

        val intent = when (userType) {
            "Trainee" -> Intent(this, TraineeActivity::class.java)
            "Trainer" -> Intent(this, TrainerActivity::class.java)
            "Doctor" -> Intent(this, DoctorActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToChatActivity(userModel: UserModel, userType: String) {
        // Determine which activity to navigate back to based on the user's type
        val intent = when (userType) {
            "Trainee" -> Intent(this, TraineeActivity::class.java)
            "Trainer" -> Intent(this, TrainerActivity::class.java)
            "Doctor" -> Intent(this, DoctorActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        // Navigate to chat activity
        val chatIntent = Intent(this, DoChatActivity::class.java)
        CommonActivity.passUserModelAsIntent(chatIntent, userModel)
        chatIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(chatIntent)
        // Finish the current activity
        finish()
    }


    companion object {
        private const val SPLASH_DELAY = 2000L
    }
}
