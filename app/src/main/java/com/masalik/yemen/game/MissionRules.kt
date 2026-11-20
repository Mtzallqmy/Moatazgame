package com.masalik.yemen.game

import kotlin.math.abs

enum class MissionOutcome(val messageAr: String) {
    SUCCESS("أُنجزت المهمة"),
    TIME_EXPIRED("انتهى الوقت"),
    CARGO_DAMAGED("انخفضت سلامة الحمولة عن الحد المطلوب"),
    RIVAL_WON("وصل المنافس أولًا")
}

data class MissionRunResult(
    val mission: MissionDefinition,
    val outcome: MissionOutcome,
    val elapsedSec: Float
)

object MissionRules {
    private const val STOP_RADIUS_M = 15f
    private const val STOP_SPEED_KMH = 2f

    fun pickupDistanceM(mission: MissionDefinition): Float = mission.startDistanceM + 40f

    fun isStoppedAt(telemetry: VehicleTelemetry, distanceM: Float): Boolean =
        abs(telemetry.distanceAlongRoadM - distanceM) <= STOP_RADIUS_M && telemetry.speedKmh <= STOP_SPEED_KMH

    fun evaluate(
        mission: MissionDefinition,
        telemetry: VehicleTelemetry,
        elapsedSec: Float,
        pickedUp: Boolean
    ): MissionOutcome? {
        if (elapsedSec > mission.targetTimeSec) return MissionOutcome.TIME_EXPIRED
        if (telemetry.damage.cargoIntegrityPct < mission.cargoRequiredPct) return MissionOutcome.CARGO_DAMAGED

        val finishM = mission.finishDistanceM - STOP_RADIUS_M
        if (mission.hasAiRival && (telemetry.aiRivalDistanceM ?: 0f) >= finishM && telemetry.playerRaceRank > 1) {
            return MissionOutcome.RIVAL_WON
        }

        val finished = if (mission.requiresServiceStops) {
            pickedUp && isStoppedAt(telemetry, mission.finishDistanceM)
        } else {
            telemetry.distanceAlongRoadM >= finishM
        }
        return if (finished) MissionOutcome.SUCCESS else null
    }
}
