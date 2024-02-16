package com.example.smartgymapp.ui.trainee.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.BookingStatus
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainersViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _getAllTrainers =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getAllTrainers = _getAllTrainers.asStateFlow()


    private val _sendTraineeToTrainerRequests =
        MutableLiveData<CommonActivity.NetworkResult<UserModel>>()
    val sendTraineeToTrainerRequests: LiveData<CommonActivity.NetworkResult<UserModel>>
        get() = _sendTraineeToTrainerRequests


    init {
        getAllTrainers()
    }

    private fun getAllTrainers() {
        _getAllTrainers.value = CommonActivity.NetworkResult.Loading()
        firestore.collection("users")
            .whereEqualTo("userType", "Trainer")
            .get()
            .addOnSuccessListener { result ->
                viewModelScope.launch {
                    val trainers = result.documents.mapNotNull { document ->
                        val trainer = document.toObject(UserModel::class.java)
                        trainer?.userId = document.id // Set the trainer's ID
                        trainer
                    }
                    _getAllTrainers.emit(CommonActivity.NetworkResult.Success(trainers))
                }
            }
            .addOnFailureListener { exception ->
                viewModelScope.launch {
                    _getAllTrainers.emit(CommonActivity.NetworkResult.Error(exception.message))
                }
            }
    }
    fun sendTraineeToTrainerRequests(trainerUserId: String, traineeUserId: String) {
        // Get the trainee's details from Firestore using traineeUserId
        firestore.collection("users").document(traineeUserId).get()
            .addOnSuccessListener { traineeDocumentSnapshot ->
                val trainee = traineeDocumentSnapshot.toObject(UserModel::class.java)

                // Now, add the trainee to the trainer's traineeRequests sub-collection
                val trainerRequestsRef = firestore.collection("users").document(trainerUserId)
                    .collection("traineeRequests")

                // Create a batched write operation
                val batch = firestore.batch()

                trainee?.let { traineeModel ->

                    // Set trainee request in the trainer's sub-collection
                    val traineeRequestDoc = trainerRequestsRef.document(traineeModel.userId)
                    batch.set(traineeRequestDoc, traineeModel)

                    val trainerDocumentRef = firestore.collection("users").document(trainerUserId)

                    // Update userBookedIdsPending directly in the batch
                    batch.update(trainerDocumentRef, "userBookedIdsPending", FieldValue.arrayUnion(traineeUserId))

                    // Commit in real-time
                    batch.commit()
                        .addOnSuccessListener {
                            _sendTraineeToTrainerRequests.postValue(CommonActivity.NetworkResult.Success(traineeModel))
                        }
                        .addOnFailureListener { exception ->
                            _sendTraineeToTrainerRequests.postValue(CommonActivity.NetworkResult.Error(exception.message))
                        }

                    // Listen for changes in the trainer's document
                    trainerDocumentRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            // Handle errors
                            Log.e("TrainersViewModel", "Error listening to trainer document", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val updatedTrainer = snapshot.toObject(UserModel::class.java)
                            if (updatedTrainer != null) {
                                _sendTraineeToTrainerRequests.postValue(CommonActivity.NetworkResult.Success(updatedTrainer))
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                _sendTraineeToTrainerRequests.postValue(CommonActivity.NetworkResult.Error(exception.message))
            }
    }



}



