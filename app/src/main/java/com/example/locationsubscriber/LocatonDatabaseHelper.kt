@file:Suppress("LocalVariableName")

package com.example.locationsubscriber

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper



class LocationDatabaseHelper(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION )
{


    companion object {
        private const val DATABASE_NAME =   "student_location_db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "student_locations"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_TIMESTAMP = "timestamp"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_STUDENT_ID TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_TIMESTAMP INTEGER
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
            $COLUMN_TIMESTAMP)
            VALUES(?, ?, ?, ?)"""

        db.execSQL(insertQuery,
            arrayOf(
                studentLocation.student_id,
                studentLocation.latitude,
                studentLocation.longitude,
                studentLocation.timestamp
            )
        )
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

            locationList.add(StudentLocation(studentId, latitude, longitude, timestamp))
        }

        cursor.close()
        db.close()
        return locationList
    }

    fun getMostRecentLocations(): List<StudentLocation> {
        val studentLocations = mutableListOf<StudentLocation>()
        val query = """
            SELECT $COLUMN_STUDENT_ID,
            $COLUMN_LATITUDE,
            $COLUMN_LONGITUDE,
            MAX($COLUMN_TIMESTAMP) AS latest_timestamp
            FROM $TABLE_NAME
            GROUP BY $COLUMN_STUDENT_ID
        """

        val db = readableDatabase
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("latest_timestamp"))

            studentLocations.add(StudentLocation(studentId, latitude, longitude, timestamp))
        }
        cursor.close()

        return studentLocations
    }

}