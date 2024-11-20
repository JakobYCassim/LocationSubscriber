package com.example.locationsubscriber



@Suppress("PropertyName")
data class StudentLocation(
    val student_id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    var speed: Double = 0.0,
    var minSpeed: Double = Double.MAX_VALUE,
    var maxSpeed: Double = 0.0
) {



}
