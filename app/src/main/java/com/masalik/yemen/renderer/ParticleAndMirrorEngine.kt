package com.masalik.yemen.renderer

import android.opengl.GLES30
import com.masalik.yemen.game.SurfaceType
import com.masalik.yemen.game.VehicleSpec
import com.masalik.yemen.game.VehicleTelemetry
import com.masalik.yemen.game.WeatherPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Self-contained OpenGL ES 3.0 Visual FX & Rear-View Mirror FBO subsystem:
 * 1) 3D Tire Drift Smoke, Desert/Dirt Dust Plumes, Wet Rain Spray & Exhaust Backfire Flame Pops.
 * 2) Persistent 3D Asphalt Skidmarks left behind rear tires when drifting or braking hard.
 * 3) Live Render-to-Texture FBO Rear-View Mirror (320x96 color + depth buffer).
 */
class ParticleAndMirrorEngine {

    private class Particle {
        var active = false
        var x = 0f; var y = 0f; var z = 0f
        var vx = 0f; var vy = 0f; var vz = 0f
        var size = 0.3f
        var growRate = 0.9f
        var age = 0f
        var maxAge = 0.8f
        var r = 0.85f; var g = 0.85f; var b = 0.88f
        var emR = 0f; var emG = 0f; var emB = 0f
    }

    private class Skidmark {
        var active = false
        var x = 0f; var y = 0f; var z = 0f
        var headingRad = 0f
        var lengthM = 0.85f
        var alpha = 0.65f
    }

    private val particles = Array(160) { Particle() }
    private var nextParticleIdx = 0

    private val skidmarks = Array(140) { Skidmark() }
    private var nextSkidIdx = 0
    private var skidEmitTimer = 0f
    private var particleEmitTimer = 0f
    private val rng = Random(2026)

    // Genuine OpenGL ES 3.0 Rear-View Mirror Framebuffer Object
    private var mirrorFbo = 0
    private var mirrorColorTex = 0
    private var mirrorDepthRbo = 0
    val mirrorWidth = 320
    val mirrorHeight = 96
    var mirrorFboReady = false
        private set

