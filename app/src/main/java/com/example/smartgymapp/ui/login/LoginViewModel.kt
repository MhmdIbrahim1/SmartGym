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
) : ViewModel(
) {

    private val _login =
        MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()

    companion object {
        const val USER_COLLECTION = "users"
        const val USER_TYPE_FIELD = "userType"
    }
    fun login(email: String, password: String) {
        // Use the default user type for logging in
        performLogin(email, password)
    }

    private fun performLogin(email: String, password: String) {
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
                    fetchUserType(userUid)
                } else {
                    handleLoginFailure(Exception("User UID is null"))
                }
            }
            .addOnFailureListener {
                handleLoginFailure(it)
            }
    }

    private fun fetchUserType(uid: String) {
        firestore.collection(USER_COLLECTION).document(uid).get()
            .addOnSuccessListener { document ->
                val userTypeFromDB = document.getString(USER_TYPE_FIELD)
                if (!userTypeFromDB.isNullOrBlank()) {
                    handleLoginSuccess(userTypeFromDB)
                } else {
                    handleLoginFailure(Exception("User type not found in DB"))
                }
            }
            .addOnFailureListener {
                handleLoginFailure(it)
            }
    }

    private fun handleLoginSuccess(userType: String) {
        viewModelScope.launch {
            _login.emit(NetworkResult.Success(userType))
        }
    }

    private fun handleLoginFailure(exception: Exception) {
        val errorMessage = "Login Failed: ${exception.message}"
        viewModelScope.launch {
            _login.emit(NetworkResult.Error(errorMessage))
        }
        Log.e("LoginViewModel", errorMessage)
    }

}