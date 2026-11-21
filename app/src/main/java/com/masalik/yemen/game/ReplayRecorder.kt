package com.masalik.yemen.game

/**
 * Lock-free circular 20-second driving telemetry recorder (30Hz = 600 frames)
 * powering the Instant Cinematic Replay Studio, Slow-Motion & Drone Camera.
 */
data class ReplayFrame(
    val telemetry: VehicleTelemetry,
    val worldTimeHours: Float,
    val weather: WeatherPreset,
    val blinkerOn: Boolean
)

class ReplayRecorder(private val maxFrames: Int = 600) {

    private val ring = arrayOfNulls<ReplayFrame>(maxFrames)
    private var writeIndex = 0
    private var count = 0
    private var sampleAccumSec = 0f
    private var playbackSnapshot: List<ReplayFrame> = emptyList()

    @Volatile var playbackProgress: Float = 0f
    @Volatile var playbackSpeed: Float = 1.0f // 0.25x, 0.5x, 1.0x
    @Volatile var isPlaying: Boolean = true
    @Volatile var cameraAngle: ReplayCameraAngle = ReplayCameraAngle.DRONE_ORBIT

    fun recordStep(dt: Float, frame: ReplayFrame) {
        sampleAccumSec += dt
        if (sampleAccumSec < 0.033f) return
        sampleAccumSec = 0f
        synchronized(this) {
            ring[writeIndex] = frame
            writeIndex = (writeIndex + 1) % maxFrames
            if (count < maxFrames) count++
        }
    }

    fun hasRecording(): Boolean = synchronized(this) { count >= 8 }

    fun enterReplaySession(fallbackFrame: ReplayFrame) {
        synchronized(this) {
            val ordered = ArrayList<ReplayFrame>(count.coerceAtLeast(1))
            if (count == 0) {
                ordered.add(fallbackFrame)
            } else {
                val start = if (count < maxFrames) 0 else writeIndex
                for (i in 0 until count) {
                    val idx = (start + i) % maxFrames
                    ring[idx]?.let { ordered.add(it) }
                }
            }
            playbackSnapshot = ordered
        }
        playbackProgress = 0f
        playbackSpeed = 0.5f
        isPlaying = true
    }

    fun stepPlayback(dt: Float): ReplayFrame? {
        val snap = playbackSnapshot
        if (snap.isEmpty()) return null
        val totalDurationSec = (snap.size * 0.033f).coerceAtLeast(1f)
        if (isPlaying) {
            playbackProgress += (dt * playbackSpeed) / totalDurationSec
            if (playbackProgress > 1f) playbackProgress = 0f
        }
        val idx = ((playbackProgress.coerceIn(0f, 1f)) * (snap.size - 1)).toInt().coerceIn(0, snap.lastIndex)
        return snap[idx]
    }

    fun durationSeconds(): Float = (playbackSnapshot.size * 0.033f).coerceAtLeast(0f)

    fun clear() {
        synchronized(this) {
            for (i in ring.indices) ring[i] = null
            writeIndex = 0
            count = 0
            playbackSnapshot = emptyList()
        }
    }
}
