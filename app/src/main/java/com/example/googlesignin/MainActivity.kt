package com.example.googlesignin

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.googlesignin.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // declare the GoogleSignInClient
    lateinit var mGoogleSignInClient: GoogleSignInClient

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button

    // view for image view
    private lateinit var imageView: ImageView

    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    // request code
    private val PICK_IMAGE_REQUEST = 22

    // instance for firebase storage and StorageReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // call requestIdToken as follows
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.logoutButton.setOnClickListener {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(this, SignInActivity::class.java)
                Toast.makeText(this, "Logging Out", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
        }

        supportActionBar?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#0F9D58")))
        }

        // initialise views
        btnSelect = findViewById(R.id.select_Image)
        btnUpload = findViewById(R.id.upload_Image)
        imageView = findViewById(R.id.imageView)

        // get the Firebase storage reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        // on pressing btnSelect SelectImage() is called
        btnSelect.setOnClickListener {
            selectImage()
        }

        // on pressing btnUpload uploadImage() is called
        btnUpload.setOnClickListener {
            uploadImage()
        }
    }

    // Select Image method
    private fun selectImage() {
        // Defining Implicit Intent to mobile gallery
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Select Image from here..."
            ),
            PICK_IMAGE_REQUEST
        )
    }

    // Override onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
            && resultCode == RESULT_OK
            && data != null
            && data.data != null
        ) {
            // Get the Uri of data
            filePath = data.data
            try {
                // Setting image on image view using Bitmap
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    filePath
                )
                imageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                // Log the exception
                e.printStackTrace()
            }
        }
    }

    // UploadImage method
    private fun uploadImage() {
        filePath?.let { filePath ->
            // Code for showing progressDialog while uploading
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            // Defining the child of storageReference
            val ref = storageReference.child("images/${UUID.randomUUID()}")

            // adding listeners on upload or failure of image
            ref.putFile(filePath)
                .addOnSuccessListener(
                    OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        // Image uploaded successfully
                        // Dismiss dialog
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@MainActivity,
                            "Image Uploaded!!",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                .addOnFailureListener(OnFailureListener { e ->
                    // Error, Image not uploaded
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "Failed " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                })
                .addOnProgressListener(
                    OnProgressListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        // Progress Listener for loading percentage on the dialog box
                        val progress =
                            100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                    })
        }
    }

}
