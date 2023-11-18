package com.example.taller3

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.location.LocationManager
import android.provider.MediaStore
import com.google.firebase.FirebaseApp



class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private lateinit var imageViewProfile: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextID: EditText
    private lateinit var btnRegister: Button


    private var imageUri: Uri? = null

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()


        btnSelectImage = findViewById(R.id.btnSelectImage)
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextID = findViewById(R.id.editTextID)
        btnRegister = findViewById(R.id.btnRegister)

        btnSelectImage.setOnClickListener {
            showImagePickerDialog()
        }

        btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun showImagePickerDialog() {
        val items = arrayOf("Tomar Foto", "Elegir desde Galería")
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar Imagen")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            dispatchTakePictureIntent()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> {
                        if (checkStoragePermission()) {
                            pickImageFromGallery()
                        } else {
                            requestStoragePermission()
                        }
                    }
                }
            }
            .create()

        dialog.show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            REQUEST_IMAGE_CAPTURE
        )
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_IMAGE_PICK
        )
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        imageViewProfile.setImageBitmap(it)
                    }
                }

                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri: Uri? = data?.data
                    selectedImageUri?.let {
                        imageViewProfile.setImageURI(it)
                    }
                }
            }
        }
    }

    private fun registerUser() {
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = auth.currentUser
                    //saveUserInfo(user)
                    requestLocationPermission()
                    uploadProfileImage(user)
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "El registro no pudo ser completado", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Ya se han concedido los permisos, puedes obtener la ubicación aquí
            getLocation()
        }
    }

    private fun getLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude


                saveUserInfoWithLocation(latitude, longitude)
            } else {

            }
        } catch (ex: SecurityException) {

        }
    }

    private fun saveUserInfoWithLocation(latitude: Double, longitude: Double) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        val firstName = editTextFirstName.text.toString()
        val lastName = editTextLastName.text.toString()
        val email = editTextEmail.text.toString()
        val id = editTextID.text.toString()

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId ?: "")
        userRef.child("firstName").setValue(firstName)
        userRef.child("lastName").setValue(lastName)
        userRef.child("email").setValue(email)
        userRef.child("id").setValue(id)
        userRef.child("latitude").setValue(latitude)
        userRef.child("longitude").setValue(longitude)
    }
    private fun saveUserInfo(user: FirebaseUser?) {
        val userId = user?.uid
        val firstName = editTextFirstName.text.toString()
        val lastName = editTextLastName.text.toString()
        val email = editTextEmail.text.toString()
        val id = editTextID.text.toString()

        val userRef = database.getReference("users").child(userId ?: "")
        userRef.child("firstName").setValue(firstName)
        userRef.child("lastName").setValue(lastName)
        userRef.child("email").setValue(email)
        userRef.child("id").setValue(id)
    }

    private fun uploadProfileImage(user: FirebaseUser?) {
        val userId = user?.uid
        val storageRef: StorageReference = storage.reference
        val profileImagesRef: StorageReference = storageRef.child("profile_images/$userId.jpg")

        imageUri?.let {
            profileImagesRef.putFile(it)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->

                    val downloadUrl: String =
                        taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    saveProfileImageUrl(user, downloadUrl)
                }
                .addOnFailureListener {

                }
        }
    }

    private fun saveProfileImageUrl(user: FirebaseUser?, downloadUrl: String) {
        val userId = user?.uid
        val userRef = database.getReference("users").child(userId ?: "")
        userRef.child("profileImageUrl").setValue(downloadUrl)
    }


}


