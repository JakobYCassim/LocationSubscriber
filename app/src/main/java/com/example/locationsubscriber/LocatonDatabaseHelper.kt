@file:Suppress("LocalVariableName")

package com.example.locationsubscriber

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class LocationDatabaseHelper(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION )
{


    companion object {
        private const val DATABASE_NAME =   "student_location_db"
        private const val DATABASE_VERSION = 2

        const val TABLE_NAME = "student_locations"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SPEED = "speed"
        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_STUDENT_ID TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_SPEED REAL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertLocation(studentLocation: StudentLocation) {
        val db = writableDatabase
        val insertQuery = """INSERT INTO $TABLE_NAME (
            $COLUMN_STUDENT_ID,
            $COLUMN_LATITUDE,
            $COLUMN_LONGITUDE,
            $COLUMN_TIMESTAMP,
            $COLUMN_SPEED)
            VALUES(?, ?, ?, ?,?)"""

        db.execSQL(insertQuery,
            arrayOf(
                studentLocation.student_id,
                studentLocation.latitude,
                studentLocation.longitude,
                studentLocation.timestamp,
                studentLocation.speed
            )
        )
        Log.d("Database", "Student ID: ${studentLocation.student_id}")
        db.close()
    }

    fun getLocationsForStudent(student_id: String): List<StudentLocation> {
        val locationList = mutableListOf<StudentLocation>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(student_id))

        while(cursor.moveToNext()) {
            val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPEED))
            locationList.add(StudentLocation(studentId, latitude, longitude, timestamp, speed))
        }

        cursor.close()
        db.close()
        return locationList
    }

    fun getRecentLocationsList(lastMinutes: Int= 5): List<StudentLocation> {
        Log.d("Database", "Query called")
        val studentLocations = mutableListOf<StudentLocation>()
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - (lastMinutes * 60 * 1000)

        val query = """
            SELECT $COLUMN_STUDENT_ID,
            $COLUMN_LATITUDE,
            $COLUMN_LONGITUDE,
            MAX($COLUMN_TIMESTAMP) AS latest_timestamp,
            $COLUMN_SPEED
            FROM $TABLE_NAME
            WHERE timestamp > ?
            GROUP BY $COLUMN_STUDENT_ID
        """

        val db = readableDatabase
        val cursor = db.rawQuery(query, arrayOf(cutoffTime.toString()))
        Log.d("Database", "called query to create cursor")
        if(cursor.moveToFirst()) {
            Log.d("Database", "Cursor has first value")
            do {
                Log.d("Database", "Cursor move to next loop begins")
                val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("latest_timestamp"))
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPEED))
                studentLocations.add(StudentLocation(studentId, latitude, longitude, timestamp, speed))
                Log.d("Database", "Student ID: $studentId, Lat: $latitude, Long: $longitude, Speed: $speed")
            } while (cursor.moveToNext())
        }else {
            Log.e("Database", "No location data found")
        }
        cursor.close()

        return studentLocations
    }

    fun getRecentLocationsMap(lastMinutes: Int = 5): List<StudentLocation> {
        val db = this.readableDatabase
        val recentLocations = mutableListOf<StudentLocation>()

        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - (lastMinutes * 60 * 1000) // Convert minutes to milliseconds

        val query = """
        SELECT * FROM $TABLE_NAME
        WHERE timestamp > ?
    """
        val cursor = db.rawQuery(query, arrayOf(cutoffTime.toString()))

        if (cursor.moveToFirst()) {
            do {
                val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPEED))

                recentLocations.add(StudentLocation(studentId, latitude, longitude, timestamp, speed))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return recentLocations
    }


    fun getMaxSpeedForStudent(studentId: String): Double {
        val db = readableDatabase
        val query = "SELECT MAX($COLUMN_SPEED) FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(studentId))
        var maxSpeed = 0.0
        if (cursor.moveToFirst()) {
            maxSpeed = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return maxSpeed
    }

    fun getMinSpeedForStudent(studentId: String): Double {
        val db = readableDatabase
        val query = "SELECT MIN($COLUMN_SPEED) FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(studentId))
        var minSpeed = 0.0
        if (cursor.moveToFirst()) {
            minSpeed = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return minSpeed
    }

    fun getEarliestDateForStudent(studentId: String): Long? {
        val db = this.readableDatabase
        val queryEarliest = "SELECT MIN($COLUMN_TIMESTAMP) FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursorEarliest = db.rawQuery(queryEarliest, arrayOf(studentId))

        var earliestDate: Long? = null
        if (cursorEarliest.moveToFirst()) {
            earliestDate = cursorEarliest.getLong(0) // This will give the earliest date as a String
        }
        cursorEarliest.close()
        return earliestDate
    }

    fun getLatestDateForStudent(studentId: String): Long? {
        val db = this.readableDatabase
        val queryLatest = "SELECT MAX($COLUMN_TIMESTAMP) FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursorLatest = db.rawQuery(queryLatest, arrayOf(studentId))

        var latestDate: Long? = null
        if (cursorLatest.moveToFirst()) {
            latestDate = cursorLatest.getLong(0) // This will give the latest date as a String
        }
        cursorLatest.close()
        return latestDate
    }

    fun getAverageSpeedForStudent(studentId: String): Double {
        val db = this.readableDatabase
        val query = "SELECT AVG($COLUMN_SPEED) FROM $TABLE_NAME WHERE $COLUMN_STUDENT_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(studentId))

        var averageSpeed = 0.0
        if (cursor.moveToFirst()) {
            averageSpeed = cursor.getDouble(0) // Get the average speed value
        }
        cursor.close()
        return averageSpeed
    }

}