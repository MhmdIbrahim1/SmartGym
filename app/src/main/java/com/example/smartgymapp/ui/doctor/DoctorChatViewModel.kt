package com.example.smartgymapp.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DoctorChatViewModel@Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): ViewModel(){
    private val _getTraineesFromUsersCollection =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getTraineesFromUsersCollection = _getTraineesFromUsersCollection.asStateFlow()

    init {
       // getTraineesFromTraineeCollection()
        getUsersFromUserBookedIdsAccepted()
    }

    private fun getTraineesFromTraineeCollection() {
        _getTraineesFromUsersCollection.value = CommonActivity.NetworkResult.Loading()

        val traineesCollectionRef = firestore.collection("users")
            .document(firebaseAuth.currentUser!!.uid)
            .collection("BookedChat")

        traineesCollectionRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                _getTraineesFromUsersCollection.value =
                    CommonActivity.NetworkResult.Error(exception.message ?: "Unknown Error")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val userModels = snapshot.documents.mapNotNull { document ->
                    document.toObject(UserModel::class.java)
                }

                _getTraineesFromUsersCollection.value =
                    if (userModels.isNotEmpty()) {
                        CommonActivity.NetworkResult.Success(userModels)
                    } else {
                        CommonActivity.NetworkResult.Error("No Trainees found")
                    }
            }
        }
    }

    private fun getUsersFromUserBookedIdsAccepted() {
        viewModelScope.launch {
            _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Loading())

            try {
                val currentUserDocRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                val currentUserSnapshot = currentUserDocRef.get().await()
                val userBookedIdsAccepted = currentUserSnapshot.get("userBookedIdsAccepted") as? List<String>

                if (!userBookedIdsAccepted.isNullOrEmpty()) {
                    val users = mutableListOf<UserModel>()

                    for (userId in userBookedIdsAccepted) {
                        val userDocRef = firestore.collection("users").document(userId)
                        val userSnapshot = userDocRef.get().await()

                        if (userSnapshot.exists()) {
                            val userModel = userSnapshot.toObject(UserModel::class.java)
                            userModel?.let { users.add(it) }
                        }
                    }

                    if (users.isNotEmpty()) {
                        _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Success(users))
                    } else {
                        _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Error("No users found"))
                    }
                } else {
                    _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Error("No userBookedIdsAccepted found"))
                }
            } catch (e: Exception) {
                _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error"))
            }
        }
    }
}