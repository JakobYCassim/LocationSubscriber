package com.example.locationsubscriber

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val pointsList = mutableListOf<MarkerPoints>()
    private lateinit var adapter: StudentLocationAdapter
    private val studentLocations = mutableListOf<StudentLocation>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mqttClient: Mqtt5AsyncClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        recyclerView = findViewById(R.id.rvStudent_List)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentLocationAdapter(studentLocations)
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
            runOnUiThread {
                studentLocations.add(studentLocation)
                adapter.notifyItemInserted(studentLocations.size - 1)
                addMarkerAtLocation(location)
            }
        } catch (e: Exception) {
            Log.e("handleInformation", "Error parsing message: $message", e)
        }
    }

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