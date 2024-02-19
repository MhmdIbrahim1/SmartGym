package com.example.smartgymapp.ui.dochat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartgymapp.R
import com.example.smartgymapp.SmartGymApp
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.ui.doctor.DoctorActivity
import com.example.smartgymapp.ui.login.LoginActivity
import com.example.smartgymapp.ui.trainee.TraineeActivity
import com.example.smartgymapp.ui.trainer.TrainerActivity
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.Coroutines.ioSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class FCMNotificationService : FirebaseMessagingService() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMNotificationService", "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)


        // Check if the current activity is DoChatActivity
        val currentActivity = (applicationContext  as SmartGymApp).getCurrentActivity()
        if (currentActivity == "DoChatActivity") {
            // If the current activity is DoChatActivity, do not display notification
            return
        }

        // Handle the incoming message and display notification
        handleIncomingMessage(remoteMessage)

        // Check if the notification action indicates the notification was clicked
        if (remoteMessage.data["action"] == "NOTIFICATION_CLICKED") {
            val userId = remoteMessage.data["userId"]
            if (userId != null) {
                // Fetch the user model from Firestore
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userModel = task.result?.toObject(UserModel::class.java)
                            if (userModel != null) {
                                // Navigate to chat activity
                                navigateToChatActivity(userModel)
                            } else {
                                // navigate to main activity
                                checkLoginState()
                            }
                        }
                    }
            }
        }
    }


    private fun handleIncomingMessage(remoteMessage: RemoteMessage) {
        // Extract necessary data from the remote message
        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val dataPayload = remoteMessage.data

        // Customize notification behavior as needed
        showNotification(notificationTitle, notificationBody, dataPayload)
    }

    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        // Define notification content and behavior
        val channelId = "Default"
        val userId = data["userId"]
        if (userId != null) {
            // Fetch the user model from Firestore
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userModel = task.result?.toObject(UserModel::class.java)
                        if (userModel != null) {
                            // Define intent to navigate to chat activity and pass UserModel as intent
                            val intent = Intent(this, DoChatActivity::class.java)
                            CommonActivity.passUserModelAsIntent(intent, userModel)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                            // Set the action indicating notification click
                            intent.action = "NOTIFICATION_CLICKED"

                            // Create pending intent for notification
                            val pendingIntent = PendingIntent.getActivity(
                                this,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            // Build notification
                            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                                .setSmallIcon(R.drawable.icon_send)
                                .setContentTitle(title)
                                .setContentText(body)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent) // Set the pending intent

                            // Display the notification
                            val notificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel = NotificationChannel(
                                    channelId,
                                    "Default channel",
                                    NotificationManager.IMPORTANCE_DEFAULT
                                )
                                notificationManager.createNotificationChannel(channel)
                            }
                            notificationManager.notify(0, notificationBuilder.build())
                        }
                    }
                }
        }
    }


    private fun navigateToChatActivity(userModel: UserModel) {
        val intent = Intent(this, DoChatActivity::class.java)
        CommonActivity.passUserModelAsIntent(intent, userModel)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun checkLoginState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            ioSafe {
                val userType = getUserTypeFromFirestore(currentUser.uid)
                navigateToActivity(userType)
            }

        } else {
            // If user not logged in, navigate to login screen
            navigateToLoginScreen()
        }
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
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
    }

}
