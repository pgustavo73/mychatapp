package com.example.mychatapp.adapters


import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mychatapp.activities.ChatActivity
import com.example.mychatapp.activities.ImageActivity
import com.example.mychatapp.databinding.ItemContainerReceivedMessageBinding
import com.example.mychatapp.databinding.ItemContainerSentMessageBinding
import com.example.mychatapp.models.ChatMessage
import com.example.mychatapp.models.User
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.getBitmapFromEncodedString


class ChatAdapter(
    var chatMessages: MutableList<ChatMessage>, var senderId: String,
    private var receiverProfileImage: Bitmap,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val VIEW_TYPE_SENT = 1
    val VIEW_TYPE__RECEIVED = 2
    private var isReceiverAvailable: Boolean = false
    lateinit var imageMessages: Bitmap

    fun setReceiverProfileImage(bitmap: Bitmap) {
        receiverProfileImage = bitmap
    }

    fun setImageMessage(bitmap: Bitmap) {
        imageMessages = bitmap
    }

    inner class SentMessageViewHolder(itemContainerSentMessageBinding: ItemContainerSentMessageBinding) :
        RecyclerView.ViewHolder(itemContainerSentMessageBinding.root) {
        val binding  = itemContainerSentMessageBinding

        fun setData(chatMessages: ChatMessage) {
            binding.textMessage.text = chatMessages.message
            binding.container.setOnLongClickListener { ChatMessage ->
                val cActivity = ChatActivity()
                cActivity.showDialogOne(ChatMessage.context, chatMessages)
                return@setOnLongClickListener true
            }
            binding.textDateTime.text = chatMessages.dateTime
            if (chatMessages.message == "") {
                binding.textMessage.visibility = View.GONE
                binding.imageMessage.visibility = View.VISIBLE
                setImageMessage(getBitmapFromEncodedString(chatMessages.encodedImage)!!)
                binding.imageMessage.setImageBitmap(imageMessages)
                binding.imageMessage.setOnClickListener{ ChatMessage ->
                    val intent = Intent(ChatMessage.context, ImageActivity::class.java)
                    intent.putExtra("Image",chatMessages.imageId)
                    ChatMessage.context.startActivity(intent)
                }
            }
        }

    }

    inner class ReceivedMessageViewHolder(itemContainerReceivedMessageBinding: ItemContainerReceivedMessageBinding) :
        RecyclerView.ViewHolder(itemContainerReceivedMessageBinding.root) {
        val binding = itemContainerReceivedMessageBinding

        fun setData(chatMessages: ChatMessage, receiverProfileImage: Bitmap) {

            binding.textMessage.text = chatMessages.message
            binding.textDateTime.text = chatMessages.dateTime
            if(receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage)
            }
            if (chatMessages.message == "") {
                binding.textMessage.visibility = View.GONE
                binding.imageMessageChat.visibility = View.VISIBLE
                setImageMessage(getBitmapFromEncodedString(chatMessages.encodedImage)!!)
                binding.imageMessageChat.setImageBitmap(imageMessages)
                binding.imageMessageChat.setOnClickListener{ ChatMessage ->
                    val intent = Intent(ChatMessage.context, ImageActivity::class.java)
                    intent.putExtra("Image",chatMessages.imageId)
                    ChatMessage.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        this.chatMessages = chatMessages
        this.receiverProfileImage = receiverProfileImage
        this.senderId = senderId
        return if (viewType == VIEW_TYPE_SENT) {
            SentMessageViewHolder(
                ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            return ReceivedMessageViewHolder(
                ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).setData(chatMessages[position])
        } else {
            (holder as ReceivedMessageViewHolder).setData(chatMessages[position],
                receiverProfileImage)
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT
        } else {
            return VIEW_TYPE__RECEIVED
        }
    }
}