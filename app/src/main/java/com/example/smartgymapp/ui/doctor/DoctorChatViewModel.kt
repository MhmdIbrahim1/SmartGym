package com.example.smartgymapp.ui.doctor

import androidx.lifecycle.ViewModel
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        getTraineesFromTraineeCollection()
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
}