package com.example.taller_3_icm

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class FollowMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvDistance: TextView

    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var myPoint: GeoPoint? = null

    private var targetPoint: GeoPoint? = null
    private var targetMarker: Marker? = null
    private var line: Polyline? = null

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // ---- Simulación de “tiempo real” (sin Firebase)
    private val handler = Handler(Looper.getMainLooper())
    private var tick = 0
    private val simulateRunnable = object : Runnable {
        override fun run() {
            targetPoint = targetPoint?.let {
                val lat = it.latitude + 0.00012   // ~13 m
                val lon = it.longitude + 0.00012
                GeoPoint(lat, lon)
            }
            updateTargetMarker()
            updateDistanceAndLine()
            handler.postDelayed(this, 2000L)
            tick++
        }
    }

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) startLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar configuración de osmdroid + user agent (sin PreferenceManager)
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_follow_map)
        mapView = findViewById(R.id.mapView)
        tvDistance = findViewById(R.id.tvDistance)

        val trackedName = intent.getStringExtra("TRACKED_NAME") ?: "Usuario seguido"

        // ---------- Mapa: tiles HTTPS para evitar cleartext ----------
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
        mapView.controller.setZoom(16.0)

        // Mi ubicación (overlay azul) -> NO activar hasta tener permiso
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        mapView.overlays.add(myLocationOverlay)

        // Marcador del seguido
        targetMarker = Marker(mapView).apply { title = trackedName }
        mapView.overlays.add(targetMarker)

        // Línea recta
        line = Polyline().apply { setPoints(emptyList()) }
        mapView.overlays.add(line)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissions()

        // Punto inicial del seguido (fallback Bogotá)
        val startCenter = GeoPoint(4.7110, -74.0721)
        targetPoint = startCenter
        updateTargetMarker()

        // ---- Inicia simulación (quita esto cuando uses Firebase) ----
        handler.postDelayed(simulateRunnable, 1500L)
        // subscribeToTargetLocationFromFirebase(trackedUid) // cuando lo actives
    }

    // ---------- Permisos ----------
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
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestPerms.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ---------- Ubicación del seguidor ----------
    @SuppressLint("MissingPermission") // seguro: hasLocationPermission() arriba
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        // Activar overlay solo cuando ya hay permiso
        if (!myLocationOverlay.isMyLocationEnabled) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
        }

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location = result.lastLocation ?: return
                myPoint = GeoPoint(loc.latitude, loc.longitude)
                if (!myLocationOverlay.isFollowLocationEnabled) {
                    mapView.controller.setCenter(myPoint)
                }
                updateDistanceAndLine()
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                req,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (se: SecurityException) {
            Log.e("FollowMapActivity", "Location permission revoked at runtime", se)
        }
    }

    // ---------- UI helpers ----------
    private fun updateTargetMarker() {
        targetMarker?.position = targetPoint
        targetMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.invalidate()
    }

    private fun updateDistanceAndLine() {
        val me = myPoint
        val tg = targetPoint
        if (me == null || tg == null) return

        val results = FloatArray(1)
        Location.distanceBetween(
            me.latitude, me.longitude,
            tg.latitude, tg.longitude,
            results
        )
        val meters = results[0].toDouble()
        tvDistance.text = if (meters < 1000)
            "${meters.toInt()} m"
        else
            String.format("%.2f km", meters / 1000.0)

        line?.setPoints(listOf(me, tg))
        mapView.invalidate()
    }

    // ---------- Ciclo de vida ----------
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
        handler.removeCallbacks(simulateRunnable)
        // Si usas Firebase: quitar listener aquí.
    }
}
