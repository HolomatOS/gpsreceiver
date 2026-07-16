package com.holomatos.gpsreceiver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.holomatos.gpsreceiver.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var marker: Marker? = null
    private var listener: ValueEventListener? = null
    private var currentCode: String? = null

    // Esri World Imagery satellite tiles (free, no API key required)
    private val satelliteSource = object : XYTileSource(
        "EsriWorldImagery", 0, 19, 256, ".jpg",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
            val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
            val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$zoom/$y/$x$mImageFilenameEnding"
        }
    }

    // Esri reference layer: city/state/country labels + boundaries, transparent PNG overlay
    private val labelsSource = object : XYTileSource(
        "EsriBoundariesPlaces", 0, 19, 256, ".png",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
            val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
            val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$zoom/$y/$x$mImageFilenameEnding"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.map.setTileSource(satelliteSource)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(17.0)
        binding.map.controller.setCenter(GeoPoint(0.0, 0.0))

        val labelsProvider = org.osmdroid.tileprovider.MapTileProviderBasic(applicationContext, labelsSource)
        val labelsOverlay = org.osmdroid.views.overlay.TilesOverlay(labelsProvider, this)
        labelsOverlay.loadingBackgroundColor = android.graphics.Color.TRANSPARENT
        labelsOverlay.loadingLineColor = android.graphics.Color.TRANSPARENT
        binding.map.overlays.add(labelsOverlay)

        binding.trackButton.setOnClickListener {
            val code = binding.pairingCodeInput.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter the pairing code from the sender device", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            trackCode(code)
        }
    }

    private fun trackCode(code: String) {
        listener?.let { oldListener ->
            currentCode?.let { old ->
                FirebaseDatabase.getInstance().getReference("locations").child(old)
                    .removeEventListener(oldListener)
            }
        }
        currentCode = code
        binding.statusText.text = "CONNECTING..."

        val ref = FirebaseDatabase.getInstance().getReference("locations").child(code)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                if (lat == null || lng == null) {
                    binding.statusText.text = "NO DATA FOR CODE: $code"
                    return
                }
                val point = GeoPoint(lat, lng)

                if (marker == null) {
                    marker = Marker(binding.map)
                    marker!!.title = "Tracked device"
                    binding.map.overlays.add(marker)
                }
                marker!!.position = point
                binding.map.controller.animateTo(point)
                binding.map.invalidate()
                binding.statusText.text = "LIVE \u2022 CODE: $code"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Sync error: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.statusText.text = "ERROR"
            }
        }
        ref.addValueEventListener(listener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let { l ->
            currentCode?.let { code ->
                FirebaseDatabase.getInstance().getReference("locations").child(code).removeEventListener(l)
            }
        }
    }
}
