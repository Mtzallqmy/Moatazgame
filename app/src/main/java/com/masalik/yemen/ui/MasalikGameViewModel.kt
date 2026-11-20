package com.masalik.yemen.ui

import android.app.Application
import android.util.Log
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
import com.masalik.yemen.game.MissionOutcome
import com.masalik.yemen.game.MissionRules
import com.masalik.yemen.game.MissionRunResult
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class ActiveModal {
    MENU, DRIVING, PAUSE, WORLD_MAP, GARAGE, MISSIONS, CONTROL_STUDIO, SETTINGS, PHOTO, RESULTS
}

data class MasalikUiState(
    val saveState: SaveGameSchema = SaveGameSchema(),
    val saveLoading: Boolean = true,
    val activeModal: ActiveModal = ActiveModal.MENU,
    val telemetry: VehicleTelemetry = VehicleTelemetry(),
    val nearbySignal: NearbyTrafficSignalInfo? = null,
    val cameraMode: CameraMode = CameraMode.CHASE_NEAR,
    val cleanCinematicHud: Boolean = false,
    val previewVehicleId: VehicleId = VehicleId.SHAHIQ,
    val activeMission: MissionDefinition? = null,
    val missionTimeRemainingSec: Int = 0,
    val missionPickupDone: Boolean = false,
    val missionResult: MissionRunResult? = null,
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
    private val repository = MasalikRepository(application, database)
    private val saveOperations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    @Volatile private var saveAvailable = false

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
    private var currentFps: Int = 0
    private var physicsAccumulator: Float = 0f
    private var blinkerTimer: Float = 0f
    private var blinkerPhase: Boolean = false

    private var tripStartDistM: Float = 0f
    private var careerRecordedAtM: Float = 0f
    private var tripMaxSpeedKmh: Float = 0f
    private var tripStartTimeMs: Long = System.currentTimeMillis()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            for (operation in saveOperations) {
                try {
                    operation()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    Log.e("MasalikSave", "Could not write local progress", error)
                    _uiState.update { it.copy(toastMessageAr = "تعذر حفظ التقدم على الجهاز") }
                }
            }
        }
        viewModelScope.launch {
            try {
                val loaded = repository.loadSaveGame()
                val stats = repository.getDatabaseStats(loaded)
                synchronized(this@MasalikGameViewModel) {
                    worldTimeHours = loaded.worldTimeHours
                    applySaveToEngines(loaded)
                    saveAvailable = true
                    _uiState.update {
                        it.copy(
                            saveLoading = false,
                            saveState = loaded,
                            previewVehicleId = loaded.selectedVehicleId,
                            radioOpen = loaded.radioSettings.isOpen,
                            displayClockStr = LivingWorldSimulation.formatWorldTime(worldTimeHours),
                            activeTimePhase = LivingWorldSimulation.hoursToPreset(worldTimeHours),
                            dbStats = stats
                        )
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("MasalikSave", "Could not load local progress", error)
                _uiState.update { it.copy(saveLoading = false, toastMessageAr = "تعذر قراءة الحفظ. يمكنك القيادة دون حفظ التقدم.") }
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

    @Synchronized
    fun onResumeLifecycle() {
        audioEngine.start()
        if (_uiState.value.saveState.hudSettings.controlMode == TouchControlMode.TILT) {
            gyroController.start()
        }
    }

    @Synchronized
    fun onPauseLifecycle() {
        clearDrivingControls()
        gyroController.stop()
        audioEngine.stop()
        if (saveAvailable) saveSnapshotNow()
    }

    override fun onCleared() {
        super.onCleared()
        gyroController.stop()
        audioEngine.stop()
        saveOperations.close()
    }

    @Synchronized
    fun stepSimulationFrame(frameTimeSec: Float): MasalikGLRenderer.RenderFrameSnapshot {
        val state = _uiState.value
        val modal = state.activeModal
        val isDriving = modal == ActiveModal.DRIVING
        val isLobbyOrGarage = modal == ActiveModal.MENU || modal == ActiveModal.GARAGE
        val dt = frameTimeSec.coerceIn(0f, 0.2f)

        fpsFrameCount++
        fpsAccumulator += frameTimeSec
        if (fpsAccumulator >= 0.7f) {
            currentFps = (fpsFrameCount / fpsAccumulator).toInt()
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

        var steppedTelemetry: VehicleTelemetry? = null
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
            physicsAccumulator += dt
            while (physicsAccumulator >= PHYSICS_STEP_SEC && _uiState.value.activeModal == ActiveModal.DRIVING) {
                physicsAccumulator -= PHYSICS_STEP_SEC
                livingWorld.update(PHYSICS_STEP_SEC, physics.distanceAlongRoadM, physics.lateralOffsetM, physics.forwardSpeedMs, atmo.nightFactor)
                val telemetry = physics.step(PHYSICS_STEP_SEC, inputs, livingWorld)
                steppedTelemetry = telemetry
                tripMaxSpeedKmh = maxOf(tripMaxSpeedKmh, telemetry.speedKmh)
                if (physics.lastCollisionImpulse > 2f) {
                    audioEngine.triggerCollision((physics.lastCollisionImpulse / 14f).coerceIn(0.25f, 1f))
                }
                state.activeMission?.let { updateActiveMission(it, telemetry) }
            }

            // Periodic auto-save every 15s while driving
            autoSaveAccumulator += dt
            if (autoSaveAccumulator >= 15f) {
                autoSaveAccumulator = 0f
                saveSnapshotNow()
            }
        } else if (isLobbyOrGarage) {
            livingWorld.update(dt * 0.45f, physics.distanceAlongRoadM, physics.lateralOffsetM, 0f, atmo.nightFactor)
        }
        val telemetry = steppedTelemetry ?: physics.snapshot(livingWorld, inputThrottle, inputBrake, inputHandbrake)

        // Update live audio synthesizer parameters
        audioEngine.drivingActive = _uiState.value.activeModal == ActiveModal.DRIVING
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
            cameraMode = if (modal == ActiveModal.PHOTO) CameraMode.FREE_PHOTO else _uiState.value.cameraMode,
            graphicsQuality = currentSave.graphicsQuality,
            photoFovDeg = _uiState.value.photoFovDeg,
            worldTimeHours = worldTimeHours,
            weather = currentSave.weather,
            livingWorld = livingWorld,
            missionMarkerM = _uiState.value.activeMission?.let { mission ->
                if (mission.requiresServiceStops && !_uiState.value.missionPickupDone) MissionRules.pickupDistanceM(mission)
                else mission.finishDistanceM
            },
            blinkerOn = blinkerPhase
        )
    }

    private fun updateActiveMission(mission: MissionDefinition, telemetry: VehicleTelemetry) {
        missionElapsedSec += PHYSICS_STEP_SEC
        if (mission.requiresServiceStops && !_uiState.value.missionPickupDone &&
            MissionRules.isStoppedAt(telemetry, MissionRules.pickupDistanceM(mission))
        ) {
            _uiState.update { it.copy(missionPickupDone = true, missionBannerAr = "تم الالتقاط. توقّف عند نقطة الوصول.") }
        }
        val outcome = MissionRules.evaluate(mission, telemetry, missionElapsedSec, _uiState.value.missionPickupDone)
        if (outcome != null) finishActiveMission(mission, outcome)
    }

    private fun finishActiveMission(mission: MissionDefinition, outcome: MissionOutcome) {
        val result = MissionRunResult(mission, outcome, missionElapsedSec)
        missionElapsedSec = 0f
        recordCurrentTripIfSignificant()
        clearDrivingControls()
        clearMissionVehicleSetup()
        val current = snapshotSave()
        val saved = if (outcome == MissionOutcome.SUCCESS) {
            val best = minOf(result.elapsedSec, current.missionBestTimesSec[mission.id] ?: result.elapsedSec)
            val completions = (current.missionCompletions[mission.id] ?: 0) + 1
            current.copy(
                masalikCoins = current.masalikCoins + mission.rewardCoins,
                driverXp = current.driverXp + mission.rewardXp,
                missionBestTimesSec = current.missionBestTimesSec + (mission.id to best),
                missionCompletions = current.missionCompletions + (mission.id to completions)
            )
        } else current
        commitSave(saved)
        _uiState.update {
            it.copy(
                activeMission = null,
                missionBannerAr = null,
                missionResult = result,
                activeModal = ActiveModal.RESULTS
            )
        }
    }

    fun dismissMissionBanner() = _uiState.update { it.copy(missionBannerAr = null) }
    fun dismissToast() = _uiState.update { it.copy(toastMessageAr = null) }

    @Synchronized
    fun setModal(modal: ActiveModal) {
        if (modal == ActiveModal.DRIVING && _uiState.value.activeModal != ActiveModal.DRIVING) {
            beginTrip()
            if (_uiState.value.saveState.hudSettings.controlMode == TouchControlMode.TILT) {
                gyroController.start()
            }
        } else if (modal != ActiveModal.DRIVING && _uiState.value.activeModal == ActiveModal.DRIVING) {
            recordCurrentTripIfSignificant()
            clearDrivingControls()
            saveSnapshotNow()
        }
        if (modal == ActiveModal.SETTINGS) {
            refreshDbStats()
        }
        _uiState.update { it.copy(activeModal = modal) }
    }

    private fun beginTrip() {
        tripStartDistM = physics.totalDistanceDrivenM
        tripMaxSpeedKmh = 0f
        tripStartTimeMs = System.currentTimeMillis()
        physicsAccumulator = 0f
        autoSaveAccumulator = 0f
    }

    private fun clearDrivingControls() {
        inputThrottle = 0f
        inputBrake = 0f
        inputSteerNorm = 0f
        inputHandbrake = false
        audioEngine.hornActive = false
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
        if (saveAvailable) saveOperations.trySend {
            repository.logDrivingTrip(log)
            refreshDbStats()
        }
    }

    @Synchronized
    fun startMission(mission: MissionDefinition) {
        clearDrivingControls()
        val current = snapshotSave()
        val chosenVehicle = if (mission.recommendedVehicleId in current.unlockedVehicles) {
            mission.recommendedVehicleId
        } else current.selectedVehicleId

        val custom = current.customizations[chosenVehicle]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(chosenVehicle)
        physics.selectVehicle(chosenVehicle, custom, current.fuelByVehicle[chosenVehicle], current.damageByVehicle[chosenVehicle])
        if (mission.cargoRequiredPct > 0) {
            physics.updateCustomization(custom.copy(cargoLoadPct = maxOf(45f, custom.cargoLoadPct)))
            physics.damage = physics.damage.copy(cargoIntegrityPct = 100f)
        }
        physics.weather = mission.weatherPreset
        physics.resetToRoad(mission.startDistanceM)
        physics.configureAiRival(mission.hasAiRival, mission.startDistanceM, mission.aiRivalSpeedMs)
        worldTimeHours = mission.timeOfDayPreset.hours
        missionElapsedSec = 0f
        beginTrip()

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
                missionPickupDone = false,
                missionResult = null,
                missionTimeRemainingSec = mission.targetTimeSec,
                missionBannerAr = mission.dialogueBriefAr ?: "بدأت المهمة: ${mission.titleAr}"
            )
        }
    }

    private fun clearMissionVehicleSetup(save: SaveGameSchema = _uiState.value.saveState) {
        physics.configureAiRival(false)
        val custom = save.customizations[physics.vehicleId]
            ?: VehicleFleetCatalog.defaultCustomizations().getValue(physics.vehicleId)
        physics.updateCustomization(custom)
    }

    @Synchronized
    fun startFreeRoam() {
        clearMissionVehicleSetup()
        clearDrivingControls()
        beginTrip()
        _uiState.update { it.copy(activeMission = null, missionResult = null, missionBannerAr = null, activeModal = ActiveModal.DRIVING) }
    }

    @Synchronized
    fun fastTravelToCity(city: YemenCityHub) {
        if (_uiState.value.activeMission != null) {
            _uiState.update { it.copy(toastMessageAr = "أنهِ المهمة أو ابدأ تجوالًا حرًا قبل الانتقال السريع.") }
            return
        }
        physics.resetToRoad(city.distanceM)
        clearDrivingControls()
        beginTrip()
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

    @Synchronized
    fun orbitShowroom(deltaRad: Float) {
        showroomOrbitRad = (showroomOrbitRad + deltaRad) % (2f * kotlin.math.PI.toFloat())
    }

    @Synchronized
    fun confirmSelectVehicle(id: VehicleId) {
        if (_uiState.value.activeMission != null) {
            _uiState.update { it.copy(toastMessageAr = "يمكنك تغيير المركبة بعد إنهاء المهمة أو مغادرتها.") }
            return
        }
        if (id !in _uiState.value.saveState.unlockedVehicles) return
        val current = snapshotSave()
        val custom = current.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        physics.selectVehicle(id, custom, current.fuelByVehicle[id], current.damageByVehicle[id])
        commitSave(current.copy(selectedVehicleId = id))
        _uiState.update { it.copy(previewVehicleId = id, toastMessageAr = "تم تجهيز مركبة «${VehicleFleetCatalog.specOf(id).nameAr}»") }
    }

    @Synchronized
    fun unlockVehicle(id: VehicleId) {
        val spec = VehicleFleetCatalog.specOf(id)
        val existing = _uiState.value.saveState
        if (id in existing.unlockedVehicles || existing.masalikCoins < spec.unlockPriceCoins) return
        val current = snapshotSave()
        val updated = current.copy(
            masalikCoins = current.masalikCoins - spec.unlockPriceCoins,
            unlockedVehicles = (current.unlockedVehicles + id).distinct(),
            selectedVehicleId = if (_uiState.value.activeMission == null) id else current.selectedVehicleId
        )
        commitSave(updated)
        val custom = updated.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        if (updated.selectedVehicleId == id) physics.selectVehicle(id, custom, updated.fuelByVehicle[id], updated.damageByVehicle[id])
        _uiState.update { it.copy(previewVehicleId = id, toastMessageAr = "مبارك! تم شراء مركبة «${spec.nameAr}»") }
    }

    @Synchronized
    fun updateVehicleCustomization(id: VehicleId, custom: VehicleCustomization) {
        val current = _uiState.value.saveState
        val updated = current.copy(customizations = current.customizations + (id to custom))
        if (physics.vehicleId == id) physics.updateCustomization(custom)
        commitSave(updated)
    }

    @Synchronized
    fun upgradeMechanicalPart(id: VehicleId, part: String, cost: Int) {
        val current = _uiState.value.saveState
        if (cost < 0 || current.masalikCoins < cost || id !in current.unlockedVehicles) return
        val old = current.customizations[id] ?: VehicleFleetCatalog.defaultCustomizations().getValue(id)
        val next = when (part) {
            "engine" -> old.copy(engineUpgradeLevel = (old.engineUpgradeLevel + 1).coerceAtMost(5))
            "transmission" -> old.copy(transmissionUpgradeLevel = (old.transmissionUpgradeLevel + 1).coerceAtMost(5))
            "suspension" -> old.copy(suspensionUpgradeLevel = (old.suspensionUpgradeLevel + 1).coerceAtMost(5))
            "brake" -> old.copy(brakeUpgradeLevel = (old.brakeUpgradeLevel + 1).coerceAtMost(5))
            else -> old
        }
        if (next == old) return
        val updated = current.copy(
            masalikCoins = current.masalikCoins - cost,
            customizations = current.customizations + (id to next)
        )
        if (physics.vehicleId == id) physics.updateCustomization(next)
        commitSave(updated)
    }

    @Synchronized
    fun serviceAction(type: String, targetVehicleId: VehicleId = physics.vehicleId, cost: Int = 0) {
        val current = _uiState.value.saveState
        if (cost < 0 || targetVehicleId !in current.unlockedVehicles) return
        if (_uiState.value.activeMission != null && type == "workshop" && targetVehicleId == physics.vehicleId) {
            _uiState.update { it.copy(toastMessageAr = "يمكن صيانة المركبة بعد انتهاء المهمة.") }
            return
        }
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
            "car_wash" -> {
                val old = curDmg[targetVehicleId] ?: com.masalik.yemen.game.DamageState()
                curDmg[targetVehicleId] = old.copy(dirtMudLevel = 0f)
                if (physics.vehicleId == targetVehicleId) physics.washVehicle()
                "تم غسيل وتلميع المركبة وإزالة الطين"
            }
            else -> return
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

    @Synchronized
    fun setShifter(shifter: AutomaticShifterState) { physics.autoShifter = shifter }
    @Synchronized
    fun toggleTransmissionMode() { physics.transmissionMode = if (physics.transmissionMode == "auto") "manual" else "auto" }
    @Synchronized
    fun manualGearUp() = physics.shiftManualUp()
    @Synchronized
    fun manualGearDown() = physics.shiftManualDown()
    @Synchronized
    fun cycleDrivetrain() = physics.cycleDrivetrain()
    @Synchronized
    fun toggleDiffLock() { physics.diffLockActive = !physics.diffLockActive }
    @Synchronized
    fun toggleEngine() { physics.engineRunning = !physics.engineRunning }
    @Synchronized
    fun toggleHeadlights() { physics.headlightsOn = !physics.headlightsOn }
    @Synchronized
    fun toggleWipers() { physics.wipersOn = !physics.wipersOn }
    @Synchronized
    fun toggleLeftIndicator() {
        physics.leftIndicatorOn = !physics.leftIndicatorOn
        if (physics.leftIndicatorOn) physics.rightIndicatorOn = false
    }
    @Synchronized
    fun toggleRightIndicator() {
        physics.rightIndicatorOn = !physics.rightIndicatorOn
        if (physics.rightIndicatorOn) physics.leftIndicatorOn = false
    }
    @Synchronized
    fun toggleCruiseControl() {
        physics.cruiseControlEnabled = !physics.cruiseControlEnabled
        if (physics.cruiseControlEnabled) {
            physics.cruiseTargetKmh = (physics.forwardSpeedMs * 3.6f).coerceIn(35f, 120f)
        }
    }
    @Synchronized
    fun toggleAssist(name: String) {
        val a = physics.assists
        physics.assists = when (name) {
            "abs" -> a.copy(absEnabled = !a.absEnabled)
            "tcs" -> a.copy(tcsEnabled = !a.tcsEnabled)
            "esc" -> a.copy(escEnabled = !a.escEnabled)
            else -> a.copy(hdcEnabled = !a.hdcEnabled)
        }
    }
    @Synchronized
    fun recoverVehicleOnRoad() = physics.resetToRoad(physics.distanceAlongRoadM, 2.55f)
    fun setHornActive(active: Boolean) { audioEngine.hornActive = active }

    // Time, Weather, Audio, Radio, HUD Studio & Settings
    @Synchronized
    fun setWeather(weather: WeatherPreset) {
        physics.weather = weather
        audioEngine.weather = weather
        commitSave(_uiState.value.saveState.copy(weather = weather))
    }

    @Synchronized
    fun setTimeOfDay(preset: TimeOfDayPreset) {
        worldTimeHours = preset.hours
        commitSave(_uiState.value.saveState.copy(timeOfDay = preset, worldTimeHours = preset.hours))
    }

    @Synchronized
    fun toggleAutoTimeCycle() {
        val next = !_uiState.value.saveState.autoTimeCycleEnabled
        commitSave(_uiState.value.saveState.copy(autoTimeCycleEnabled = next))
    }

    @Synchronized
    fun setDrivingRealism(mode: DrivingRealismMode) {
        physics.realismMode = mode
        commitSave(_uiState.value.saveState.copy(drivingRealism = mode))
    }

    @Synchronized
    fun setGraphicsQuality(quality: GraphicsQuality) {
        commitSave(_uiState.value.saveState.copy(graphicsQuality = quality))
    }

    @Synchronized
    fun updateAudioMixer(mixer: AudioMixerSettings) {
        audioEngine.mixer = mixer
        commitSave(_uiState.value.saveState.copy(audioMixer = mixer))
    }

    @Synchronized
    fun toggleRadioOpen() {
        val open = !_uiState.value.radioOpen
        _uiState.update { it.copy(radioOpen = open) }
        commitSave(_uiState.value.saveState.copy(radioSettings = _uiState.value.saveState.radioSettings.copy(isOpen = open)))
    }

    @Synchronized
    fun toggleRadioPlaying() {
        val cur = _uiState.value.saveState.radioSettings
        val next = cur.copy(isPlaying = !cur.isPlaying)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    @Synchronized
    fun selectRadioStation(stationId: RadioStationId) {
        val next = _uiState.value.saveState.radioSettings.copy(stationId = stationId, isPlaying = true)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    @Synchronized
    fun setRadioVolume(vol: Float) {
        val next = _uiState.value.saveState.radioSettings.copy(volume = vol.coerceIn(0f, 1f))
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    @Synchronized
    fun toggleRadioVinyl() {
        val cur = _uiState.value.saveState.radioSettings
        val next = cur.copy(lofiVinylEnabled = !cur.lofiVinylEnabled)
        audioEngine.radioSettings = next
        commitSave(_uiState.value.saveState.copy(radioSettings = next))
    }

    @Synchronized
    fun updateHudSettings(hud: HudCustomizationSettings) {
        physics.wheelRotationRangeDeg = hud.smartWheel.rotationRangeDeg
        physics.steeringAssistEnabled = hud.smartWheel.steeringAssist
        if (hud.controlMode == TouchControlMode.TILT) gyroController.start() else gyroController.stop()
        commitSave(_uiState.value.saveState.copy(hudSettings = hud))
    }

    @Synchronized
    fun runDiagnostics() {
        val res = physics.runEngineeringDiagnostics()
        _uiState.update { it.copy(diagnosticResults = res) }
    }

    fun refreshDbStats() {
        if (!saveAvailable) return
        viewModelScope.launch {
            try {
                val stats = repository.getDatabaseStats(_uiState.value.saveState)
                _uiState.update { it.copy(dbStats = stats) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("MasalikSave", "Could not read local statistics", error)
            }
        }
    }

    @Synchronized
    fun exportSaveJson(): String = repository.exportSaveJson(saveSnapshotNow())

    @Synchronized
    fun importSaveJson(raw: String) {
        if (!saveAvailable) return
        val result = repository.importSaveJson(raw)
        result.onSuccess { imported ->
            worldTimeHours = imported.worldTimeHours
            applySaveToEngines(imported)
            clearMissionVehicleSetup(imported)
            beginTrip()
            careerRecordedAtM = physics.totalDistanceDrivenM
            commitSave(imported, replaceMissionRecords = true)
            _uiState.update {
                it.copy(
                    activeMission = null,
                    missionResult = null,
                    missionBannerAr = null,
                    previewVehicleId = imported.selectedVehicleId,
                    radioOpen = imported.radioSettings.isOpen
                )
            }
        }.onFailure {
            _uiState.update { it.copy(toastMessageAr = "تعذر قراءة ملف الحفظ، تأكد من صلاحية الملف.") }
        }
    }

    @Synchronized
    fun resetAllProgress() {
        if (!saveAvailable) return
        val fresh = SaveGameSchema(timestampIso = Instant.now().toString())
        worldTimeHours = fresh.worldTimeHours
        applySaveToEngines(fresh)
        clearMissionVehicleSetup(fresh)
        beginTrip()
        careerRecordedAtM = physics.totalDistanceDrivenM
        commitSave(fresh, replaceMissionRecords = true, clearTrips = true)
        _uiState.update {
            it.copy(
                activeMission = null,
                missionResult = null,
                missionBannerAr = null,
                previewVehicleId = fresh.selectedVehicleId,
                radioOpen = fresh.radioSettings.isOpen
            )
        }
    }

    private fun snapshotSave(): SaveGameSchema {
        val cur = _uiState.value.saveState
        val distanceDelta = (physics.totalDistanceDrivenM - careerRecordedAtM).coerceAtLeast(0f)
        careerRecordedAtM = physics.totalDistanceDrivenM
        return cur.copy(
            worldTimeHours = worldTimeHours,
            timeOfDay = LivingWorldSimulation.hoursToPreset(worldTimeHours),
            playerDistanceM = physics.distanceAlongRoadM,
            lastRegionId = RoadSplineNetwork.getRegionAt(physics.distanceAlongRoadM).id,
            fuelByVehicle = cur.fuelByVehicle + (physics.vehicleId to physics.fuelLiters),
            damageByVehicle = cur.damageByVehicle + (physics.vehicleId to physics.damage),
            totalCareerDistanceM = cur.totalCareerDistanceM + distanceDelta
        )
    }

    private fun saveSnapshotNow(): SaveGameSchema = commitSave(snapshotSave())

    private fun commitSave(
        next: SaveGameSchema,
        replaceMissionRecords: Boolean = false,
        clearTrips: Boolean = false
    ): SaveGameSchema {
        val stamped = next.copy(timestampIso = Instant.now().toString())
        _uiState.update { it.copy(saveState = stamped) }
        if (saveAvailable) saveOperations.trySend {
            repository.persistSaveGame(stamped, replaceMissionRecords, clearTrips)
            if (replaceMissionRecords) {
                val message = if (clearTrips) "تم تصفير التقدم." else "تم استيراد ملف الحفظ بنجاح."
                _uiState.update { it.copy(toastMessageAr = message) }
                refreshDbStats()
            }
        }
        return stamped
    }

    private companion object {
        const val PHYSICS_STEP_SEC = 1f / 60f
    }
}
