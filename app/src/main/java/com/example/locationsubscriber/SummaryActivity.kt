package com.example.locationsubscriber


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.locationsubscriber.ColorUtils.getUniqueColorForStudent
import com.example.locationsubscriber.LocationUtils.formatTimestamp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class SummaryActivity: AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var studentId: String? = null
    private val pointsList = mutableListOf<MarkerPoints>()
    private lateinit var dbHelper: LocationDatabaseHelper
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        studentId = intent.getStringExtra("studentId")
        if (studentId == null) {
            Log.e("Summary", "No Student ID provided")
            finish()
            return
        }

        val titleText: TextView = findViewById(R.id.sumText)
        dbHelper = LocationDatabaseHelper(this)
        val startDate = formatTimestamp(dbHelper.getEarliestDateForStudent(studentId!!))
        val endDate = formatTimestamp(dbHelper.getLatestDateForStudent(studentId!!))
        titleText.text = "Summary of $studentId, Start Date: $startDate, End Date: $endDate"
        val mapFragment = supportFragmentManager.findFragmentById(R.id.Smap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val returnButton: Button = findViewById(R.id.btn_return)
        returnButton.setOnClickListener {
            finish()
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadStudentLocations() {
        val locations = dbHelper.getLocationsForStudent(studentId!!)
        for (location in locations) {
            val latLng = LatLng(location.latitude, location.longitude)
            addMarkerAtLocation(latLng)
            drawPolyline()
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.clear()
        loadStudentLocations()
        val minSpeedText: TextView = findViewById(R.id.minSpeedSum)
        val maxSpeedText: TextView = findViewById(R.id.maxSpeedSum)
        val avgSpeedText: TextView = findViewById(R.id.avgSpeedSum)
        minSpeedText.text = "Min Speed: ${dbHelper.getMinSpeedForStudent(studentId!!)}"
        maxSpeedText.text = "Max Speed: ${dbHelper.getMaxSpeedForStudent(studentId!!)}"
        avgSpeedText.text = "Average Speed: ${dbHelper.getAverageSpeedForStudent(studentId!!)}"
    }


    private fun addMarkerAtLocation(latLng: LatLng) {
        val newCustomPoint = MarkerPoints(pointsList.size + 1, latLng)

        pointsList.add(newCustomPoint)

        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Marker ${newCustomPoint.id}")
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private fun drawPolyline() {
        val latLngPoints = pointsList.map { it.point }
        val color = studentId?.let { getUniqueColorForStudent(it) }
        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(color!!)
            .width(5f)
            .geodesic(true)

        map.addPolyline(polylineOptions)

        val bounds = LatLngBounds.builder()
        latLngPoints.forEach { bounds.include(it)}
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

}
