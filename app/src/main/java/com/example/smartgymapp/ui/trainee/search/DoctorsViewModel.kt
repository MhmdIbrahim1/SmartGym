package com.example.smartgymapp.ui.trainee.search

import androidx.lifecycle.ViewModel
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DoctorsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
): ViewModel(){
    private val _getAllDoctors =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getAllDoctors = _getAllDoctors.asStateFlow()

    init {
        getAllDoctors()
    }

    private fun getAllDoctors() {
        _getAllDoctors.value = CommonActivity.NetworkResult.Loading()
        firestore.collection("users")
            .whereEqualTo("userType", "Doctor")
            .get()
            .addOnSuccessListener { result ->
                val doctors = result.toObjects(UserModel::class.java)
                _getAllDoctors.value = CommonActivity.NetworkResult.Success(doctors)
            }
            .addOnFailureListener { exception ->
                _getAllDoctors.value = CommonActivity.NetworkResult.Error(exception.message)
            }
    }
}