package com.example.smartgymapp.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainTrainerViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): ViewModel(){

    private val _getAllTraineesRequest
    = MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())

    val getAllTraineesRequest = _getAllTraineesRequest.asStateFlow()

    init {
        getAllTrainees()
    }
    private fun getAllTrainees(){
        viewModelScope.launchSafe {
            _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Loading())
        }
        firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("traineeRequests")
            .addSnapshotListener(){ value, error ->
                if(error != null){
                    viewModelScope.launchSafe {
                        _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Error(error.message))
                    }
                    return@addSnapshotListener
                }
               if (value != null){
                   val trainees = value.toObjects(UserModel::class.java)
                   viewModelScope.launchSafe {
                       _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Success(trainees))
                   }
               }
            }
    }
}