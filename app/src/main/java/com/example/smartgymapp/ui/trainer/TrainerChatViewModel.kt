package com.example.smartgymapp.ui.trainer

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
class TrainerChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): ViewModel(){

    private val _getTraineesFromUsersCollection =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getTraineesFromUsersCollection = _getTraineesFromUsersCollection.asStateFlow()

    init {
        getUsersFromUserBookedIdsAcceptedRealTime()
        //getTraineesFromTraineeCollection()
    }

    private fun getTraineesFromTraineeCollection() {
        viewModelScope.launch {
            _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Loading())

            val traineesCollectionRef = firestore.collection("users")
                .document(firebaseAuth.currentUser!!.uid)
                .collection("BookedChat")

            try {
                val snapshotListener = traineesCollectionRef.addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        viewModelScope.launch {
                            _getTraineesFromUsersCollection.emit(
                                CommonActivity.NetworkResult.Error(exception.message ?: "Unknown Error")
                            )
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        viewModelScope.launch(Dispatchers.IO) { // Offload mapping to IO dispatcher
                            val userModels = snapshot.documents.mapNotNull { document ->
                                document.toObject(UserModel::class.java)
                            }

                            _getTraineesFromUsersCollection.emit(
                                if (userModels.isNotEmpty()) {
                                    CommonActivity.NetworkResult.Success(userModels)
                                } else {
                                    CommonActivity.NetworkResult.Error("No trainees found")
                                }
                            )
                        }
                    }
                }

                _getTraineesFromUsersCollection.onCompletion { snapshotListener.remove() }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _getTraineesFromUsersCollection.emit(
                    CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error" )
                )
            }
        }
    }

    private fun getUsersFromUserBookedIdsAcceptedRealTime() {
        viewModelScope.launch {
            _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Loading())

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
                                        _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Success(users))
                                    }
                                }
                            }
                        } else {
                            main {
                                _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Error("No trainees found"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error"))
            }
        }
    }


}