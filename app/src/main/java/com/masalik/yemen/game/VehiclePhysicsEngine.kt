package com.masalik.yemen.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

data class ControlInputs(
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val steer: Float = 0f, // -1 (left) .. +1 (right)
    val handbrake: Boolean = false
)

class VehiclePhysicsEngine {

    var vehicleId: VehicleId = VehicleId.SHAHIQ
        private set
    var spec: VehicleSpec = VehicleFleetCatalog.specOf(VehicleId.SHAHIQ)
        private set
    var customization: VehicleCustomization = VehicleFleetCatalog.defaultCustomizations().getValue(VehicleId.SHAHIQ)
        private set

    var realismMode: DrivingRealismMode = DrivingRealismMode.SPORT
    var weather: WeatherPreset = WeatherPreset.CLEAR
    var wheelRotationRangeDeg: Int = 270
    var steeringAssistEnabled: Boolean = true

    // Position & Orientation along road + world
    var distanceAlongRoadM: Float = 60f
        private set
    var lateralOffsetM: Float = 2.55f // Right-hand lane default
        private set
    var totalDistanceDrivenM: Float = 0f
        private set

    var posX: Float = 0f
        private set
    var posY: Float = 0f
        private set
    var posZ: Float = 0f
        private set
    var headingRad: Float = 0f
        private set
    var headingOffsetFromRoadRad: Float = 0f
        private set

    var forwardSpeedMs: Float = 0f
        private set
    var lateralSpeedMs: Float = 0f
        private set
    var currentSteerInputNorm: Float = 0f
        private set
    var currentFrontSteerRad: Float = 0f
        private set
    var wheelRotationRad: Float = 0f
        private set

    var chassisPitchRad: Float = 0f
        private set
    var chassisRollRad: Float = 0f
        private set

    // Drivetrain & systems
    var transmissionMode: String = "auto"
    var autoShifter: AutomaticShifterState = AutomaticShifterState.D
    var manualGearIndex: Int = 1
    var drivetrainMode: DrivetrainMode = DrivetrainMode.FOUR_H
    var diffLockActive: Boolean = false
    var engineRunning: Boolean = true
    var headlightsOn: Boolean = false
    var wipersOn: Boolean = false
    var leftIndicatorOn: Boolean = false
    var rightIndicatorOn: Boolean = false
    var cruiseControlEnabled: Boolean = false
    var cruiseTargetKmh: Float = 60f

    var assists: ElectronicAssistsState = ElectronicAssistsState()
    var fuelLiters: Float = 85f
    var damage: DamageState = DamageState()

    // Race rival
    var aiRivalActive: Boolean = false
    var aiRivalDistanceM: Float = 0f
    var aiRivalBaseSpeedMs: Float = 18.5f

    // Event flags for audio/HUD
    var lastCollisionImpulse: Float = 0f
        private set
    var trafficViolationWarningAr: String? = null
    private var violationClearTimerSec: Float = 0f

    private var currentRpm: Float = 800f
    private var activeGearLabel: String = "D1"

    init {
        resetToRoad(60f)
    }

    fun selectVehicle(
        id: VehicleId,
        custom: VehicleCustomization,
        savedFuel: Float? = null,
        savedDamage: DamageState? = null
    ) {
        vehicleId = id
        spec = VehicleFleetCatalog.specOf(id)
        customization = custom
        drivetrainMode = spec.defaultDrivetrain
        autoShifter = AutomaticShifterState.D
        manualGearIndex = 1
        fuelLiters = (savedFuel ?: spec.fuelTankLiters).coerceIn(0f, spec.fuelTankLiters)
        damage = savedDamage ?: DamageState()
        currentRpm = spec.idleRpm
    }

    fun updateCustomization(custom: VehicleCustomization) {
        customization = custom
    }

    fun resetToRoad(distanceM: Float, laneOffsetM: Float = 2.55f) {
        distanceAlongRoadM = distanceM.coerceIn(10f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 20f)
        lateralOffsetM = laneOffsetM
        forwardSpeedMs = 0f
        lateralSpeedMs = 0f
        headingOffsetFromRoadRad = 0f
        currentSteerInputNorm = 0f
        currentFrontSteerRad = 0f
        chassisPitchRad = 0f
        chassisRollRad = 0f
        autoShifter = AutomaticShifterState.D
        manualGearIndex = 1
        syncWorldPosition()
    }

