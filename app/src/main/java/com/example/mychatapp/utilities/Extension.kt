package com.example.mychatapp.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

fun getBitmapFromEncodedString(encodedImage: String): Bitmap? {
    if(encodedImage != null) {
        val bytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else {
        return null
    }
}