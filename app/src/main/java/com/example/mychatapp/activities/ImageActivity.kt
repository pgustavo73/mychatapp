package com.example.mychatapp.activities

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mychatapp.databinding.ActivityImageBinding
import com.example.mychatapp.utilities.getBitmapFromEncodedString

class ImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageBinding
    lateinit var imageMessages: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageSender = intent.getStringExtra("Image")
        setImageMessage(getBitmapFromEncodedString(imageSender!!)!!)
        binding.image.setImageBitmap(imageMessages)
        binding.imageBack.setOnClickListener { v-> onBackPressed()}
    }

    fun setImageMessage(bitmap: Bitmap) {
        imageMessages = bitmap
    }
}