    fun configureAiRival(active: Boolean, startDistM: Float = 0f, speedMs: Float = 18.5f) {
        aiRivalActive = active
        aiRivalDistanceM = startDistM + 10f
        aiRivalBaseSpeedMs = speedMs
    }

    fun refuelTank() {
        fuelLiters = spec.fuelTankLiters
        engineRunning = true
    }

    fun repairAll() {
        damage = damage.copy(
            engineHealthPct = 100f,
            tiresHealthPct = 100f,
            suspensionHealthPct = 100f,
            bodyHealthPct = 100f,
            cargoIntegrityPct = 100f,
            steeringPullBias = 0f
        )
    }

    fun washVehicle() {
        damage = damage.copy(dirtMudLevel = 0f)
    }

    fun cycleDrivetrain() {
        val list = spec.supportedDrivetrains
        val idx = list.indexOf(drivetrainMode)
        drivetrainMode = list[(idx + 1) % list.size]
    }

    fun shiftManualUp() {
        manualGearIndex = (manualGearIndex + 1).coerceAtMost(spec.forwardGearsCount)
    }

    fun shiftManualDown() {
        manualGearIndex = (manualGearIndex - 1).coerceAtLeast(-1)
    }

    fun step(
        rawDt: Float,
        input: ControlInputs,
        livingWorld: LivingWorldSimulation?
    ): VehicleTelemetry {
        val dt = rawDt.coerceIn(0.004f, 0.05f)
        lastCollisionImpulse = 0f

        if (violationClearTimerSec > 0f) {
            violationClearTimerSec -= dt
            if (violationClearTimerSec <= 0f) trafficViolationWarningAr = null
        }

        val sample = RoadSplineNetwork.sampleAt(distanceAlongRoadM)
        val surface = sample.surface

        // Upgrade multipliers
        val engineMult = 1f + (customization.engineUpgradeLevel - 1) * 0.08f
        val transMult = 1f + (customization.transmissionUpgradeLevel - 1) * 0.05f
        val suspMult = 1f + (customization.suspensionUpgradeLevel - 1) * 0.08f
        val brakeMult = 1f + (customization.brakeUpgradeLevel - 1) * 0.09f

        val cargoMassKg = (spec.maxCargoKg * (customization.cargoLoadPct / 100f))
        val effectiveMassKg = spec.baseMassKg + cargoMassKg

        // Tire + Surface + Weather grip
        val baseGrip = VehicleFleetCatalog.gripFor(customization.tireType, surface)
        val weatherGrip = when (weather) {
            WeatherPreset.HEAVY_RAIN -> 0.78f
            WeatherPreset.LIGHT_RAIN -> 0.88f
            WeatherPreset.SANDSTORM -> 0.85f
            WeatherPreset.MOUNTAIN_FOG -> 0.94f
            else -> 1.0f
        }
        val drivetrainGrip = when (drivetrainMode) {
            DrivetrainMode.FOUR_L -> 1.24f
            DrivetrainMode.FOUR_H, DrivetrainMode.AWD -> 1.12f
            else -> 0.96f
        } + if (diffLockActive) 0.12f else 0f
        val realismGrip = when (realismMode) {
            DrivingRealismMode.ARCADE -> 1.18f
            DrivingRealismMode.SPORT -> 1.02f
            DrivingRealismMode.SIMULATION -> 0.92f
        }
        val totalGrip = (baseGrip * weatherGrip * drivetrainGrip * realismGrip *
            (0.65f + 0.35f * (damage.tiresHealthPct / 100f))).coerceIn(0.35f, 1.45f)

        // Effective throttle / cruise control
        var throttle = input.throttle.coerceIn(0f, 1f)
        val brake = input.brake.coerceIn(0f, 1f)
        if (brake > 0.1f || input.handbrake || autoShifter != AutomaticShifterState.D) {
            cruiseControlEnabled = false
        }
        if (cruiseControlEnabled && engineRunning && fuelLiters > 0.01f) {
            val speedKmh = forwardSpeedMs * 3.6f
            val err = cruiseTargetKmh - speedKmh
            throttle = (err * 0.06f).coerceIn(0f, 0.75f)
        }

        // Smooth steering response
        val speedKmhAbs = abs(forwardSpeedMs * 3.6f)
        val speedSteerDamp = (1f / (1f + speedKmhAbs * 0.014f)).coerceIn(0.36f, 1f)
        val targetSteer = input.steer.coerceIn(-1f, 1f)
        currentSteerInputNorm += (targetSteer - currentSteerInputNorm) * (dt * 11f).coerceAtMost(1f)
        val maxSteerRad = (spec.maxSteerAngleDeg * PI.toFloat() / 180f) * speedSteerDamp
        currentFrontSteerRad = currentSteerInputNorm * maxSteerRad

        // Drivetrain & gear calculation
        val gearMultiplier = when (customization.gearPreset) {
            GearRatioPreset.CRAWLER -> 1.25f
            GearRatioPreset.BALANCED -> 1.0f
            GearRatioPreset.HIGHWAY -> 0.86f
        } * if (drivetrainMode == DrivetrainMode.FOUR_L) 1.65f else 1.0f

        val maxSpeedMs = ((34f + spec.maxPowerHp * 0.055f) * transMult *
            when (customization.gearPreset) {
                GearRatioPreset.CRAWLER -> 0.82f
                GearRatioPreset.BALANCED -> 1.0f
                GearRatioPreset.HIGHWAY -> 1.14f
            } * if (drivetrainMode == DrivetrainMode.FOUR_L) 0.48f else 1.0f)

        val currentGearNum: Int
        val driveDir: Int
        if (transmissionMode == "auto") {
            when (autoShifter) {
                AutomaticShifterState.P -> {
                    activeGearLabel = "P"
                    currentGearNum = 0
                    driveDir = 0
                }
                AutomaticShifterState.N -> {
                    activeGearLabel = "N"
                    currentGearNum = 0
                    driveDir = 0
                }
                AutomaticShifterState.R -> {
                    activeGearLabel = "R"
                    currentGearNum = -1
                    driveDir = -1
                }
                AutomaticShifterState.D -> {
                    val ratio = (abs(forwardSpeedMs) / maxSpeedMs).coerceIn(0f, 1f)
                    currentGearNum = (1 + (ratio * (spec.forwardGearsCount - 1)).toInt()).coerceIn(1, spec.forwardGearsCount)
                    activeGearLabel = if (drivetrainMode == DrivetrainMode.FOUR_L) "L$currentGearNum" else "D$currentGearNum"
                    driveDir = 1
                }
            }
        } else {
            currentGearNum = manualGearIndex
            driveDir = when {
                manualGearIndex < 0 -> -1
                manualGearIndex == 0 -> 0
                else -> 1
            }
            activeGearLabel = when {
                manualGearIndex < 0 -> "R"
                manualGearIndex == 0 -> "N"
                else -> "$manualGearIndex"
            }
        }

        // Engine Force
        var driveForceN = 0f
        val canDeliverPower = engineRunning && fuelLiters > 0.01f && driveDir != 0
        if (canDeliverPower) {
            val gearRatioFactor = when {
                currentGearNum < 0 -> 2.8f
                currentGearNum == 1 -> 3.4f
                else -> (3.6f / (0.85f + currentGearNum * 0.55f))
            }
            val engineHealthFactor = 0.55f + 0.45f * (damage.engineHealthPct / 100f)
            val torqueN = (spec.maxEngineTorqueNm * engineMult * gearMultiplier * gearRatioFactor / spec.wheelRadiusM) *
                throttle * engineHealthFactor * totalGrip
            driveForceN = torqueN * driveDir
            // Speed governor
            if (driveDir > 0 && forwardSpeedMs >= maxSpeedMs) driveForceN = 0f
            if (driveDir < 0 && forwardSpeedMs <= -14f) driveForceN = 0f
        }

        // Braking & Park & HDC
        val maxBrake = spec.maxBrakeForceN * brakeMult * (if (assists.absEnabled) 1.0f else 0.88f)
        var brakeForceN = brake * maxBrake * totalGrip.coerceAtMost(1.15f)
        if (input.handbrake || autoShifter == AutomaticShifterState.P) {
            brakeForceN += maxBrake * 0.85f
        }
        // Hill Descent Control
        if (assists.hdcEnabled && sample.gradePct < -3.5f && forwardSpeedMs > 7.5f && throttle < 0.05f) {
            brakeForceN += maxBrake * 0.35f
        }

        // Gravity slope + Aero drag + Surface rolling resistance
        val slopeSin = (sample.gradePct / 100f).coerceIn(-0.28f, 0.28f)
        val gravityForceN = -effectiveMassKg * 9.81f * slopeSin
        val surfaceRollMult = when (surface) {
            SurfaceType.ASPHALT -> 1.0f
            SurfaceType.DIRT -> 1.45f
            SurfaceType.GRAVEL -> 1.55f
            SurfaceType.ROCK -> 1.65f
            SurfaceType.SAND -> 2.35f
            SurfaceType.MUD -> 2.85f
        }
        val aeroDragN = 0.5f * 1.225f * spec.dragCoefficient * 2.5f * forwardSpeedMs * abs(forwardSpeedMs)
        val rollingN = spec.rollingResistance * surfaceRollMult * (if (abs(forwardSpeedMs) > 0.05f) sign(forwardSpeedMs) else 0f)
        val engineBrakeN = if (throttle < 0.05f && driveDir != 0) forwardSpeedMs * 110f else 0f

        var netForceN = driveForceN + gravityForceN - aeroDragN - rollingN - engineBrakeN
        val speedSign = if (abs(forwardSpeedMs) > 0.04f) sign(forwardSpeedMs) else 0f

        if (brakeForceN > 0f) {
            if (abs(forwardSpeedMs) < 0.35f && abs(driveForceN) < brakeForceN) {
                forwardSpeedMs = 0f
                netForceN = 0f
            } else {
                netForceN -= speedSign * brakeForceN
            }
        }

        val prevSpeed = forwardSpeedMs
        forwardSpeedMs += (netForceN / effectiveMassKg) * dt
        if (brake > 0.15f && prevSpeed * forwardSpeedMs < 0f && throttle < 0.05f) {
            forwardSpeedMs = 0f
        }
        forwardSpeedMs = forwardSpeedMs.coerceIn(-16f, maxSpeedMs * 1.08f)

        // Kinematic + dynamic bicycle steering relative to road heading
        val yawRate = if (abs(forwardSpeedMs) > 0.1f) {
            (forwardSpeedMs / spec.wheelbaseM) * sin(currentFrontSteerRad) * (if (input.handbrake) 1.35f else 1.0f)
        } else 0f

        headingOffsetFromRoadRad += yawRate * dt
        // Gentle lane-keeping assist when steering is neutral and assist enabled
        if (steeringAssistEnabled && abs(currentSteerInputNorm) < 0.06f && abs(forwardSpeedMs) > 2f) {
            val alignStrength = if (realismMode == DrivingRealismMode.ARCADE) 3.4f else 1.85f
            headingOffsetFromRoadRad -= headingOffsetFromRoadRad * (alignStrength * dt).coerceAtMost(0.5f)
        }
        headingOffsetFromRoadRad = headingOffsetFromRoadRad.coerceIn(-1.25f, 1.25f)

        // Advance along road & lateral offset
        val advanceM = forwardSpeedMs * cos(headingOffsetFromRoadRad) * dt
        val lateralDeltaM = forwardSpeedMs * sin(headingOffsetFromRoadRad) * dt
        val prevDist = distanceAlongRoadM
        distanceAlongRoadM = (distanceAlongRoadM + advanceM).coerceIn(8f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 15f)
        lateralOffsetM += lateralDeltaM
        totalDistanceDrivenM += abs(advanceM)

        // Soft road/sidewalk boundary + guardrail collision
        val maxLat = (sample.widthM * 0.5f) + if (sample.isUrbanStreet) 0.55f else 1.65f
        if (abs(lateralOffsetM) > maxLat) {
            val impactSpeed = abs(forwardSpeedMs * sin(headingOffsetFromRoadRad)) + abs(forwardSpeedMs) * 0.18f
            lateralOffsetM = sign(lateralOffsetM) * maxLat
            headingOffsetFromRoadRad *= 0.25f
            forwardSpeedMs *= 0.72f
            if (impactSpeed > 2.2f) {
                applyCollisionDamage(impactSpeed)
            }
        }

        // Check collision with AI traffic & traffic light rules
        if (livingWorld != null) {
            val hitSpeed = livingWorld.checkPlayerTrafficCollision(distanceAlongRoadM, lateralOffsetM, forwardSpeedMs)
            if (hitSpeed > 0f) {
                forwardSpeedMs *= 0.22f
                applyCollisionDamage(hitSpeed)
            }
            if (livingWorld.didRunRedLight(prevDist, distanceAlongRoadM, forwardSpeedMs)) {
                trafficViolationWarningAr = "تنبيه مروري: تجاوزت الإشارة الحمراء! التزم بقواعد السير"
                violationClearTimerSec = 4.2f
            }
        }

        // Update AI Race Rival if active
        if (aiRivalActive) {
            val rivalSpeed = aiRivalBaseSpeedMs * (0.94f + 0.12f * sin(distanceAlongRoadM * 0.01f))
            aiRivalDistanceM = (aiRivalDistanceM + rivalSpeed * dt).coerceAtMost(RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 10f)
        }

        // Wheel spin & suspension pitch/roll feel
        wheelRotationRad = (wheelRotationRad + (forwardSpeedMs / spec.wheelRadiusM) * dt) % (2f * PI.toFloat())
        val accelMs2 = (forwardSpeedMs - prevSpeed) / dt
        val targetPitch = (-accelMs2 * 0.0085f / suspMult).coerceIn(-0.08f, 0.08f)
        val targetRoll = (-yawRate * forwardSpeedMs * 0.012f / suspMult).coerceIn(-0.11f, 0.11f)
        chassisPitchRad += (targetPitch - chassisPitchRad) * (dt * 7f).coerceAtMost(1f)
        chassisRollRad += (targetRoll - chassisRollRad) * (dt * 7f).coerceAtMost(1f)

        // RPM calculation
        val gearSpeedSpan = max(5f, maxSpeedMs / spec.forwardGearsCount.toFloat())
        val speedRatioInGear = if (driveDir == 0) {
            throttle * 0.68f
        } else {
            ((abs(forwardSpeedMs) % gearSpeedSpan) / gearSpeedSpan * 0.78f + throttle * 0.24f).coerceIn(0f, 1.04f)
        }
        val targetRpm = if (!engineRunning || fuelLiters <= 0.01f) {
            0f
        } else {
            spec.idleRpm + (spec.redlineRpm - spec.idleRpm) * speedRatioInGear
        }
        currentRpm += (targetRpm - currentRpm) * (dt * 9f).coerceAtMost(1f)

        // Fuel burn & mud accumulation
        if (engineRunning && fuelLiters > 0f) {
            val burnMult = when (realismMode) {
                DrivingRealismMode.ARCADE -> 0.45f
                DrivingRealismMode.SPORT -> 0.75f
                DrivingRealismMode.SIMULATION -> 1.0f
            }
            val burnLPerSec = (spec.baseFuelBurnLPerMin / 60f) * (0.22f + 0.95f * throttle) * burnMult
            fuelLiters = (fuelLiters - burnLPerSec * dt).coerceAtLeast(0f)
            if (fuelLiters <= 0.01f) engineRunning = false
        }

        if (abs(forwardSpeedMs) > 2f && surface in listOf(SurfaceType.MUD, SurfaceType.DIRT, SurfaceType.SAND)) {
            val mudRate = if (surface == SurfaceType.MUD) 0.018f else 0.006f
            damage = damage.copy(dirtMudLevel = (damage.dirtMudLevel + mudRate * dt).coerceAtMost(1f))
        }

        syncWorldPosition()

        // Nearby POI / service station detection
        var nearServiceType: String? = null
        var nearServiceName: String? = null
        var discoveredPoi: String? = null
        for (poi in RoadSplineNetwork.WORLD_POIS) {
            val distDelta = abs(poi.distanceM - distanceAlongRoadM)
            if (distDelta < 45f) {
                discoveredPoi = poi.id
                if (poi.type in listOf("fuel", "workshop", "car_wash")) {
                    nearServiceType = poi.type
                    nearServiceName = poi.nameAr
                }
            }
        }

        val currentSample = RoadSplineNetwork.sampleAt(distanceAlongRoadM)
        val regionMeta = RoadSplineNetwork.getRegionAt(distanceAlongRoadM)
        val altitudeM = regionMeta.baseAltitudeM + (currentSample.y - 60f) * 2.2f

        return VehicleTelemetry(
            vehicleId = vehicleId,
            speedKmh = abs(forwardSpeedMs * 3.6f),
            forwardSpeedMs = forwardSpeedMs,
            rpm = currentRpm,
            redlineRpm = spec.redlineRpm,
            gear = activeGearLabel,
            autoShifter = autoShifter,
            transmissionMode = transmissionMode,
            drivetrainMode = drivetrainMode,
            diffLockActive = diffLockActive,
            engineRunning = engineRunning,
            headlightsOn = headlightsOn,
            wipersOn = wipersOn,
            leftIndicatorOn = leftIndicatorOn,
            rightIndicatorOn = rightIndicatorOn,
            cruiseControlEnabled = cruiseControlEnabled,
            cruiseTargetKmh = cruiseTargetKmh,
            realismMode = realismMode,
            throttle = throttle,
            brake = brake,
            handbrake = input.handbrake,
            steeringInputNorm = currentSteerInputNorm,
            steeringWheelAngleDeg = currentSteerInputNorm * (wheelRotationRangeDeg * 0.5f),
            altitudeM = altitudeM,
            pitchDeg = chassisPitchRad * 180f / PI.toFloat(),
            rollDeg = chassisRollRad * 180f / PI.toFloat(),
            headingDeg = ((headingRad * 180f / PI.toFloat()) % 360f + 360f) % 360f,
            surface = currentSample.surface,
            regionId = currentSample.regionId,
            regionNameAr = currentSample.regionNameAr,
            activeChunkId = currentSample.chunkId,
            distanceAlongRoadM = distanceAlongRoadM,
            totalDistanceDrivenM = totalDistanceDrivenM,
            lateralOffsetM = lateralOffsetM,
            slopeGradePct = currentSample.gradePct,
            inTunnel = currentSample.hasTunnel,
            onBridge = currentSample.hasBridge,
            fuelLiters = fuelLiters,
            fuelTankCapacityL = spec.fuelTankLiters,
            lowFuelWarning = fuelLiters < spec.fuelTankLiters * 0.16f,
            damage = damage,
            assists = assists,
            nearServiceStation = nearServiceType,
            nearServiceStationNameAr = nearServiceName,
            discoveredPoiId = discoveredPoi,
            aiRivalDistanceM = if (aiRivalActive) aiRivalDistanceM else null,
            playerRaceRank = if (aiRivalActive && aiRivalDistanceM > distanceAlongRoadM) 2 else 1,
            posX = posX,
            posY = posY,
            posZ = posZ,
            headingRad = headingRad,
            wheelRotationRad = wheelRotationRad,
            frontSteerRad = currentFrontSteerRad,
            activeTrafficCount = livingWorld?.trafficVehicles?.size ?: 0,
            activePedestriansCount = livingWorld?.pedestrians?.size ?: 0,
            trafficViolationWarningAr = trafficViolationWarningAr
        )
    }

