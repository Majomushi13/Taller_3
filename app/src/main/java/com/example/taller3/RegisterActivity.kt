package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            registerUser()
            val intent = Intent (this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val editTextFirstName = findViewById<EditText>(R.id.editTextFirstName)
        val editTextLastName = findViewById<EditText>(R.id.editTextLastName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)

        val firstName = editTextFirstName.text.toString()
        val lastName = editTextLastName.text.toString()
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()

        // Obtener la ubicación del usuario
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid
                                    saveUserData(userId, firstName, lastName, email,password, it.latitude, it.longitude)
                                } else {
                                    Toast.makeText(
                                        baseContext, "Error en el registro. Por favor, inténtelo de nuevo.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                    } ?: run {
                        Toast.makeText(
                            baseContext, "No se pudo obtener la ubicación.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun saveUserData(
        userId: String?,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        latitude: Double,
        longitude: Double
    ) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId ?: "")

        val userData = UserData(firstName, lastName, email,password, latitude, longitude)
        Log.d("Latitude", "Latitude: $latitude")
        Log.d("Longitude", "Longitude: $longitude")


        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                baseContext, "Registro exitoso.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // En este punto, puedes redirigir al usuario a otra actividad o realizar otras acciones.
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                baseContext, "Error al guardar los datos del usuario.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        baseContext, "Error en el registro. Por favor, inténtelo de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}
