package com.example.smartgymapp.ui.login

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sharedPreferences: SharedPreferences
) : ViewModel(
) {

    private val _login =
        MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()

    private val _navigateState = MutableStateFlow(0)
    val navigateState = _navigateState.asStateFlow()


    companion object {
        const val TRAINING_ACTIVITY = 23
        const val TRAINER_ACTIVITY = 24
        const val DOCTOR_ACTIVITY = 25
        const val USER_COLLECTION = "users"
        const val USER_TYPE_FIELD = "userType"
        const val USER_LOGGED_IN_KEY = "USER_LOGGED_IN" // Add key for shared preferences
    }
    init {
        val userLoggedIn = sharedPreferences.getBoolean(USER_LOGGED_IN_KEY, false)
        if (userLoggedIn && auth.currentUser != null) {
            // User is logged in, emit navigation state accordingly
            viewModelScope.launch {
                val activityCode = getActivityForUserType(auth.currentUser!!)
                _navigateState.emit(activityCode)
                Log.d("LoginViewModel", "User already logged in. Navigating to activity code: $activityCode")
            }
        }
    }

    private suspend fun getActivityForUserType(user: FirebaseUser): Int {
        return when (getUserTypeFromFirestore(user.uid)) {
            "Trainee" -> TRAINING_ACTIVITY
            "Trainer" -> TRAINER_ACTIVITY
            "Doctor" -> DOCTOR_ACTIVITY
            else -> 0
        }
    }
    private suspend fun getUserTypeFromFirestore(uid: String): String {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("userType") ?: "Unknown"
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error fetching user type: ${e.message}")
            "Unknown"
        }
    }
    fun login(email: String, password: String, userType: String) {
        Log.d("LoginViewModel", "Attempting login...")
        // Check for empty fields
        if (email.isEmpty() || password.isEmpty()) {
            viewModelScope.launch {
                _login.emit(NetworkResult.Error("Email or password cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _login.emit(NetworkResult.Loading())
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userUid = authResult.user?.uid
                if (userUid != null) {
                    checkUserType(userUid, userType)
                } else {
                    handleLoginFailure(Exception("User UID is null"))
                }
            }
            .addOnFailureListener {
                handleLoginFailure(it)
            }
    }


    private fun checkUserType(uid: String, userType: String) {
        firestore.collection(USER_COLLECTION).document(uid).get()
            .addOnSuccessListener { document ->
                val userTypeFromDB = document.getString(USER_TYPE_FIELD)
                    ?: throw Exception("User type not found in DB")
                if (userTypeFromDB == userType) {
                    handleLoginSuccess()
                } else {
                    handleLoginFailure(Exception("User type mismatch"))
                }
            }
            .addOnFailureListener {
                handleLoginFailure(it)
            }
    }
    private fun handleLoginFailure(exception: Exception) {
        val errorMessage = "Login Failed: ${exception.message}"
        viewModelScope.launch {
            _login.emit(NetworkResult.Error(errorMessage))
        }
        Log.e("LoginViewModel", errorMessage)
    }

    private fun handleLoginSuccess() {
        viewModelScope.launch {
            _login.emit(NetworkResult.Success("Login Success"))
            // Save user login state to SharedPreferences
            sharedPreferences.edit().putBoolean(USER_LOGGED_IN_KEY, true).apply()
            Log.d("LoginViewModel", "Login success. User login state saved.")
        }
    }
}