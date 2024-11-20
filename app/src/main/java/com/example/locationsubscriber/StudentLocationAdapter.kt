package com.example.locationsubscriber

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locationsubscriber.ColorUtils.getUniqueColorForStudent

class StudentLocationAdapter(
    private var studentLocations: MutableList<StudentLocation>,
    private val onStudentClick: (String)-> Unit
) : RecyclerView.Adapter<StudentLocationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val studentId: TextView = itemView.findViewById(R.id.studentId)
            val location: TextView = itemView.findViewById(R.id.Location)
            val timestamp: TextView = itemView.findViewById(R.id.timestamp)
            val viewStudentSummary: Button = itemView.findViewById(R.id.studentSummaryBtn)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_location, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = studentLocations[position]
        holder.studentId.text = student.student_id
        val time = student.formatTimestamp()
        holder.location.text = "Lat: ${student.latitude}, Long: ${student.longitude}"
        holder.timestamp.text = "Time: $time"
        val color = getUniqueColorForStudent(student.student_id)
        holder.studentId.setTextColor(color)
        holder.viewStudentSummary.setOnClickListener {
            onStudentClick(student.student_id)
        }
    }

    override fun getItemCount(): Int {
        return studentLocations.size
    }





}