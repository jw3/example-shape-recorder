package com.github.jw3.exampleshaperecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val SR = SpatialReferences.getWgs84()
    val emptyPtc = PointCollection(SR)
    var currentTrack = emptyPtc

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView.map = ArcGISMap(Basemap.Type.IMAGERY, 34.056295, -117.195800, 16)

        toggleButton.setOnCheckedChangeListener { _, starting ->
            cancelButton.isVisible = starting
            println("toggle $starting")

            if (!starting) when (currentTrack) {
                emptyPtc ->
                    println("no points")
                else ->
                    println("saving points")
            }
        }
        cancelButton.setOnClickListener {
            println("cancel")
            currentTrack = emptyPtc
            toggleButton.isChecked = false
            cancelButton.isVisible = false
        }
    }
}
