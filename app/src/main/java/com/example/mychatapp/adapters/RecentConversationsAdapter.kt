package com.example.mychatapp.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mychatapp.databinding.ItemContainerRecentConversionBinding
import com.example.mychatapp.listeners.ConversionListener
import com.example.mychatapp.models.User
import com.example.mychatapp.models.ChatMessage as ChatMessage

class RecentConversationsAdapter(var chatMessages: MutableList<ChatMessage>, var conversionListener: ConversionListener):
    RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder>() {

    inner class ConversionViewHolder(private val binding: ItemContainerRecentConversionBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun setData(chatMessage: ChatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage))
            binding.textName.text = chatMessage.conversionName
            binding.textRecentMessage.text = chatMessage.message
            binding.root.setOnClickListener { v->
                val user = User()
                user.id = chatMessage.conversionId
                user.name = chatMessage.conversionName
                user.image = chatMessage.conversionImage
                conversionListener.onConversionClicked(user)
            }
        }

        fun getConversionImage(encodedImage: String): Bitmap? {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        this.chatMessages = chatMessages
        this.conversionListener = conversionListener
        return ConversionViewHolder(
            ItemContainerRecentConversionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
       holder.setData(chatMessages[position])
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }


}