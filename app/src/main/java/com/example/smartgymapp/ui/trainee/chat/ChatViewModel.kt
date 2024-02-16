package com.example.smartgymapp.ui.trainee.chat

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
        getTrainersFromUserCollection()
       // fetchTrainersAndDoctorsIds()
    }

    private fun getTrainersFromUserCollection() {
        viewModelScope.launch {
            _getTrainersFromUserCollection.emit(CommonActivity.NetworkResult.Loading())

            val trainersCollectionRef = firestore.collection("users")
                .document(firebaseAuth.currentUser!!.uid)
                .collection("BookedChat")

            try {
                val snapshotListener = trainersCollectionRef.addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                       viewModelScope.launch {
                            _getTrainersFromUserCollection.emit(
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

                            _getTrainersFromUserCollection.emit(
                                if (userModels.isNotEmpty()) {
                                    CommonActivity.NetworkResult.Success(userModels)
                                } else {
                                    CommonActivity.NetworkResult.Error("No trainers found")
                                }
                            )
                        }
                    }
                }

                _getTrainersFromUserCollection.onCompletion { snapshotListener.remove() }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _getTrainersFromUserCollection.emit(
                    CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error" )
                )
            }
        }
    }


    fun fetchTrainersAndDoctorsIds() {
        viewModelScope.launch {
            _getTrainersAndDoctorsIds.emit(CommonActivity.NetworkResult.Loading())

            try {
                val trainerAndDoctorIds = mutableListOf<String>()

                val querySnapshot = firestore.collection("users")
                    .whereArrayContains("TrainersAndDoctors", "dummyValue") // Replace "dummyValue" with any non-empty string
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    _getTrainersAndDoctorsIds.emit(
                        CommonActivity.NetworkResult.Error("No trainers and doctors found")
                    )
                    return@launch
                }

                for (document in querySnapshot.documents) {
                    val userModel = document.toObject(UserModel::class.java)
                    if (userModel?.TrainersAndDoctors.isNullOrEmpty()) {
                        // Skip if the TrainersAndDoctors list is empty
                        continue
                    }
                    trainerAndDoctorIds.addAll(userModel!!.TrainersAndDoctors)
                }

                if (trainerAndDoctorIds.isEmpty()) {
                    _getTrainersAndDoctorsIds.emit(
                        CommonActivity.NetworkResult.Error("No trainer or doctor IDs found")
                    )
                } else {
                    _getTrainersAndDoctorsIds.emit(CommonActivity.NetworkResult.Success(trainerAndDoctorIds))
                }
            } catch (e: Exception) {
                _getTrainersAndDoctorsIds.emit(
                    CommonActivity.NetworkResult.Error(e.message ?: "Unknown Error")
                )
            }
        }
    }



}

