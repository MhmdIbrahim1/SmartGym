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
                val currentUserDocRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                currentUserDocRef.addSnapshotListener { currentUserSnapshot, _ ->
                    currentUserSnapshot?.let { snapshot ->
                        val userBookedIdsAccepted = snapshot.get("userBookedIdsAccepted") as? List<String>

                        if (!userBookedIdsAccepted.isNullOrEmpty()) {
                            val users = mutableListOf<UserModel>()

                            for (userId in userBookedIdsAccepted) {
                                val userDocRef = firestore.collection("users").document(userId)
                                userDocRef.addSnapshotListener { userSnapshot, _ ->
                                    userSnapshot?.let { snapshot ->
                                        if (snapshot.exists()) {
                                            val userModel = snapshot.toObject(UserModel::class.java)
                                            userModel?.let { users.add(it) }
                                        }
                                    }
                                    main {
                                        _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Success(users))
                                    }
                                }
                            }
                        } else {
                            main {
                                _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Error("No trainees found"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error"))
            }
        }
    }

    private fun getUsersFromUserBookedIdsAccepted() {
        viewModelScope.launch {
            _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Loading())

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
                        _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Success(users))
                    } else {
                        _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Error("No users found"))
                    }
                } else {
                    _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Error("No userBookedIdsAccepted found"))
                }
            } catch (e: Exception) {
                _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error"))
            }
        }
    }


}

