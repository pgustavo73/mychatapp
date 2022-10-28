package com.example.mychatapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContentProviderCompat
import com.example.mychatapp.adapters.UsersAdapter
import com.example.mychatapp.databinding.ActivityUserBinding
import com.example.mychatapp.listeners.UserListener
import com.example.mychatapp.models.User
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.String.format


class UserActivity : BaseActivity(), UserListener {

    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        getUser()
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {v -> onBackPressed()}
    }

    private fun getUser() {
        loading(true)
        val sharedPreference = SharedPreference(this)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                val currentUserID: String? =
                    sharedPreference.getString(Constants.KEY_USER_ID)
                if (task.isSuccessful && task.result != null) {
                    val users: MutableList<User> = arrayListOf()
                    for (queryDocumentSnapshot in task.result!!) {
                        if (currentUserID.equals(queryDocumentSnapshot.id)) {
                            continue
                        }
                        val user = User()
                        user.name =
                            queryDocumentSnapshot.getString(Constants.KEY_NAME).toString()
                        user.email =
                            queryDocumentSnapshot.getString(Constants.KEY_EMAIL).toString()
                        user.image =
                            queryDocumentSnapshot.getString(Constants.KEY_IMAGE).toString()
                        user.token =
                            queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString()
                        user.id = queryDocumentSnapshot.id
                        users += user
                    }

                    if (users.isNotEmpty()) {
                        val usersAdapter = UsersAdapter(users, this)
                        binding.usersRecyclerView.adapter = usersAdapter
                        binding.usersRecyclerView.visibility = View.VISIBLE
                    } else {
                        showErrorMessage()
                    }
                } else {
                    showErrorMessage()
                }
            }
    }

    private fun showErrorMessage() {
        binding.textErrorMessage.text = format("%s", "No user available")
        binding.textErrorMessage.visibility = View.VISIBLE
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onUserClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
        finish()
    }

}