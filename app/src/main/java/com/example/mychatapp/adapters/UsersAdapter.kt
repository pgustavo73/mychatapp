package com.example.mychatapp.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mychatapp.databinding.ItemContainerUserBinding
import com.example.mychatapp.listeners.UserListener
import com.example.mychatapp.models.User


class UsersAdapter(var users: MutableList<User>, var userListener: UserListener) :
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {


    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun setUserData(user: User) {
            user.name.also { binding.textName.text = it }
            binding.textEmail.text = user.email
            binding.imageProfile.setImageBitmap(getUserBitmap(user.image))
            binding.root.setOnClickListener { v ->  userListener.onUserClicked(user)}
        }

        fun getUserBitmap(encodedImage: String): Bitmap? {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemContainerUserBinding = ItemContainerUserBinding.inflate(inflater, parent,
            false)
        this.users = users
        this.userListener = userListener
        return UserViewHolder(itemContainerUserBinding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
       holder.setUserData(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

}

