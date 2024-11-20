package com.example.locationsubscriber

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("PropertyName")
data class StudentLocation(
    val student_id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) {
    fun formatTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }


}
