package com.masalik.yemen.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

enum class TrafficVehicleType { SEDAN, PICKUP, MINIBUS, TRUCK }

data class TrafficVehicleAgent(
    val id: Int,
    var distanceM: Float,
    var speedMs: Float,
    val baseSpeedMs: Float,
    val direction: Int, // +1 forward lane (right), -1 oncoming lane (left)
    val laneOffsetM: Float,
    val type: TrafficVehicleType,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    var braking: Boolean = false,
    var headlightsOn: Boolean = false,
    var wheelSpinRad: Float = 0f,
    var worldX: Float = 0f,
    var worldY: Float = 0f,
    var worldZ: Float = 0f,
    var headingRad: Float = 0f
)

enum class PedestrianStyle { THOBE_WHITE, MAAWIS_COLORFUL, SHAWL_ELDER, ABAYA_CLASSIC }

data class PedestrianAgent(
    val id: Int,
    val anchorDistanceM: Float,
    var distanceM: Float,
    var lateralOffsetM: Float,
    val isCrosswalkPedestrian: Boolean,
    val intersectionId: String?,
    val sideSign: Int, // -1 left sidewalk, +1 right sidewalk
    var walkDir: Int, // +1 or -1 along sidewalk or across crosswalk
    val walkSpeedMs: Float,
    val style: PedestrianStyle,
    val accentR: Float,
    val accentG: Float,
    val accentB: Float,
    var walkPhaseRad: Float = 0f,
    var isMoving: Boolean = true,
    var worldX: Float = 0f,
    var worldY: Float = 0f,
    var worldZ: Float = 0f,
    var headingRad: Float = 0f
)

data class ParkedCarPlacement(
    val distanceM: Float,
    val lateralOffsetM: Float,
    val type: TrafficVehicleType,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val headingRad: Float
)

data class TrafficSignalRuntime(
    val def: TrafficIntersectionDef,
    var phase: TrafficSignalPhase = TrafficSignalPhase.GREEN,
    var secondsRemaining: Int = 10,
    var pedestriansCrossing: Boolean = false
)

data class AtmosphereSnapshot(
    val skyR: Float,
    val skyG: Float,
    val skyB: Float,
    val horizonR: Float,
    val horizonG: Float,
    val horizonB: Float,
    val sunR: Float,
    val sunG: Float,
    val sunB: Float,
    val sunIntensity: Float,
    val ambientIntensity: Float,
    val nightFactor: Float,
    val fogDensity: Float,
    val sunDirX: Float,
    val sunDirY: Float,
    val sunDirZ: Float
)

class LivingWorldSimulation {

    val trafficVehicles: MutableList<TrafficVehicleAgent> = mutableListOf()
    val pedestrians: MutableList<PedestrianAgent> = mutableListOf()
    val parkedCars: List<ParkedCarPlacement>
    val trafficSignals: List<TrafficSignalRuntime> =
        RoadSplineNetwork.TRAFFIC_INTERSECTIONS.map { TrafficSignalRuntime(it) }

    var nearbyTrafficSignal: NearbyTrafficSignalInfo? = null
        private set

    private var elapsedSec: Float = 0f
    private val carColors = listOf(
        Triple(0.92f, 0.93f, 0.95f), // Yemeni pearl white pickup/sedan
        Triple(0.76f, 0.78f, 0.80f), // Silver metallic
        Triple(0.82f, 0.72f, 0.50f), // Desert champagne
        Triple(0.16f, 0.36f, 0.62f), // Coastal blue
        Triple(0.68f, 0.18f, 0.16f), // Deep crimson
        Triple(0.20f, 0.24f, 0.28f), // Basalt charcoal
        Triple(0.86f, 0.56f, 0.14f), // Taxi amber/gold
        Triple(0.18f, 0.44f, 0.32f)  // Highland green
    )

    init {
        parkedCars = generateRoadsideParkedCars()
        spawnInitialTraffic(60f)
        spawnCityPedestrians()
    }

