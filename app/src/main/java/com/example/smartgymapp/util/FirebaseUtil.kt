package com.example.smartgymapp.util

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContentProviderCompat.requireContext

class FirebaseUtil {

    fun getChatRoomReference(chatRoomId :String ): DocumentReference {
        return FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomId)
    }
    fun getChatroomMessageReference(chatroomId: String?): CollectionReference {
        return getChatRoomReference(chatroomId!!).collection("chats")
    }


    fun getCharRoomId(userId1: String, userId2: String): String {
        val sortedIds = listOf(userId1.trim(), userId2.trim()).sorted()
        return sortedIds.joinToString("_")
    }
}