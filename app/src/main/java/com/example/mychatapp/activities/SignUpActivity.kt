package com.example.mychatapp.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mychatapp.databinding.ActivitySingnUpBinding
import com.example.mychatapp.utilities.Constants
import com.example.mychatapp.utilities.SharedPreference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.lang.Exception
import java.util.HashMap

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingnUpBinding
    private var encodedImage: String = "empty"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingnUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListener()
    }

    private fun setListener() {
        binding.textSignIn.setOnClickListener { v -> onBackPressed() }
        binding.buttonSignUp.setOnClickListener { v ->
            if (isValidSignUpDetails()) {
                    signUp()
                }
        }
        binding.layoutImage.setOnClickListener{v ->
            val intent = Intent( Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun signUp() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        val user = HashMap<String, Any>()
        user[Constants.KEY_NAME] = binding.inputName.text.toString()
        user[Constants.KEY_EMAIL] = binding.inputEmail.text.toString()
        user[Constants.KEY_PASSWORD] = binding.inputPassword.text.toString()
        user[Constants.KEY_IMAGE] = encodedImage
        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener { documentReference ->
                loading(false)
                val sharedPreference = SharedPreference(this)
                sharedPreference.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                sharedPreference.putString(Constants.KEY_USER_ID, documentReference.id)
                sharedPreference.putString(Constants.KEY_NAME, binding.inputName.text.toString())
                sharedPreference.putString(Constants.KEY_IMAGE, encodedImage)
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener { exeption: Exception? ->
                loading(false)
                showToast(exeption?.message.toString())
            }
    }

    private fun encodeImage(bitmap: Bitmap): String? {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private val pickImage: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val imageUri: Uri = result.data!!.data!!
                try {
                    val imageStream =
                        contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(imageStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    binding.textAddImage.setVisibility(View.GONE)
                    encodedImage = encodeImage(bitmap)!!
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isValidSignUpDetails(): Boolean {
        if (encodedImage == "empty") {
            showToast("Select profile image")
            return false
        } else if (binding.inputName.text.toString().trim().isEmpty()) {
            showToast("Enter Name")
            return false
        } else if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter Email")
            return false
        } else binding.inputEmail.text.toString()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text).matches()) {
            showToast("Enter valid Email")
            return false
        } else if (binding.inputPassword.text.trim().isEmpty()) {
            showToast("Enter Password")
            return false
        } else if (binding.inputConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Confirm your password")
            return false
        } else if (!binding.inputConfirmPassword.text.toString().equals(binding.inputPassword.text.toString())) {
            showToast("Password & confirm password must be same")
            return false
        } else {
            return true
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE)
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignUp.setVisibility(View.VISIBLE)
        }
    }

}

