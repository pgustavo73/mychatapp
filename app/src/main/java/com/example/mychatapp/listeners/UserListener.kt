package com.example.mychatapp.listeners

import com.example.mychatapp.models.User

interface UserListener {
    fun onUserClicked(user: User)
}