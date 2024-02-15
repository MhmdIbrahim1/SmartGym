package com.example.smartgymapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel(
    var userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val userType: String,
    val profile_picture: String ="",
    var userBookedIdsAccepted: List<String> = emptyList(),
    var userBookedIdsPending: List<String> = emptyList(),
    var userBookedIdsRejected: List<String> = emptyList(),

    val fcmToken: String = ""

    ): Parcelable{
    constructor() : this("", "", "", "", "", "")
}

enum class BookingStatus {
    NOT_BOOKED,
    BOOKED,
    ACCEPTED,
    REQUESTED,
}




