package com.github.jw3.exampleshaperecorder

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.esri.arcgisruntime.geometry.*
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var course = 0.0
    var track = emptyPtc

    var trackG: Graphic? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val shapes = mutableListOf<String>()
        val adapter = ArrayAdapter<String>(this, R.layout.simple_list_view_item, shapes)
        listView.adapter = adapter

        askGpsPermission()

        mapView.map = ArcGISMap(Basemap.Type.IMAGERY, 42.0, 42.0, 4)

        val prevTracks = GraphicsOverlay()
        val currTracks = GraphicsOverlay()
        mapView.graphicsOverlays.addAll(listOf(prevTracks, currTracks))

        mapView.locationDisplay.startAsync()
        mapView.locationDisplay.addLocationChangedListener { e ->
            e.location.position?.apply {
                if (track.isNotEmpty()) when {
                    course != e.location.course -> {
                        course = e.location.course
                        track.add(this)
                        trackG?.geometry = Polyline(track)
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
                        if (geo.distance > PinDist) {
                            track.add(this)
                            trackG?.geometry = Polyline(track)
                        }
                    }
                }
            }
        }

        toggleButton.setOnCheckedChangeListener { _, starting ->
            cancelButton.isVisible = starting

            if (starting) {
                track = PointCollection(SR)
                track.add(mapView.locationDisplay.location.position)

                trackG = Graphic(Polyline(track), trackMarker)
                currTracks.graphics.add(trackG)
            } else when (track) {
                emptyPtc -> {
                    Log.d(TAG, "no points recorded")
                    currTracks.graphics.clear()
                    trackG = null
                }
                else -> {
                    val b = PolygonBuilder(track)
                    val g = b.toGeometry()
                    Log.d(TAG, "saving polygon: ${g.toJson()}")

                    val a = GeometryEngine.areaGeodetic(g, AreaUnitAcres, GeodeticCurveType.GEODESIC)
                    Log.d(TAG, "Acres: $a")

                    currTracks.graphics.clear()
                    trackG?.geometry = g
                    prevTracks.graphics.add(trackG)
                    trackG = null

                    shapes.add("Acres: $a")
                    adapter.notifyDataSetChanged()
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
        private const val PinDist = 5 // meters
        private val SR = SpatialReferences.getWgs84()
        private val emptyPtc = PointCollection(SR)
        private val AreaUnitAcres = AreaUnit(AreaUnitId.ACRES)
        private val LinearUnitMeters = LinearUnit(LinearUnitId.METERS)
        private val AngularUnitMeters = AngularUnit(AngularUnitId.DEGREES)
        private val trackMarker = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.argb(.75f, 1f, 1f, 0f), 5f)
        private val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, -0x10000, 10f)
    }
}
