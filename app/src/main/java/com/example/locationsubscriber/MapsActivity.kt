package com.example.locationsubscriber

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
 import androidx.recyclerview.widget.RecyclerView
import com.example.locationsubscriber.ColorUtils.getUniqueColorForStudent

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val pointsMap = mutableMapOf<String, MutableList<Marker>>()
    private lateinit var adapter: StudentLocationAdapter
    private val studentLocations = mutableListOf<StudentLocation>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mqttClient: Mqtt5AsyncClient
    private val polylinesMap = mutableMapOf<String, Polyline>()


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

    private fun handleInformation(message: String) {
        try {
            val studentLocation = Gson().fromJson(message, StudentLocation::class.java)
            val location = LatLng(studentLocation.latitude, studentLocation.longitude)
            val databaseHelper = LocationDatabaseHelper(this)
            databaseHelper.insertLocation(studentLocation)
            runOnUiThread {
                updateData(studentLocation)
                addMarkerAtLocation(studentLocation.student_id, location)
            }
        } catch (e: Exception) {
            Log.e("handleInformation", "Error parsing message: $message", e)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateRecyclerView() {
        studentLocations.clear()
        val dbHelper = LocationDatabaseHelper(this)
        studentLocations.addAll(dbHelper.getMostRecentLocations())
        adapter.notifyDataSetChanged()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.clear()
        populateRecyclerView()
        populateMap()

    }

    private fun populateMap() {
        for(location in studentLocations) {
            addMarkerAtLocation(location.student_id, LatLng(location.latitude, location.longitude))
        }
    }

    private fun addMarkerAtLocation(studentId: String, newLocation: LatLng) {
        if(!pointsMap.containsKey(studentId)) {
            pointsMap[studentId] = mutableListOf()
        }
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(newLocation)
                .title("Marker $studentId")
                .snippet("Lat: ${newLocation.latitude}, Lng: ${newLocation.longitude}")
        )

        marker?.let { pointsMap[studentId]?.add(it) }
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

    private fun updateData(studentLocation: StudentLocation) {
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