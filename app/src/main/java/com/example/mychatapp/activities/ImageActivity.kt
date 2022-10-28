package com.example.mychatapp.activities

import android.app.DownloadManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.example.mychatapp.databinding.ActivityImageBinding
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class ImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageName = intent.getStringExtra("Image")
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$imageName.jpg")


        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.image.setImageBitmap(bitmap)
        }.addOnFailureListener{
            showToast("There is no image to show")
        }

        binding.imageBack.setOnClickListener { v-> onBackPressed()}
        binding.download.setOnClickListener { v->  storageRef.downloadUrl.addOnSuccessListener {
            download(imageName!!,it.toString())
        } }
    }

    private fun download(fileName: String, uri:String) {
        val request = DownloadManager.Request(Uri.parse(uri))
            .setTitle("$fileName.jpg")
            .setDescription("Downloading....")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$fileName.jpg")
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}