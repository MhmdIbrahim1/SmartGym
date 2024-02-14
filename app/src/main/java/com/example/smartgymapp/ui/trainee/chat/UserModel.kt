package com.example.smartgymapp.ui.trainee.chat

data class UserModel(
    val email: String,
    val firstName: String,
    val lastName: String,
    val userId: String,
    val userType: String,
    val profile_picture: String,

    ) {
    constructor() : this("", "", "", "", "", "")
}
