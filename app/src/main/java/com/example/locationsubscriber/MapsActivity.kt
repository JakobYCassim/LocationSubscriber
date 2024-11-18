package com.example.locationsubscriber

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.locationsubscriber.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val pointsList = mutableListOf<MarkerPoints>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapClickListener { latLng ->
            addMarkerAtLocation(latLng)
            drawPolyline()
        }
    }

    private fun addMarkerAtLocation(latLng: LatLng) {
        val newCustomPoint = MarkerPoints(pointsList.size + 1, latLng)

        pointsList.add(newCustomPoint)

        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Marker ${newCustomPoint.id}")
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private fun drawPolyline() {
        val latLngPoints = pointsList.map { it.point }

        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(Color.BLUE)
            .width(5f)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)

        val bounds = LatLngBounds.builder()
        latLngPoints.forEach { bounds.include(it)}
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }
}