package com.masalik.yemen.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DrivingRegressionTest {
    @Test
    fun roadEndDoesNotAccumulateSpeedOrMileage() {
        val physics = VehiclePhysicsEngine()
        val end = RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 15f
        physics.resetToRoad(end)

        repeat(600) { physics.step(1f / 60f, ControlInputs(throttle = 1f), null) }

        assertEquals(end, physics.distanceAlongRoadM, 0f)
        assertEquals(0f, physics.forwardSpeedMs, 0f)
        assertEquals(0f, physics.totalDistanceDrivenM, 0f)
    }

    @Test
    fun allRegionBoundariesHaveContinuousRoadElevation() {
        val samples = RoadSplineNetwork.getAllSamples()
        assertTrue(samples.asList().zipWithNext().all { (a, b) -> abs(b.y - a.y) <= 0.8f })
    }

    @Test
    fun catalogUsesUniqueIdsAndReachableFinishPoints() {
        val missions = MissionsCatalog.ALL_OFFLINE_MISSIONS
        assertEquals(65, missions.size)
        assertEquals(missions.size, missions.map { it.id }.toSet().size)
        assertTrue(missions.all {
            it.startDistanceM < it.finishDistanceM &&
                it.finishDistanceM <= RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 15f
        })
    }

    @Test
    fun cargoAndTimeLimitsPreventCompletion() {
        val mission = MissionsCatalog.ALL_OFFLINE_MISSIONS.first { it.category == GameModeCategory.CARGO }
        val atFinish = VehicleTelemetry(distanceAlongRoadM = mission.finishDistanceM)
        val damaged = atFinish.copy(damage = DamageState(cargoIntegrityPct = 40f))

        assertEquals(MissionOutcome.CARGO_DAMAGED, MissionRules.evaluate(mission, damaged, 15f, false))
        assertEquals(MissionOutcome.TIME_EXPIRED, MissionRules.evaluate(mission, atFinish, mission.targetTimeSec + 1f, false))
        assertEquals(MissionOutcome.SUCCESS, MissionRules.evaluate(mission, atFinish, 15f, false))
    }

    @Test
    fun rivalCanWinBeforePlayerReachesTheFinish() {
        val mission = MissionsCatalog.ALL_OFFLINE_MISSIONS.first { it.hasAiRival }
        val behind = VehicleTelemetry(
            distanceAlongRoadM = mission.finishDistanceM - 40f,
            aiRivalDistanceM = mission.finishDistanceM,
            playerRaceRank = 2
        )
        assertEquals(MissionOutcome.RIVAL_WON, MissionRules.evaluate(mission, behind, 15f, false))
    }

    @Test
    fun rescueRequiresPickupAndAStopAtTheDestination() {
        val mission = MissionsCatalog.ALL_OFFLINE_MISSIONS.first { it.category == GameModeCategory.RESCUE }
        val stopped = VehicleTelemetry(distanceAlongRoadM = mission.finishDistanceM, speedKmh = 0f)

        assertNull(MissionRules.evaluate(mission, stopped, 15f, false))
        assertNull(MissionRules.evaluate(mission, stopped.copy(speedKmh = 25f), 15f, true))
        assertEquals(MissionOutcome.SUCCESS, MissionRules.evaluate(mission, stopped, 15f, true))
    }
}