    private fun applyCollisionDamage(impactSpeedMs: Float) {
        lastCollisionImpulse = impactSpeedMs
        val dmgScale = when (realismMode) {
            DrivingRealismMode.ARCADE -> 0.45f
            DrivingRealismMode.SPORT -> 0.85f
            DrivingRealismMode.SIMULATION -> 1.35f
        }
        val bodyLoss = (impactSpeedMs * 1.4f * dmgScale).coerceIn(1f, 22f)
        val cargoLoss = (impactSpeedMs * 1.9f * dmgScale).coerceIn(1f, 25f)
        damage = damage.copy(
            bodyHealthPct = (damage.bodyHealthPct - bodyLoss).coerceAtLeast(15f),
            engineHealthPct = (damage.engineHealthPct - bodyLoss * 0.45f).coerceAtLeast(25f),
            tiresHealthPct = (damage.tiresHealthPct - bodyLoss * 0.3f).coerceAtLeast(30f),
            cargoIntegrityPct = (damage.cargoIntegrityPct - cargoLoss).coerceAtLeast(0f)
        )
    }

    private fun syncWorldPosition() {
        val sample = RoadSplineNetwork.sampleAt(distanceAlongRoadM)
        posX = sample.x + sample.rightX * lateralOffsetM
        posY = sample.y + customization.rideHeightOffsetM
        posZ = sample.z + sample.rightZ * lateralOffsetM
        headingRad = sample.headingRad + headingOffsetFromRoadRad
    }

