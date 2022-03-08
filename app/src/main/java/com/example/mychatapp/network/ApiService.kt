package com.example.mychatapp.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import java.util.HashMap

interface ApiService {

    @POST("send")
    fun sendMessage(
        @HeaderMap Headers: HashMap<String, String>?,
        @Body messageBody: String?,
    ): Call<String?>?
}