    fun initGlResources() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        val rbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)
        GLES30.glGenRenderbuffers(1, rbos, 0)

        mirrorFbo = fbos[0]
        mirrorColorTex = texs[0]
        mirrorDepthRbo = rbos[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mirrorColorTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            mirrorWidth, mirrorHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mirrorDepthRbo)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, mirrorWidth, mirrorHeight)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mirrorFbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mirrorColorTex, 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, mirrorDepthRbo)

        mirrorFboReady = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0)
    }

    fun beginRearMirrorPass(): Boolean {
        if (!mirrorFboReady) return false
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mirrorFbo)
        GLES30.glViewport(0, 0, mirrorWidth, mirrorHeight)
        return true
    }

    fun endRearMirrorPass(screenWidth: Int, screenHeight: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
    }

    fun drawMirrorOverlay(shaders: ShaderLibrary, skyQuadVboId: Int, isCockpit: Boolean) {
        if (!mirrorFboReady) return
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(shaders.mirrorProgram)

        // Centered top automotive mirror bezel positioned cleanly below top telemetry pill
        val wNdc = if (isCockpit) 0.50f else 0.36f
        val hNdc = if (isCockpit) 0.24f else 0.18f
        val leftNdc = -wNdc * 0.5f
        val bottomNdc = if (isCockpit) 0.56f else 0.58f
        GLES30.glUniform4f(shaders.uMirrorRect, leftNdc, bottomNdc, wNdc, hNdc)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mirrorColorTex)
        GLES30.glUniform1i(shaders.uMirrorTex, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, skyQuadVboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    fun stepParticlesAndSkids(dt: Float, t: VehicleTelemetry, spec: VehicleSpec, weather: WeatherPreset, isDriving: Boolean) {
        // Advance existing particles
        for (p in particles) {
            if (!p.active) continue
            p.age += dt
            if (p.age >= p.maxAge) {
                p.active = false
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.size += p.growRate * dt
        }

        if (!isDriving) return

        val visualHeading = t.headingRad + (t.driftSlipAngleDeg * PI.toFloat() / 180f) * 0.35f
        val fwdX = sin(visualHeading)
        val fwdZ = cos(visualHeading)
        val rightX = cos(visualHeading)
        val rightZ = -sin(visualHeading)
        val wb = spec.wheelbaseM * 0.5f
        val tr = spec.trackWidthM * 0.5f

        val isDriftingOrSkidding = t.tireScreechIntensity > 0.18f || abs(t.driftSlipAngleDeg) > 4.2f
        val isOffroadDust = t.speedKmh > 14f && t.surface in listOf(SurfaceType.DIRT, SurfaceType.SAND, SurfaceType.GRAVEL, SurfaceType.MUD)
        val isWetSpray = t.speedKmh > 25f && (weather == WeatherPreset.LIGHT_RAIN || weather == WeatherPreset.HEAVY_RAIN)

        // 1. Lay persistent dark asphalt rubber skidmarks under rear wheels
        skidEmitTimer += dt
        if (isDriftingOrSkidding && t.speedKmh > 10f && t.surface == SurfaceType.ASPHALT && skidEmitTimer >= 0.032f) {
            skidEmitTimer = 0f
            for (sx in intArrayOf(-1, 1)) {
                val sk = skidmarks[nextSkidIdx]
                nextSkidIdx = (nextSkidIdx + 1) % skidmarks.size
                sk.active = true
                sk.x = t.posX + rightX * (sx * tr) - fwdX * wb
                sk.y = t.posY + 0.055f
                sk.z = t.posZ + rightZ * (sx * tr) - fwdZ * wb
                sk.headingRad = visualHeading
                sk.lengthM = (t.forwardSpeedMs * 0.055f).coerceIn(0.65f, 1.9f)
                sk.alpha = (0.35f + t.tireScreechIntensity * 0.48f).coerceIn(0.3f, 0.85f)
            }
        }

        // 2. Emit Tire Drift Smoke, Offroad Dust Plumes, Wet Rain Spray & Exhaust Backfire Flame Pops!
        particleEmitTimer += dt
        if (particleEmitTimer >= 0.030f) {
            particleEmitTimer = 0f

            if (isDriftingOrSkidding && t.speedKmh > 12f) {
                for (sx in intArrayOf(-1, 1)) {
                    spawnParticle(
                        x = t.posX + rightX * (sx * tr) - fwdX * wb,
                        y = t.posY + 0.22f,
                        z = t.posZ + rightZ * (sx * tr) - fwdZ * wb,
                        vx = (rng.nextFloat() - 0.5f) * 1.4f - fwdX * 1.2f,
                        vy = 0.85f + rng.nextFloat() * 0.85f,
                        vz = (rng.nextFloat() - 0.5f) * 1.4f - fwdZ * 1.2f,
                        size = 0.42f, grow = 1.65f, maxAge = 0.82f,
                        r = 0.86f, g = 0.87f, b = 0.90f
                    )
                }
            } else if (isOffroadDust) {
                val (dr, dg, db) = when (t.surface) {
                    SurfaceType.SAND -> Triple(0.84f, 0.68f, 0.42f)
                    SurfaceType.MUD -> Triple(0.38f, 0.28f, 0.18f)
                    else -> Triple(0.64f, 0.52f, 0.38f)
                }
                for (sx in intArrayOf(-1, 1)) {
                    spawnParticle(
                        x = t.posX + rightX * (sx * tr) - fwdX * wb,
                        y = t.posY + 0.25f,
                        z = t.posZ + rightZ * (sx * tr) - fwdZ * wb,
                        vx = (rng.nextFloat() - 0.5f) * 1.8f,
                        vy = 0.95f + rng.nextFloat() * 0.7f,
                        vz = (rng.nextFloat() - 0.5f) * 1.8f,
                        size = 0.46f, grow = 1.85f, maxAge = 0.72f,
                        r = dr, g = dg, b = db
                    )
                }
            } else if (isWetSpray) {
                for (sx in intArrayOf(-1, 1)) {
                    spawnParticle(
                        x = t.posX + rightX * (sx * tr) - fwdX * (wb + 0.35f),
                        y = t.posY + 0.24f,
                        z = t.posZ + rightZ * (sx * tr) - fwdZ * (wb + 0.35f),
                        vx = (rng.nextFloat() - 0.5f) * 0.9f,
                        vy = 0.75f + rng.nextFloat() * 0.5f,
                        vz = (rng.nextFloat() - 0.5f) * 0.9f,
                        size = 0.28f, grow = 1.15f, maxAge = 0.42f,
                        r = 0.78f, g = 0.86f, b = 0.96f
                    )
                }
            }

            // Exhaust Backfire Flame Pops out of Dual Rear Tips!
            if (t.exhaustPopIntensity > 0.35f) {
                for (sx in intArrayOf(-1, 1)) {
                    val exX = t.posX - fwdX * (spec.bodyLengthM * 0.54f) + rightX * (sx * spec.bodyWidthM * 0.30f)
                    val exZ = t.posZ - fwdZ * (spec.bodyLengthM * 0.54f) + rightZ * (sx * spec.bodyWidthM * 0.30f)
                    spawnParticle(
                        x = exX, y = t.posY + 0.46f, z = exZ,
                        vx = -fwdX * 3.5f, vy = 0.25f, vz = -fwdZ * 3.5f,
                        size = 0.24f, grow = 0.9f, maxAge = 0.14f,
                        r = 1.0f, g = 0.55f, b = 0.12f,
                        emR = 2.4f, emG = 1.35f, emB = 0.35f
                    )
                }
            }
        }
    }

    private fun spawnParticle(
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        size: Float, grow: Float, maxAge: Float,
        r: Float, g: Float, b: Float,
        emR: Float = 0f, emG: Float = 0f, emB: Float = 0f
    ) {
        val p = particles[nextParticleIdx]
        nextParticleIdx = (nextParticleIdx + 1) % particles.size
        p.active = true
        p.x = x; p.y = y; p.z = z
        p.vx = vx; p.vy = vy; p.vz = vz
        p.size = size; p.growRate = grow
        p.age = 0f; p.maxAge = maxAge
        p.r = r; p.g = g; p.b = b
        p.emR = emR; p.emG = emG; p.emB = emB
    }

    fun renderSkidmarks(
        drawSkidQuad: (x: Float, y: Float, z: Float, headingRad: Float, width: Float, length: Float, alpha: Float) -> Unit
    ) {
        GLES30.glDepthMask(false)
        for (sk in skidmarks) {
            if (!sk.active) continue
            drawSkidQuad(sk.x, sk.y, sk.z, sk.headingRad, 0.34f, sk.lengthM, sk.alpha)
        }
        GLES30.glDepthMask(true)
    }

    fun renderParticles(
        drawParticleCube: (
            x: Float, y: Float, z: Float, size: Float,
            r: Float, g: Float, b: Float, alpha: Float,
            emR: Float, emG: Float, emB: Float
        ) -> Unit
    ) {
        GLES30.glDepthMask(false)
        for (p in particles) {
            if (!p.active) continue
            val lifeFrac = (1f - (p.age / p.maxAge)).coerceIn(0f, 1f)
            val alpha = lifeFrac * (if (p.emR > 0.5f) 0.92f else 0.52f)
            drawParticleCube(p.x, p.y, p.z, p.size, p.r, p.g, p.b, alpha, p.emR * lifeFrac, p.emG * lifeFrac, p.emB * lifeFrac)
        }
        GLES30.glDepthMask(true)
    }
}
