package com.masalik.yemen.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.masalik.yemen.game.AudioMixerSettings
import com.masalik.yemen.game.RadioPlayerSettings
import com.masalik.yemen.game.RadioStationDefinition
import com.masalik.yemen.game.RadioStationId
import com.masalik.yemen.game.SurfaceType
import com.masalik.yemen.game.WeatherPreset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object YemeniRadioCatalog {
    val STATIONS: List<RadioStationDefinition> = listOf(
        RadioStationDefinition(
            RadioStationId.TAIZ_OUD_LOFI, "92.4 FM", "إذاعة تعز الجبلية",
            "عود صبري دافئ × Lo-Fi مسائي", "تعز — جبل صبر", "مقام حجاز", 76, "#F59E0B"
        ),
        RadioStationDefinition(
            RadioStationId.SANAA_QAMARIYA_CHILL, "96.8 FM", "نسيم صنعاء القديمة",
            "قانون وناي صنعاني × Chillhop", "صنعاء — باب اليمن", "مقام راست", 72, "#38BDF8"
        ),
        RadioStationDefinition(
            RadioStationId.IBB_RAIN_OUD, "101.2 FM", "ليالي إب الخضراء",
            "عود المدرجات والمطر الهادئ", "إب — نقيل سمارة", "مقام بياتي", 68, "#10B981"
        ),
        RadioStationDefinition(
            RadioStationId.ADEN_COAST_BREEZE, "104.5 FM", "موج عدن الساحلي",
            "إيقاع عدني هادئ × Lo-Fi بحري", "عدن — خليج صيرة", "مقام كرد", 82, "#FB7185"
        ),
        RadioStationDefinition(
            RadioStationId.HADRAMOUT_DAN_LOFI, "107.9 FM", "صدى شبام وحضرموت",
            "دان حضرمي آلي × Ambient Lo-Fi", "شبام — وادي حضرموت", "خماسي حضرمي", 74, "#EAB308"
        )
    )

    fun stationOf(id: RadioStationId): RadioStationDefinition =
        STATIONS.firstOrNull { it.id == id } ?: STATIONS.first()
}

private data class StationSynthSpec(
    val bpm: Int,
    val scaleHz: FloatArray,
    val oudPattern: IntArray,
    val chordRootsHz: FloatArray
)

class MasalikAudioEngine {

    private val sampleRate = 22050
    private val running = AtomicBoolean(false)
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    @Volatile var mixer: AudioMixerSettings = AudioMixerSettings()
    @Volatile var radioSettings: RadioPlayerSettings = RadioPlayerSettings()
    @Volatile var engineRunning: Boolean = true
    @Volatile var engineBaseFreqHz: Float = 46f
    @Volatile var engineRpm: Float = 800f
    @Volatile var throttle: Float = 0f
    @Volatile var speedKmh: Float = 0f
    @Volatile var surface: SurfaceType = SurfaceType.ASPHALT
    @Volatile var weather: WeatherPreset = WeatherPreset.CLEAR
    @Volatile var hornActive: Boolean = false
    @Volatile var drivingActive: Boolean = false

    @Volatile private var collisionTrigger: Float = 0f

    private val stationSpecs: Map<RadioStationId, StationSynthSpec> = mapOf(
        RadioStationId.TAIZ_OUD_LOFI to StationSynthSpec(
            76,
            floatArrayOf(146.83f, 155.56f, 185.0f, 196.0f, 220.0f, 233.08f, 277.18f, 293.66f),
            intArrayOf(0, -1, 2, 3, 4, -1, 3, 2, 3, -1, 4, 5, 4, 3, 2, -1, 7, -1, 5, 4, 3, -1, 2, 1, 0, -1, 1, 2, 0, -1, -1, -1),
            floatArrayOf(146.83f, 116.54f, 130.81f, 146.83f)
        ),
        RadioStationId.SANAA_QAMARIYA_CHILL to StationSynthSpec(
            72,
            floatArrayOf(130.81f, 146.83f, 161.0f, 174.61f, 196.0f, 220.0f, 241.5f, 261.63f),
            intArrayOf(0, -1, 4, -1, 3, 2, 1, -1, 2, -1, 4, 5, 7, -1, 5, 4, 7, -1, 5, 4, 3, -1, 2, 1, 0, -1, 4, -1, 0, -1, -1, -1),
            floatArrayOf(130.81f, 174.61f, 146.83f, 196.0f)
        ),
        RadioStationId.IBB_RAIN_OUD to StationSynthSpec(
            68,
            floatArrayOf(146.83f, 160.2f, 174.61f, 196.0f, 220.0f, 233.08f, 261.63f, 293.66f),
            intArrayOf(0, -1, -1, 2, 3, -1, 2, 1, 0, -1, 3, 4, 5, -1, 4, -1, 7, -1, 6, 5, 4, -1, 3, 2, 1, -1, 2, 1, 0, -1, -1, -1),
            floatArrayOf(146.83f, 174.61f, 130.81f, 146.83f)
        ),
        RadioStationId.ADEN_COAST_BREEZE to StationSynthSpec(
            82,
            floatArrayOf(164.81f, 174.61f, 196.0f, 220.0f, 246.94f, 261.63f, 293.66f, 329.63f),
            intArrayOf(0, 2, 3, -1, 4, 3, 2, -1, 4, 5, 6, -1, 5, 4, 3, -1, 7, 6, 5, -1, 4, 3, 2, -1, 1, 2, 0, -1, 0, -1, -1, -1),
            floatArrayOf(164.81f, 196.0f, 146.83f, 164.81f)
        ),
        RadioStationId.HADRAMOUT_DAN_LOFI to StationSynthSpec(
            74,
            floatArrayOf(146.83f, 164.81f, 196.0f, 220.0f, 261.63f, 293.66f, 329.63f, 392.0f),
            intArrayOf(0, -1, 2, 3, 4, -1, 3, -1, 2, -1, 4, 5, 4, 3, 2, -1, 5, -1, 4, 3, 2, -1, 1, 0, 2, -1, 1, 0, 0, -1, -1, -1),
            floatArrayOf(146.83f, 196.0f, 164.81f, 146.83f)
        )
    )

