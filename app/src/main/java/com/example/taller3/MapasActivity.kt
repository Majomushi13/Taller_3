package com.example.taller3

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject

class MapasActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configura el Toolbar como ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                val intent = Intent (this,MainActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_set_available -> {
                // Manejar la acción de establecer como disponible
                return true
            }
            R.id.action_set_offline -> {
                // Manejar la acción de establecer como desconectado
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Solicitar permisos de ubicación si no están otorgados
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            // Configurar el mapa
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomGesturesEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true


            // Obtener la ubicación actual del usuario y agregar un marcador
            val locationProvider = LocationServices.getFusedLocationProviderClient(this)
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Ubicación Actual"))
                }
            }


            // Obtener el JSON desde el archivo raw
            val inputStream = resources.openRawResource(R.raw.locations)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val locations = JSONObject(jsonString)

            // Acceder a las ubicaciones y agregar marcadores en el mapa
            val locationsArray = locations.getJSONArray("locationsArray")

            for (i in 0 until locationsArray.length()) {
                val location = locationsArray.getJSONObject(i)
                val latitude = location.getDouble("latitude")
                val longitude = location.getDouble("longitude")
                val name = location.getString("name")

                val poiLatLng = LatLng(latitude, longitude)
                mMap.addMarker(MarkerOptions().position(poiLatLng).title(name))
            }

            // Centrar el mapa en la última ubicación del JSON
            val lastLocation = locationsArray.getJSONObject(locationsArray.length() - 1)
            val lastLatitude = lastLocation.getDouble("latitude")
            val lastLongitude = lastLocation.getDouble("longitude")
            val lastLatLng = LatLng(lastLatitude, lastLongitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 12.0f))
        }
    }
}
