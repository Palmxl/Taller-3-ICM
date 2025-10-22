package com.example.taller_3_icm

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

data class Poi(val name: String, val lat: Double, val lng: Double)

class HomeMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var tvHint: TextView? = null

    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) startLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Config OSMDroid sin PreferenceManager (evita dependencia extra)
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_home_map)
        mapView = findViewById(R.id.mapView)
        tvHint = findViewById(R.id.tvHint) // puede no existir; por eso es nullable

        // Tiles por HTTPS (evita bloqueo cleartext)
        val httpsOSM = XYTileSource(
            "OpenStreetMap", 0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            )
        )
        mapView.setTileSource(httpsOSM)
        mapView.setMultiTouchControls(true)

        // Mi ubicación (activar tras permiso)
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        mapView.overlays.add(myLocationOverlay)

        // Cargar y pintar POIs
        val pois = runCatching { readPoisFromAssets() }
            .onFailure {
                Log.e("HomeMapActivity", "Error leyendo locations.json", it)
                Toast.makeText(this, "No se pudo leer locations.json", Toast.LENGTH_LONG).show()
            }
            .getOrElse { emptyList() }

        addPoiMarkers(pois)
        zoomToPois(pois)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissions()
    }

    // --------- JSON -> List<Poi> ----------
    private fun readPoisFromAssets(): List<Poi> {
        val jsonText = assets.open("locations.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val out = mutableListOf<Poi>()

        if (root.has("locationsArray")) {
            val arr = root.getJSONArray("locationsArray")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Poi(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude")))
            }
        } else if (root.has("locations")) {
            val obj = root.getJSONObject("locations")
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = obj.getJSONObject(k)
                out.add(Poi(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude")))
            }
        }
        return out
    }

    private fun addPoiMarkers(pois: List<Poi>) {
        pois.forEach { poi ->
            val m = Marker(mapView).apply {
                position = GeoPoint(poi.lat, poi.lng)
                title = poi.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(m)
        }
        mapView.invalidate()
        tvHint?.text = "POIs: ${pois.size}  •  Tu ubicación: activada"
    }

    private fun zoomToPois(pois: List<Poi>) {
        if (pois.isEmpty()) {
            // Fallback Bogotá si el JSON viniera vacío
            mapView.controller.setZoom(13.0)
            mapView.controller.setCenter(GeoPoint(4.65, -74.06))
            return
        }
        val points = pois.map { GeoPoint(it.lat, it.lng) }
        val box = BoundingBox.fromGeoPoints(points)
        mapView.zoomToBoundingBox(box, true)
    }

    // --------- Permisos / ubicación del usuario ----------
    private fun hasLocationPermission(): Boolean {
        val fineOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineOk || coarseOk
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermission()) startLocationUpdates() else requestPerms.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        if (!myLocationOverlay.isMyLocationEnabled) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
        }

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location = result.lastLocation ?: return
                // Si quieres centrar una vez al usuario, descomenta:
                // mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                req, locationCallback as LocationCallback, Looper.getMainLooper()
            )
        } catch (se: SecurityException) {
            Log.e("HomeMapActivity", "Location permission revoked at runtime", se)
        }
    }

    // --------- Ciclo de vida ----------
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }
}
