package com.example.smartgymapp.ui.doctor.dChat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val _getUsersFromUserBookedIdsAccepted =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getUsersFromUserBookedIdsAccepted = _getUsersFromUserBookedIdsAccepted.asStateFlow()

    init {
       // getTraineesFromTraineeCollection()
        getUsersFromUserBookedIdsAcceptedRealTime()
    }

    private fun getTraineesFromTraineeCollection() {
        _getUsersFromUserBookedIdsAccepted.value = CommonActivity.NetworkResult.Loading()

        val traineesCollectionRef = firestore.collection("users")
            .document(firebaseAuth.currentUser!!.uid)
            .collection("BookedChat")

        traineesCollectionRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                _getUsersFromUserBookedIdsAccepted.value =
                    CommonActivity.NetworkResult.Error(exception.message ?: "Unknown Error")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val userModels = snapshot.documents.mapNotNull { document ->
                    document.toObject(UserModel::class.java)
                }

                _getUsersFromUserBookedIdsAccepted.value =
                    if (userModels.isNotEmpty()) {
                        CommonActivity.NetworkResult.Success(userModels)
                    } else {
                        CommonActivity.NetworkResult.Error("No Trainees found")
                    }
            }
        }
    }

    private fun getUsersFromUserBookedIdsAcceptedRealTime() {
        viewModelScope.launch {
            _getUsersFromUserBookedIdsAccepted.emit(CommonActivity.NetworkResult.Loading())

            try {
                val userRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                // get the userBookedIdsAccepted list from the user collection
                val userBookedIdsAccepted =
                    userRef.get().await().toObject(UserModel::class.java)?.userBookedIdsAccepted

                // get all the users from the ids in the userBookedIdsAccepted list
                firestore.collection("users").addSnapshotListener() { snapshot, exception ->
                    if (exception != null) {
                        viewModelScope.launch {
                            _getUsersFromUserBookedIdsAccepted.emit(
                                CommonActivity.NetworkResult.Error(
                                    exception.message ?: "Unknown Error"
                                )
                            )
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        viewModelScope.launch(Dispatchers.IO) { // Offload mapping to IO dispatcher
                            val userModels = snapshot.documents.mapNotNull { document ->
                                document.toObject(UserModel::class.java)
                            }

                            val trainees = userModels.filter { userModel ->
                                userBookedIdsAccepted?.contains(userModel.userId) == true
                            }

                            _getUsersFromUserBookedIdsAccepted.emit(
                                if (trainees.isNotEmpty()) {
                                    CommonActivity.NetworkResult.Success(trainees)
                                } else {
                                    CommonActivity.NetworkResult.Error("No trainees found")
                                }
                            )
                        }
                    }
                }


            } catch (e: Exception) {
                _getUsersFromUserBookedIdsAccepted.emit(
                    CommonActivity.NetworkResult.Error(
                        e.message ?: "Unknown Error"
                    )
                )
            }
        }
    }
}