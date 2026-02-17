package com.example.voltroute.domain.model

/**
 * VehiclePreset - Predefined electric vehicle configurations
 *
 * Pure Kotlin domain model with no Android dependencies.
 * Represents real-world EV specs that users can select from.
 * Each preset can be converted to a Vehicle instance with current battery state.
 */
data class VehiclePreset(
    val id: String,
    val displayName: String,
    val manufacturer: String,
    val model: String,
    val batteryCapacityKWh: Double,
    val efficiencyKWhPerKm: Double,
    // Computed range assumes 80% usable battery capacity (industry standard)
    val rangeKm: Double = batteryCapacityKWh * 0.8 / efficiencyKWhPerKm
) {
    /**
     * Formatted range display for UI
     * Example: "~600 km range"
     */
    val displayRange: String
        get() = "~${rangeKm.toInt()} km range"

    /**
     * Formatted specifications for UI
     * Example: "135.0kWh • 0.18kWh/km"
     */
    val displaySpecs: String
        get() = "${batteryCapacityKWh}kWh • ${efficiencyKWhPerKm}kWh/km"

    /**
     * Converts preset to Vehicle instance with specified battery level
     * @param currentBatteryPercent Current state of charge (0-100)
     * @return Vehicle instance ready for route calculations
     */
    fun toVehicle(currentBatteryPercent: Double = 80.0): Vehicle {
        return Vehicle(
            batteryCapacityKWh = batteryCapacityKWh,
            efficiencyKWhPerKm = efficiencyKWhPerKm,
            currentBatteryPercent = currentBatteryPercent
        )
    }

    companion object {
        /**
         * Curated list of popular electric vehicles with real-world specs
         * Ordered by manufacturer, then model
         */
        val ALL: List<VehiclePreset> = listOf(
            VehiclePreset(
                id = "rivian_r1t",
                displayName = "Rivian R1T",
                manufacturer = "Rivian",
                model = "R1T",
                batteryCapacityKWh = 135.0,
                efficiencyKWhPerKm = 0.18
            ),
            VehiclePreset(
                id = "rivian_r1s",
                displayName = "Rivian R1S",
                manufacturer = "Rivian",
                model = "R1S",
                batteryCapacityKWh = 135.0,
                efficiencyKWhPerKm = 0.19
            ),
            VehiclePreset(
                id = "tesla_model3",
                displayName = "Tesla Model 3",
                manufacturer = "Tesla",
                model = "Model 3",
                batteryCapacityKWh = 75.0,
                efficiencyKWhPerKm = 0.14
            ),
            VehiclePreset(
                id = "tesla_modely",
                displayName = "Tesla Model Y",
                manufacturer = "Tesla",
                model = "Model Y",
                batteryCapacityKWh = 75.0,
                efficiencyKWhPerKm = 0.16
            ),
            VehiclePreset(
                id = "tesla_models",
                displayName = "Tesla Model S",
                manufacturer = "Tesla",
                model = "Model S",
                batteryCapacityKWh = 100.0,
                efficiencyKWhPerKm = 0.17
            ),
            VehiclePreset(
                id = "ford_mache",
                displayName = "Ford Mustang Mach-E",
                manufacturer = "Ford",
                model = "Mustang Mach-E",
                batteryCapacityKWh = 88.0,
                efficiencyKWhPerKm = 0.17
            ),
            VehiclePreset(
                id = "chevy_bolt",
                displayName = "Chevrolet Bolt EV",
                manufacturer = "Chevrolet",
                model = "Bolt EV",
                batteryCapacityKWh = 65.0,
                efficiencyKWhPerKm = 0.15
            ),
            VehiclePreset(
                id = "hyundai_ioniq6",
                displayName = "Hyundai IONIQ 6",
                manufacturer = "Hyundai",
                model = "IONIQ 6",
                batteryCapacityKWh = 77.4,
                efficiencyKWhPerKm = 0.14
            ),
            VehiclePreset(
                id = "kia_ev6",
                displayName = "Kia EV6",
                manufacturer = "Kia",
                model = "EV6",
                batteryCapacityKWh = 77.4,
                efficiencyKWhPerKm = 0.15
            ),
            VehiclePreset(
                id = "porsche_taycan",
                displayName = "Porsche Taycan",
                manufacturer = "Porsche",
                model = "Taycan",
                batteryCapacityKWh = 93.4,
                efficiencyKWhPerKm = 0.20
            )
        )

        /**
         * Default preset - Rivian R1T
         * Matches existing Vehicle() default specifications
         */
        val DEFAULT: VehiclePreset = ALL.first()
    }
}

