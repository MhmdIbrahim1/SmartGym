package com.example.smartgymapp.ui.trainer.tChat

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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class TrainerChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

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
                val snapshotListener =
                    traineesCollectionRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            viewModelScope.launch {
                                _getTraineesFromUsersCollection.emit(
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
                    CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error")
                )
            }
        }
    }

    private fun getUsersFromUserBookedIdsAcceptedRealTime() {
        viewModelScope.launch {
            _getTraineesFromUsersCollection.emit(CommonActivity.NetworkResult.Loading())

            try {
                val userRef = firestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                // get the userBookedIdsAccepted list from the user collection
                val userBookedIdsAccepted =
                    userRef.get().await().toObject(UserModel::class.java)?.userBookedIdsAccepted

                // get all the users from the ids in the userBookedIdsAccepted list
                firestore.collection("users").addSnapshotListener() { snapshot, exception ->
                    if (exception != null) {
                        viewModelScope.launch {
                            _getTraineesFromUsersCollection.emit(
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

                            _getTraineesFromUsersCollection.emit(
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
                _getTraineesFromUsersCollection.emit(
                    CommonActivity.NetworkResult.Error(
                        e.message ?: "Unknown Error"
                    )
                )
            }
        }
    }


}