package com.example.smartgymapp.ui.trainer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.BookingStatus
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainTrainerViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _getAllTraineesRequest =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())

    val getAllTraineesRequest = _getAllTraineesRequest.asStateFlow()

    private val acceptedTraineeRequest =
        MutableLiveData<CommonActivity.NetworkResult<UserModel>>(CommonActivity.NetworkResult.UnSpecified())
    val acceptedTraineeRequestLiveData: LiveData<CommonActivity.NetworkResult<UserModel>> =
        acceptedTraineeRequest

    private val _rejectedTraineeRequest =
        MutableLiveData<CommonActivity.NetworkResult<UserModel>>(CommonActivity.NetworkResult.UnSpecified())
    val rejectedTraineeRequestLiveData: LiveData<CommonActivity.NetworkResult<UserModel>> = _rejectedTraineeRequest

    init {
        getAllTrainees()
    }

    private fun getAllTrainees() {
        viewModelScope.launchSafe {
            _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Loading())
        }
        firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("traineeRequests")
            .addSnapshotListener() { value, error ->
                if (error != null) {
                    viewModelScope.launchSafe {
                        _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Error(error.message))
                    }
                    return@addSnapshotListener
                }
                if (value != null) {
                    val trainees = value.toObjects(UserModel::class.java)
                    viewModelScope.launchSafe {
                        _getAllTraineesRequest.emit(CommonActivity.NetworkResult.Success(trainees))
                    }
                    Log.d("TrainersViewModel", "Trainees: $trainees")
                }
            }
    }

        fun acceptAndSendTrainees(traineeUserId: String, trainerUserId: String) {
            viewModelScope.launchSafe { acceptedTraineeRequest.postValue(CommonActivity.NetworkResult.Loading()) }

            val trainerDocRef = firestore.collection("users").document(trainerUserId)
            val traineeRequestDocRef = trainerDocRef.collection("traineeRequests").document(traineeUserId)

            Log.d("TrainersViewModel", "Checking if trainee request document exists...")
            traineeRequestDocRef.get().addOnSuccessListener { traineeRequestSnapshot ->
                if (traineeRequestSnapshot.exists()) {
                    Log.d("TrainersViewModel", "Trainee request document exists. Proceeding with batch operation...")

                    // Fetch trainee and trainer objects from Firestore
                    firestore.collection("users").document(traineeUserId).get().addOnSuccessListener { traineeSnapshot ->
                        val trainee = traineeSnapshot.toObject(UserModel::class.java)
                        if (trainee != null) {
                            firestore.collection("users").document(trainerUserId).get().addOnSuccessListener { trainerSnapshot ->
                                val trainer = trainerSnapshot.toObject(UserModel::class.java)
                                if (trainer != null) {
                                    firestore.runBatch { batch ->
                                        // Add Accepted trainee into user Trainees Sub-collection
                                        val trainerTraineesRef = firestore.collection("users").document(trainerUserId)
                                            .collection("Trainees")
                                        batch.set(trainerTraineesRef.document(traineeUserId), trainee)

                                        // Add Current Trainer into user's Trainers Sub-collection
                                        val traineeTrainersRef = firestore.collection("users").document(traineeUserId)
                                            .collection("Trainers")
                                        batch.set(traineeTrainersRef.document(trainerUserId), trainer)

                                        // Add the traineeUserId to the userBookedIdsAccepted list in the trainer's document
                                        val trainerDocumentRef = firestore.collection("users").document(trainerUserId)
                                        //fetching the userBookedIdsAccepted list
                                        val userBookedIdsAccepted = trainer.userBookedIdsAccepted.toMutableList()
                                        userBookedIdsAccepted.add(traineeUserId)
                                        batch.update(trainerDocumentRef, "userBookedIdsAccepted", userBookedIdsAccepted)

                                        // delete the specific userTraineeId from userBookedIdsPending
                                        val userBookedIdsPending = trainer.userBookedIdsPending.toMutableList()
                                        userBookedIdsPending.remove(traineeUserId)
                                        batch.update(trainerDocumentRef, "userBookedIdsPending", userBookedIdsPending)

                                        // Delete the trainee from the traineeRequests Sub-collection
                                        batch.delete(traineeRequestDocRef)
                                    }.addOnSuccessListener {
                                        Log.d("TrainersViewModel", "Batch operation completed successfully.")
                                        viewModelScope.launchSafe {
                                            acceptedTraineeRequest.postValue(
                                                CommonActivity.NetworkResult.Success(
                                                    UserModel()
                                                )
                                            )
                                        }
                                    }.addOnFailureListener { exception ->
                                        Log.e("TrainersViewModel", "Error executing batch operation: ${exception.message}", exception)
                                        viewModelScope.launchSafe {
                                            acceptedTraineeRequest.postValue(
                                                CommonActivity.NetworkResult.Error(
                                                    exception.message
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Log.e("TrainersViewModel", "Trainer document does not exist.")
                                    viewModelScope.launchSafe {
                                        acceptedTraineeRequest.postValue(
                                            CommonActivity.NetworkResult.Error(
                                                "Trainer document does not exist."
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            Log.e("TrainersViewModel", "Trainee document does not exist.")
                            viewModelScope.launchSafe {
                                acceptedTraineeRequest.postValue(
                                    CommonActivity.NetworkResult.Error(
                                        "Trainee document does not exist."
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Log.e("TrainersViewModel", "Trainee request document does not exist.")
                    viewModelScope.launchSafe {
                        acceptedTraineeRequest.postValue(
                            CommonActivity.NetworkResult.Error(
                                "Trainee request document does not exist."
                            )
                        )
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("TrainersViewModel", "Error fetching trainee request document: ${exception.message}", exception)
                viewModelScope.launchSafe {
                    acceptedTraineeRequest.postValue(
                        CommonActivity.NetworkResult.Error(
                            exception.message
                        )
                    )
                }
            }
        }

    fun rejectTraineeRequest(traineeUserId: String, trainerUserId: String) {
        viewModelScope.launchSafe { _rejectedTraineeRequest.postValue(CommonActivity.NetworkResult.Loading()) }

        val trainerDocRef = firestore.collection("users").document(trainerUserId)
        val traineeRequestDocRef = trainerDocRef.collection("traineeRequests").document(traineeUserId)

        Log.d("TrainersViewModel", "Checking if trainee request document exists...")
        traineeRequestDocRef.get().addOnSuccessListener { traineeRequestSnapshot ->
            if (traineeRequestSnapshot.exists()) {
                Log.d("TrainersViewModel", "Trainee request document exists. Proceeding with batch operation...")

                firestore.runBatch { batch ->
                    // Delete the trainee request document from the traineeRequests sub-collection
                    batch.delete(traineeRequestDocRef)

                    // Remove the traineeUserId from the userBookedIdsPending list in the trainer's document
                    val trainerDocumentRef = firestore.collection("users").document(trainerUserId)
                    batch.update(trainerDocumentRef, "userBookedIdsPending", FieldValue.arrayRemove(traineeUserId))
                }.addOnSuccessListener {
                    Log.d("TrainersViewModel", "Batch operation completed successfully.")
                    viewModelScope.launchSafe {
                        _rejectedTraineeRequest.postValue(
                            CommonActivity.NetworkResult.Success(
                                UserModel()
                            )
                        )
                    }
                }.addOnFailureListener { exception ->
                    Log.e("TrainersViewModel", "Error executing batch operation: ${exception.message}", exception)
                    viewModelScope.launchSafe {
                        _rejectedTraineeRequest.postValue(
                            CommonActivity.NetworkResult.Error(
                                exception.message
                            )
                        )
                    }
                }
            } else {
                Log.e("TrainersViewModel", "Trainee request document does not exist.")
                viewModelScope.launchSafe {
                   _rejectedTraineeRequest.postValue(
                        CommonActivity.NetworkResult.Error(
                            "Trainee request document does not exist."
                        )
                    )
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("TrainersViewModel", "Error fetching trainee request document: ${exception.message}", exception)
            viewModelScope.launchSafe {
                _rejectedTraineeRequest.postValue(
                    CommonActivity.NetworkResult.Error(
                        exception.message
                    )
                )
            }
        }
    }


}