    private fun spawnInitialTraffic(centerDistanceM: Float) {
        trafficVehicles.clear()
        val types = TrafficVehicleType.entries
        for (i in 0 until 26) {
            val dir = if (i % 2 == 0) 1 else -1
            val dist = (centerDistanceM + (i - 12) * 34f + (i % 3) * 7f)
                .coerceIn(25f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 30f)
            val type = types[i % types.size]
            val baseSpeed = when (type) {
                TrafficVehicleType.SEDAN -> 13.5f + (i % 4) * 1.1f
                TrafficVehicleType.PICKUP -> 12.2f + (i % 3) * 1.0f
                TrafficVehicleType.MINIBUS -> 11.0f + (i % 3) * 0.8f
                TrafficVehicleType.TRUCK -> 9.4f + (i % 3) * 0.7f
            }
            val color = carColors[i % carColors.size]
            val laneOffset = if (dir > 0) 2.75f else -2.75f
            trafficVehicles.add(
                TrafficVehicleAgent(
                    id = i,
                    distanceM = dist,
                    speedMs = baseSpeed * 0.8f,
                    baseSpeedMs = baseSpeed,
                    direction = dir,
                    laneOffsetM = laneOffset,
                    type = type,
                    colorR = color.first,
                    colorG = color.second,
                    colorB = color.third
                )
            )
        }
    }

    private fun spawnCityPedestrians() {
        pedestrians.clear()
        var nextId = 1
        val styles = PedestrianStyle.entries
        val accents = listOf(
            Triple(0.85f, 0.22f, 0.20f),
            Triple(0.15f, 0.52f, 0.42f),
            Triple(0.86f, 0.62f, 0.18f),
            Triple(0.24f, 0.42f, 0.74f),
            Triple(0.62f, 0.28f, 0.46f)
        )

        // 1. Crosswalk pedestrians at every traffic light intersection (2 per intersection)
        for ((idx, sig) in RoadSplineNetwork.TRAFFIC_INTERSECTIONS.withIndex()) {
            for (k in 0..1) {
                val side = if (k == 0) -1 else 1
                val style = styles[(idx + k) % styles.size]
                val acc = accents[(idx + k * 2) % accents.size]
                val sample = RoadSplineNetwork.sampleAt(sig.distanceM + 2.5f)
                val lat = side * (sample.widthM * 0.5f + 1.35f)
                pedestrians.add(
                    PedestrianAgent(
                        id = nextId++,
                        anchorDistanceM = sig.distanceM + 2.5f,
                        distanceM = sig.distanceM + 2.5f + (if (k == 0) -0.8f else 0.8f),
                        lateralOffsetM = lat,
                        isCrosswalkPedestrian = true,
                        intersectionId = sig.id,
                        sideSign = side,
                        walkDir = -side,
                        walkSpeedMs = 1.45f,
                        style = style,
                        accentR = acc.first,
                        accentG = acc.second,
                        accentB = acc.third,
                        walkPhaseRad = (idx * 1.3f + k)
                    )
                )
            }
        }

        // 2. Sidewalk residents strolling through every Yemeni city hub & village market
        for ((hubIdx, hub) in RoadSplineNetwork.YEMEN_CITY_HUBS.withIndex()) {
            val countForHub = if (hub.id in listOf("city_taiz_sabir", "city_sanaa_old", "city_shibam_hadramout", "city_aden_mokha_coast")) 8 else 5
            for (p in 0 until countForHub) {
                val offsetAlong = (p - countForHub / 2) * 28f + ((hubIdx * 13 + p * 7) % 15)
                val dist = (hub.distanceM + offsetAlong).coerceIn(25f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 40f)
                val sample = RoadSplineNetwork.sampleAt(dist)
                val side = if (p % 2 == 0) -1 else 1
                val lat = side * (sample.widthM * 0.5f + 1.65f + (p % 3) * 0.45f)
                val style = styles[(hubIdx + p) % styles.size]
                val acc = accents[(hubIdx * 3 + p) % accents.size]
                pedestrians.add(
                    PedestrianAgent(
                        id = nextId++,
                        anchorDistanceM = dist,
                        distanceM = dist,
                        lateralOffsetM = lat,
                        isCrosswalkPedestrian = false,
                        intersectionId = null,
                        sideSign = side,
                        walkDir = if (p % 3 == 0) -1 else 1,
                        walkSpeedMs = 1.05f + (p % 4) * 0.18f,
                        style = style,
                        accentR = acc.first,
                        accentG = acc.second,
                        accentB = acc.third,
                        walkPhaseRad = p * 0.9f
                    )
                )
            }
        }
    }

