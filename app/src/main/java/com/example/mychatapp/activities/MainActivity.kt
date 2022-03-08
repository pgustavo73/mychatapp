package com.example.mychatapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import com.example.mychatapp.adapters.RecentConversationsAdapter
import com.example.mychatapp.databinding.ActivityMainBinding
import com.example.mychatapp.listeners.ConversionListener
import com.example.mychatapp.models.ChatMessage
import com.example.mychatapp.models.User
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class MainActivity : BaseActivity(), ConversionListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversations: MutableList<ChatMessage>
    private lateinit var conversationsAdapter: RecentConversationsAdapter
    private lateinit var dataBase: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        loadings()
        getToken()
        setListeners()
        listenConversations()
    }

    private fun init() {
        conversations = ArrayList()
        conversationsAdapter = RecentConversationsAdapter(conversations, this)
        binding.conversationsRecyclerView.adapter = conversationsAdapter
        dataBase = FirebaseFirestore.getInstance()
    }

    private fun setListeners() {
        binding.imageSignOut.setOnClickListener {v -> signOut()}
        binding.fabNewChat.setOnClickListener {v ->
            startActivity(Intent(applicationContext, UserActivity::class.java))}
    }

    private fun loadings() {
        val sharedPreference = SharedPreference(this)
        binding.textName.text = sharedPreference.getString(Constants.KEY_NAME)
        val bytes: ByteArray =
            Base64.decode(sharedPreference.getString(Constants.KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imageProfile.setImageBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun listenConversations() {
        val sharedPreference = SharedPreference(this)
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, sharedPreference.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, sharedPreference.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener = EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
        if (error != null) {
            return@EventListener
        }
        if(value != null) {
            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                    val receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                    val chatMessage = ChatMessage()
                    chatMessage.senderId = senderId!!
                    chatMessage.receiverId = receiverId!!
                    val sharedPreference = SharedPreference(this)
                    if (sharedPreference.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE)!!
                        chatMessage.conversionName = documentChange.document.getString(Constants.KEY_RECEIVER_NAME)!!
                        chatMessage.conversionId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)!!
                    }else {
                        chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_SENDER_IMAGE)!!
                        chatMessage.conversionName = documentChange.document.getString(Constants.KEY_SENDER_NAME)!!
                        chatMessage.conversionId = documentChange.document.getString(Constants.KEY_SENDER_ID)!!
                    }
                    chatMessage.message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                    chatMessage.dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                    conversations.add(chatMessage)
                }else if(documentChange.type == DocumentChange.Type.MODIFIED) {
                    for (i in 0 until  conversations.size ) {
                        val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)!!
                        val receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)!!
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                            conversations.get(i).dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                            break
                        }
                    }
                }
            }
            conversations.sortWith(Comparator { obj1, obj2 -> obj2.dateObject.compareTo(obj1.dateObject)})
            conversationsAdapter.notifyDataSetChanged()
            binding.conversationsRecyclerView.smoothScrollToPosition(0)
            binding.conversationsRecyclerView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::updateToken)
    }

    private fun updateToken(token: String) {
        val sharedPreference = SharedPreference(this)
        sharedPreference.putString(Constants.KEY_FCM_TOKEN, token)
        val database = FirebaseFirestore.getInstance()
        val documentReference: DocumentReference =
            database.collection(Constants.KEY_COLLECTION_USERS).document(
            sharedPreference.getString(Constants.KEY_USER_ID)!!
        )
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnFailureListener { e -> showToast("Unable to update token") }
    }

    private fun signOut() {
        showToast("Signing out...")
        val sharedPreference = SharedPreference(this)
        val database = FirebaseFirestore.getInstance()
        val documentReference: DocumentReference =
            database.collection(Constants.KEY_COLLECTION_USERS).document(
                sharedPreference.getString(Constants.KEY_USER_ID)!!
            )
        val updates = HashMap<String, Any>()
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete())
        documentReference.update(updates)
            .addOnSuccessListener {unused ->
                sharedPreference.clear()
                startActivity(Intent(applicationContext, SignInActivity::class.java))
                finish()
            }
            .addOnFailureListener { e -> showToast("Unable to sign out") }
    }

    override fun onConversionClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
    }


}