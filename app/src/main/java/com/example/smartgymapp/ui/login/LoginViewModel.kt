package com.example.smartgymapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel(
) {

    private val _login =
        MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()

    fun login(email: String, password: String, userType: String) {
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
    }

    private fun handleLoginSuccess() {
        viewModelScope.launch {
            _login.emit(NetworkResult.Success("Login Success"))
        }
    }

    companion object {
        const val USER_COLLECTION = "users"
        const val USER_TYPE_FIELD = "userType"
    }
}