package com.example.smartgymapp.ui.trainer

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.validateEmail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.smartgymapp.SmartGymApp

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: StorageReference,
    app: Application
) : AndroidViewModel(app) {

    private val _getUser =
        MutableStateFlow<CommonActivity.NetworkResult<UserModel>>(CommonActivity.NetworkResult.UnSpecified())
    val getUser = _getUser.asStateFlow()

    private val _updateInfo =
        MutableStateFlow<CommonActivity.NetworkResult<UserModel>>(CommonActivity.NetworkResult.UnSpecified())
    val updateInfo = _updateInfo.asStateFlow()

    init {
        getUser()
    }

    private fun getUser() {
        viewModelScope.launch {
            _getUser.emit(CommonActivity.NetworkResult.Loading())
        }
        firestore.collection("users").document(auth.uid!!)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    viewModelScope.launch {
                        _getUser.emit(CommonActivity.NetworkResult.Error(error.message.toString()))
                    }
                } else {
                    val user = value?.toObject(UserModel::class.java)
                    user?.let {
                        viewModelScope.launch {
                            _getUser.emit(CommonActivity.NetworkResult.Success(user))
                        }
                    }
                }
            }
    }

    fun updateUser(user: UserModel, imageUri: Uri?){
        val areInputsValid = validateEmail(user.email) is CommonActivity.RegisterValidation.Success
                && user.firstName.trim().isNotEmpty()
                && user.lastName.trim().isNotEmpty()


        if (!areInputsValid) {
            viewModelScope.launch {
                _updateInfo.emit(CommonActivity.NetworkResult.Error("Invalid inputs"))
            }
            return
        }

        viewModelScope.launch {
            _updateInfo.emit(CommonActivity.NetworkResult.Loading())
        }


        if (imageUri == null) {
            saveUserInformation(user, true)
        } else {
            saveUserInformationWithNewImage(user, imageUri)
        }

    }

    private fun saveUserInformationWithNewImage(user: UserModel, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val imageBitmap = MediaStore.Images.Media.getBitmap(
                  getApplication<SmartGymApp>().contentResolver,
                    imageUri
                )
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 96, byteArrayOutputStream)
                val imageByteArray = byteArrayOutputStream.toByteArray()
                val imageDirectory =
                    storage.child("profileImages/${user.email}/${UUID.randomUUID()}")
                val result = imageDirectory.putBytes(imageByteArray).await()
                val imageUrl = result.storage.downloadUrl.await().toString()
                saveUserInformation(user.copy(profile_picture = imageUrl), false)
            } catch (e: Exception) {
                viewModelScope.launch {
                    _getUser.emit(CommonActivity.NetworkResult.Error(e.message.toString()))
                }
            }
        }
    }

    private fun saveUserInformation(user: UserModel, shouldRetrieveOldImage: Boolean) {
        // Log the start of the function
        Log.d(TAG, "Saving user information: ${user.userId}, $user")

        // Update user information in Firestore
        firestore.runTransaction { transaction ->
            val docRef = firestore.collection("users").document(auth.uid!!)
            if (shouldRetrieveOldImage) {
                val currentUser = transaction.get(docRef).toObject(UserModel::class.java)
                val newUser = user.copy(profile_picture = currentUser?.profile_picture ?: "")
                transaction.set(docRef, newUser)
            } else {
                transaction.set(docRef, user)
            }
        }.addOnSuccessListener {
            Log.d(TAG, "User information updated successfully: ${user.userId}")
            // Update successful, now update Trainers sub-collection
            updateUserInTrainersCollection(user)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to update user information: ${user.userId}, ${exception.message}")
            viewModelScope.launch {
                _updateInfo.emit(CommonActivity.NetworkResult.Error(exception.message.toString()))
            }
        }
    }

    private fun updateUserInTrainersCollection(user: UserModel) {
        viewModelScope.launch {
            try {
                // Log the start of the function
                Log.d(TAG, "Updating user in Trainers sub-collection: ${user.userId}, $user")
                // Retrieve the list of userBookedIdsAccepted for the current user
                val currentUserBookedIdsAccepted = user.userBookedIdsAccepted
                // Update the Trainers sub-collection for each user in the userBookedIdsAccepted list
                currentUserBookedIdsAccepted.forEach { userId ->
                    val trainersCollectionRef = firestore.collection("users").document(userId)
                        .collection("BookedChat").document(auth.uid!!)
                    trainersCollectionRef.set(user)
                        .addOnSuccessListener {
                            // No action needed
                        }
                        .addOnFailureListener { exception ->
                            // No action needed
                        }
                }
                // Emit success for the user update process
                _updateInfo.emit(CommonActivity.NetworkResult.Success(user))
            } catch (e: Exception) {
                // Handle exception if needed
                _updateInfo.emit(CommonActivity.NetworkResult.Error(e.message.toString()))
            }
        }
    }

    companion object {
        private const val TAG = "TrainerProfileFragment"
    }


    fun clearUpdateInfo() {
        _updateInfo.value = CommonActivity.NetworkResult.UnSpecified()
    }
}