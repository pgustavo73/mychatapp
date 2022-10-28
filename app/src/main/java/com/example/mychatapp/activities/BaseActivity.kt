package com.example.mychatapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity : AppCompatActivity() {

    private lateinit var documentReference: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreference = SharedPreference(applicationContext)
        val dataBase = FirebaseFirestore.getInstance()
        documentReference = dataBase.collection(Constants.KEY_COLLECTION_USERS)
            .document(sharedPreference.getString(Constants.KEY_USER_ID)!!)
    }

    override fun onPause() {
        super.onPause()
        documentReference.update(Constants.KEY_AVAILABILITY, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        documentReference.update(Constants.KEY_AVAILABILITY, 0)
    }

    override fun onResume() {
        super.onResume()
        documentReference.update(Constants.KEY_AVAILABILITY, 1)
    }

}
