package com.masalik.yemen.data

import android.content.Context
import com.masalik.yemen.game.AudioMixerSettings
import com.masalik.yemen.game.DamageState
import com.masalik.yemen.game.DrivingRealismMode
import com.masalik.yemen.game.DrivingTripLog
import com.masalik.yemen.game.GraphicsQuality
import com.masalik.yemen.game.HudCustomizationSettings
import com.masalik.yemen.game.PhoneDatabaseStats
import com.masalik.yemen.game.RadioPlayerSettings
import com.masalik.yemen.game.RegionId
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.TimeOfDayPreset
import com.masalik.yemen.game.VehicleCustomization
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.game.VehicleId
import com.masalik.yemen.game.WeatherPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class MasalikRepository(
    private val context: Context,
    private val dao: MasalikDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val recentTripsFlow: Flow<List<DrivingTripLog>> = dao.observeRecentTrips().map { list ->
        list.map { e ->
            DrivingTripLog(
                id = e.id,
                timestampIso = e.timestampIso,
                vehicleId = runCatching { VehicleId.valueOf(e.vehicleId) }.getOrDefault(VehicleId.SHAHIQ),
                regionId = runCatching { RegionId.valueOf(e.regionId) }.getOrDefault(RegionId.TAIZ_HIGHLANDS),
                regionNameAr = e.regionNameAr,
                distanceKm = e.distanceKm,
                maxSpeedKmh = e.maxSpeedKmh,
                durationSec = e.durationSec
            )
        }
    }

    suspend fun loadSaveGame(): SaveGameSchema = withContext(Dispatchers.IO) {
        val profile = dao.getProfileOnce() ?: return@withContext SaveGameSchema(
            timestampIso = Instant.now().toString()
        ).also { persistSaveGame(it) }

        val fleetRows = dao.getGarageFleetOnce()
        val missionRows = dao.getMissionRecordsOnce()
        val hudRow = dao.getHudSettingsOnce()
        val radioRow = dao.getRadioPrefsOnce()

        val unlocked = fleetRows.filter { it.unlocked }
            .mapNotNull { runCatching { VehicleId.valueOf(it.vehicleId) }.getOrNull() }
            .ifEmpty { listOf(VehicleId.SHAHIQ) }

        val defaultCustom = VehicleFleetCatalog.defaultCustomizations().toMutableMap()
        val defaultFuel = VehicleFleetCatalog.defaultFuelMap().toMutableMap()
        val defaultDamage = VehicleFleetCatalog.defaultDamageMap().toMutableMap()

        for (row in fleetRows) {
            val vid = runCatching { VehicleId.valueOf(row.vehicleId) }.getOrNull() ?: continue
            defaultFuel[vid] = row.fuelLiters
            runCatching { json.decodeFromString<DamageState>(row.damageJson) }.getOrNull()?.let {
                defaultDamage[vid] = it
            }
            runCatching { json.decodeFromString<VehicleCustomization>(row.customizationJson) }.getOrNull()?.let {
                defaultCustom[vid] = it
            }
        }

        val hudSettings = hudRow?.let {
            runCatching { json.decodeFromString<HudCustomizationSettings>(it.hudSettingsJson) }.getOrNull()
        } ?: HudCustomizationSettings()

        val audioMixer = hudRow?.let {
            runCatching { json.decodeFromString<AudioMixerSettings>(it.audioMixerJson) }.getOrNull()
        } ?: AudioMixerSettings()

        val radioSettings = radioRow?.let {
            runCatching { json.decodeFromString<RadioPlayerSettings>(it.radioSettingsJson) }.getOrNull()
        } ?: RadioPlayerSettings()

        val visitedCities = runCatching { json.decodeFromString<List<String>>(profile.visitedCitiesJson) }
            .getOrDefault(listOf("city_taiz_sabir"))
        val discoveredPois = runCatching { json.decodeFromString<List<String>>(profile.discoveredPoisJson) }
            .getOrDefault(emptyList())

        SaveGameSchema(
            schemaVersion = 2,
            timestampIso = profile.timestampIso,
            playerNameAr = profile.playerNameAr,
            selectedVehicleId = runCatching { VehicleId.valueOf(profile.selectedVehicleId) }.getOrDefault(VehicleId.SHAHIQ),
            unlockedVehicles = unlocked,
            masalikCoins = profile.masalikCoins,
            driverXp = profile.driverXp,
            drivingRealism = runCatching { DrivingRealismMode.valueOf(profile.drivingRealism) }.getOrDefault(DrivingRealismMode.SPORT),
            graphicsQuality = runCatching { GraphicsQuality.valueOf(profile.graphicsQuality) }.getOrDefault(GraphicsQuality.HIGH),
            hudSettings = hudSettings,
            audioMixer = audioMixer,
            radioSettings = radioSettings,
            timeOfDay = runCatching { TimeOfDayPreset.valueOf(profile.timeOfDay) }.getOrDefault(TimeOfDayPreset.MORNING),
            autoTimeCycleEnabled = profile.autoTimeCycleEnabled,
            worldTimeHours = profile.worldTimeHours,
            weather = runCatching { WeatherPreset.valueOf(profile.weather) }.getOrDefault(WeatherPreset.CLEAR),
            playerDistanceM = profile.playerDistanceM,
            lastRegionId = runCatching { RegionId.valueOf(profile.lastRegionId) }.getOrDefault(RegionId.TAIZ_HIGHLANDS),
            visitedCities = visitedCities,
            fuelByVehicle = defaultFuel,
            damageByVehicle = defaultDamage,
            customizations = defaultCustom,
            discoveredPois = discoveredPois,
            missionBestTimesSec = missionRows.associate { it.missionId to it.bestTimeSec },
            missionCompletions = missionRows.associate { it.missionId to it.completionCount },
            totalCareerDistanceM = profile.totalCareerDistanceM
        )
    }

    suspend fun persistSaveGame(save: SaveGameSchema) = withContext(Dispatchers.IO) {
        val nowIso = Instant.now().toString()
        dao.upsertProfile(
            PlayerProfileEntity(
                id = 1,
                playerNameAr = save.playerNameAr,
                selectedVehicleId = save.selectedVehicleId.name,
                masalikCoins = save.masalikCoins,
                driverXp = save.driverXp,
                drivingRealism = save.drivingRealism.name,
                graphicsQuality = save.graphicsQuality.name,
                timeOfDay = save.timeOfDay.name,
                autoTimeCycleEnabled = save.autoTimeCycleEnabled,
                worldTimeHours = save.worldTimeHours,
                weather = save.weather.name,
                playerDistanceM = save.playerDistanceM,
                lastRegionId = save.lastRegionId.name,
                visitedCitiesJson = json.encodeToString(save.visitedCities),
                discoveredPoisJson = json.encodeToString(save.discoveredPois),
                totalCareerDistanceM = save.totalCareerDistanceM,
                timestampIso = nowIso
            )
        )

        val fleetEntities = VehicleId.entries.map { vid ->
            val spec = VehicleFleetCatalog.specOf(vid)
            val custom = save.customizations[vid] ?: VehicleFleetCatalog.defaultCustomizations().getValue(vid)
            val dmg = save.damageByVehicle[vid] ?: DamageState()
            GarageVehicleEntity(
                vehicleId = vid.name,
                unlocked = vid in save.unlockedVehicles,
                fuelLiters = save.fuelByVehicle[vid] ?: spec.fuelTankLiters,
                damageJson = json.encodeToString(dmg),
                customizationJson = json.encodeToString(custom)
            )
        }
        dao.upsertGarageVehicles(fleetEntities)

        val missionIds = (save.missionCompletions.keys + save.missionBestTimesSec.keys).distinct()
        if (missionIds.isNotEmpty()) {
            dao.upsertMissionRecords(
                missionIds.map { mid ->
                    MissionRecordEntity(
                        missionId = mid,
                        bestTimeSec = save.missionBestTimesSec[mid] ?: 0f,
                        completionCount = save.missionCompletions[mid] ?: 0
                    )
                }
            )
        }

        dao.upsertHudSettings(
            SettingsHudEntity(
                id = 1,
                hudSettingsJson = json.encodeToString(save.hudSettings),
                audioMixerJson = json.encodeToString(save.audioMixer)
            )
        )
        dao.upsertRadioPrefs(
            RadioPreferenceEntity(
                id = 1,
                radioSettingsJson = json.encodeToString(save.radioSettings)
            )
        )
    }

    suspend fun logDrivingTrip(trip: DrivingTripLog) = withContext(Dispatchers.IO) {
        dao.insertTripLog(
            DrivingTripLogEntity(
                timestampIso = trip.timestampIso,
                vehicleId = trip.vehicleId.name,
                regionId = trip.regionId.name,
                regionNameAr = trip.regionNameAr,
                distanceKm = trip.distanceKm,
                maxSpeedKmh = trip.maxSpeedKmh,
                durationSec = trip.durationSec
            )
        )
    }

    suspend fun resetAllData(): SaveGameSchema = withContext(Dispatchers.IO) {
        dao.clearProfile()
        dao.clearGarage()
        dao.clearMissions()
        dao.clearHudSettings()
        dao.clearRadioPrefs()
        dao.clearTripLogs()
        val fresh = SaveGameSchema(timestampIso = Instant.now().toString())
        persistSaveGame(fresh)
        fresh
    }

    suspend fun getDatabaseStats(currentSave: SaveGameSchema): PhoneDatabaseStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(MasalikDatabase.DB_NAME)
        val sizeKb = if (dbFile.exists()) (dbFile.length() / 1024L).coerceAtLeast(12L) else 16L
        val trips = dao.getRecentTripsOnce().map { e ->
            DrivingTripLog(
                id = e.id,
                timestampIso = e.timestampIso,
                vehicleId = runCatching { VehicleId.valueOf(e.vehicleId) }.getOrDefault(VehicleId.SHAHIQ),
                regionId = runCatching { RegionId.valueOf(e.regionId) }.getOrDefault(RegionId.TAIZ_HIGHLANDS),
                regionNameAr = e.regionNameAr,
                distanceKm = e.distanceKm,
                maxSpeedKmh = e.maxSpeedKmh,
                durationSec = e.durationSec
            )
        }
        PhoneDatabaseStats(
            dbName = MasalikDatabase.DB_NAME,
            schemaVersion = 2,
            engineLabelAr = "Room SQLite (محلي 100% على الهاتف)",
            storageUsedKb = sizeKb,
            totalTripsCount = dao.getTotalTripsCount(),
            completedMissionsCount = currentSave.missionCompletions.values.count { it > 0 },
            unlockedVehiclesCount = currentSave.unlockedVehicles.size,
            recentTrips = trips,
            lastSavedIso = currentSave.timestampIso
        )
    }

    fun exportSaveJson(save: SaveGameSchema): String = json.encodeToString(save)

    fun importSaveJson(raw: String): Result<SaveGameSchema> = runCatching {
        json.decodeFromString<SaveGameSchema>(raw)
    }
}
