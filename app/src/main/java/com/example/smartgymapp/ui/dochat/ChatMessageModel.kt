package com.example.smartgymapp.ui.dochat

import com.google.firebase.Timestamp

data class ChatMessageModel (
    val message: String,
    val senderId: String,
    val timestamp: Timestamp
){
    constructor() : this("", "", Timestamp.now())
}