package com.example.voltroute.domain.model

data class Vehicle(
    val batteryCapacityKWh: Double = 135.0,  // Rivian R1T battery capacity
    val efficiencyKWhPerKm: Double = 0.18,   // Average EV efficiency
    val currentBatteryPercent: Double = 80.0  // Starting battery level
) {
    /**
     * Calculate current energy available in kWh
     */
    val currentEnergyKWh: Double
        get() = batteryCapacityKWh * (currentBatteryPercent / 100.0)

    /**
     * Calculate remaining range in kilometers
     */
    val remainingRangeKm: Double
        get() = currentEnergyKWh / efficiencyKWhPerKm
}