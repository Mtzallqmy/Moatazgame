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

    // Cinematic mechanical & environment channels
    @Volatile var tireScreech: Float = 0f
    @Volatile var turboBoostPsi: Float = 0f
    @Volatile var exhaustPopIntensity: Float = 0f
    @Volatile var inTunnel: Boolean = false
    @Volatile var emergencySirenProximity: Float = 0f

    @Volatile private var collisionTrigger: Float = 0f
    @Volatile private var shutterTrigger: Float = 0f

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

    fun triggerCameraShutter() {
        shutterTrigger = 1.0f
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
        var turboPhase = 0.0
        var screechPhase1 = 0.0
        var screechPhase2 = 0.0
        var sirenPhase = 0.0
        var sirenLfoPhase = 0.0
        var hornPhase1 = 0.0
        var hornPhase2 = 0.0

        // Short 95ms circular echo buffer for realistic mountain tunnel acoustics
        val echoBuf = FloatArray(2100)
        var echoIdx = 0

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
        var shutterEnv = 0f
        var popCooldown = 0
        var popBurstEnv = 0f
        var rainFilter = 0f

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
            if (shutterTrigger > 0f) {
                shutterEnv = 1.0f
                shutterTrigger = 0f
            }

            val engVol = if (drivingActive && engineRunning) m.engineVolume * 0.28f else 0f
            val envVol = if (drivingActive) m.environmentVolume * 0.16f else 0f
            val musicVol = if (r.isPlaying && m.musicEnabled) m.musicVolume * r.volume * 0.34f else 0f
            val sfxVol = m.sfxVolume * 0.30f

            val rpmRatio = (engineRpm / 6000f).coerceIn(0.12f, 1.35f)
            val engFreq = (engineBaseFreqHz * (0.72f + rpmRatio * 2.45f)).toDouble()
            val turboFreq = (1750.0 + turboBoostPsi * 115.0).coerceIn(1600.0, 3900.0)
            val turboGain = ((turboBoostPsi - 2.5f) / 16f).coerceIn(0f, 1f) * engVol * 0.24f
            val screechGain = tireScreech.coerceIn(0f, 1f) * sfxVol * 0.42f
            val popLevel = exhaustPopIntensity.coerceIn(0f, 1f)
            val sirenProx = if (drivingActive) emergencySirenProximity.coerceIn(0f, 1f) else 0f
            val tunnelMix = if (inTunnel && drivingActive) 0.44f else 0f

            val roadNoiseGain = (speedKmh / 110f).coerceIn(0f, 1f) * when (surface) {
                SurfaceType.GRAVEL, SurfaceType.ROCK -> 1.45f
                SurfaceType.SAND, SurfaceType.DIRT -> 1.15f
                SurfaceType.MUD -> 1.25f
                SurfaceType.ASPHALT -> 0.55f
            }
            val weatherNoiseGain = when (weather) {
                WeatherPreset.HEAVY_RAIN -> 0.78f
                WeatherPreset.LIGHT_RAIN -> 0.42f
                WeatherPreset.SANDSTORM -> 0.62f
                else -> 0.07f
            }
            val isRaining = weather == WeatherPreset.LIGHT_RAIN || weather == WeatherPreset.HEAVY_RAIN

            for (i in 0 until chunkSamples) {
                var mechSample = 0f
                var sample = 0f

                // 1. Procedural Engine (Harmonics + Turbo Spool Whistle + Exhaust Backfire Pops!)
                if (engVol > 0.001f) {
                    engPhase += twoPi * engFreq * invSr
                    engPhase2 += twoPi * (engFreq * 2.0) * invSr
                    if (engPhase > twoPi) engPhase -= twoPi
                    if (engPhase2 > twoPi) engPhase2 -= twoPi
                    val load = 0.54f + throttle * 0.46f
                    val engWave = (sin(engPhase) * 0.52 + sin(engPhase2) * 0.30 + sin(engPhase * 3.0) * 0.18).toFloat()
                    mechSample += engWave * engVol * load

                    // High-frequency Turbo Spool Whistle
                    if (turboGain > 0.002f) {
                        turboPhase += twoPi * turboFreq * invSr
                        if (turboPhase > twoPi) turboPhase -= twoPi
                        mechSample += sin(turboPhase).toFloat() * turboGain
                    }

                    // Crisp Exhaust Backfire Crackles / Pops on throttle lift-off & downshift
                    if (popLevel > 0.15f) {
                        if (popCooldown <= 0 && rng.nextFloat() < 0.0045f * popLevel) {
                            popBurstEnv = 0.9f * popLevel
                            popCooldown = 950
                        }
                    }
                    if (popCooldown > 0) popCooldown--
                    if (popBurstEnv > 0.01f) {
                        val popWave = (rng.nextFloat() * 2f - 1f) * popBurstEnv * sin(engPhase * 1.5).toFloat()
                        mechSample += popWave * engVol * 1.85f
                        popBurstEnv *= 0.992f
                    }
                }

                // 2. Asphalt Tire Screech (Dual resonant pitch + skid noise)
                if (screechGain > 0.003f && drivingActive) {
                    screechPhase1 += twoPi * (875.0 + speedKmh * 1.4) * invSr
                    screechPhase2 += twoPi * (1335.0 + speedKmh * 1.9) * invSr
                    if (screechPhase1 > twoPi) screechPhase1 -= twoPi
                    if (screechPhase2 > twoPi) screechPhase2 -= twoPi
                    val skidTone = (sin(screechPhase1) * 0.55 + sin(screechPhase2) * 0.35 + (rng.nextFloat() * 2f - 1f) * 0.25).toFloat()
                    mechSample += skidTone * screechGain
                }

                // Apply Tunnel Acoustic Echo Feedback to mechanical vehicle sounds!
                if (tunnelMix > 0.01f) {
                    val delayed = echoBuf[echoIdx]
                    echoBuf[echoIdx] = mechSample + delayed * 0.42f
                    echoIdx = (echoIdx + 1) % echoBuf.size
                    sample += mechSample + delayed * tunnelMix
                } else {
                    echoBuf[echoIdx] = 0f
                    echoIdx = (echoIdx + 1) % echoBuf.size
                    sample += mechSample
                }

                // 3. Road Surface + Highland Wind + Rain-on-Roof Patter
                if (envVol > 0.001f) {
                    val rawNoise = (rng.nextFloat() * 2f - 1f)
                    rainFilter = rainFilter * 0.82f + rawNoise * 0.18f
                    val rainDrop = if (isRaining && rng.nextFloat() < (if (weather == WeatherPreset.HEAVY_RAIN) 0.006f else 0.0025f)) {
                        (rng.nextFloat() * 2f - 1f) * 0.9f
                    } else 0f
                    val windGain = (speedKmh / 145f).coerceIn(0f, 1f) * 0.42f
                    sample += (rawNoise * roadNoiseGain * 0.40f + rainFilter * (weatherNoiseGain * 0.65f + windGain) + rainDrop * 0.45f) * envVol
                }

                // 4. Nearby Police / Ambulance Siren Wail (Doppler proximity modulated)
                if (sirenProx > 0.05f) {
                    sirenLfoPhase += twoPi * 1.45 * invSr
                    if (sirenLfoPhase > twoPi) sirenLfoPhase -= twoPi
                    val sirenHz = 760.0 + sin(sirenLfoPhase) * 280.0
                    sirenPhase += twoPi * sirenHz * invSr
                    if (sirenPhase > twoPi) sirenPhase -= twoPi
                    sample += sin(sirenPhase).toFloat() * sfxVol * sirenProx * 0.28f
                }

                // 5. Yemeni Dual-Tone Horn, Collision Impact & Radar/Camera Shutter Click
                if (hornActive && drivingActive) {
                    hornPhase1 += twoPi * 415.0 * invSr
                    hornPhase2 += twoPi * 523.0 * invSr
                    if (hornPhase1 > twoPi) hornPhase1 -= twoPi
                    if (hornPhase2 > twoPi) hornPhase2 -= twoPi
                    sample += (sin(hornPhase1) * 0.5 + sin(hornPhase2) * 0.5).toFloat() * sfxVol * 0.68f
                }
                if (impactEnv > 0.005f) {
                    val thud = (rng.nextFloat() * 2f - 1f) * impactEnv
                    sample += thud * sfxVol
                    impactEnv *= 0.9985f
                }
                if (shutterEnv > 0.01f) {
                    val click = (rng.nextFloat() * 2f - 1f) * shutterEnv * 0.85f
                    sample += click * sfxVol
                    shutterEnv *= 0.990f
                }

                // 6. 5-Station Offline Yemeni Lo-Fi Oud & Darbuka Radio
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

                    val oudEnv = exp((-oudAgeSec * 4.2f).toDouble()).toFloat()
                    oudPhase += twoPi * oudFreq * invSr
                    if (oudPhase > twoPi) oudPhase -= twoPi
                    val oudSig = (sin(oudPhase) * 0.62 + sin(oudPhase * 2.0) * 0.26 + sin(oudPhase * 3.0) * 0.12).toFloat() * oudEnv

                    val chordIdx = ((currentStep.coerceAtLeast(0) / 8) % spec.chordRootsHz.size)
                    val root = spec.chordRootsHz[chordIdx] * 0.5f
                    padPhase1 += twoPi * root * invSr
                    padPhase2 += twoPi * (root * 1.25f) * invSr
                    padPhase3 += twoPi * (root * 1.5f) * invSr
                    if (padPhase1 > twoPi) padPhase1 -= twoPi
                    if (padPhase2 > twoPi) padPhase2 -= twoPi
                    if (padPhase3 > twoPi) padPhase3 -= twoPi
                    val padSig = (sin(padPhase1) + sin(padPhase2) * 0.7 + sin(padPhase3) * 0.6).toFloat() * 0.16f

                    val kickEnv = exp((-kickAgeSec * 16.0).toDouble()).toFloat()
                    val kickSig = sin(twoPi * (58.0 + 90.0 * kickEnv) * kickAgeSec).toFloat() * kickEnv * 0.34f
                    val tekEnv = exp((-percAgeSec * 38.0).toDouble()).toFloat()
                    val tekSig = (rng.nextFloat() * 2f - 1f) * tekEnv * 0.12f
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
