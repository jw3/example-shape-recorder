package com.github.jw3.exampleshaperecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.esri.arcgisruntime.geometry.*
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var track = emptyPtc
    var course = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askGpsPermission()

        mapView.map = ArcGISMap(Basemap.Type.IMAGERY, 0.0, 0.0, 4)

        mapView.locationDisplay.startAsync()
        mapView.locationDisplay.addLocationChangedListener { e ->
            e.location.position?.apply {
                if (track.isNotEmpty()) when {
                    course != e.location.course -> {
                        course = e.location.course
                        track.add(this)
                    }
                    track.last() != e.location.position -> {
                        val geo = GeometryEngine.distanceGeodetic(
                            this,
                            track.last(),
                            LinearUnitMeters,
                            AngularUnitMeters,
                            GeodeticCurveType.GEODESIC
                        )

                        Log.d(TAG, "moved ${geo.distance} ${geo.distanceUnit.displayName}")
                        if (geo.distance > PinDist)
                            track.add(this)
                    }
                }
            }
        }

        toggleButton.setOnCheckedChangeListener { _, starting ->
            cancelButton.isVisible = starting

            if (starting) {
                track = PointCollection(SR)
                track.add(mapView.locationDisplay.location.position)
            } else when (track) {
                emptyPtc ->
                    Log.d(TAG, "no points recorded")
                else -> {
                    val b = PolygonBuilder(track)
                    val g = b.toGeometry()
                    Log.d(TAG, "saving polygon: ${g.toJson()}")

                    val a = GeometryEngine.areaGeodetic(g, AreaUnitAcres, GeodeticCurveType.GEODESIC)
                    Log.d(TAG, "Acres: $a")
                }
            }
        }
        cancelButton.setOnClickListener {
            Log.d(TAG, "cancel recording")
            track = emptyPtc
            toggleButton.isChecked = false
            cancelButton.isVisible = false
        }
    }

    private fun askGpsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LocationPermissionRequest
            )
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val LocationPermissionRequest = 111
        private val LinearUnitMeters = LinearUnit(LinearUnitId.METERS)
        private val AngularUnitMeters = AngularUnit(AngularUnitId.DEGREES)
        private val AreaUnitAcres = AreaUnit(AreaUnitId.ACRES)
        val PinDist = 5 // meters
        val SR = SpatialReferences.getWgs84()
        val emptyPtc = PointCollection(SR)
    }
}
