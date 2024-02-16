package com.example.smartgymapp.ui.dochat

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
import com.example.smartgymapp.util.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays

@AndroidEntryPoint
class DoChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoChatBinding
    private lateinit var chatRoomId: String
    private lateinit var otherUser: UserModel
    private lateinit var chatroomModel: ChatroomModel
    private lateinit var chatAdapter: ChatRecyclerAdapter
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

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.messageSendBtn.setOnClickListener {
            val message = binding.chatMessageInput.text.toString()
            if (message.isEmpty()) {
                return@setOnClickListener
            } else {
                sendMessageToUser(message);
                //sendNotificationToUser(message)
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

//    private fun sendNotificationToUser(message: String) {
//        FirebaseFirestore.getInstance().collection("users")
//            .document(FirebaseAuth.getInstance().currentUser!!.uid)
//            .get().addOnCompleteListener {
//                if (it.isSuccessful){
//                    val userModel = it.result?.toObject(UserModel::class.java)
//                    try {
//                        val jsonObject = JSONObject()
//                        val notificationObject = JSONObject()
//                        notificationObject.put("title", "${userModel?.firstName} ${userModel?.lastName}")
//                        notificationObject.put("body", message)
//
//                        val dataObject = JSONObject()
//                        dataObject.put("userId", userModel?.userId)
//
//                        jsonObject.put("notification", notificationObject)
//                        jsonObject.put("data", dataObject)
//                        jsonObject.put("to", otherUser.fcmToken)
//
//                        callApi(jsonObject)
//
//                    }catch (e: Exception){
//                        e.printStackTrace()
//                    }
//                }
//            }
//    }

//    fun callApi(jsonObject: JSONObject){
//        val json  = "application/json; charset=utf-8".toMediaType()
//        val client = okhttp3.OkHttpClient()
//        val url = "https://fcm.googleapis.com/fcm/send"
//        val body = RequestBody.create(json, jsonObject.toString())
//        val request = okhttp3.Request.Builder()
//            .url(url)
//            .post(body)
//            .header("Authorization", "Bearer AAAA9ko5xrQ:APA91bGj2TeIUwq4v9jloJ2sOxwBBfIdI-WWduF7lWBxnYvrf7dsuZXcDBdXt1nGHwGeIBq9yGDk4hnIHZEa0q78KGGnxi6qQv7IpwovRR6PyUDSAcMYFdxrF1S-uqwqUiDfapHZQzj7")
//            .build()
//        client.newCall(request).execute()
//
//    }


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

