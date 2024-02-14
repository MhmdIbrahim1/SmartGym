package com.example.smartgymapp.ui.dochat

import com.google.firebase.Timestamp

data class ChatroomModel(
    val chatRoomId: String,
    val userId: List<String>,
    var lastMessageTimestamp: Timestamp,
    var lastMessageSenderId: String,
    var lastMessage: String = ""
){
    constructor() : this("", listOf(), Timestamp.now(), "", "")
}