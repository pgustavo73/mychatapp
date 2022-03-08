package com.example.mychatapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mychatapp.databinding.ActivitySignInBinding
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        val sharedPreference = SharedPreference(this)
        if (sharedPreference.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()}
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListener()
    }

    private fun setListener() {
        binding.textCreateNewAccount.setOnClickListener { v ->
            startActivity(Intent(applicationContext,SignUpActivity::class.java))}
        binding.buttonSignIn.setOnClickListener { v ->
            if (isValidSignInDetails()) {
                signIn()
            }
        }
    }

    private fun signIn() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.text.toString())
            .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.text.toString())
            .get()
            .addOnCompleteListener { task: Task<QuerySnapshot?> ->
              if (task.isSuccessful && task.result != null && task.result!!.documents.size > 0
              ) {val documentSnapshot = task.result!!.documents[0]
                  val sharedPreference = SharedPreference(this)
                  sharedPreference.putBoolean(Constants.KEY_IS_SIGNED_IN,true)
                  sharedPreference.putString(Constants.KEY_USER_ID, documentSnapshot.id)
                  sharedPreference.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME)!!)
                  sharedPreference.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE)!!)
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    loading(false)
                    showToast("Unable to sign in")
                }
            }
    }

        private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE)
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignIn.setVisibility(View.VISIBLE)
        }
    }

    fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun isValidSignInDetails(): Boolean {
        return if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter email")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text).matches()) {
            showToast("Enter valid email")
            false
        } else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            false
        } else {
            true
        }
    }
}