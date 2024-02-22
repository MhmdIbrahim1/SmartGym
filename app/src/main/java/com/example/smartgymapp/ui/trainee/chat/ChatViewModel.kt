package com.example.smartgymapp.ui.trainee.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.Coroutines.main
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _getTrainersFromUserCollection =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getTrainersFromUserCollection = _getTrainersFromUserCollection.asStateFlow()

    private val _getTrainersAndDoctorsIds =
        MutableStateFlow<CommonActivity.NetworkResult<List<String>>>(CommonActivity.NetworkResult.UnSpecified())
    val getTrainersAndDoctorsIds = _getTrainersAndDoctorsIds.asStateFlow()

    private val _getTrainersFromBookedList =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getTrainersFromBookedList = _getTrainersFromBookedList.asStateFlow()
    init {
        getUsersFromUserBookedIdsAcceptedRealTime()
      //  getUsersFromUserBookedIdsAccepted()
        // getTrainersFromUserCollection()
    }

    //    private fun getTrainersFromUserCollection() {
//        viewModelScope.launch {
//            _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Loading())
//
//            val trainersCollectionRef = firestore.collection("users")
//                .document(firebaseAuth.currentUser!!.uid)
//                .collection("BookedChat")
//
//            try {
//                val snapshotListener = trainersCollectionRef.addSnapshotListener { snapshot, exception ->
//                    if (exception != null) {
//                       viewModelScope.launch {
//                            _getTrainersFromUserCollection.emit(
//                                CommonActivity.NetworkResult.Error(exception.message ?: "Unknown Error")
//                            )
//                       }
//                        return@addSnapshotListener
//                    }
//
//                    if (snapshot != null) {
//                        viewModelScope.launch(Dispatchers.IO) { // Offload mapping to IO dispatcher
//                            val userModels = snapshot.documents.mapNotNull { document ->
//                                document.toObject(UserModel::class.java)
//                            }
//
//                            _getTrainersFromUserCollection.emit(
//                                if (userModels.isNotEmpty()) {
//                                    CommonActivity.NetworkResult.Success(userModels)
//                                } else {
//                                    CommonActivity.NetworkResult.Error("No trainers found")
//                                }
//                            )
//                        }
//                    }
//                }
//
//                _getTrainersFromUserCollection.onCompletion { snapshotListener.remove() }
//
//            } catch (e: CancellationException) {
//                throw e
//            } catch (e: Exception) {
//                _getTrainersFromUserCollection.emit(
//                    CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error" )
//                )
//            }
//        }
//    }
//
    private fun getUsersFromUserBookedIdsAcceptedRealTime() {
        viewModelScope.launch {
            _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Loading())

            try {
                val userRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                // get the userBookedIdsAccepted list from the user collection
                val userBookedIdsAccepted =
                    userRef.get().await().toObject(UserModel::class.java)?.userBookedIdsAccepted

                // get all the users from the ids in the userBookedIdsAccepted list
                firestore.collection("users").addSnapshotListener() { snapshot, exception ->
                    if (exception != null) {
                        viewModelScope.launch {
                            _getTrainersFromUserCollection.emit(
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

                            _getTrainersFromUserCollection.emit(
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
                _getTrainersFromUserCollection.emit(
                    CommonActivity.NetworkResult.Error(
                        e.message ?: "Unknown Error"
                    )
                )
            }
        }
    }

     fun getTrainersFromBookedAccepted(userType: String) {
        viewModelScope.launch {
            _getTrainersFromBookedList.emit(CommonActivity.NetworkResult.Loading())

            try {
                val userRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                // get the userBookedIdsAccepted list from the user collection
                val userBookedIdsAccepted =
                    userRef.get().await().toObject(UserModel::class.java)?.userBookedIdsAccepted

                // get all the users from the ids in the userBookedIdsAccepted list
                firestore.collection("users")
                    .whereEqualTo("userType", userType)
                    .addSnapshotListener() { snapshot, exception ->
                    if (exception != null) {
                        viewModelScope.launch {
                            _getTrainersFromBookedList.emit(
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

                            _getTrainersFromBookedList.emit(
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
                _getTrainersFromBookedList.emit(
                    CommonActivity.NetworkResult.Error(
                        e.message ?: "Unknown Error"
                    )
                )
            }
        }
    }

}

