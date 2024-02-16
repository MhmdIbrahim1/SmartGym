package com.example.smartgymapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartGymApp: Application(){
    override fun onCreate() {
        super.onCreate()
        //initialize firebase
        FirebaseApp.initializeApp(this)
    }
}
