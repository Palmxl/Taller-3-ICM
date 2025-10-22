package com.example.taller_3_icm.menu

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3_icm.databinding.ActivityVerPosicionBinding
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.*

class VerPosicionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerPosicionBinding
    private lateinit var map: MapView
    private lateinit var marcadorYo: Marker
    private lateinit var marcadorOtro: Marker
    private lateinit var ref: DatabaseReference

    private var latOtro = 0.0
    private var lonOtro = 0.0
    private var nombreOtro = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerPosicionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        map = binding.mapView
        map.setMultiTouchControls(true)

        nombreOtro = intent.getStringExtra("nombre") ?: "Usuario"
        latOtro = intent.getDoubleExtra("lat", 0.0)
        lonOtro = intent.getDoubleExtra("lon", 0.0)

        marcadorYo = Marker(map)
        marcadorOtro = Marker(map)

        // Permiso de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            obtenerUbicacionYMostrar()
        }

        // Escuchar actualizaciones del otro usuario en Firebase
        escucharCambiosUsuario(nombreOtro)
    }

    private fun obtenerUbicacionYMostrar() {
        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val miGeo = GeoPoint(loc.latitude, loc.longitude)
                    val otroGeo = GeoPoint(latOtro, lonOtro)

                    map.controller.setZoom(15.0)
                    map.controller.setCenter(miGeo)

                    marcadorYo.position = miGeo
                    marcadorYo.title = "Yo"
                    map.overlays.add(marcadorYo)

                    marcadorOtro.position = otroGeo
                    marcadorOtro.title = nombreOtro
                    map.overlays.add(marcadorOtro)

                    actualizarDistancia(miGeo, otroGeo)
                } else {
                    Toast.makeText(this, "No se pudo obtener tu ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarDistancia(miGeo: GeoPoint, otroGeo: GeoPoint) {
        val distanciaMetros = calcularDistancia(miGeo.latitude, miGeo.longitude, otroGeo.latitude, otroGeo.longitude)
        val texto = if (distanciaMetros >= 1000)
            String.format("Distancia: %.2f km", distanciaMetros / 1000)
        else
            String.format("Distancia: %.0f m", distanciaMetros)
        binding.tvDistancia.text = texto
    }

    private fun escucharCambiosUsuario(nombre: String) {
        // Buscar en la BD al usuario con ese nombre
        ref = FirebaseDatabase.getInstance().getReference("users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnap in snapshot.children) {
                    val n = userSnap.child("nombre").getValue(String::class.java)
                    val a = userSnap.child("apellido").getValue(String::class.java)
                    val full = "$n $a"
                    if (full == nombre) {
                        val lat = userSnap.child("latitud").getValue(Double::class.java) ?: 0.0
                        val lon = userSnap.child("longitud").getValue(Double::class.java) ?: 0.0
                        latOtro = lat
                        lonOtro = lon
                        val otroGeo = GeoPoint(lat, lon)
                        marcadorOtro.position = otroGeo
                        map.invalidate()

                        // Actualizar distancia si ya tengo mi ubicación
                        val miPos = marcadorYo.position
                        actualizarDistancia(miPos, otroGeo)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Fórmula de Haversine para distancia en línea recta (metros)
    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
