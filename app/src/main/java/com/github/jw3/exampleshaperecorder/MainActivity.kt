package com.github.jw3.exampleshaperecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val LocationPermissionRequest = 111

    val PinDist = 5 // meters
    val SR = SpatialReferences.getWgs84()
    val emptyPtc = PointCollection(SR)
    var track = emptyPtc
    var course = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askGpsPermission()

        mapView.map = ArcGISMap(Basemap.Type.IMAGERY, 34.056295, -117.195800, 16)

        mapView.locationDisplay.addLocationChangedListener { e ->
            if (e.location.position != null) when {
                track.isEmpty() ->
                    track.add(e.location.position)
                course != e.location.course -> {
                    course = e.location.course
                    track.add(e.location.position)
                }
                GeometryEngine.distanceBetween(e.location.position, track.last()) > PinDist ->
                    track.add(e.location.position)
            }
        }

        toggleButton.setOnCheckedChangeListener { _, starting ->
            cancelButton.isVisible = starting
            println("toggle $starting")

            if (!starting) when (track) {
                emptyPtc ->
                    println("no points")
                else ->
                    println("saving points")
            }
        }
        cancelButton.setOnClickListener {
            println("cancel")
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
}
