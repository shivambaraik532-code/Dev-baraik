package com.example.ui

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

object FilterUtils {
    val FilterList = listOf("Normal", "Clarendon", "Sepia", "Mono", "Vintage", "Warm", "Cool")

    fun getColorFilter(filterName: String): ColorFilter? {
        return when (filterName) {
            "Sepia" -> {
                val matrix = ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            "Clarendon" -> {
                val matrix = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, -0.05f,
                    0f, 1.05f, 0f, 0f, -0.05f,
                    0f, 0f, 1.25f, 0f, 0.05f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            "Mono" -> {
                val matrix = ColorMatrix().apply {
                    setToSaturation(0f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            "Vintage" -> {
                val matrix = ColorMatrix(floatArrayOf(
                    0.9f, 0.1f, 0.1f, 0f, 0.05f,
                    0.1f, 0.8f, 0.1f, 0f, 0.05f,
                    0.05f, 0.05f, 0.6f, 0f, 0.1f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            "Warm" -> {
                val matrix = ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 0.1f,
                    0f, 1.0f, 0f, 0f, 0.05f,
                    0f, 0f, 0.8f, 0f, -0.1f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            "Cool" -> {
                val matrix = ColorMatrix(floatArrayOf(
                    0.8f, 0f, 0f, 0f, -0.1f,
                    0f, 1.0f, 0f, 0f, 0.05f,
                    0f, 0f, 1.3f, 0f, 0.1f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            else -> null // Normal - no filter
        }
    }
}