    private fun generateRoadsideParkedCars(): List<ParkedCarPlacement> {
        val list = mutableListOf<ParkedCarPlacement>()
        val types = listOf(TrafficVehicleType.PICKUP, TrafficVehicleType.SEDAN, TrafficVehicleType.MINIBUS, TrafficVehicleType.PICKUP)
        var d = 48f
        var idx = 0
        while (d < RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 60f) {
            val sample = RoadSplineNetwork.sampleAt(d)
            if (sample.isUrbanStreet && !sample.hasTunnel && !sample.hasBridge) {
                // Keep clear of traffic signal intersections
                val nearSignal = RoadSplineNetwork.TRAFFIC_INTERSECTIONS.any { abs(it.distanceM - d) < 26f }
                if (!nearSignal) {
                    val side = if (idx % 2 == 0) 1 else -1
                    val lat = side * (sample.widthM * 0.5f + 0.15f)
                    val color = carColors[(idx * 3 + 1) % carColors.size]
                    val heading = if (side > 0) sample.headingRad else sample.headingRad + PI.toFloat()
                    list.add(
                        ParkedCarPlacement(
                            distanceM = d,
                            lateralOffsetM = lat,
                            type = types[idx % types.size],
                            colorR = color.first,
                            colorG = color.second,
                            colorB = color.third,
                            worldX = sample.x + sample.rightX * lat,
                            worldY = sample.y,
                            worldZ = sample.z + sample.rightZ * lat,
                            headingRad = heading
                        )
                    )
                }
                d += 34f + (idx % 3) * 14f
            } else {
                d += 140f
            }
            idx++
        }
        return list
    }

