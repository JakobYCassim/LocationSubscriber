package com.example.locationsubscriber


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.locationsubscriber.ColorUtils.getUniqueColorForStudent
import com.example.locationsubscriber.LocationUtils.formatTimestamp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.text.SimpleDateFormat
import java.util.Locale

class SummaryActivity: AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var studentId: String? = null
    private val pointsList = mutableListOf<MarkerPoints>()
    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
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
        val earliestDate = formatTimestamp(dbHelper.getEarliestDateForStudent(studentId!!))
        val latestDate = formatTimestamp(dbHelper.getLatestDateForStudent(studentId!!))
        titleText.text = "Summary of $studentId"
        startDateInput = findViewById(R.id.startDate)
        endDateInput = findViewById(R.id.endDate)

        startDateInput.setText(earliestDate)
        endDateInput.setText(latestDate)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.Smap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val textWatcher = object: TextWatcher {
            override fun beforeTextChanged(s:CharSequence?, start: Int, count: Int, afer: Int) {}
            override fun onTextChanged(s:CharSequence?,start: Int, before: Int, count: Int){
                filterMapData()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        startDateInput.addTextChangedListener(textWatcher)
        endDateInput.addTextChangedListener((textWatcher))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val returnButton: Button = findViewById(R.id.btn_return)
        returnButton.setOnClickListener {
            finish()
        }


    }

    private fun filterMapData() {
        val startDate = startDateInput.text.toString()
        val endDate = endDateInput.text.toString()
            if (startDate.isNotBlank() && endDate.isNotBlank()) {
                filterDataByRange(startDate, endDate)

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


    @Suppress("UNUSED_VARIABLE")
    private fun addMarkerAtLocation(latLng: LatLng) {
        val newCustomPoint = MarkerPoints(pointsList.size + 1, latLng)

        pointsList.add(newCustomPoint)

        val marker = map.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(1.0)
                .fillColor(getUniqueColorForStudent(studentId!!))
                .strokeColor(getUniqueColorForStudent(studentId!!))
                .strokeWidth(1f)
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

    private fun filterDataByRange(startDate:String, endDate: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startTimestamp = dateFormat.parse(startDate)?.time
            val endTimestamp = dateFormat.parse(endDate)?.time

            if (startTimestamp == null || endTimestamp == null) {
                Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD.", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            if (startTimestamp > endTimestamp) {
                Toast.makeText(this, "Start date must be before end date.", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            updateSpeedMetrics(studentId!!, startTimestamp, endTimestamp)
            val filteredLocations =
                dbHelper.getLocationsWithinDateRange(studentId!!, startTimestamp, endTimestamp)
            updateMap(filteredLocations)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSpeedMetrics(studentId: String, startDate: Long, endDate: Long) {

        val minSpeedText: TextView = findViewById(R.id.minSpeedSum)
        val maxSpeedText: TextView = findViewById(R.id.maxSpeedSum)
        val avgSpeedText: TextView = findViewById(R.id.avgSpeedSum)


        val maxSpeed = dbHelper.getMaxSpeedForDateRange(studentId, startDate, endDate)
        val minSpeed = dbHelper.getMinSpeedForDateRange(studentId, startDate, endDate)
        val avgSpeed = dbHelper.getAverageSpeedForDateRange(studentId, startDate, endDate)

        // Update the UI with these values
        maxSpeedText.text = "Max Speed: %.2f m/s".format(maxSpeed)
        minSpeedText.text = "Min Speed: %.2f m/s".format(minSpeed)
        avgSpeedText.text = "Avg Speed: %.2f m/s".format(avgSpeed)
    }


    private fun updateMap(filteredLocations: List<StudentLocation>) {
        map.clear()
        filteredLocations.forEach {
            addMarkerAtLocation(LatLng(it.latitude, it.longitude))
            drawPolyline()
        }
    }



}
