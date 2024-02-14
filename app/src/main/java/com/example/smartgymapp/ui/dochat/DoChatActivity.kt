package com.example.smartgymapp.ui.dochat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ActivityDoChatBinding
import com.example.smartgymapp.mvvm.launchSafe
import com.example.smartgymapp.ui.trainee.chat.UserModel
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
        chatRoomId = FirebaseUtil().getCharRoomId(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId)


        chatRoomId = intent.getStringExtra("chatRoomId") ?:
                FirebaseUtil().getCharRoomId(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.messageSendBtn.setOnClickListener {
            val message = binding.chatMessageInput.text.toString()
            if (message.isEmpty()){
                return@setOnClickListener
            }else{
                sendMessageToUser(message);
            }
        }



        binding.apply {
            otherUserName.text = "${otherUser.firstName} ${otherUser.lastName}"
            lifecycleScope.launchSafe {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    withContext(Dispatchers.IO) {
                        otherUser.profile_picture.let { imageUrl ->
                            if (imageUrl.isNotEmpty()) {
                                profilePicImageView.load(imageUrl) {
                                    placeholder(R.drawable.man_user)
                                    crossfade(true)
                                }
                            } else {
                                profilePicImageView.setImageResource(R.drawable.man_user)
                            }
                        }
                    }
                }
            }
        }
        getOrCreateChatroomModel()
        setupChatRecyclerView()
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

        chatAdapter = ChatRecyclerAdapter(options,applicationContext)
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
        val chatRoomId = FirebaseUtil().getCharRoomId(FirebaseAuth.getInstance().currentUser!!.uid, otherUser.userId)
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
        }
    }

}

