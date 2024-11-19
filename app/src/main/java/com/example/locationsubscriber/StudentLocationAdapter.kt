package com.example.locationsubscriber

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentLocationAdapter(private val studentLocations: List<StudentLocation>) :
    RecyclerView.Adapter<StudentLocationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val studentId: TextView = itemView.findViewById(R.id.studentId)
            val location: TextView = itemView.findViewById(R.id.Location)
            val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = studentLocations[position]
        holder.studentId.text = student.student_id
        val time = student.formatTimestamp()
        holder.location.text = "Lat: ${student.latitude}, Long: ${student.longitude}"
        holder.timestamp.text = "Time: $time"
    }

    override fun getItemCount(): Int {
        return studentLocations.size
    }



}