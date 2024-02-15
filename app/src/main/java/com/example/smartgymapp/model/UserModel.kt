package com.example.smartgymapp.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel(
    var userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val userType: String,
    val profile_picture: String,
    var bookingStatus: BookingStatus,
    val bookingStatusText: String,
    val traineeRequestsCount: Int,

    val fcmToken: String = ""

    ): Parcelable{
    constructor() : this("", "", "", "", "", "", BookingStatus.NOT_BOOKED, "",0)
}

enum class BookingStatus {
    NOT_BOOKED,
    BOOKED,
    ACCEPTED,
    REJECTED,
    REQUESTED
}




