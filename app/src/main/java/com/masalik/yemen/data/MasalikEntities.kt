package com.masalik.yemen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerNameAr: String = "سائق القمم",
    val selectedVehicleId: String = "SHAHIQ",
    val masalikCoins: Int = 2500,
    val driverXp: Int = 0,
    val drivingRealism: String = "SPORT",
    val graphicsQuality: String = "HIGH",
    val timeOfDay: String = "MORNING",
    val autoTimeCycleEnabled: Boolean = true,
    val worldTimeHours: Float = 9.0f,
    val weather: String = "CLEAR",
    val playerDistanceM: Float = 60f,
    val lastRegionId: String = "TAIZ_HIGHLANDS",
    val visitedCitiesJson: String = "[\"city_taiz_sabir\"]",
    val discoveredPoisJson: String = "[]",
    val totalCareerDistanceM: Float = 0f,
    val timestampIso: String = ""
)

@Entity(tableName = "garage_fleet")
data class GarageVehicleEntity(
    @PrimaryKey val vehicleId: String,
    val unlocked: Boolean,
    val fuelLiters: Float,
    val damageJson: String,
    val customizationJson: String
)

@Entity(tableName = "mission_records")
data class MissionRecordEntity(
    @PrimaryKey val missionId: String,
    val bestTimeSec: Float,
    val completionCount: Int
)

@Entity(tableName = "settings_hud")
data class SettingsHudEntity(
    @PrimaryKey val id: Int = 1,
    val hudSettingsJson: String,
    val audioMixerJson: String
)

@Entity(tableName = "radio_preferences")
data class RadioPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val radioSettingsJson: String
)

@Entity(tableName = "driving_logs")
data class DrivingTripLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampIso: String,
    val vehicleId: String,
    val regionId: String,
    val regionNameAr: String,
    val distanceKm: Float,
    val maxSpeedKmh: Float,
    val durationSec: Int
)
