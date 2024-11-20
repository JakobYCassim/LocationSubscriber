package com.example.locationsubscriber

import android.graphics.Color

object ColorUtils {
    private val studentColorMap = mutableMapOf<String, Int>()

    fun getUniqueColorForStudent(studentId: String): Int {
        return studentColorMap.getOrPut(studentId) {
            // Generate a color from the hash of the studentId

            val hash = studentId.hashCode()

            // Step 2: Break the hash into separate byte-sized chunks for each RGB component
            val partA = (hash and 0xFF0000) shr 16
            val partB = (hash and 0x00FF00) shr 8
            val partC = hash and 0x0000FF

            // Step 3: Randomly assign the parts to R, G, and B channels
            val rgbParts = listOf(partA, partB, partC).shuffled()

            // Step 4: Apply the randomized parts to R, G, B
            val r = (rgbParts[0] + 100) % 256  // Apply offset and modulus for red
            val g = (rgbParts[1] + 150) % 256  // Apply offset and modulus for green
            val b = (rgbParts[2] + 200) % 256  // Apply offset and modulus for blue

            // Step 5: Return the final color using the randomized values
            Color.rgb(r, g, b)
        }
    }
}