    fun update(
        dt: Float,
        playerDistanceM: Float,
        playerLateralM: Float,
        playerSpeedMs: Float,
        nightFactor: Float
    ) {
        elapsedSec += dt

        // 1. Update Traffic Signals (26-second realistic cycle: 12s Green, 3s Yellow, 11s Red)
        var closestSignalAhead: NearbyTrafficSignalInfo? = null
        var bestAheadDist = 115f
        for (sig in trafficSignals) {
            val cyclePos = (elapsedSec + sig.def.cycleOffsetSec) % 26f
            when {
                cyclePos < 12f -> {
                    sig.phase = TrafficSignalPhase.GREEN
                    sig.secondsRemaining = (12f - cyclePos).toInt().coerceAtLeast(1)
                    sig.pedestriansCrossing = false
                }
                cyclePos < 15f -> {
                    sig.phase = TrafficSignalPhase.YELLOW
                    sig.secondsRemaining = (15f - cyclePos).toInt().coerceAtLeast(1)
                    sig.pedestriansCrossing = false
                }
                else -> {
                    sig.phase = TrafficSignalPhase.RED
                    sig.secondsRemaining = (26f - cyclePos).toInt().coerceAtLeast(1)
                    sig.pedestriansCrossing = cyclePos < 24.5f
                }
            }
            val ahead = sig.def.distanceM - playerDistanceM
            if (ahead in -4f..bestAheadDist) {
                bestAheadDist = ahead
                closestSignalAhead = NearbyTrafficSignalInfo(
                    id = sig.def.id,
                    nameAr = sig.def.nameAr,
                    distanceAheadM = ahead.coerceAtLeast(0f),
                    phase = sig.phase,
                    secondsRemaining = sig.secondsRemaining,
                    pedestriansCrossing = sig.pedestriansCrossing
                )
            }
        }
        nearbyTrafficSignal = closestSignalAhead

        // 2. Update Pedestrians (Sidewalk stroll + Crosswalk signal-obeying crossing)
        for (ped in pedestrians) {
            if (abs(ped.anchorDistanceM - playerDistanceM) > 260f) continue
            val sample = RoadSplineNetwork.sampleAt(ped.distanceM)
            val halfRoad = sample.widthM * 0.5f

            if (ped.isCrosswalkPedestrian) {
                val sig = trafficSignals.firstOrNull { it.def.id == ped.intersectionId }
                val playerVeryClose = abs(ped.distanceM - playerDistanceM) < 9f && abs(playerSpeedMs) > 4f
                val onRoad = abs(ped.lateralOffsetM) < halfRoad + 0.4f
                val canCross = (sig?.pedestriansCrossing == true) && !playerVeryClose

                if (canCross || onRoad) {
                    ped.isMoving = true
                    ped.lateralOffsetM += ped.walkDir * ped.walkSpeedMs * dt
                    val targetEdge = -ped.sideSign * (halfRoad + 1.45f)
                    if ((ped.walkDir > 0 && ped.lateralOffsetM >= halfRoad + 1.45f) ||
                        (ped.walkDir < 0 && ped.lateralOffsetM <= -(halfRoad + 1.45f))
                    ) {
                        ped.lateralOffsetM = ped.lateralOffsetM.coerceIn(-(halfRoad + 1.45f), halfRoad + 1.45f)
                        ped.walkDir = -ped.walkDir
                        ped.isMoving = false
                    }
                    ped.headingRad = sample.headingRad + if (ped.walkDir > 0) (PI.toFloat() * 0.5f) else (-PI.toFloat() * 0.5f)
                } else {
                    ped.isMoving = false
                    ped.headingRad = sample.headingRad + if (ped.walkDir > 0) (PI.toFloat() * 0.5f) else (-PI.toFloat() * 0.5f)
                }
            } else {
                // Sidewalk resident walking back and forth along Yemeni street shops
                ped.isMoving = true
                ped.distanceM += ped.walkDir * ped.walkSpeedMs * dt
                if (abs(ped.distanceM - ped.anchorDistanceM) > 36f) {
                    ped.walkDir = -ped.walkDir
                }
                ped.headingRad = if (ped.walkDir > 0) sample.headingRad else sample.headingRad + PI.toFloat()
            }

            if (ped.isMoving) {
                ped.walkPhaseRad = (ped.walkPhaseRad + ped.walkSpeedMs * 4.4f * dt) % (2f * PI.toFloat())
            }
            val pedSample = RoadSplineNetwork.sampleAt(ped.distanceM)
            val sidewalkLift = if (abs(ped.lateralOffsetM) > halfRoad && pedSample.isUrbanStreet) 0.18f else 0f
            ped.worldX = pedSample.x + pedSample.rightX * ped.lateralOffsetM
            ped.worldY = pedSample.y + sidewalkLift
            ped.worldZ = pedSample.z + pedSample.rightZ * ped.lateralOffsetM
        }

        // 3. Update Rule-Following AI Traffic Cars
        for (car in trafficVehicles) {
            // Respawn far-away cars ahead/behind the player to keep every city street bustling
            val deltaToPlayer = car.distanceM - playerDistanceM
            if (deltaToPlayer > 380f) {
                car.distanceM = (playerDistanceM - 240f - (car.id % 5) * 18f)
                    .coerceIn(20f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 25f)
            } else if (deltaToPlayer < -290f) {
                car.distanceM = (playerDistanceM + 260f + (car.id % 5) * 20f)
                    .coerceIn(20f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 25f)
            }

            val sample = RoadSplineNetwork.sampleAt(car.distanceM)
            var targetSpeed = car.baseSpeedMs * if (sample.isUrbanStreet) 0.78f else 1.0f

            // Rule A: Stop cleanly at RED or YELLOW traffic signals before the crosswalk
            for (sig in trafficSignals) {
                val distToStopLine = (sig.def.distanceM - car.distanceM) * car.direction - 4.8f
                if (distToStopLine in -1.5f..36f) {
                    if (sig.phase == TrafficSignalPhase.RED || (sig.phase == TrafficSignalPhase.YELLOW && distToStopLine > 7f)) {
                        targetSpeed = if (distToStopLine <= 2.2f) 0f else minOf(targetSpeed, distToStopLine * 0.42f)
                    }
                }
            }

            // Rule B: Yield to any pedestrian currently on the road ahead
            for (ped in pedestrians) {
                if (ped.isCrosswalkPedestrian && abs(ped.lateralOffsetM) < sample.widthM * 0.5f + 0.2f) {
                    val ahead = (ped.distanceM - car.distanceM) * car.direction
                    if (ahead in 2f..22f) {
                        targetSpeed = 0f
                        break
                    }
                }
            }

            // Rule C: Maintain safe following distance from other AI cars in same lane
            for (other in trafficVehicles) {
                if (other.id == car.id || other.direction != car.direction) continue
                val gap = (other.distanceM - car.distanceM) * car.direction
                if (gap in 1f..24f) {
                    val safeLimit = if (gap < 11.5f) 0f else other.speedMs * ((gap - 10f) / 14f).coerceIn(0f, 1f)
                    targetSpeed = minOf(targetSpeed, safeLimit)
                }
            }

            // Rule D: Yield cleanly to player vehicle if player is ahead in the same lane
            val sameLaneAsPlayer = abs(playerLateralM - car.laneOffsetM) < 2.25f
            if (sameLaneAsPlayer) {
                val gapToPlayer = (playerDistanceM - car.distanceM) * car.direction
                if (gapToPlayer in 2f..26f) {
                    val playerDirSpeed = maxOf(0f, playerSpeedMs * car.direction)
                    targetSpeed = if (gapToPlayer < 12f) 0f else minOf(targetSpeed, playerDirSpeed * 0.9f)
                }
            }

            // Smooth acceleration & braking
            val prevSpeed = car.speedMs
            val rate = if (targetSpeed < car.speedMs) 9.5f else 4.2f
            car.speedMs += (targetSpeed - car.speedMs).coerceIn(-rate * dt, rate * dt)
            car.speedMs = car.speedMs.coerceAtLeast(0f)
            car.braking = (targetSpeed < prevSpeed - 0.8f) || car.speedMs < 0.4f
            car.headlightsOn = nightFactor > 0.32f || sample.hasTunnel

            car.distanceM = (car.distanceM + car.direction * car.speedMs * dt)
                .coerceIn(15f, RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 15f)
            car.wheelSpinRad = (car.wheelSpinRad + (car.speedMs / 0.38f) * dt) % (2f * PI.toFloat())

            val updatedSample = RoadSplineNetwork.sampleAt(car.distanceM)
            car.worldX = updatedSample.x + updatedSample.rightX * car.laneOffsetM
            car.worldY = updatedSample.y
            car.worldZ = updatedSample.z + updatedSample.rightZ * car.laneOffsetM
            car.headingRad = if (car.direction > 0) updatedSample.headingRad else updatedSample.headingRad + PI.toFloat()
        }
    }

