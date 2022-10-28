package com.example.mychatapp.utilities

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.example.mychatapp.adapters.UsersAdapter
import com.example.mychatapp.models.User
import com.google.firebase.firestore.FirebaseFirestore
import java.security.AccessController.getContext

fun getBitmapFromEncodedString(encodedImage: String): Bitmap? {
    if(encodedImage != null) {
        val bytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else {
        return null
    }
}

