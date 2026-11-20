package com.masalik.yemen.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasalikDao {

    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: PlayerProfileEntity)

    @Query("SELECT * FROM garage_fleet")
    fun observeGarageFleet(): Flow<List<GarageVehicleEntity>>

    @Query("SELECT * FROM garage_fleet")
    suspend fun getGarageFleetOnce(): List<GarageVehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGarageVehicles(vehicles: List<GarageVehicleEntity>)

    @Query("SELECT * FROM mission_records")
    fun observeMissionRecords(): Flow<List<MissionRecordEntity>>

    @Query("SELECT * FROM mission_records")
    suspend fun getMissionRecordsOnce(): List<MissionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMissionRecords(records: List<MissionRecordEntity>)

    @Query("SELECT * FROM settings_hud WHERE id = 1")
    suspend fun getHudSettingsOnce(): SettingsHudEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHudSettings(entity: SettingsHudEntity)

    @Query("SELECT * FROM radio_preferences WHERE id = 1")
    suspend fun getRadioPrefsOnce(): RadioPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRadioPrefs(entity: RadioPreferenceEntity)

    @Query("SELECT * FROM driving_logs ORDER BY id DESC LIMIT 20")
    fun observeRecentTrips(): Flow<List<DrivingTripLogEntity>>

    @Query("SELECT * FROM driving_logs ORDER BY id DESC LIMIT 20")
    suspend fun getRecentTripsOnce(): List<DrivingTripLogEntity>

    @Query("SELECT COUNT(*) FROM driving_logs")
    suspend fun getTotalTripsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripLog(entry: DrivingTripLogEntity)

    @Query("DELETE FROM player_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM garage_fleet")
    suspend fun clearGarage()

    @Query("DELETE FROM mission_records")
    suspend fun clearMissions()

    @Query("DELETE FROM settings_hud")
    suspend fun clearHudSettings()

    @Query("DELETE FROM radio_preferences")
    suspend fun clearRadioPrefs()

    @Query("DELETE FROM driving_logs")
    suspend fun clearTripLogs()
}