    fun checkPlayerTrafficCollision(playerDistM: Float, playerLatM: Float, playerSpeedMs: Float): Float {
        for (car in trafficVehicles) {
            val dDiff = abs(car.distanceM - playerDistM)
            val lDiff = abs(car.laneOffsetM - playerLatM)
            val carHalfLen = if (car.type == TrafficVehicleType.TRUCK) 3.4f else 2.5f
            if (dDiff < carHalfLen + 1.8f && lDiff < 1.95f) {
                val rel = abs(playerSpeedMs - car.speedMs * car.direction)
                if (rel > 2.0f) {
                    car.speedMs = 0f
                    return rel
                }
            }
        }
        return 0f
    }

    fun didRunRedLight(prevDistM: Float, newDistM: Float, speedMs: Float): Boolean {
        if (speedMs < 3.5f) return false
        for (sig in trafficSignals) {
            if (sig.phase == TrafficSignalPhase.RED && prevDistM < sig.def.distanceM && newDistM >= sig.def.distanceM) {
                return true
            }
        }
        return false
    }

    companion object {
        fun formatWorldTime(rawHours: Float): String {
            val h = ((rawHours % 24f) + 24f) % 24f
            val hh = floor(h).toInt()
            val mm = floor((h - hh) * 60f).toInt()
            return "%02d:%02d".format(hh, mm)
        }

        fun hoursToPreset(rawHours: Float): TimeOfDayPreset {
            val h = ((rawHours % 24f) + 24f) % 24f
            return when {
                h in 4.8f..5.9f -> TimeOfDayPreset.DAWN
                h in 5.9f..7.2f -> TimeOfDayPreset.SUNRISE
                h in 7.2f..11.2f -> TimeOfDayPreset.MORNING
                h in 11.2f..14.5f -> TimeOfDayPreset.NOON
                h in 14.5f..17.2f -> TimeOfDayPreset.AFTERNOON
                h in 17.2f..19.3f -> TimeOfDayPreset.SUNSET
                else -> TimeOfDayPreset.NIGHT
            }
        }

        fun sampleAtmosphere(rawHours: Float, weather: WeatherPreset): AtmosphereSnapshot {
            val h = ((rawHours % 24f) + 24f) % 24f
            val sunAngle = ((h - 6f) / 12f) * PI.toFloat()
            val sunY = sin(sunAngle).coerceIn(-0.4f, 0.98f)
            val sunX = cos(sunAngle) * 0.72f
            val sunZ = 0.48f

            val dayFactor = ((sunY + 0.12f) / 0.55f).coerceIn(0f, 1f)
            val sunsetFactor = (1f - abs(sunY - 0.08f) * 3.8f).coerceIn(0f, 1f)
            val nightFactor = (1f - dayFactor).coerceIn(0f, 1f)

            var skyR = lerp(0.03f, 0.36f, dayFactor) + sunsetFactor * 0.42f
            var skyG = lerp(0.05f, 0.62f, dayFactor) + sunsetFactor * 0.14f
            var skyB = lerp(0.11f, 0.92f, dayFactor) - sunsetFactor * 0.28f

            var horR = lerp(0.06f, 0.72f, dayFactor) + sunsetFactor * 0.25f
            var horG = lerp(0.09f, 0.80f, dayFactor) + sunsetFactor * 0.06f
            var horB = lerp(0.16f, 0.92f, dayFactor) - sunsetFactor * 0.35f

            var fogDensity = 0.0022f
            var sunIntensity = lerp(0.28f, 1.15f, dayFactor)

            when (weather) {
                WeatherPreset.CLOUDY -> {
                    skyR *= 0.80f; skyG *= 0.82f; skyB *= 0.86f
                    fogDensity = 0.0038f
                    sunIntensity *= 0.75f
                }
                WeatherPreset.LIGHT_RAIN -> {
                    skyR = lerp(skyR, 0.34f, 0.55f); skyG = lerp(skyG, 0.40f, 0.55f); skyB = lerp(skyB, 0.48f, 0.55f)
                    fogDensity = 0.0055f
                    sunIntensity *= 0.62f
                }
                WeatherPreset.HEAVY_RAIN -> {
                    skyR = lerp(skyR, 0.20f, 0.7f); skyG = lerp(skyG, 0.25f, 0.7f); skyB = lerp(skyB, 0.32f, 0.7f)
                    fogDensity = 0.0085f
                    sunIntensity *= 0.45f
                }
                WeatherPreset.MOUNTAIN_FOG -> {
                    skyR = lerp(skyR, 0.68f, 0.65f); skyG = lerp(skyG, 0.72f, 0.65f); skyB = lerp(skyB, 0.76f, 0.65f)
                    horR = skyR; horG = skyG; horB = skyB
                    fogDensity = 0.0135f
                    sunIntensity *= 0.55f
                }
                WeatherPreset.SANDSTORM -> {
                    skyR = lerp(skyR, 0.76f, 0.78f); skyG = lerp(skyG, 0.56f, 0.78f); skyB = lerp(skyB, 0.32f, 0.78f)
                    horR = 0.78f; horG = 0.58f; horB = 0.34f
                    fogDensity = 0.0145f
                    sunIntensity *= 0.58f
                }
                WeatherPreset.CLEAR -> Unit
            }

            return AtmosphereSnapshot(
                skyR = skyR.coerceIn(0.02f, 0.96f),
                skyG = skyG.coerceIn(0.04f, 0.96f),
                skyB = skyB.coerceIn(0.09f, 0.98f),
                horizonR = horR.coerceIn(0.04f, 0.96f),
                horizonG = horG.coerceIn(0.06f, 0.96f),
                horizonB = horB.coerceIn(0.12f, 0.96f),
                sunR = (1.0f).coerceIn(0f, 1f),
                sunG = (lerp(0.68f, 0.96f, dayFactor) - sunsetFactor * 0.2f).coerceIn(0.45f, 1f),
                sunB = (lerp(0.85f, 0.88f, dayFactor) - sunsetFactor * 0.45f).coerceIn(0.28f, 1f),
                sunIntensity = sunIntensity,
                ambientIntensity = lerp(0.32f, 0.56f, dayFactor),
                nightFactor = nightFactor,
                fogDensity = fogDensity,
                sunDirX = sunX,
                sunDirY = maxOf(0.18f, sunY),
                sunDirZ = sunZ
            )
        }

        private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    }
}
