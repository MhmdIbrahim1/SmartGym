package com.example.smartgymapp.ui.trainee.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DoctorsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
): ViewModel(){
    private val _getAllDoctors =
        MutableStateFlow<CommonActivity.NetworkResult<List<UserModel>>>(CommonActivity.NetworkResult.UnSpecified())
    val getAllDoctors = _getAllDoctors.asStateFlow()

    private val _sendTraineeToTrainerRequests =
        MutableLiveData<CommonActivity.NetworkResult<UserModel>>()
    val sendTraineeToTrainerRequests: LiveData<CommonActivity.NetworkResult<UserModel>>
        get() = _sendTraineeToTrainerRequests
    init {
        getAllDoctors()
    }

    private fun getAllDoctors() {
        _getAllDoctors.value = CommonActivity.NetworkResult.Loading()
        firestore.collection("users")
            .whereEqualTo("userType", "Doctor")
            .get()
            .addOnSuccessListener { result ->
                val doctors = result.toObjects(UserModel::class.java)
                _getAllDoctors.value = CommonActivity.NetworkResult.Success(doctors)
            }
            .addOnFailureListener { exception ->
                _getAllDoctors.value = CommonActivity.NetworkResult.Error(exception.message)
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