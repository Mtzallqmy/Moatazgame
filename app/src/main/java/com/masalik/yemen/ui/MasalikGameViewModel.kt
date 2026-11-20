package com.masalik.yemen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masalik.yemen.audio.MasalikAudioEngine
import com.masalik.yemen.data.MasalikDatabase
import com.masalik.yemen.data.MasalikRepository
import com.masalik.yemen.game.AudioMixerSettings
import com.masalik.yemen.game.AutomaticShifterState
import com.masalik.yemen.game.CameraMode
import com.masalik.yemen.game.ControlInputs
import com.masalik.yemen.game.DrivingRealismMode
import com.masalik.yemen.game.DrivingTripLog
import com.masalik.yemen.game.EngineeringTestResult
import com.masalik.yemen.game.GraphicsQuality
import com.masalik.yemen.game.HudCustomizationSettings
import com.masalik.yemen.game.LivingWorldSimulation
import com.masalik.yemen.game.MissionDefinition
import com.masalik.yemen.game.MissionsCatalog
import com.masalik.yemen.game.NearbyTrafficSignalInfo
import com.masalik.yemen.game.PhoneDatabaseStats
import com.masalik.yemen.game.PhotoColorFilter
import com.masalik.yemen.game.RadioStationId
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.TimeOfDayPreset
import com.masalik.yemen.game.TouchControlMode
import com.masalik.yemen.game.VehicleCustomization
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.game.VehicleId
import com.masalik.yemen.game.VehiclePhysicsEngine
import com.masalik.yemen.game.VehicleTelemetry
import com.masalik.yemen.game.WeatherPreset
import com.masalik.yemen.game.YemenCityHub
import com.masalik.yemen.input.GyroSteeringController
import com.masalik.yemen.renderer.MasalikGLRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class ActiveModal {
    MENU, DRIVING, PAUSE, WORLD_MAP, GARAGE, MISSIONS, CONTROL_STUDIO, SETTINGS, PHOTO
}

data class MasalikUiState(
    val saveState: SaveGameSchema = SaveGameSchema(),
    val activeModal: ActiveModal = ActiveModal.MENU,
    val telemetry: VehicleTelemetry = VehicleTelemetry(),
    val nearbySignal: NearbyTrafficSignalInfo? = null,
    val cameraMode: CameraMode = CameraMode.CHASE_NEAR,
    val cleanCinematicHud: Boolean = false,
    val previewVehicleId: VehicleId = VehicleId.SHAHIQ,
    val activeMission: MissionDefinition? = MissionsCatalog.ALL_OFFLINE_MISSIONS.firstOrNull(),
    val missionTimeRemainingSec: Int = 95,
    val missionBannerAr: String? = null,
    val radioOpen: Boolean = false,
    val displayClockStr: String = "09:00",
    val activeTimePhase: TimeOfDayPreset = TimeOfDayPreset.MORNING,
    val photoFilter: PhotoColorFilter = PhotoColorFilter.NATURAL,
    val photoFovDeg: Float = 58f,
    val fps: Int = 60,
    val dbStats: PhoneDatabaseStats = PhoneDatabaseStats(),
    val diagnosticResults: List<EngineeringTestResult> = emptyList(),
    val toastMessageAr: String? = null
)

class MasalikGameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MasalikDatabase.getInstance(application)
    private val repository = MasalikRepository(application, database.masalikDao())

    val physics = VehiclePhysicsEngine()
    val livingWorld = LivingWorldSimulation()
    val audioEngine = MasalikAudioEngine()
    val gyroController = GyroSteeringController(application)

    private val _uiState = MutableStateFlow(MasalikUiState())
    val uiState: StateFlow<MasalikUiState> = _uiState.asStateFlow()

    // Lock-free volatile touch inputs read directly by the 60fps GL render thread
    @Volatile var inputSteerNorm: Float = 0f
    @Volatile var inputThrottle: Float = 0f
    @Volatile var inputBrake: Float = 0f
    @Volatile var inputHandbrake: Boolean = false
    @Volatile var showroomOrbitRad: Float = 0.65f

    private var worldTimeHours: Float = 9.0f
    private var missionElapsedSec: Float = 0f
    private var uiSyncAccumulator: Float = 0f
    private var autoSaveAccumulator: Float = 0f
    private var fpsFrameCount: Int = 0
    private var fpsAccumulator: Float = 0f
    private var currentFps: Int = 60
    private var blinkerTimer: Float = 0f
    private var blinkerPhase: Boolean = false

    private var tripStartDistM: Float = 0f
    private var tripMaxSpeedKmh: Float = 0f
    private var tripStartTimeMs: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            val loaded = repository.loadSaveGame()
            worldTimeHours = loaded.worldTimeHours
            applySaveToEngines(loaded)
            val stats = repository.getDatabaseStats(loaded)
            _uiState.update {
                it.copy(
                    saveState = loaded,
                    previewVehicleId = loaded.selectedVehicleId,
                    radioOpen = loaded.radioSettings.isOpen,
                    displayClockStr = LivingWorldSimulation.formatWorldTime(worldTimeHours),
                    activeTimePhase = LivingWorldSimulation.hoursToPreset(worldTimeHours),
                    dbStats = stats
                )
            }
        }
    }

    private fun applySaveToEngines(save: SaveGameSchema) {
        val custom = save.customizations[save.selectedVehicleId]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(save.selectedVehicleId)
        physics.selectVehicle(
            id = save.selectedVehicleId,
            custom = custom,
            savedFuel = save.fuelByVehicle[save.selectedVehicleId],
            savedDamage = save.damageByVehicle[save.selectedVehicleId]
        )
        physics.realismMode = save.drivingRealism
        physics.weather = save.weather
        physics.wheelRotationRangeDeg = save.hudSettings.smartWheel.rotationRangeDeg
        physics.steeringAssistEnabled = save.hudSettings.smartWheel.steeringAssist
        physics.resetToRoad(save.playerDistanceM)

        audioEngine.mixer = save.audioMixer
        audioEngine.radioSettings = save.radioSettings
        audioEngine.engineBaseFreqHz = physics.spec.engineBaseFreqHz
        audioEngine.weather = save.weather
    }

    fun onResumeLifecycle() {
        audioEngine.start()
        if (_uiState.value.saveState.hudSettings.controlMode == TouchControlMode.TILT) {
            gyroController.start()
        }
    }

    fun onPauseLifecycle() {
        gyroController.stop()
        audioEngine.stop()
        saveSnapshotNow()
    }

    override fun onCleared() {
        super.onCleared()
        gyroController.stop()
        audioEngine.stop()
    }

    /** Called every frame on the OpenGL thread (~60fps) to step simulation & build render snapshot. */
    fun stepSimulationFrame(dt: Float): MasalikGLRenderer.RenderFrameSnapshot {
        val state = _uiState.value
        val modal = state.activeModal
        val isDriving = modal == ActiveModal.DRIVING
        val isLobbyOrGarage = modal == ActiveModal.MENU || modal == ActiveModal.GARAGE

        fpsFrameCount++
        fpsAccumulator += dt
        if (fpsAccumulator >= 0.7f) {
            currentFps = (fpsFrameCount / fpsAccumulator).toInt().coerceIn(15, 120)
            fpsFrameCount = 0
            fpsAccumulator = 0f
        }

        blinkerTimer += dt
        if (blinkerTimer >= 0.42f) {
            blinkerTimer = 0f
            blinkerPhase = !blinkerPhase
        }

        if (isLobbyOrGarage) {
            if (modal == ActiveModal.MENU) {
                showroomOrbitRad = (showroomOrbitRad + dt * 0.28f) % (2f * kotlin.math.PI.toFloat())
            }
        }

        // Continuous 24h day/night clock
        if (state.saveState.autoTimeCycleEnabled && isDriving) {
            worldTimeHours = (worldTimeHours + dt * 0.045f) % 24f
        }

        val atmo = LivingWorldSimulation.sampleAtmosphere(worldTimeHours, state.saveState.weather)

        val telemetry: VehicleTelemetry
        if (isDriving) {
            val effectiveSteer = if (state.saveState.hudSettings.controlMode == TouchControlMode.TILT) {
                gyroController.steerNorm
            } else {
                inputSteerNorm
            }
            val inputs = ControlInputs(
                throttle = inputThrottle,
                brake = inputBrake,
                steer = effectiveSteer,
                handbrake = inputHandbrake
            )
            livingWorld.update(dt, physics.distanceAlongRoadM, physics.lateralOffsetM, physics.forwardSpeedMs, atmo.nightFactor)
            telemetry = physics.step(dt, inputs, livingWorld)

            if (physics.lastCollisionImpulse > 2.0f) {
                audioEngine.triggerCollision((physics.lastCollisionImpulse / 14f).coerceIn(0.25f, 1f))
            }
            if (telemetry.speedKmh > tripMaxSpeedKmh) tripMaxSpeedKmh = telemetry.speedKmh

            // Mission progress evaluation
            val mission = state.activeMission
            if (mission != null) {
                missionElapsedSec += dt
                val reachedFinish = telemetry.distanceAlongRoadM >= mission.finishDistanceM - 15f
                if (reachedFinish) {
                    completeActiveMission(mission, missionElapsedSec)
                }
            }

            // Periodic auto-save every 15s while driving
            autoSaveAccumulator += dt
            if (autoSaveAccumulator >= 15f) {
                autoSaveAccumulator = 0f
                saveSnapshotNow()
            }
        } else {
            livingWorld.update(dt * 0.45f, physics.distanceAlongRoadM, physics.lateralOffsetM, 0f, atmo.nightFactor)
            telemetry = physics.step(0.004f, ControlInputs(handbrake = true), null)
        }

        // Update live audio synthesizer parameters
        audioEngine.drivingActive = isDriving
        audioEngine.engineRunning = telemetry.engineRunning
        audioEngine.engineBaseFreqHz = physics.spec.engineBaseFreqHz
        audioEngine.engineRpm = telemetry.rpm
        audioEngine.throttle = telemetry.throttle
        audioEngine.speedKmh = telemetry.speedKmh
        audioEngine.surface = telemetry.surface
        audioEngine.weather = state.saveState.weather

        // Sync UI StateFlow at ~20Hz so Compose HUD stays buttery smooth with low recomposition cost
        uiSyncAccumulator += dt
        if (uiSyncAccumulator >= 0.05f) {
            uiSyncAccumulator = 0f
            val mission = _uiState.value.activeMission
            val remSec = if (mission != null) (mission.targetTimeSec - missionElapsedSec.toInt()).coerceAtLeast(0) else 0
            _uiState.update {
                it.copy(
                    telemetry = telemetry,
                    nearbySignal = livingWorld.nearbyTrafficSignal,
                    displayClockStr = LivingWorldSimulation.formatWorldTime(worldTimeHours),
                    activeTimePhase = LivingWorldSimulation.hoursToPreset(worldTimeHours),
                    missionTimeRemainingSec = remSec,
                    fps = currentFps
                )
            }
        }

        val currentSave = _uiState.value.saveState
        val activeCustom = currentSave.customizations[telemetry.vehicleId]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(telemetry.vehicleId)
        val previewId = _uiState.value.previewVehicleId
        val previewCustom = currentSave.customizations[previewId]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(previewId)

        return MasalikGLRenderer.RenderFrameSnapshot(
            telemetry = telemetry,
            customization = activeCustom,
            previewVehicleId = previewId,
            previewCustomization = previewCustom,
            isLobbyOrGarage = isLobbyOrGarage,
            showroomOrbitRad = showroomOrbitRad,
            cameraMode = _uiState.value.cameraMode,
            photoFovDeg = _uiState.value.photoFovDeg,
            worldTimeHours = worldTimeHours,
            weather = currentSave.weather,
            livingWorld = livingWorld,
            finishDistanceM = _uiState.value.activeMission?.finishDistanceM,
            blinkerOn = blinkerPhase
        )
    }

    private fun completeActiveMission(mission: MissionDefinition, elapsedSec: Float) {
        missionElapsedSec = 0f
        physics.configureAiRival(false)
        val current = _uiState.value.saveState
        val nextCoins = current.masalikCoins + mission.rewardCoins
        val nextXp = current.driverXp + mission.rewardXp
        val prevBest = current.missionBestTimesSec[mission.id]
        val newBest = if (prevBest == null || elapsedSec < prevBest) elapsedSec else prevBest
        val prevCount = current.missionCompletions[mission.id] ?: 0

        val updatedSave = current.copy(
            masalikCoins = nextCoins,
            driverXp = nextXp,
            missionBestTimesSec = current.missionBestTimesSec + (mission.id to newBest),
            missionCompletions = current.missionCompletions + (mission.id to (prevCount + 1))
        )
        commitSave(updatedSave)
        _uiState.update {
            it.copy(
                activeMission = null,
                missionBannerAr = "أحسنت! أنجزت «${mission.titleAr}» وحصلت على +${mission.rewardCoins} ر.ي و +${mission.rewardXp} XP"
            )
        }
    }

    fun dismissMissionBanner() = _uiState.update { it.copy(missionBannerAr = null) }
    fun dismissToast() = _uiState.update { it.copy(toastMessageAr = null) }

    fun setModal(modal: ActiveModal) {
        if (modal == ActiveModal.DRIVING && _uiState.value.activeModal != ActiveModal.DRIVING) {
            tripStartDistM = physics.totalDistanceDrivenM
            tripMaxSpeedKmh = 0f
            tripStartTimeMs = System.currentTimeMillis()
            if (_uiState.value.saveState.hudSettings.controlMode == TouchControlMode.TILT) {
                gyroController.start()
            }
        } else if (modal != ActiveModal.DRIVING && _uiState.value.activeModal == ActiveModal.DRIVING) {
            recordCurrentTripIfSignificant()
            inputThrottle = 0f
            inputBrake = 0f
            inputSteerNorm = 0f
            audioEngine.hornActive = false
            saveSnapshotNow()
        }
        if (modal == ActiveModal.SETTINGS) {
            refreshDbStats()
        }
        _uiState.update { it.copy(activeModal = modal) }
    }

    private fun recordCurrentTripIfSignificant() {
        val drivenKm = (physics.totalDistanceDrivenM - tripStartDistM) / 1000f
        if (drivenKm < 0.08f) return
        val durSec = ((System.currentTimeMillis() - tripStartTimeMs) / 1000L).toInt().coerceAtLeast(1)
        val region = RoadSplineNetwork.getRegionAt(physics.distanceAlongRoadM)
        val log = DrivingTripLog(
            timestampIso = Instant.now().toString(),
            vehicleId = physics.vehicleId,
            regionId = region.id,
            regionNameAr = region.nameAr,
            distanceKm = drivenKm,
            maxSpeedKmh = tripMaxSpeedKmh,
            durationSec = durSec
        )
        viewModelScope.launch {
            repository.logDrivingTrip(log)
            refreshDbStats()
        }
    }

    fun startMission(mission: MissionDefinition) {
        val current = _uiState.value.saveState
        val chosenVehicle = if (mission.recommendedVehicleId in current.unlockedVehicles) {
            mission.recommendedVehicleId
        } else current.selectedVehicleId

        val custom = current.customizations[chosenVehicle]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(chosenVehicle)
        physics.selectVehicle(chosenVehicle, custom, current.fuelByVehicle[chosenVehicle], current.damageByVehicle[chosenVehicle])
        physics.weather = mission.weatherPreset
        physics.resetToRoad(mission.startDistanceM)
        physics.configureAiRival(mission.hasAiRival, mission.startDistanceM, mission.aiRivalSpeedMs)
        worldTimeHours = mission.timeOfDayPreset.hours
        missionElapsedSec = 0f

        val nextSave = current.copy(
            selectedVehicleId = chosenVehicle,
            weather = mission.weatherPreset,
            timeOfDay = mission.timeOfDayPreset,
            worldTimeHours = worldTimeHours,
            playerDistanceM = mission.startDistanceM
        )
        commitSave(nextSave)
        _uiState.update {
            it.copy(
                activeMission = mission,
                activeModal = ActiveModal.DRIVING,
                missionTimeRemainingSec = mission.targetTimeSec,
                missionBannerAr = mission.dialogueBriefAr ?: "بدأت المهمة: ${mission.titleAr}"
            )
        }
    }

    fun startFreeRoam() {
        physics.configureAiRival(false)
        _uiState.update { it.copy(activeMission = null, activeModal = ActiveModal.DRIVING) }
    }

    fun fastTravelToCity(city: YemenCityHub) {
        physics.resetToRoad(city.distanceM)
        physics.weather = city.defaultWeather
        worldTimeHours = city.defaultTimeOfDay.hours
        val current = _uiState.value.saveState
        val visited = (current.visitedCities + city.id).distinct()
        val updated = current.copy(
            playerDistanceM = city.distanceM,
            lastRegionId = city.regionId,
            visitedCities = visited,
            weather = city.defaultWeather,
            timeOfDay = city.defaultTimeOfDay,
            worldTimeHours = worldTimeHours
        )
        commitSave(updated)
        _uiState.update {
            it.copy(
                activeModal = ActiveModal.DRIVING,
                toastMessageAr = "تم الانتقال السريع إلى ${city.nameAr}"
            )
        }
    }

    // Garage & Customization
    fun selectPreviewVehicle(id: VehicleId) = _uiState.update { it.copy(previewVehicleId = id) }

    fun orbitShowroom(deltaRad: Float) {
        showroomOrbitRad = (showroomOrbitRad + deltaRad) % (2f * kotlin.math.PI.toFloat())
    }

    fun confirmSelectVehicle(id: VehicleId) {
        val current = _uiState.value.saveState
        if (id !in current.unlockedVehicles) return
        val custom = current.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        physics.selectVehicle(id, custom, current.fuelByVehicle[id], current.damageByVehicle[id])
        commitSave(current.copy(selectedVehicleId = id))
        _uiState.update { it.copy(previewVehicleId = id, toastMessageAr = "تم تجهيز مركبة «${VehicleFleetCatalog.specOf(id).nameAr}»") }
    }

    fun unlockVehicle(id: VehicleId) {
        val spec = VehicleFleetCatalog.specOf(id)
        val current = _uiState.value.saveState
        if (id in current.unlockedVehicles || current.masalikCoins < spec.unlockPriceCoins) return
        val updated = current.copy(
            masalikCoins = current.masalikCoins - spec.unlockPriceCoins,
            unlockedVehicles = (current.unlockedVehicles + id).distinct(),
            selectedVehicleId = id
        )
        commitSave(updated)
        val custom = updated.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        physics.selectVehicle(id, custom, updated.fuelByVehicle[id], updated.damageByVehicle[id])
        _uiState.update { it.copy(previewVehicleId = id, toastMessageAr = "مبارك! تم شراء مركبة «${spec.nameAr}»") }
    }

    fun updateVehicleCustomization(id: VehicleId, custom: VehicleCustomization) {
        val current = _uiState.value.saveState
        val updated = current.copy(customizations = current.customizations + (id to custom))
        if (physics.vehicleId == id) physics.updateCustomization(custom)
        commitSave(updated)
    }

    fun upgradeMechanicalPart(id: VehicleId, part: String, cost: Int) {
        val current = _uiState.value.saveState
        if (current.masalikCoins < cost) return
        val old = current.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        val next = when (part) {
            "engine" -> old.copy(engineUpgradeLevel = (old.engineUpgradeLevel + 1).coerceAtMost(5))
            "transmission" -> old.copy(transmissionUpgradeLevel = (old.transmissionUpgradeLevel + 1).coerceAtMost(5))
            "suspension" -> old.copy(suspensionUpgradeLevel = (old.suspensionUpgradeLevel + 1).coerceAtMost(5))
            "brake" -> old.copy(brakeUpgradeLevel = (old.brakeUpgradeLevel + 1).coerceAtMost(5))
            else -> old
        }
        val updated = current.copy(
            masalikCoins = current.masalikCoins - cost,
            customizations = current.customizations + (id to next)
        )
        if (physics.vehicleId == id) physics.updateCustomization(next)
        commitSave(updated)
    }

    fun serviceAction(type: String, targetVehicleId: VehicleId = physics.vehicleId, cost: Int = 0) {
        val current = _uiState.value.saveState
        if (cost > 0 && current.masalikCoins < cost) {
            _uiState.update { it.copy(toastMessageAr = "الرصيد غير كافٍ لهذه الخدمة") }
            return
        }
        val spec = VehicleFleetCatalog.specOf(targetVehicleId)
        val curFuel = current.fuelByVehicle.toMutableMap()
        val curDmg = current.damageByVehicle.toMutableMap()
        val msg = when (type) {
            "fuel" -> {
                curFuel[targetVehicleId] = spec.fuelTankLiters
                if (physics.vehicleId == targetVehicleId) physics.refuelTank()
                "تم تعبئة خزان الوقود بالكامل"
            }
            "workshop" -> {
                val old = curDmg[targetVehicleId] ?: com.masalik.yemen.game.DamageState()
                curDmg[targetVehicleId] = old.copy(engineHealthPct = 100f, tiresHealthPct = 100f, suspensionHealthPct = 100f, bodyHealthPct = 100f, cargoIntegrityPct = 100f)
                if (physics.vehicleId == targetVehicleId) physics.repairAll()
                "تمت الصيانة الشاملة وإصلاح المركبة"
            }
            else -> {
                val old = curDmg[targetVehicleId] ?: com.masalik.yemen.game.DamageState()
                curDmg[targetVehicleId] = old.copy(dirtMudLevel = 0f)
                if (physics.vehicleId == targetVehicleId) physics.washVehicle()
                "تم غسيل وتلميع المركبة وإزالة الطين"
            }
        }
        commitSave(current.copy(masalikCoins = current.masalikCoins - cost, fuelByVehicle = curFuel, damageByVehicle = curDmg))
        _uiState.update { it.copy(toastMessageAr = msg) }
    }

    // Driving Controls & Systems
    fun cycleCameraMode() {
        val modes = CameraMode.entries
        val next = modes[(modes.indexOf(_uiState.value.cameraMode) + 1) % modes.size]
        _uiState.update { it.copy(cameraMode = next) }
    }

    fun setCameraMode(mode: CameraMode) = _uiState.update { it.copy(cameraMode = mode) }
    fun toggleCleanHud() = _uiState.update { it.copy(cleanCinematicHud = !it.cleanCinematicHud) }
    fun setPhotoFilter(filter: PhotoColorFilter) = _uiState.update { it.copy(photoFilter = filter) }
    fun setPhotoFov(fov: Float) = _uiState.update { it.copy(photoFovDeg = fov.coerceIn(32f, 82f)) }

    fun setShifter(shifter: AutomaticShifterState) { physics.autoShifter = shifter }
    fun toggleTransmissionMode() { physics.transmissionMode = if (physics.transmissionMode == "auto") "manual" else "auto" }
    fun manualGearUp() = physics.shiftManualUp()
    fun manualGearDown() = physics.shiftManualDown()
    fun cycleDrivetrain() = physics.cycleDrivetrain()
    fun toggleDiffLock() { physics.diffLockActive = !physics.diffLockActive }
    fun toggleEngine() { physics.engineRunning = !physics.engineRunning }
    fun toggleHeadlights() { physics.headlightsOn = !physics.headlightsOn }
    fun toggleWipers() { physics.wipersOn = !physics.wipersOn }
    fun toggleLeftIndicator() {
        physics.leftIndicatorOn = !physics.leftIndicatorOn
        if (physics.leftIndicatorOn) physics.rightIndicatorOn = false
    }
    fun toggleRightIndicator() {
        physics.rightIndicatorOn = !physics.rightIndicatorOn
        if (physics.rightIndicatorOn) physics.leftIndicatorOn = false
    }
    fun toggleCruiseControl() {
        physics.cruiseControlEnabled = !physics.cruiseControlEnabled
        if (physics.cruiseControlEnabled) {
            physics.cruiseTargetKmh = (physics.forwardSpeedMs * 3.6f).coerceIn(35f, 120f)
        }
    }
    fun toggleAssist(name: String) {
        val a = physics.assists
        physics.assists = when (name) {
            "abs" -> a.copy(absEnabled = !a.absEnabled)
            "tcs" -> a.copy(tcsEnabled = !a.tcsEnabled)
            "esc" -> a.copy(escEnabled = !a.escEnabled)
            else -> a.copy(hdcEnabled = !a.hdcEnabled)
        }
    }
    fun recoverVehicleOnRoad() = physics.resetToRoad(physics.distanceAlongRoadM, 2.55f)
    fun setHornActive(active: Boolean) { audioEngine.hornActive = active }

    // Time, Weather, Audio, Radio, HUD Studio & Settings
    fun setWeather(weather: WeatherPreset) {
        physics.weather = weather
        audioEngine.weather = weather
        commitSave(_uiState.value.saveState.copy(weather = weather))
    }

    fun setTimeOfDay(preset: TimeOfDayPreset) {
        worldTimeHours = preset.hours
        commitSave(_uiState.value.saveState.copy(timeOfDay = preset, worldTimeHours = preset.hours))
    }

    fun toggleAutoTimeCycle() {
        val next = !_uiState.value.saveState.autoTimeCycleEnabled
        commitSave(_uiState.value.saveState.copy(autoTimeCycleEnabled = next))
    }

    fun setDrivingRealism(mode: DrivingRealismMode) {
        physics.realismMode = mode
        commitSave(_uiState.value.saveState.copy(drivingRealism = mode))
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        commitSave(_uiState.value.saveState.copy(graphicsQuality = quality))
    }

    fun updateAudioMixer(mixer: AudioMixerSettings) {
        audioEngine.mixer = mixer
        commitSave(_uiState.value.saveState.copy(audioMixer = mixer))
    }

    fun toggleRadioOpen() {
        val open = !_uiState.value.radioOpen
        _uiState.update { it.copy(radioOpen = open) }
        commitSave(_uiState.value.saveState.copy(radioSettings = _uiState.value.saveState.radioSettings.copy(isOpen = open)))
    }

    fun toggleRadioPlaying() {
        val cur = _uiState.value.saveState.radioSettings
        val next = cur.copy(isPlaying = !cur.isPlaying)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    fun selectRadioStation(stationId: RadioStationId) {
        val next = _uiState.value.saveState.radioSettings.copy(stationId = stationId, isPlaying = true)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    fun setRadioVolume(vol: Float) {
        val next = _uiState.value.saveState.radioSettings.copy(volume = vol.coerceIn(0f, 1f))
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    fun toggleRadioVinyl() {
        val cur = _uiState.value.saveState.radioSettings
        val next = cur.copy(lofiVinylEnabled = !cur.lofiVinylEnabled)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    fun updateHudSettings(hud: HudCustomizationSettings) {
        physics.wheelRotationRangeDeg = hud.smartWheel.rotationRangeDeg
        physics.steeringAssistEnabled = hud.smartWheel.steeringAssist
        if (hud.controlMode == TouchControlMode.TILT) gyroController.start() else gyroController.stop()
        commitSave(_uiState.value.saveState.copy(hudSettings = hud))
    }

    fun runDiagnostics() {
        val res = physics.runEngineeringDiagnostics()
        _uiState.update { it.copy(diagnosticResults = res) }
    }

    fun refreshDbStats() {
        viewModelScope.launch {
            val stats = repository.getDatabaseStats(_uiState.value.saveState)
            _uiState.update { it.copy(dbStats = stats) }
        }
    }

    fun exportSaveJson(): String = repository.exportSaveJson(_uiState.value.saveState)

    fun importSaveJson(raw: String) {
        val result = repository.importSaveJson(raw)
        result.onSuccess { imported ->
            worldTimeHours = imported.worldTimeHours
            applySaveToEngines(imported)
            commitSave(imported)
            _uiState.update { it.copy(toastMessageAr = "تم استيراد ملف الحفظ بنجاح!") }
        }.onFailure {
            _uiState.update { it.copy(toastMessageAr = "تعذر قراءة ملف الحفظ، تأكد من صلاحية الملف.") }
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            val fresh = repository.resetAllData()
            worldTimeHours = fresh.worldTimeHours
            applySaveToEngines(fresh)
            val stats = repository.getDatabaseStats(fresh)
            _uiState.update {
                it.copy(
                    saveState = fresh,
                    previewVehicleId = fresh.selectedVehicleId,
                    dbStats = stats,
                    toastMessageAr = "تم تصفير قاعدة البيانات المحلية بالكامل."
                )
            }
        }
    }

    private fun saveSnapshotNow() {
        val cur = _uiState.value.saveState
        val updated = cur.copy(
            worldTimeHours = worldTimeHours,
            timeOfDay = LivingWorldSimulation.hoursToPreset(worldTimeHours),
            playerDistanceM = physics.distanceAlongRoadM,
            lastRegionId = RoadSplineNetwork.getRegionAt(physics.distanceAlongRoadM).id,
            fuelByVehicle = cur.fuelByVehicle + (physics.vehicleId to physics.fuelLiters),
            damageByVehicle = cur.damageByVehicle + (physics.vehicleId to physics.damage),
            totalCareerDistanceM = cur.totalCareerDistanceM + (physics.totalDistanceDrivenM - tripStartDistM).coerceAtLeast(0f)
        )
        tripStartDistM = physics.totalDistanceDrivenM
        commitSave(updated)
    }

    private fun commitSave(next: SaveGameSchema) {
        _uiState.update { it.copy(saveState = next) }
        viewModelScope.launch {
            repository.persistSaveGame(next)
        }
    }
}
