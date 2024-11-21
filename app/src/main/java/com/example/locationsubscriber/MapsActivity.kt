package com.example.locationsubscriber

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locationsubscriber.ColorUtils.getUniqueColorForStudent
import com.example.locationsubscriber.LocationUtils.calculateSpeed
import com.example.locationsubscriber.LocationUtils.convertToStudentLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val pointsMap = mutableMapOf<String, MutableList<Circle>>()
    private lateinit var adapter: StudentLocationAdapter
    private val studentLocations = mutableListOf<StudentLocation>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mqttClient: Mqtt5AsyncClient
    private val polylinesMap = mutableMapOf<String, Polyline>()
    private lateinit var dbHelper: LocationDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        recyclerView = findViewById(R.id.rvStudent_List)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentLocationAdapter(studentLocations) {studentId ->

            val intent = Intent(this, SummaryActivity::class.java).apply {
                putExtra("studentId", studentId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        dbHelper = LocationDatabaseHelper(this)
        connectToBroker()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun connectToBroker() {
        mqttClient = Mqtt5Client.builder()
            .identifier("Subscriber-816041437")
            .serverHost("broker-816041437.sundaebytestt.com")
            .serverPort(1883)
            .buildAsync()

        mqttClient.connectWith()
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to connect to broker", Toast.LENGTH_SHORT).show()
                    }
                }else {
                    runOnUiThread {
                        Toast.makeText(this, "Connected to Broker", Toast.LENGTH_SHORT).show()
                    }
                    subscribeToTopic("assignment/location")
                }
            }
    }

    @Suppress("SameParameterValue")
    private fun subscribeToTopic(topic: String) {
        mqttClient.subscribeWith()
            .topicFilter(topic)
            .callback{ publish: Mqtt5Publish ->
                val message = String(publish.payloadAsBytes, Charsets.UTF_8)
                Log.d("subscribe", "Received Message: $message")
                handleInformation(message)
            }
            .send()
            .whenComplete { _, throwable ->
                if(throwable != null) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to subscribe to topic", Toast.LENGTH_SHORT).show()
                    }
                }else {
                    runOnUiThread {
                        Toast.makeText(this, "Subscribed to topic: $topic", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private val previousLocations = mutableMapOf<String, StudentLocation>()
    private fun handleInformation(message: String) {
        try {
            val incomingLocation = Gson().fromJson(message, IncomingMessage::class.java)
            val location = LatLng(incomingLocation.latitude, incomingLocation.longitude)
            val studentLocation = convertToStudentLocation(incomingLocation)
            studentLocation.speed = speedCalc(studentLocation)
            dbHelper.insertLocation(studentLocation)
            runOnUiThread {
                addNewData(studentLocation)
                addMarkerAtLocation(studentLocation.student_id, location)
            }
            previousLocations[studentLocation.student_id] = studentLocation
        } catch (e: Exception) {
            Log.e("handleInformation", "Error parsing message: $message", e)
        }
    }

    private fun speedCalc(studentLocation: StudentLocation): Double {
        val previousLocation = previousLocations[studentLocation.student_id]
        var speed = 0.0
        if(previousLocation != null) {

            speed = calculateSpeed(
                previousLocation.latitude,
                previousLocation.longitude,
                previousLocation.timestamp,
                studentLocation.latitude,
                studentLocation.longitude,
                studentLocation.timestamp
            )

            if (speed < previousLocation.minSpeed) studentLocation.minSpeed = speed
            if (speed > previousLocation.maxSpeed) studentLocation.maxSpeed = speed
        }

        return speed
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView() {

        try {
            studentLocations.clear()
            studentLocations.addAll(dbHelper.getRecentLocationsList())
            Log.d("MapsActivity", "Locations added from db")
        } catch(e: Exception) {Log.e("Database", "${e.message}")}
        studentLocations.forEach {
            it.minSpeed = dbHelper.getMinSpeedForStudent(it.student_id)
            it.maxSpeed = dbHelper.getMaxSpeedForStudent(it.student_id)
            previousLocations[it.student_id] = it
        }
        Log.d("MapsActivity", "Adapter being notified")
        adapter.notifyDataSetChanged()
    }

    private fun updateRecentData() {

        updateRecyclerView()

        updateMap()
    }

    private val handler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateRecentData()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapsActivity", "onMapReady called")
        mMap = googleMap
        mMap.clear()
        updateRecyclerView()
        updateMap()

    }

    private fun updateMap() {
       val recentLocations = dbHelper.getRecentLocationsMap()

        mMap.clear()
        recentLocations.forEach {
            addMarkerAtLocation(it.student_id, LatLng(it.latitude, it.longitude))
        }
    }

    private fun addMarkerAtLocation(studentId: String, newLocation: LatLng) {
        if(!pointsMap.containsKey(studentId)) {
            pointsMap[studentId] = mutableListOf()
        }
        val marker = mMap.addCircle(
            CircleOptions()
                .center(newLocation)
                .radius(1.0)
                .fillColor(getUniqueColorForStudent(studentId))
                .strokeColor(getUniqueColorForStudent(studentId))
                .strokeWidth(1f)
        )

        marker.let { pointsMap[studentId]?.add(it) }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 10f))
        drawPolyline(studentId, newLocation)
    }


    private fun drawPolyline(studentId: String, newLocation: LatLng) {

        val studentColor = getUniqueColorForStudent(studentId)
        val existingPolyline = polylinesMap[studentId]
        if(existingPolyline != null) {
            val points = existingPolyline.points
            points.add(newLocation)
            existingPolyline.points = points
        }else {

            val polylineOptions = PolylineOptions()
                .add(newLocation)
                .color(studentColor)
                .width(5f)
                .geodesic(true)

            val polyline = mMap.addPolyline(polylineOptions)
            polylinesMap[studentId] = polyline
        }
        val bounds = LatLngBounds.builder()
        val points = polylinesMap[studentId]?.points ?: listOf()
        points.forEach { bounds.include(it) }
        bounds.include(newLocation)
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun addNewData(studentLocation: StudentLocation) {
        val existingStudentIndex = studentLocations.indexOfFirst {it.student_id == studentLocation.student_id}
        if(existingStudentIndex != -1) {
            studentLocations[existingStudentIndex] = studentLocation
            adapter.notifyItemChanged(existingStudentIndex)
        }else {
            studentLocations.add(studentLocation)
            adapter.notifyItemInserted(studentLocations.size - 1)
        }
    }




}