package com.example.data.model

data class Report(
    val id: String = "",
    val stationName: String = "",
    val fuelType: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val notes: String = "",
    val userId: String = "",
    val userName: String = "",
    val userUsername: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
