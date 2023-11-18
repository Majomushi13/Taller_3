package com.example.taller3

import android.app.Service
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserStatusService : Service() {

    private lateinit var userStatusReference: DatabaseReference
    private lateinit var userStatusListener: ValueEventListener

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Obtén la referencia a la lista de usuarios en la base de datos
        userStatusReference = FirebaseDatabase.getInstance().getReference("users")

        // Define un ValueEventListener para escuchar cambios en la disponibilidad de los usuarios
        userStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    val userName = userSnapshot.child("firstName").getValue(String::class.java)
                    val isAvailable = userSnapshot.child("disponibilidad").getValue(Boolean::class.java)

                    if (userId != null && userName != null && isAvailable == true) {
                        showToast("¡$userName está disponible!")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }


        userStatusReference.addValueEventListener(userStatusListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remueve el listener cuando el servicio se detiene
        userStatusReference.removeEventListener(userStatusListener)
    }

    private fun showToast(message: String) {
        // Muestra un Toast en el hilo principal
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
