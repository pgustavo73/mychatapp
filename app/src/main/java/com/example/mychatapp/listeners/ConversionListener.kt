package com.example.mychatapp.listeners

import com.example.mychatapp.models.User

interface ConversionListener {
    fun onConversionClicked(user: User)
}