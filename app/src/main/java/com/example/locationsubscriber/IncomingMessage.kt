package com.example.locationsubscriber

data class IncomingMessage (
    val student_id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