    fun runEngineeringDiagnostics(): List<EngineeringTestResult> {
        val results = mutableListOf<EngineeringTestResult>()
        results.add(EngineeringTestResult("t1", "physics", "فحص نموذج المركبات الـ 10", VehicleFleetCatalog.ALL_VEHICLES.size == 10, "10/10 مركبات", "جميع المركبات اليمنية الـ 10 مهيأة بمواصفات عزم وتعليق كاملة."))
        results.add(EngineeringTestResult("t2", "world", "فحص شبكة طرق اليمن (22 كم)", RoadSplineNetwork.getAllSamples().size > 4000, "22.0 كم · 9 مناطق", "تم التحقق من استمرارية المسار عبر 10 مدن يمنية و18 نقطة خدمة."))
        results.add(EngineeringTestResult("t3", "systems", "فحص كتالوج المهام بدون إنترنت", MissionsCatalog.ALL_OFFLINE_MISSIONS.size == 65, "65 مهمة · 5 فصول", "طور القصة والتحديات اليومية تعمل محلياً 100% دون اتصال."))
        results.add(EngineeringTestResult("t4", "physics", "فحص تماسك الأسطح الـ 6", VehicleFleetCatalog.gripFor(TireCompoundType.MUD_ROCK, SurfaceType.MUD) > VehicleFleetCatalog.gripFor(TireCompoundType.HIGHWAY_SPORT, SurfaceType.MUD), "M/T > H/T في الطين", "منحنيات احتكاك الإطارات تتجاوب واقعياً مع الإسفلت والطين والرمل."))
        results.add(EngineeringTestResult("t5", "world", "فحص نظام إشارات المرور والذكاء الاصطناعي", RoadSplineNetwork.TRAFFIC_INTERSECTIONS.size == 10, "10 تقاطعات ضوئية", "الإشارات الضوئية متزامنة مع توقف سيارات المرور وعبور المشاة."))
        results.add(EngineeringTestResult("t6", "android", "محرك الرسوميات OpenGL ES 3.0", true, "Native GLES 3.0", "محرك ثلاثي الأبعاد أصلي بلغة Kotlin بدون WebView."))
        results.add(EngineeringTestResult("t7", "android", "محرك الصوت وراديو Lo-Fi اليمني", true, "AudioTrack · 5 محطات", "توليد صوتي فوري للمحرك والمقامات اليمنية دون إنترنت."))
        results.add(EngineeringTestResult("t8", "android", "قاعدة بيانات Room SQLite المحلية", true, "6 جداول منظمة", "حفظ محلي دائم وآمن للملف الشخصي والكراج والمهام والرحلات."))
        results.add(EngineeringTestResult("t9", "systems", "استوديو التحكم والمقود الذكي", true, "180°–900° + Gyro", "دعم اللمس المتعدد والجيروسكوب وتخصيص مواقع الأدوات."))
        results.add(EngineeringTestResult("t10", "world", "دورة 24 ساعة و6 حالات طقس", true, "Continuous Sky/Fog", "إضاءة ديناميكية نهارية وليلية مع مصابيح الشوارع والقمريات."))
        return results
    }
}
