package com.example.locationsubscriber

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocationUtils {
    fun calculateSpeed(
        lat1: Double,
        lon1: Double,
        timestamp1:Long,
        lat2: Double,
        lon2: Double,
        timestamp2:Long
): Double {
        val previousLocation = Location("previous").apply {
            latitude = lat1
            longitude = lon1
        }

        val currentLocation = Location("current").apply {
            latitude = lat2
            longitude = lon2
        }

        val distance = previousLocation.distanceTo(currentLocation)

        val timeElapsed = (timestamp2 - timestamp1) / 1000.0

        return if (timeElapsed > 0) {
            (distance / timeElapsed)
        }else {
            0.0
        }
    }

    fun convertToStudentLocation(incomingMessage: IncomingMessage): StudentLocation {
        val studentLocation = StudentLocation(
            incomingMessage.student_id,
            incomingMessage.latitude,
            incomingMessage.longitude,
            incomingMessage.timestamp
        )
        return studentLocation
    }

    fun formatTimestamp(timestamp: Long?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp!!))
    }
}