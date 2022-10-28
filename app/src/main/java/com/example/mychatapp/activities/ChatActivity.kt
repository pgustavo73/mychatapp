package com.example.mychatapp.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.example.mychatapp.R
import com.example.mychatapp.adapters.ChatAdapter
import com.example.mychatapp.databinding.ActivityChatBinding
import com.example.mychatapp.databinding.EditDialogBinding
import com.example.mychatapp.databinding.SheetOneBinding
import com.example.mychatapp.models.ChatMessage
import com.example.mychatapp.models.User
import com.example.mychatapp.network.ApiClient.client
import com.example.mychatapp.network.ApiService
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.example.mychatapp.utilities.getBitmapFromEncodedString
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var receiverUser: User
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dataBase: FirebaseFirestore
    private var conversationId: String? = null
    private var isReceiverAvailable: Boolean = false
    var imageUri: Uri? = null
    lateinit var progressDialog: ProgressDialog
    private var images: ArrayList<Uri?>? = null
    private var encodedImage: String? = null
    lateinit var fileName: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        images = ArrayList()
        loadReceiverDetails()
        setListener()
        init()
        listenMessages()
    }

    private fun init() {
        val sharedPreference = SharedPreference(this)
        chatMessages = ArrayList()
        chatAdapter = ChatAdapter(
            chatMessages,
            sharedPreference.getString(Constants.KEY_USER_ID)!!,
            getBitmapFromEncodedString(receiverUser.image)!!,
        )
        binding.chatRecyclerView.setAdapter(chatAdapter)
        dataBase = FirebaseFirestore.getInstance()
    }

    private fun sendMessage() {
        val sharedPreference = SharedPreference(this)
        val message = HashMap<String, Any>()
        message[Constants.KEY_SENDER_ID] = sharedPreference.getString(Constants.KEY_USER_ID)!!
        message[Constants.KEY_RECEIVER_ID] = receiverUser.id
        if (encodedImage != null) {
            message[Constants.KEY_IMAGE_MESSAGE] = encodedImage!!
            message[Constants.KEY_IMAGE_NAME] = fileName
        }
        message[Constants.KEY_MESSAGE] = binding.inputMessage.text.toString()
        message[Constants.KEY_TIMESTAMP] = Date()
        dataBase.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        if (conversationId != null) {
            if (encodedImage != null) {
                updateConversion("\uD83D\uDCF8 Photo")
            } else {
                updateConversion(binding.inputMessage.text.toString())
            }
        } else {
            val conversion = HashMap<String, Any>()
            conversion.put(
                Constants.KEY_SENDER_ID,
                sharedPreference.getString(Constants.KEY_USER_ID)!!
            )
            conversion.put(
                Constants.KEY_SENDER_NAME,
                sharedPreference.getString(Constants.KEY_NAME)!!
            )
            conversion.put(
                Constants.KEY_SENDER_IMAGE,
                sharedPreference.getString(Constants.KEY_IMAGE)!!
            )
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id)
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name)
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image)
            if (encodedImage != null) {
                conversion.put(Constants.KEY_LAST_MESSAGE, "\uD83D\uDCF8 Photo")
            } else {
                conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.text.toString())
            }
            conversion.put(Constants.KEY_TIMESTAMP, Date())
            addConversion(conversion)
        }
        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(receiverUser.token)

                val data = JSONObject()
                data.put(Constants.KEY_USER_ID, sharedPreference.getString(Constants.KEY_USER_ID))
                data.put(Constants.KEY_NAME, sharedPreference.getString(Constants.KEY_NAME))
                data.put(
                    Constants.KEY_FCM_TOKEN,
                    sharedPreference.getString(Constants.KEY_FCM_TOKEN)
                )
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.text.toString())

                val body = JSONObject()
                body.put(Constants.REMOTE_MSG_DATA, data)
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)

                sendNotification(body.toString())
            } catch (exception: Exception) {
                showToast(exception.message!!)
            }
        }
        binding.inputMessage.text = null
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendNotification(messageBody: String) {
        client!!.create(ApiService::class.java).sendMessage(
            Constants.getRemoteMsgHeaders(),
            messageBody
        )!!.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (response.isSuccessful) {
                    try {
                        if (response.body() != null) {
                            val responseJson = JSONObject(response.body())
                            val results = responseJson.getJSONArray("results")
                            if (responseJson.getInt("failures") == 1) {
                                val error: JSONObject = results[0] as JSONObject
                                showToast(error.getString("error"))
                                return
                            }

                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    showToast("Error: " + response.code())
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                showToast(t.message!!)
            }
        })
    }

    private fun listenAvailabilityOfReceiver() {
        dataBase.collection(Constants.KEY_COLLECTION_USERS).document(
            receiverUser.id
        ).addSnapshotListener(this@ChatActivity) { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    val availability: Int = Objects.requireNonNull(
                        value.getLong(Constants.KEY_AVAILABILITY)!!
                    ).toInt()
                    isReceiverAvailable = availability == 1
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN).toString()
                if (receiverUser.image == null) {
                    receiverUser.image = value.getString(Constants.KEY_IMAGE)!!
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image)!!)
                    chatAdapter.notifyItemRangeChanged(0, chatMessages.size)
                }
            }
            if (isReceiverAvailable) {
                binding.textAvailability.visibility = View.VISIBLE
            } else {
                binding.textAvailability.visibility = View.GONE
            }
        }
    }

    private fun listenMessages() {
        val sharedPreference = SharedPreference(this)
        dataBase.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(
                Constants.KEY_SENDER_ID,
                sharedPreference.getString(Constants.KEY_USER_ID)
            )
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
            .addSnapshotListener(eventListener)
        dataBase.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
            .whereEqualTo(
                Constants.KEY_RECEIVER_ID,
                sharedPreference.getString(Constants.KEY_USER_ID)
            )
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener =
        EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
            if (error != null) {
                return@EventListener
            }
            if (value != null) {
                val count: Int = chatMessages.size
                for (documentChange in value.documentChanges) {
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val chatMessage = ChatMessage()
                        chatMessage.senderId = documentChange.document
                            .getString(Constants.KEY_SENDER_ID)!!
                        chatMessage.receiverId = documentChange.document
                            .getString(Constants.KEY_RECEIVER_ID)!!
                        chatMessage.message = documentChange.document
                            .getString(Constants.KEY_MESSAGE)!!
                        if (chatMessage.message == "") {
                            chatMessage.encodedImage = documentChange.document
                                .getString(Constants.KEY_IMAGE_MESSAGE)!!
                            chatMessage.imageId = documentChange.document
                                .getString(Constants.KEY_IMAGE_NAME)!!
                        }
                        chatMessage.dateTime =
                            getReadableDateTime(documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!)
                        chatMessage.realDate =
                            documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                        chatMessage.dateObject = documentChange.document
                            .getDate(Constants.KEY_TIMESTAMP)!!
                        chatMessages.add(chatMessage)
                    }
                }
                Collections.sort(chatMessages) { obj1, obj2 -> obj1.dateObject.compareTo(obj2.dateObject) }
                if (count == 0) {
                    chatAdapter.notifyDataSetChanged()
                } else {
                    chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                    binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
                }
                binding.chatRecyclerView.visibility = View.VISIBLE
            }
            binding.progressBar.visibility = View.GONE
            if (conversationId == null) {
                checkForConversion()
            }
        }

    private fun loadReceiverDetails() {
        receiverUser = (intent.getSerializableExtra(Constants.KEY_USER) as User)
        binding.textName.text = receiverUser.name
    }

    private fun setListener() {
        binding.imageBack.setOnClickListener { v -> onBackPressed() }
        binding.layoutSend.setOnClickListener { v ->
            sendMessage()
            binding.inputMessage.onEditorAction(EditorInfo.IME_ACTION_DONE)
        }
        binding.layoutAttached.setOnClickListener { v -> pickImage() }
    }

    private fun getReadableDateTime(date: Date): String {
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .add(conversion)
            .addOnSuccessListener { documentReference -> conversationId = documentReference.id }
    }

    private fun updateConversion(message: String) {
        dataBase = FirebaseFirestore.getInstance()
        val documentReference: DocumentReference =
            dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId!!)
        documentReference.update(
            Constants.KEY_LAST_MESSAGE, message,
            Constants.KEY_TIMESTAMP, Date()
        )
    }

    private fun checkForConversion() {
        val sharedPreference = SharedPreference(this)
        if (chatMessages.size != 0) {
            checkForConversationRemotely(
                sharedPreference.getString(Constants.KEY_USER_ID)!!,
                receiverUser.id
            )
            checkForConversationRemotely(
                receiverUser.id,
                sharedPreference.getString(Constants.KEY_USER_ID)!!
            )
        }
    }

    private fun checkForConversationRemotely(senderId: String, receiverId: String) {
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener: OnCompleteListener<QuerySnapshot> =
        OnCompleteListener<QuerySnapshot> { task ->
            if (task.isSuccessful && task.result != null && task.result?.documents?.size!! > 0) {
                val documentSnapshot: DocumentSnapshot = task.result!!.documents[0]
                conversationId = documentSnapshot.id
            }
        }

    private fun pickImage() {
        val intent = Intent()
        intent.type = ("image/*")
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image"), 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && data != null && data.getData() != null) {
            imageUri = data.data
            val imageStream = contentResolver.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(imageStream)
            encodedImage = encodeImage(bitmap)!!
            uploadImage()
            sendMessage()
        }
    }

    private fun uploadImage() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Sending Image....")
        progressDialog.show()

        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
        val now = Date()
        fileName = formatter.format(now)
        val storageReference = FirebaseStorage.getInstance().getReference("images/$fileName.jpg")
        storageReference.putFile(imageUri!!)
            .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot?> {
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot?) {
                    showToast("Successfully Sent")
                    if (progressDialog.isShowing) progressDialog.dismiss()
                }
            }).addOnFailureListener(object : OnFailureListener {
                override fun onFailure(p0: java.lang.Exception) {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    showToast("Failed to Send")
                }
            })
    }

    private fun encodeImage(bitmap: Bitmap): String? {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }

    override fun onStart() {
        super.onStart()
        this.parentActivityIntent
    }

    fun showDialogOne(context: Context, document: ChatMessage) {

        val bottomSheet = BottomSheetDialog(context, R.style.BottomSheetDialog)
        val bottomSheetBinding: SheetOneBinding = SheetOneBinding.inflate(LayoutInflater.from(context))
        bottomSheet.setContentView(bottomSheetBinding.root)
        bottomSheet.show()

        bottomSheetBinding.rlEdit.setOnClickListener {
            showDialogEdit(context, document)
            bottomSheet.dismiss()
        }
        bottomSheetBinding.rlDelete.setOnClickListener {
            dataBase = FirebaseFirestore.getInstance()
            newUpdateConversion(
                "\uD83D\uDEABYou deleted your last message", document.message)
            val receiverId = document.receiverId
            val chatRef = dataBase.collection(Constants.KEY_COLLECTION_CHAT)
            val queryChat = chatRef.whereEqualTo("timestamp", document.realDate)
            queryChat.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        document.reference.delete()
                            .addOnSuccessListener(object : OnSuccessListener<Void?> {
                                override fun onSuccess(aVoid: Void?) {
                                    Toast.makeText(context,"Message was Delete", Toast.LENGTH_SHORT)
                                        .show()
                                    refreshActivity(context, receiverId)
                                }
                            }).addOnFailureListener(object : OnFailureListener {
                                override fun onFailure(e: Exception) {
                                    Log.e("teste", "Error deleting document")
                                }
                            })
                    }
                }
            }
            bottomSheet.dismiss()
        }
    }

    private fun showDialogEdit(context: Context, editMessage: ChatMessage) {
        val build = AlertDialog.Builder(context,R.style.DialogStyle)
        val dialogBinding: EditDialogBinding = EditDialogBinding
            .inflate(LayoutInflater.from(context))

        build.setView(dialogBinding.root)
        val dialog = build.create()
        dialog.show()

        dialogBinding.editMessage.setText(editMessage.message)
        dialogBinding.btnClose.setOnClickListener{dialog.dismiss()}
        dialogBinding.layoutEditMessage.setOnClickListener{
            val receiverId = editMessage.receiverId
            val msg = dialogBinding.editMessage.text.toString()
            newUpdateConversion(msg, editMessage.message)
            dataBase = FirebaseFirestore.getInstance()
            val chatRRef = dataBase.collection(Constants.KEY_COLLECTION_CHAT)
            val queryChat = chatRRef.whereEqualTo("timestamp", editMessage.realDate)
            queryChat.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        document.reference.update(
                            Constants.KEY_MESSAGE, msg,
                            Constants.KEY_TIMESTAMP, Date()
                        )
                        refreshActivity(context, receiverId)
                    }
                }
            }
            dialog.dismiss()
        }
    }

    private fun newUpdateConversion(message: String, lastMessage: String) {
        dataBase = FirebaseFirestore.getInstance()
        val convRef = dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
        val queryConv = convRef.whereEqualTo("lastMessage", lastMessage)
        queryConv.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result!!) {
                    document.reference.update(
                        Constants.KEY_LAST_MESSAGE, message,
                        Constants.KEY_TIMESTAMP, Date()
                    )
                }
            }
        }
    }

    fun refreshActivity(context: Context, receiverId: String) {
        val sharedPreference = SharedPreference(context)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .get()
            .addOnCompleteListener { task ->
                val currentUserID: String? =
                    sharedPreference.getString(Constants.KEY_USER_ID)
                if (task.isSuccessful && task.result != null) {
                    for (queryDocumentSnapshot in task.result!!) {
                        if (currentUserID.equals(queryDocumentSnapshot.id)) {
                            continue
                        }
                        val user = User()
                        user.name =
                            queryDocumentSnapshot.getString(Constants.KEY_NAME).toString()
                        user.email =
                            queryDocumentSnapshot.getString(Constants.KEY_EMAIL).toString()
                        user.image =
                            queryDocumentSnapshot.getString(Constants.KEY_IMAGE).toString()
                        user.token =
                            queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString()
                        user.id = queryDocumentSnapshot.id

                        if (user.id == receiverId) {
                            val refresh = Intent(context, ChatActivity::class.java)
                            refresh.putExtra(Constants.KEY_USER, user)
                            refresh.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                            finish()
                            context.startActivity(refresh)
                        }
                    }
                }
            }
    }
}