    fun triggerCollision(intensity: Float) {
        collisionTrigger = intensity.coerceIn(0.2f, 1.0f)
    }

    fun start() {
        if (running.getAndSet(true)) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            audioTrack = track
        } catch (_: Throwable) {
            running.set(false)
            return
        }

        audioThread = Thread({ runAudioLoop() }, "MasalikAudioSynth").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        try {
            audioThread?.join(250)
        } catch (_: Throwable) {
        }
        audioThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
    }

    private fun runAudioLoop() {
        val chunkSamples = 1024
        val buffer = ShortArray(chunkSamples)
        val rng = Random(2026)

        var engPhase = 0.0
        var engPhase2 = 0.0
        var hornPhase1 = 0.0
        var hornPhase2 = 0.0

        var musicSampleCounter = 0L
        var currentStep = -1
        var oudFreq = 146.83f
        var oudAgeSec = 10f
        var oudPhase = 0.0
        var padPhase1 = 0.0
        var padPhase2 = 0.0
        var padPhase3 = 0.0
        var kickAgeSec = 10f
        var percAgeSec = 10f
        var impactEnv = 0f

        val invSr = 1.0 / sampleRate
        val twoPi = 2.0 * PI

        while (running.get()) {
            val m = mixer
            val r = radioSettings
            val master = if (m.muted) 0f else m.masterVolume
            val spec = stationSpecs[r.stationId] ?: stationSpecs.getValue(RadioStationId.TAIZ_OUD_LOFI)
            val samplesPerStep = ((sampleRate * 60.0) / (spec.bpm * 4.0)).toLong().coerceAtLeast(1000L)

            if (collisionTrigger > 0f) {
                impactEnv = maxOf(impactEnv, collisionTrigger)
                collisionTrigger = 0f
            }

            val engVol = if (drivingActive && engineRunning) m.engineVolume * 0.30f else 0f
            val envVol = if (drivingActive) m.environmentVolume * 0.16f else 0f
            val musicVol = if (r.isPlaying && m.musicEnabled) m.musicVolume * r.volume * 0.36f else 0f
            val sfxVol = m.sfxVolume * 0.32f

            val rpmRatio = (engineRpm / 6000f).coerceIn(0.12f, 1.35f)
            val engFreq = (engineBaseFreqHz * (0.72f + rpmRatio * 2.4f)).toDouble()
            val roadNoiseGain = (speedKmh / 110f).coerceIn(0f, 1f) * when (surface) {
                SurfaceType.GRAVEL, SurfaceType.ROCK -> 1.45f
                SurfaceType.SAND, SurfaceType.DIRT -> 1.15f
                SurfaceType.MUD -> 1.25f
                SurfaceType.ASPHALT -> 0.55f
            }
            val weatherNoiseGain = when (weather) {
                WeatherPreset.HEAVY_RAIN -> 0.72f
                WeatherPreset.LIGHT_RAIN -> 0.38f
                WeatherPreset.SANDSTORM -> 0.58f
                else -> 0.06f
            }

            for (i in 0 until chunkSamples) {
                var sample = 0f

                // 1. Procedural Engine (Fundamental + 2nd/3rd Harmonic warm growl)
                if (engVol > 0.001f) {
                    engPhase += twoPi * engFreq * invSr
                    engPhase2 += twoPi * (engFreq * 2.0) * invSr
                    if (engPhase > twoPi) engPhase -= twoPi
                    if (engPhase2 > twoPi) engPhase2 -= twoPi
                    val load = 0.55f + throttle * 0.45f
                    val engWave = (sin(engPhase) * 0.55 + sin(engPhase2) * 0.30 + sin(engPhase * 3.0) * 0.15).toFloat()
                    sample += engWave * engVol * load
                }

                // 2. Road surface + Weather ambience
                if (envVol > 0.001f) {
                    val noise = (rng.nextFloat() * 2f - 1f)
                    sample += noise * envVol * (roadNoiseGain * 0.45f + weatherNoiseGain * 0.55f)
                }

                // 3. Yemeni Dual-Tone Horn & Collision SFX
                if (hornActive && drivingActive) {
                    hornPhase1 += twoPi * 415.0 * invSr
                    hornPhase2 += twoPi * 523.0 * invSr
                    if (hornPhase1 > twoPi) hornPhase1 -= twoPi
                    if (hornPhase2 > twoPi) hornPhase2 -= twoPi
                    sample += (sin(hornPhase1) * 0.5 + sin(hornPhase2) * 0.5).toFloat() * sfxVol * 0.7f
                }
                if (impactEnv > 0.005f) {
                    val thud = (rng.nextFloat() * 2f - 1f) * impactEnv
                    sample += thud * sfxVol
                    impactEnv *= 0.9985f
                }

                // 4. 5-Station Offline Yemeni Lo-Fi Oud & Darbuka Radio
                if (musicVol > 0.001f) {
                    val step = ((musicSampleCounter / samplesPerStep) % spec.oudPattern.size).toInt()
                    if (step != currentStep) {
                        currentStep = step
                        val noteIdx = spec.oudPattern[step]
                        if (noteIdx >= 0 && noteIdx < spec.scaleHz.size) {
                            oudFreq = spec.scaleHz[noteIdx]
                            oudAgeSec = 0f
                        }
                        if (step % 8 == 0 || step % 8 == 6) kickAgeSec = 0f
                        if (step % 4 == 2) percAgeSec = 0f
                    }
                    musicSampleCounter++

                    // Plucked Oud (warm decaying harmonics)
                    val oudEnv = exp((-oudAgeSec * 4.2f).toDouble()).toFloat()
                    oudPhase += twoPi * oudFreq * invSr
                    if (oudPhase > twoPi) oudPhase -= twoPi
                    val oudSig = (sin(oudPhase) * 0.62 + sin(oudPhase * 2.0) * 0.26 + sin(oudPhase * 3.0) * 0.12).toFloat() * oudEnv

                    // Warm Rhodes/Ney Chord Pad
                    val chordIdx = ((currentStep.coerceAtLeast(0) / 8) % spec.chordRootsHz.size)
                    val root = spec.chordRootsHz[chordIdx] * 0.5f
                    padPhase1 += twoPi * root * invSr
                    padPhase2 += twoPi * (root * 1.25f) * invSr
                    padPhase3 += twoPi * (root * 1.5f) * invSr
                    if (padPhase1 > twoPi) padPhase1 -= twoPi
                    if (padPhase2 > twoPi) padPhase2 -= twoPi
                    if (padPhase3 > twoPi) padPhase3 -= twoPi
                    val padSig = (sin(padPhase1) + sin(padPhase2) * 0.7 + sin(padPhase3) * 0.6).toFloat() * 0.16f

                    // Soft Lo-Fi Dum (Kick) & Tek (Darbuka)
                    val kickEnv = exp((-kickAgeSec * 16.0).toDouble()).toFloat()
                    val kickSig = sin(twoPi * (58.0 + 90.0 * kickEnv) * kickAgeSec).toFloat() * kickEnv * 0.34f
                    val tekEnv = exp((-percAgeSec * 38.0).toDouble()).toFloat()
                    val tekSig = (rng.nextFloat() * 2f - 1f) * tekEnv * 0.12f

                    // Optional Lo-Fi Vinyl crackle
                    val vinyl = if (r.lofiVinylEnabled && rng.nextFloat() < 0.0025f) (rng.nextFloat() - 0.5f) * 0.14f else 0f

                    sample += (oudSig * 0.52f + padSig + kickSig + tekSig + vinyl) * musicVol
                    oudAgeSec += invSr.toFloat()
                    kickAgeSec += invSr.toFloat()
                    percAgeSec += invSr.toFloat()
                }

                val out = (sample * master).coerceIn(-0.96f, 0.96f)
                buffer[i] = (out * 32767f).toInt().toShort()
            }

            try {
                audioTrack?.write(buffer, 0, chunkSamples)
            } catch (_: Throwable) {
                break
            }
        }
    }
}
