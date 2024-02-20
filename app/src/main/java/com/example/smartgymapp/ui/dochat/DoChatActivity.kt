package com.example.smartgymapp.ui.dochat

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.Glide
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ActivityDoChatBinding
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.callApi
import com.example.smartgymapp.util.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Arrays

private const val NOTIFICATION_CHANNEL_ID = "com.example.smartgymapp.ui.dochat"
@AndroidEntryPoint
class DoChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoChatBinding
    private lateinit var chatRoomId: String
    private lateinit var otherUser: UserModel
    private lateinit var chatroomModel: ChatroomModel
    private lateinit var chatAdapter: ChatRecyclerAdapter

    private val batchSize = 10
    private var lastVisibleMessage: DocumentSnapshot? = null
    private var isLoading = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDoChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otherUser = CommonActivity.getUserModelFromIntent(intent)
        chatRoomId = FirebaseUtil().getCharRoomId(
            FirebaseAuth.getInstance().currentUser!!.uid,
            otherUser.userId
        )


        chatRoomId = intent.getStringExtra("chatRoomId") ?: FirebaseUtil().getCharRoomId(
            FirebaseAuth.getInstance().currentUser!!.uid,
            otherUser.userId
        )

        // log the getIntent  data
        Log.d("DoChatActivity", "All data from intent: ${intent.extras}")


        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.messageSendBtn.setOnClickListener {
            val message = binding.chatMessageInput.text.toString()
            if (message.isEmpty()) {
                return@setOnClickListener
            } else {
                sendMessageToUser(message);
                sendNotificationToUser(message)
            }
        }



        binding.apply {
            otherUserName.text = "${otherUser.firstName} ${otherUser.lastName}"
            otherUser.profile_picture.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    Glide.with(this@DoChatActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.man_user)
                        .centerCrop()
                        .into(profilePicImageView)
                } else {
                    profilePicImageView.setImageResource(R.drawable.man_user)
                }
            }


        }
        getOrCreateChatroomModel()
        setupChatRecyclerView()
    }

    private fun sendNotificationToUser(message: String) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userModel = task.result!!.toObject(UserModel::class.java)
                        if (userModel != null) {
                            try {
                                val notificationObject = JSONObject()
                                notificationObject.put("title", userModel.firstName + " " + userModel.lastName)
                                notificationObject.put("body", message)
                                notificationObject.put("sound", "default")
                                notificationObject.put("icon", R.drawable.icon_send)


                                val dataObject = JSONObject()
                                dataObject.put("message", message)
                                dataObject.put("userId", userModel.userId)

                                val jsonObject = JSONObject()
                                jsonObject.put("notification", notificationObject)
                                jsonObject.put("data", dataObject)
                                jsonObject.put("to", otherUser.fcmToken)

                                Log.d("Notification JSON", jsonObject.toString())

                                // Make API call to FCM
                                lifecycleScope.launchSafe {
                                    withContext(Dispatchers.IO) {
                                        callApi(jsonObject)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Notification", "Error sending notification: ${e.message}")
        }
    }





    private fun sendMessageToUser(message: String) {

        //initialize chatroomModel
        chatroomModel = ChatroomModel(
            chatRoomId,
            listOf(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId),
            Timestamp.now(),
            "",
            "",
        )

        chatroomModel.lastMessageTimestamp = Timestamp.now()
        chatroomModel.lastMessageSenderId = FirebaseAuth.getInstance().currentUser!!.uid
        chatroomModel.lastMessage = message
        FirebaseUtil().getChatRoomReference(chatRoomId).set(chatroomModel)

        val chatMessage = ChatMessageModel(
            message,
            FirebaseAuth.getInstance().currentUser!!.uid,
            Timestamp.now()
        )
        FirebaseUtil().getChatroomMessageReference(chatRoomId).add(chatMessage)
            .addOnSuccessListener {
                binding.chatMessageInput.text?.clear()
            }
    }

    private fun setupChatRecyclerView() {
        val query = FirebaseUtil().getChatroomMessageReference(chatRoomId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<ChatMessageModel>()
            .setQuery(query, ChatMessageModel::class.java)
            .build()

        chatAdapter = ChatRecyclerAdapter(options, applicationContext)
        val manager = LinearLayoutManager(this)
        manager.reverseLayout = true
        binding.chatRecyclerView.layoutManager = manager
        binding.chatRecyclerView.adapter = chatAdapter
        chatAdapter.startListening()
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.chatRecyclerView.smoothScrollToPosition(0)
            }
        })
    }

    private fun getOrCreateChatroomModel() {
        val chatRoomId =
            FirebaseUtil().getCharRoomId(
                FirebaseAuth.getInstance().currentUser!!.uid,
                otherUser.userId
            )
        val chatRoomRef = FirebaseUtil().getChatRoomReference(chatRoomId)
        chatRoomRef.get().addOnSuccessListener {
            if (it.exists()) {
                chatroomModel = it.toObject(ChatroomModel::class.java)!!
            } else {
                chatroomModel = ChatroomModel(
                    chatRoomId,
                    Arrays.asList(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId),
                    Timestamp.now(),
                    ""
                )
                chatRoomRef.set(chatroomModel)
            }
            Log.d(
                "ChatroomModel",
                Arrays.asList(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId)
                    .toString()
            )
        }
    }

}

