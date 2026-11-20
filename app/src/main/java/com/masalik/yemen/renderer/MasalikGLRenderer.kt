package com.masalik.yemen.renderer

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.masalik.yemen.game.AtmosphereSnapshot
import com.masalik.yemen.game.BullbarStyle
import com.masalik.yemen.game.CameraMode
import com.masalik.yemen.game.GraphicsQuality
import com.masalik.yemen.game.LivingWorldSimulation
import com.masalik.yemen.game.PedestrianAgent
import com.masalik.yemen.game.PedestrianStyle
import com.masalik.yemen.game.PickupAccessoryType
import com.masalik.yemen.game.RegionId
import com.masalik.yemen.game.RoadSample
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.SurfaceType
import com.masalik.yemen.game.TrafficSignalPhase
import com.masalik.yemen.game.TrafficVehicleType
import com.masalik.yemen.game.VehicleCustomization
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.game.VehicleId
import com.masalik.yemen.game.VehicleTelemetry
import com.masalik.yemen.game.WeatherPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MasalikGLRenderer(
    private val onStepFrame: (dtSec: Float) -> RenderFrameSnapshot
) : GLSurfaceView.Renderer {

    data class RenderFrameSnapshot(
        val telemetry: VehicleTelemetry,
        val customization: VehicleCustomization,
        val previewVehicleId: VehicleId,
        val previewCustomization: VehicleCustomization,
        val isLobbyOrGarage: Boolean,
        val showroomOrbitRad: Float,
        val cameraMode: CameraMode,
        val graphicsQuality: GraphicsQuality,
        val photoFovDeg: Float,
        val worldTimeHours: Float,
        val weather: WeatherPreset,
        val livingWorld: LivingWorldSimulation,
        val missionMarkerM: Float?,
        val blinkerOn: Boolean
    )

    private lateinit var shaders: ShaderLibrary
    private lateinit var textures: ProceduralTextures
    private lateinit var cubeMesh: GpuMesh
    private lateinit var wheelMesh: GpuMesh
    private lateinit var quadMesh: GpuMesh
    private lateinit var coneMesh: GpuMesh

    private lateinit var roadDynamicMesh: DynamicMesh
    private lateinit var sidewalkDynamicMesh: DynamicMesh
    private lateinit var terrainDynamicMesh: DynamicMesh
    private var skyVboId: Int = 0

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var aspectRatio = 16f / 9f
    private var lastFrameNs = System.nanoTime()
    private var lastStreamCenterDistM = -9999f
    private var lastStreamQuality: GraphicsQuality? = null
    private var weatherAnimTime = 0f

    private var currentCamX = 0f
    private var currentCamY = 4f
    private var currentCamZ = -8f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        shaders = ShaderLibrary()
        textures = ProceduralTextures()
        cubeMesh = MeshPrimitives.createUnitCube()
        wheelMesh = MeshPrimitives.createWheelCylinder(14)
        quadMesh = MeshPrimitives.createUnitQuadXZ()
        coneMesh = MeshPrimitives.createConePyramid(6)

        roadDynamicMesh = DynamicMesh()
        sidewalkDynamicMesh = DynamicMesh()
        terrainDynamicMesh = DynamicMesh()

        val skyQuad = floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f)
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        skyVboId = ids[0]
        val buf = ByteBuffer.allocateDirect(skyQuad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(skyQuad)
        buf.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, skyVboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, skyQuad.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        lastFrameNs = System.nanoTime()
        lastStreamCenterDistM = -9999f
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 1f)
        lastFrameNs = now
        weatherAnimTime += dt.coerceAtMost(0.2f)

        val snap = onStepFrame(dt)
        val atmo = LivingWorldSimulation.sampleAtmosphere(snap.worldTimeHours, snap.weather)

        val centerDist = snap.telemetry.distanceAlongRoadM
        val quality = snap.graphicsQuality
        if (abs(centerDist - lastStreamCenterDistM) > 45f || lastStreamQuality != quality) {
            rebuildStreamingCorridor(centerDist, quality)
            lastStreamCenterDistM = centerDist
            lastStreamQuality = quality
        }

        GLES30.glClearColor(atmo.skyR, atmo.skyG, atmo.skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // 1. Draw Gradient Sky Dome with Sun/Moon & Night Stars
        drawSkyBackdrop(atmo)

        // 2. Setup Projection & Active Camera Mode
        val fov = when {
            snap.isLobbyOrGarage -> 46f
            snap.cameraMode == CameraMode.FREE_PHOTO -> snap.photoFovDeg
            snap.cameraMode == CameraMode.COCKPIT -> 68f
            else -> 60f
        }
        Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.2f, quality.sceneryDistanceM + 100f)
        updateCameraMatrix(snap)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 3. Bind World Lit Shader & Global Atmosphere Uniforms
        GLES30.glUseProgram(shaders.worldProgram)
        GLES30.glUniform3f(shaders.uSunDir, atmo.sunDirX, atmo.sunDirY, atmo.sunDirZ)
        GLES30.glUniform3f(shaders.uSunColor, atmo.sunR, atmo.sunG, atmo.sunB)
        GLES30.glUniform1f(shaders.uSunIntensity, atmo.sunIntensity)
        GLES30.glUniform1f(shaders.uAmbientIntensity, atmo.ambientIntensity)
        GLES30.glUniform3f(shaders.uFogColor, atmo.horizonR, atmo.horizonG, atmo.horizonB)
        GLES30.glUniform1f(shaders.uFogDensity, atmo.fogDensity)
        GLES30.glUniform3f(shaders.uCameraPos, currentCamX, currentCamY, currentCamZ)
        GLES30.glUniform1f(shaders.uNightFactor, atmo.nightFactor)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(shaders.uTexture, 0)

        // 4. Render Terrain, Realistic Textured Street & Stone Sidewalks
        drawCorridorGeometry(snap.telemetry.regionId, snap.telemetry.surface)

        // 5. Render Yemeni Cities, Qamariya Buildings, Landmarks, Streetlamps, Signals & Crosswalks
        drawYemeniCityEnvironment(centerDist, snap, atmo)

        // 6. Render Roadside Parked Cars
        drawParkedCars(centerDist, snap.livingWorld, quality.sceneryDistanceM)

        // 7. Render Animated Yemeni Residents / Pedestrians
        drawYemeniPedestrians(centerDist, snap.livingWorld, quality.sceneryDistanceM)

        // 8. Render Rule-Following AI Traffic Cars & Race Rival
        drawAiTrafficAndRival(centerDist, snap, atmo)

        // 9. Render Player Vehicle (or 3D Showroom Vehicle + 3D Cockpit Interior)
        val drawVehicleId = if (snap.isLobbyOrGarage) snap.previewVehicleId else snap.telemetry.vehicleId
        val drawCustom = if (snap.isLobbyOrGarage) snap.previewCustomization else snap.customization
        if (!snap.isLobbyOrGarage && snap.cameraMode == CameraMode.COCKPIT) {
            drawCockpitInterior(snap.telemetry, drawCustom, atmo)
        } else {
            drawDetailedPlayerVehicle(snap.telemetry, drawVehicleId, drawCustom, snap.blinkerOn, atmo)
        }

        // 10. Render Weather Particles (Rain / Sandstorm)
        if (snap.weather in listOf(WeatherPreset.LIGHT_RAIN, WeatherPreset.HEAVY_RAIN, WeatherPreset.SANDSTORM)) {
            drawWeatherParticles(snap.weather, quality.weatherParticleScale)
        }
    }

    private fun drawSkyBackdrop(atmo: AtmosphereSnapshot) {
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(shaders.skyProgram)
        GLES30.glUniform3f(shaders.uSkyTopColor, atmo.skyR, atmo.skyG, atmo.skyB)
        GLES30.glUniform3f(shaders.uSkyHorizonColor, atmo.horizonR, atmo.horizonG, atmo.horizonB)
        GLES30.glUniform3f(shaders.uSkySunDir, atmo.sunDirX, atmo.sunDirY, atmo.sunDirZ)
        GLES30.glUniform1f(shaders.uSkyNightFactor, atmo.nightFactor)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, skyVboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDepthMask(true)
    }

    private fun updateCameraMatrix(snap: RenderFrameSnapshot) {
        val t = snap.telemetry
        val fwdX = sin(t.headingRad)
        val fwdZ = cos(t.headingRad)

        var eyeX: Float
        var eyeY: Float
        var eyeZ: Float
        var targetX: Float
        var targetY: Float
        var targetZ: Float

        if (snap.isLobbyOrGarage) {
            val radius = 6.9f
            val angle = t.headingRad + snap.showroomOrbitRad
            eyeX = t.posX + sin(angle) * radius
            eyeY = t.posY + 2.35f
            eyeZ = t.posZ + cos(angle) * radius
            targetX = t.posX
            targetY = t.posY + 1.15f
            targetZ = t.posZ
        } else {
            when (snap.cameraMode) {
                CameraMode.CHASE_NEAR -> {
                    eyeX = t.posX - fwdX * 7.4f
                    eyeY = t.posY + 3.05f
                    eyeZ = t.posZ - fwdZ * 7.4f
                    targetX = t.posX + fwdX * 14f
                    targetY = t.posY + 1.35f
                    targetZ = t.posZ + fwdZ * 14f
                }
                CameraMode.CHASE_FAR -> {
                    eyeX = t.posX - fwdX * 11.2f
                    eyeY = t.posY + 4.4f
                    eyeZ = t.posZ - fwdZ * 11.2f
                    targetX = t.posX + fwdX * 18f
                    targetY = t.posY + 1.4f
                    targetZ = t.posZ + fwdZ * 18f
                }
                CameraMode.COCKPIT -> {
                    val rightX = cos(t.headingRad)
                    val rightZ = -sin(t.headingRad)
                    eyeX = t.posX + fwdX * 0.25f - rightX * 0.36f
                    eyeY = t.posY + 1.42f
                    eyeZ = t.posZ + fwdZ * 0.25f - rightZ * 0.36f
                    targetX = eyeX + fwdX * 25f
                    targetY = eyeY - 0.5f
                    targetZ = eyeZ + fwdZ * 25f
                }
                CameraMode.HOOD -> {
                    eyeX = t.posX + fwdX * 1.15f
                    eyeY = t.posY + 1.52f
                    eyeZ = t.posZ + fwdZ * 1.15f
                    targetX = eyeX + fwdX * 26f
                    targetY = eyeY - 0.6f
                    targetZ = eyeZ + fwdZ * 26f
                }
                CameraMode.CINEMATIC_FRONT -> {
                    eyeX = t.posX + fwdX * 8.2f + cos(t.headingRad) * 2.1f
                    eyeY = t.posY + 2.2f
                    eyeZ = t.posZ + fwdZ * 8.2f - sin(t.headingRad) * 2.1f
                    targetX = t.posX
                    targetY = t.posY + 1.1f
                    targetZ = t.posZ
                }
                CameraMode.FREE_PHOTO -> {
                    val r = 8.4f
                    val ang = t.headingRad + snap.showroomOrbitRad
                    eyeX = t.posX + sin(ang) * r
                    eyeY = t.posY + 2.8f
                    eyeZ = t.posZ + cos(ang) * r
                    targetX = t.posX
                    targetY = t.posY + 1.15f
                    targetZ = t.posZ
                }
            }
        }

        currentCamX = eyeX
        currentCamY = eyeY
        currentCamZ = eyeZ
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, targetX, targetY, targetZ, 0f, 1f, 0f)
    }

    private fun rebuildStreamingCorridor(centerDistM: Float, quality: GraphicsQuality) {
        val startM = (centerDistM - quality.sceneryDistanceM * 0.7f - 40f).coerceAtLeast(0f)
        val endM = (centerDistM + quality.sceneryDistanceM + 90f).coerceAtMost(RoadSplineNetwork.TOTAL_ROAD_LENGTH_M)
        val step = 5f
        val samples = mutableListOf<RoadSample>()
        var d = startM
        while (d <= endM) {
            samples.add(RoadSplineNetwork.sampleAt(d))
            d += step
        }
        if (samples.size < 2) return

        val roadVerts = ArrayList<Float>(samples.size * 48)
        val sidewalkVerts = ArrayList<Float>(samples.size * 96)
        val terrainVerts = ArrayList<Float>(samples.size * 192)

        fun pushTri(
            dst: MutableList<Float>,
            ax: Float, ay: Float, az: Float, au: Float, av: Float,
            bx: Float, by: Float, bz: Float, bu: Float, bv: Float,
            cx: Float, cy: Float, cz: Float, cu: Float, cv: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            dst.add(ax); dst.add(ay); dst.add(az); dst.add(nx); dst.add(ny); dst.add(nz); dst.add(au); dst.add(av)
            dst.add(bx); dst.add(by); dst.add(bz); dst.add(nx); dst.add(ny); dst.add(nz); dst.add(bu); dst.add(bv)
            dst.add(cx); dst.add(cy); dst.add(cz); dst.add(nx); dst.add(ny); dst.add(nz); dst.add(cu); dst.add(cv)
        }

        for (i in 0 until samples.lastIndex) {
            val s0 = samples[i]
            val s1 = samples[i + 1]
            val hw0 = s0.widthM * 0.5f
            val hw1 = s1.widthM * 0.5f
            val v0 = s0.distanceM * 0.08f
            val v1 = s1.distanceM * 0.08f

            // Main Road Strip
            val l0x = s0.x - s0.rightX * hw0; val l0z = s0.z - s0.rightZ * hw0
            val r0x = s0.x + s0.rightX * hw0; val r0z = s0.z + s0.rightZ * hw0
            val l1x = s1.x - s1.rightX * hw1; val l1z = s1.z - s1.rightZ * hw1
            val r1x = s1.x + s1.rightX * hw1; val r1z = s1.z + s1.rightZ * hw1

            pushTri(roadVerts, l0x, s0.y + 0.03f, l0z, 0f, v0, r0x, s0.y + 0.03f, r0z, 1f, v0, r1x, s1.y + 0.03f, r1z, 1f, v1, 0f, 1f, 0f)
            pushTri(roadVerts, l0x, s0.y + 0.03f, l0z, 0f, v0, r1x, s1.y + 0.03f, r1z, 1f, v1, l1x, s1.y + 0.03f, l1z, 0f, v1, 0f, 1f, 0f)

            // Raised Stone Sidewalks on both sides (3.4m wide)
            val swLift0 = if (s0.isUrbanStreet) 0.18f else 0.02f
            val swLift1 = if (s1.isUrbanStreet) 0.18f else 0.02f
            val swW = 3.4f
            for (side in intArrayOf(-1, 1)) {
                val in0 = side * hw0
                val out0 = side * (hw0 + swW)
                val in1 = side * hw1
                val out1 = side * (hw1 + swW)
                val p0x = s0.x + s0.rightX * in0; val p0z = s0.z + s0.rightZ * in0
                val p1x = s0.x + s0.rightX * out0; val p1z = s0.z + s0.rightZ * out0
                val p2x = s1.x + s1.rightX * out1; val p2z = s1.z + s1.rightZ * out1
                val p3x = s1.x + s1.rightX * in1; val p3z = s1.z + s1.rightZ * in1
                pushTri(sidewalkVerts, p0x, s0.y + swLift0, p0z, 0f, v0, p1x, s0.y + swLift0, p1z, 1f, v0, p2x, s1.y + swLift1, p2z, 1f, v1, 0f, 1f, 0f)
                pushTri(sidewalkVerts, p0x, s0.y + swLift0, p0z, 0f, v0, p2x, s1.y + swLift1, p2z, 1f, v1, p3x, s1.y + swLift1, p3z, 0f, v1, 0f, 1f, 0f)
            }

            // Terraced Mountain & Valley Terrain Skirts extending 75m to each side
            val offsets = floatArrayOf(hw0 + swW, hw0 + 18f, hw0 + 44f, hw0 + 82f)
            for (side in intArrayOf(-1, 1)) {
                for (band in 0 until offsets.lastIndex) {
                    val latA = side * offsets[band]
                    val latB = side * offsets[band + 1]
                    val ax0 = s0.x + s0.rightX * latA; val az0 = s0.z + s0.rightZ * latA
                    val bx0 = s0.x + s0.rightX * latB; val bz0 = s0.z + s0.rightZ * latB
                    val ax1 = s1.x + s1.rightX * latA; val az1 = s1.z + s1.rightZ * latA
                    val bx1 = s1.x + s1.rightX * latB; val bz1 = s1.z + s1.rightZ * latB

                    val ay0 = RoadSplineNetwork.terrainHeightAt(ax0, az0, s0, latA)
                    val by0 = RoadSplineNetwork.terrainHeightAt(bx0, bz0, s0, latB)
                    val ay1 = RoadSplineNetwork.terrainHeightAt(ax1, az1, s1, latA)
                    val by1 = RoadSplineNetwork.terrainHeightAt(bx1, bz1, s1, latB)

                    val ny = 0.85f
                    val nx = -side * 0.42f * s0.rightX
                    val nz = -side * 0.42f * s0.rightZ
                    pushTri(terrainVerts, ax0, ay0, az0, 0f, 0f, bx0, by0, bz0, 1f, 0f, bx1, by1, bz1, 1f, 1f, nx, ny, nz)
                    pushTri(terrainVerts, ax0, ay0, az0, 0f, 0f, bx1, by1, bz1, 1f, 1f, ax1, ay1, az1, 0f, 1f, nx, ny, nz)
                }
            }
        }

        roadDynamicMesh.upload(roadVerts.toFloatArray())
        sidewalkDynamicMesh.upload(sidewalkVerts.toFloatArray())
        terrainDynamicMesh.upload(terrainVerts.toFloatArray())
    }

    private fun drawCorridorGeometry(regionId: RegionId, surface: SurfaceType) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform3f(shaders.uEmissive, 0f, 0f, 0f)

        // Road surface tint + Asphalt markings texture
        val (rr, rg, rb, useTex) = when (surface) {
            SurfaceType.ASPHALT -> Quad4(0.95f, 0.95f, 0.98f, 1f)
            SurfaceType.DIRT -> Quad4(0.62f, 0.48f, 0.34f, 0.35f)
            SurfaceType.GRAVEL -> Quad4(0.56f, 0.54f, 0.50f, 0.40f)
            SurfaceType.SAND -> Quad4(0.86f, 0.72f, 0.44f, 0.25f)
            SurfaceType.MUD -> Quad4(0.36f, 0.26f, 0.18f, 0.25f)
            SurfaceType.ROCK -> Quad4(0.48f, 0.45f, 0.42f, 0.35f)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.roadAsphaltTex)
        GLES30.glUniform1f(shaders.uUseTexture, useTex)
        GLES30.glUniform2f(shaders.uUvScale, 1f, 1f)
        GLES30.glUniform4f(shaders.uBaseColor, rr, rg, rb, 1f)
        roadDynamicMesh.draw()

        // Stone Sidewalks & Curbs
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.sidewalkStoneTex)
        GLES30.glUniform1f(shaders.uUseTexture, 1f)
        GLES30.glUniform2f(shaders.uUvScale, 1f, 1.5f)
        GLES30.glUniform4f(shaders.uBaseColor, 0.92f, 0.90f, 0.86f, 1f)
        sidewalkDynamicMesh.draw()

        // Regional Yemeni Mountain / Terraced / Desert Terrain
        val (tr, tg, tb) = when (regionId) {
            RegionId.TAIZ_HIGHLANDS -> Triple(0.46f, 0.42f, 0.28f)
            RegionId.IBB_GREEN_PEAKS, RegionId.WADI_RAYHAN -> Triple(0.26f, 0.46f, 0.24f)
            RegionId.NAQIL_SAHAB, RegionId.SANAA_HIGHLANDS -> Triple(0.50f, 0.42f, 0.34f)
            RegionId.MASALIK_DESERT -> Triple(0.82f, 0.66f, 0.40f)
            RegionId.SHIBAM_HADRAMOUT -> Triple(0.76f, 0.60f, 0.38f)
            RegionId.TIHAMA_PLAINS -> Triple(0.72f, 0.62f, 0.38f)
            RegionId.COASTAL_ROAD -> Triple(0.76f, 0.68f, 0.46f)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.whiteTex)
        GLES30.glUniform1f(shaders.uUseTexture, 0f)
        GLES30.glUniform4f(shaders.uBaseColor, tr, tg, tb, 1f)
        terrainDynamicMesh.draw()
    }

    private fun drawYemeniCityEnvironment(
        centerDistM: Float,
        snap: RenderFrameSnapshot,
        atmo: AtmosphereSnapshot
    ) {
        val maxDistance = snap.graphicsQuality.sceneryDistanceM
        val minD = ((centerDistM - maxDistance * 0.5f) / 22f).toInt().coerceAtLeast(1) * 22
        val maxD = ((centerDistM + maxDistance) / 22f).toInt() * 22
        val nightGlow = atmo.nightFactor

        // 1. Buildings, Souks, Streetlamps, Trees & Tunnels along the corridor
        var dInt = minD
        while (dInt <= maxD) {
            val d = dInt.toFloat()
            if (d >= RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 20f) break
            val sample = RoadSplineNetwork.sampleAt(d)
            val hash = ((dInt * 2654435761L) ushr 16).toInt() and 0x7FFFFFFF
            val showDetail = abs(d - centerDistM) <= snap.graphicsQuality.detailDistanceM

            // Rock Tunnel Archway
            if (sample.hasTunnel) {
                drawOrientedBox(
                    sample.x, sample.y + 6.8f, sample.z,
                    sample.headingRad, sample.widthM + 3.5f, 1.8f, 22.5f,
                    0.34f, 0.31f, 0.28f
                )
                for (side in intArrayOf(-1, 1)) {
                    val lat = side * (sample.widthM * 0.5f + 1.2f)
                    drawOrientedBox(
                        sample.x + sample.rightX * lat, sample.y + 3.4f, sample.z + sample.rightZ * lat,
                        sample.headingRad, 1.8f, 6.8f, 22.5f,
                        0.38f, 0.34f, 0.30f
                    )
                }
            } else if (sample.isUrbanStreet) {
                // Authentic Yemeni Multi-Story Buildings & Souk Shops lining BOTH sides of the street
                for (side in intArrayOf(-1, 1)) {
                    val bHash = hash + (if (side > 0) 37 else 11)
                    val buildingW = 8.5f + (bHash % 5)
                    val buildingD = 11.5f + (bHash % 6)
                    val floors = when (sample.regionId) {
                        RegionId.SHIBAM_HADRAMOUT -> 5 + (bHash % 4) // Iconic Shibam Mud Skyscrapers!
                        RegionId.SANAA_HIGHLANDS -> 4 + (bHash % 3)  // Old Sana'a Tower Houses!
                        RegionId.TAIZ_HIGHLANDS -> 3 + (bHash % 3)   // Taiz Highland Stone Buildings
                        else -> 2 + (bHash % 3)
                    }
                    val buildingH = floors * 3.4f
                    val lat = side * (sample.widthM * 0.5f + 3.6f + buildingW * 0.52f)
                    val bx = sample.x + sample.rightX * lat
                    val bz = sample.z + sample.rightZ * lat
                    val by = sample.y + buildingH * 0.5f

                    val facadeTex = when (sample.regionId) {
                        RegionId.SANAA_HIGHLANDS -> textures.sanaaFacadeTex
                        RegionId.SHIBAM_HADRAMOUT -> textures.shibamMudTex
                        else -> if (bHash % 3 == 0) textures.sanaaFacadeTex else textures.taizStoneTex
                    }
                    // Warm illuminated Qamariya windows at night!
                    val winGlow = nightGlow * 0.26f
                    drawOrientedBox(
                        bx, by, bz, sample.headingRad,
                        buildingW, buildingH, buildingD,
                        0.96f, 0.94f, 0.90f,
                        texId = facadeTex,
                        uvScaleX = 1.2f, uvScaleY = floors * 0.55f,
                        emR = winGlow * 1.0f, emG = winGlow * 0.72f, emB = winGlow * 0.30f
                    )

                    // Whitewashed roof parapet / Mafraj penthouse on Sana'a & Taiz houses
                    if (showDetail) {
                        drawOrientedBox(
                            bx, sample.y + buildingH + 0.55f, bz, sample.headingRad,
                            buildingW + 0.4f, 0.9f, buildingD + 0.4f,
                            0.94f, 0.92f, 0.87f
                        )
                    }

                    // Street-level Yemeni Souk Shopfront with Striped Awning facing the sidewalk
                    if (showDetail && bHash % 2 == 0) {
                        val shopLat = side * (sample.widthM * 0.5f + 3.45f)
                        drawOrientedBox(
                            sample.x + sample.rightX * shopLat, sample.y + 1.85f, sample.z + sample.rightZ * shopLat,
                            sample.headingRad, 1.3f, 3.2f, buildingD * 0.84f,
                            1f, 1f, 1f,
                            texId = textures.soukShopTex,
                            uvScaleX = 1f, uvScaleY = 1f,
                            emR = nightGlow * 0.35f, emG = nightGlow * 0.24f, emB = nightGlow * 0.08f
                        )
                    }

                    // Streetlamp Pole on sidewalk curb every 44m
                    if (dInt % 44 == 0 && side == 1) {
                        val poleLat = sample.widthM * 0.5f + 0.75f
                        val px = sample.x + sample.rightX * poleLat
                        val pz = sample.z + sample.rightZ * poleLat
                        drawOrientedBox(px, sample.y + 3.4f, pz, sample.headingRad, 0.16f, 6.8f, 0.16f, 0.30f, 0.34f, 0.38f)
                        drawOrientedBox(
                            px - sample.rightX * 0.85f, sample.y + 6.75f, pz - sample.rightZ * 0.85f,
                            sample.headingRad, 1.7f, 0.14f, 0.22f, 0.34f, 0.38f, 0.42f
                        )
                        val lampEm = 0.2f + nightGlow * 1.15f
                        drawOrientedBox(
                            px - sample.rightX * 1.45f, sample.y + 6.55f, pz - sample.rightZ * 1.45f,
                            sample.headingRad, 0.45f, 0.22f, 0.45f, 1.0f, 0.88f, 0.55f,
                            emR = lampEm, emG = lampEm * 0.78f, emB = lampEm * 0.35f
                        )
                    }
                }
            } else {
                // Rural Mountain / Wadi / Desert / Coastal scenery: Guardrails, Palms, Highland Trees & Stone Houses
                val side = if (hash % 2 == 0) 1 else -1
                val lat = side * (sample.widthM * 0.5f + 5.5f + (hash % 7))
                val tx = sample.x + sample.rightX * lat
                val tz = sample.z + sample.rightZ * lat
                val ty = RoadSplineNetwork.terrainHeightAt(tx, tz, sample, lat)

                if (sample.regionId in listOf(RegionId.MASALIK_DESERT, RegionId.SHIBAM_HADRAMOUT, RegionId.TIHAMA_PLAINS, RegionId.COASTAL_ROAD)) {
                    // Desert / Wadi Date Palm Tree
                    drawOrientedBox(tx, ty + 3.1f, tz, 0f, 0.42f, 6.2f, 0.42f, 0.48f, 0.34f, 0.22f)
                    drawCone(tx, ty + 6.4f, tz, 4.6f, 2.2f, 0.18f, 0.48f, 0.20f)
                } else {
                    // Terraced Highland Cypress / Acacia Tree
                    drawOrientedBox(tx, ty + 1.5f, tz, 0f, 0.38f, 3.0f, 0.38f, 0.40f, 0.28f, 0.18f)
                    drawCone(tx, ty + 4.2f, tz, 3.4f, 4.6f, 0.15f, 0.40f, 0.18f)
                }

                // Mountain highway safety posts
                for (gSide in intArrayOf(-1, 1)) {
                    val gLat = gSide * (sample.widthM * 0.5f + 0.55f)
                    drawOrientedBox(
                        sample.x + sample.rightX * gLat, sample.y + 0.45f, sample.z + sample.rightZ * gLat,
                        sample.headingRad, 0.24f, 0.85f, 1.4f, 0.86f, 0.84f, 0.80f
                    )
                }
            }

            // Red Sea / Gulf of Aden shimmering water along Coastal Road
            if (sample.regionId == RegionId.COASTAL_ROAD) {
                val seaLat = sample.widthM * 0.5f + 46f
                drawOrientedBox(
                    sample.x + sample.rightX * seaLat, sample.y - 2.6f, sample.z + sample.rightZ * seaLat,
                    sample.headingRad, 80f, 0.4f, 24f,
                    0.08f, 0.36f, 0.58f
                )
            }

            dInt += 22
        }

        // 2. Iconic Bab al-Yemen Archway Landmark in Old Sana'a (15450m)
        if (abs(15450f - centerDistM) < 220f) {
            val s = RoadSplineNetwork.sampleAt(15450f)
            for (side in intArrayOf(-1, 1)) {
                val lat = side * (s.widthM * 0.5f + 3.2f)
                drawOrientedBox(
                    s.x + s.rightX * lat, s.y + 6.5f, s.z + s.rightZ * lat,
                    s.headingRad, 4.8f, 13f, 5.2f, 0.88f, 0.72f, 0.56f,
                    texId = textures.sanaaFacadeTex, uvScaleX = 1f, uvScaleY = 2f
                )
            }
            drawOrientedBox(
                s.x, s.y + 9.2f, s.z, s.headingRad,
                s.widthM + 6.8f, 3.4f, 4.4f, 0.92f, 0.82f, 0.68f,
                texId = textures.sanaaFacadeTex, uvScaleX = 2f, uvScaleY = 0.7f
            )
        }

        // 3. Service Stations (Fuel Canopies, Workshops & Car Washes) at POIs
        for (poi in RoadSplineNetwork.WORLD_POIS) {
            if (abs(poi.distanceM - centerDistM) > 210f) continue
            if (poi.type !in listOf("fuel", "workshop", "car_wash")) continue
            val s = RoadSplineNetwork.sampleAt(poi.distanceM)
            val px = s.x + s.rightX * poi.lateralOffsetM
            val pz = s.z + s.rightZ * poi.lateralOffsetM
            val (ar, ag, ab) = when (poi.type) {
                "fuel" -> Triple(0.95f, 0.60f, 0.10f)
                "workshop" -> Triple(0.18f, 0.56f, 0.88f)
                else -> Triple(0.10f, 0.72f, 0.52f)
            }
            // Canopy roof + two pillars + glowing sign band
            drawOrientedBox(px, s.y + 4.6f, pz, s.headingRad, 6.2f, 0.7f, 9.5f, ar, ag, ab, emR = ar * 0.25f, emG = ag * 0.25f, emB = ab * 0.25f)
            drawOrientedBox(px, s.y + 2.2f, pz + 3.2f, s.headingRad, 0.5f, 4.4f, 0.5f, 0.85f, 0.85f, 0.88f)
            drawOrientedBox(px, s.y + 2.2f, pz - 3.2f, s.headingRad, 0.5f, 4.4f, 0.5f, 0.85f, 0.85f, 0.88f)
        }

        // 4. Live Traffic Signal Gantries & Zebra Pedestrian Crosswalks
        for (sig in snap.livingWorld.trafficSignals) {
            if (abs(sig.def.distanceM - centerDistM) > 190f) continue
            val s = RoadSplineNetwork.sampleAt(sig.def.distanceM)

            // White Zebra Crosswalk painted directly on the road!
            val cwSample = RoadSplineNetwork.sampleAt(sig.def.distanceM + 2.5f)
            drawOrientedQuad(cwSample.x, cwSample.y + 0.06f, cwSample.z, cwSample.headingRad, cwSample.widthM - 0.6f, 3.8f, textures.crosswalkTex)

            // Overhead Signal Pole & Housing
            val poleLat = s.widthM * 0.5f + 0.9f
            val px = s.x + s.rightX * poleLat
            val pz = s.z + s.rightZ * poleLat
            drawOrientedBox(px, s.y + 3.2f, pz, s.headingRad, 0.22f, 6.4f, 0.22f, 0.22f, 0.24f, 0.28f)
            drawOrientedBox(
                px - s.rightX * 2.2f, s.y + 6.1f, pz - s.rightZ * 2.2f,
                s.headingRad, 4.5f, 0.22f, 0.22f, 0.22f, 0.24f, 0.28f
            )

            // Signal Box + Glowing Red / Yellow / Green Lenses
            val bx = px - s.rightX * 3.4f
            val bz = pz - s.rightZ * 3.4f
            drawOrientedBox(bx, s.y + 5.7f, bz, s.headingRad, 0.65f, 1.75f, 0.55f, 0.10f, 0.11f, 0.14f)

            val redOn = sig.phase == TrafficSignalPhase.RED
            val yelOn = sig.phase == TrafficSignalPhase.YELLOW
            val grnOn = sig.phase == TrafficSignalPhase.GREEN
            drawOrientedBox(bx, s.y + 6.22f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, if (redOn) 1f else 0.25f, 0.08f, 0.08f, emR = if (redOn) 1.4f else 0f)
            drawOrientedBox(bx, s.y + 5.72f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, if (yelOn) 1f else 0.25f, if (yelOn) 0.85f else 0.20f, 0.06f, emR = if (yelOn) 1.2f else 0f, emG = if (yelOn) 0.95f else 0f)
            drawOrientedBox(bx, s.y + 5.22f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, 0.08f, if (grnOn) 1f else 0.22f, 0.22f, emG = if (grnOn) 1.4f else 0f)
        }

        // 5. Mission Finish Archway Banner
        val finishM = snap.missionMarkerM
        if (finishM != null && abs(finishM - centerDistM) < 220f) {
            val s = RoadSplineNetwork.sampleAt(finishM)
            for (side in intArrayOf(-1, 1)) {
                val lat = side * (s.widthM * 0.5f + 0.8f)
                drawOrientedBox(s.x + s.rightX * lat, s.y + 3.5f, s.z + s.rightZ * lat, s.headingRad, 0.5f, 7f, 0.5f, 0.96f, 0.62f, 0.06f, emR = 0.45f, emG = 0.28f)
            }
            drawOrientedBox(s.x, s.y + 6.6f, s.z, s.headingRad, s.widthM + 2.2f, 1.1f, 0.7f, 0.96f, 0.62f, 0.06f, emR = 0.55f, emG = 0.35f)
        }
    }

    private fun drawParkedCars(centerDistM: Float, livingWorld: LivingWorldSimulation, viewDistanceM: Float) {
        for (car in livingWorld.parkedCars) {
            if (abs(car.distanceM - centerDistM) > viewDistanceM) continue
            drawTrafficVehicleBody(
                car.worldX, car.worldY, car.worldZ, car.headingRad,
                car.type, car.colorR, car.colorG, car.colorB,
                braking = false, headlightsOn = false, wheelSpin = 0f
            )
        }
    }

    private fun drawYemeniPedestrians(centerDistM: Float, livingWorld: LivingWorldSimulation, viewDistanceM: Float) {
        for (ped in livingWorld.pedestrians) {
            if (abs(ped.distanceM - centerDistM) > viewDistanceM * 0.75f) continue
            drawPedestrianCharacter(ped)
        }
    }

    private fun drawPedestrianCharacter(ped: PedestrianAgent) {
        val swing = if (ped.isMoving) sin(ped.walkPhaseRad) * 0.22f else 0f
        val (robeR, robeG, robeB) = when (ped.style) {
            PedestrianStyle.THOBE_WHITE -> Triple(0.94f, 0.92f, 0.86f)
            PedestrianStyle.MAAWIS_COLORFUL -> Triple(0.90f, 0.88f, 0.82f)
            PedestrianStyle.SHAWL_ELDER -> Triple(0.86f, 0.80f, 0.68f)
            PedestrianStyle.ABAYA_CLASSIC -> Triple(0.11f, 0.12f, 0.15f)
        }
        val fwdX = sin(ped.headingRad)
        val fwdZ = cos(ped.headingRad)
        val rightX = cos(ped.headingRad)
        val rightZ = -sin(ped.headingRad)

        // Articulated Legs (Ma'awis colorful lower garment or Thobe/Abaya)
        val (legR, legG, legB) = if (ped.style == PedestrianStyle.MAAWIS_COLORFUL) {
            Triple(ped.accentR, ped.accentG, ped.accentB)
        } else Triple(robeR * 0.9f, robeG * 0.9f, robeB * 0.9f)

        drawOrientedBox(ped.worldX - rightX * 0.11f + fwdX * swing, ped.worldY + 0.36f, ped.worldZ - rightZ * 0.11f + fwdZ * swing, ped.headingRad, 0.16f, 0.72f, 0.18f, legR, legG, legB)
        drawOrientedBox(ped.worldX + rightX * 0.11f - fwdX * swing, ped.worldY + 0.36f, ped.worldZ + rightZ * 0.11f - fwdZ * swing, ped.headingRad, 0.16f, 0.72f, 0.18f, legR, legG, legB)

        // Torso
        drawOrientedBox(ped.worldX, ped.worldY + 1.02f, ped.worldZ, ped.headingRad, 0.44f, 0.64f, 0.26f, robeR, robeG, robeB)

        // Traditional Jambiya belt / shawl accent
        if (ped.style != PedestrianStyle.ABAYA_CLASSIC) {
            drawOrientedBox(ped.worldX, ped.worldY + 0.82f, ped.worldZ, ped.headingRad, 0.46f, 0.11f, 0.28f, ped.accentR, ped.accentG, ped.accentB)
        }

        // Swinging Arms
        drawOrientedBox(ped.worldX - rightX * 0.28f - fwdX * swing * 0.8f, ped.worldY + 1.0f, ped.worldZ - rightZ * 0.28f - fwdZ * swing * 0.8f, ped.headingRad, 0.12f, 0.54f, 0.13f, robeR * 0.95f, robeG * 0.95f, robeB * 0.95f)
        drawOrientedBox(ped.worldX + rightX * 0.28f + fwdX * swing * 0.8f, ped.worldY + 1.0f, ped.worldZ + rightZ * 0.28f + fwdZ * swing * 0.8f, ped.headingRad, 0.12f, 0.54f, 0.13f, robeR * 0.95f, robeG * 0.95f, robeB * 0.95f)

        // Head
        drawOrientedBox(ped.worldX, ped.worldY + 1.50f, ped.worldZ, ped.headingRad, 0.25f, 0.26f, 0.25f, 0.76f, 0.56f, 0.42f)

        // Yemeni Turban (Imamah / Shemagh) or Hijab
        val (hatR, hatG, hatB) = when (ped.style) {
            PedestrianStyle.ABAYA_CLASSIC -> Triple(0.10f, 0.11f, 0.14f)
            PedestrianStyle.SHAWL_ELDER -> Triple(0.92f, 0.26f, 0.24f)
            else -> Triple(0.95f, 0.93f, 0.88f)
        }
        drawOrientedBox(ped.worldX, ped.worldY + 1.66f, ped.worldZ, ped.headingRad, 0.30f, 0.12f, 0.30f, hatR, hatG, hatB)
    }

    private fun drawAiTrafficAndRival(
        centerDistM: Float,
        snap: RenderFrameSnapshot,
        atmo: AtmosphereSnapshot
    ) {
        for (car in snap.livingWorld.trafficVehicles) {
            if (abs(car.distanceM - centerDistM) > snap.graphicsQuality.sceneryDistanceM) continue
            drawTrafficVehicleBody(
                car.worldX, car.worldY, car.worldZ, car.headingRad,
                car.type, car.colorR, car.colorG, car.colorB,
                car.braking, car.headlightsOn, car.wheelSpinRad
            )
        }

        // Race Mission AI Rival (distinct gold/crimson rally car)
        val rivalDist = snap.telemetry.aiRivalDistanceM
        if (rivalDist != null && abs(rivalDist - centerDistM) < 220f) {
            val rs = RoadSplineNetwork.sampleAt(rivalDist)
            val rx = rs.x - rs.rightX * 2.2f
            val rz = rs.z - rs.rightZ * 2.2f
            drawTrafficVehicleBody(
                rx, rs.y, rz, rs.headingRad,
                TrafficVehicleType.SEDAN, 0.96f, 0.68f, 0.08f,
                braking = false, headlightsOn = atmo.nightFactor > 0.3f, wheelSpin = weatherAnimTime * 18f
            )
        }
    }

    private fun drawTrafficVehicleBody(
        x: Float, y: Float, z: Float, heading: Float,
        type: TrafficVehicleType,
        r: Float, g: Float, b: Float,
        braking: Boolean,
        headlightsOn: Boolean,
        wheelSpin: Float
    ) {
        val (len, wid, bodyH, cabH) = when (type) {
            TrafficVehicleType.SEDAN -> Quad4(4.4f, 1.82f, 0.68f, 0.56f)
            TrafficVehicleType.PICKUP -> Quad4(4.9f, 1.88f, 0.78f, 0.72f)
            TrafficVehicleType.MINIBUS -> Quad4(5.3f, 1.98f, 1.15f, 0.85f)
            TrafficVehicleType.TRUCK -> Quad4(6.2f, 2.24f, 1.45f, 1.15f)
        }
        val fwdX = sin(heading); val fwdZ = cos(heading)
        val rightX = cos(heading); val rightZ = -sin(heading)

        // Lower chassis
        drawOrientedBox(x, y + 0.36f + bodyH * 0.5f, z, heading, wid, bodyH, len, r, g, b)

        // Cabin / Cargo box
        val cabZOff = when (type) {
            TrafficVehicleType.PICKUP -> 0.55f
            TrafficVehicleType.TRUCK -> 1.65f
            else -> -0.1f
        }
        val cabLen = when (type) {
            TrafficVehicleType.PICKUP -> 1.85f
            TrafficVehicleType.MINIBUS -> len * 0.88f
            TrafficVehicleType.TRUCK -> 1.8f
            else -> len * 0.54f
        }
        drawOrientedBox(
            x + fwdX * cabZOff, y + 0.36f + bodyH + cabH * 0.45f, z + fwdZ * cabZOff,
            heading, wid * 0.90f, cabH, cabLen,
            if (type == TrafficVehicleType.MINIBUS) 0.94f else r * 0.92f,
            if (type == TrafficVehicleType.MINIBUS) 0.94f else g * 0.92f,
            if (type == TrafficVehicleType.MINIBUS) 0.96f else b * 0.92f
        )
        // Dark tinted glass wrap
        drawOrientedBox(
            x + fwdX * cabZOff, y + 0.36f + bodyH + cabH * 0.44f, z + fwdZ * cabZOff,
            heading, wid * 0.92f, cabH * 0.68f, cabLen * 0.86f,
            0.10f, 0.14f, 0.20f
        )
        if (type == TrafficVehicleType.TRUCK) {
            drawOrientedBox(
                x - fwdX * 1.1f, y + 0.36f + bodyH + 0.85f, z - fwdZ * 1.1f,
                heading, wid * 0.96f, 1.85f, 3.6f, 0.86f, 0.84f, 0.78f
            )
        }

        // 4 Wheels
        val wb = len * 0.32f
        val tr = wid * 0.48f
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                drawWheel(
                    x + rightX * (sx * tr) + fwdX * (sz * wb),
                    y + 0.38f,
                    z + rightZ * (sx * tr) + fwdZ * (sz * wb),
                    heading, 0f, wheelSpin, 0.38f, 0.26f
                )
            }
        }

        // Headlights & Brake Lights
        val hlEm = if (headlightsOn) 1.35f else 0.08f
        val blEm = if (braking) 1.45f else 0.18f
        for (sx in intArrayOf(-1, 1)) {
            drawOrientedBox(
                x + fwdX * (len * 0.50f) + rightX * (sx * wid * 0.34f),
                y + 0.66f,
                z + fwdZ * (len * 0.50f) + rightZ * (sx * wid * 0.34f),
                heading, 0.34f, 0.20f, 0.08f, 1f, 0.96f, 0.82f,
                emR = hlEm, emG = hlEm * 0.92f, emB = hlEm * 0.72f
            )
            drawOrientedBox(
                x - fwdX * (len * 0.50f) + rightX * (sx * wid * 0.34f),
                y + 0.66f,
                z - fwdZ * (len * 0.50f) + rightZ * (sx * wid * 0.34f),
                heading, 0.36f, 0.20f, 0.08f, 0.95f, 0.10f, 0.08f,
                emR = blEm, emG = 0.02f, emB = 0.02f
            )
        }
    }

    private fun drawDetailedPlayerVehicle(
        t: VehicleTelemetry,
        vehicleId: VehicleId,
        custom: VehicleCustomization,
        blinkerOn: Boolean,
        atmo: AtmosphereSnapshot
    ) {
        val spec = VehicleFleetCatalog.specOf(vehicleId)
        val parsed = runCatching { Color.parseColor(custom.paintColorHex) }.getOrDefault(Color.rgb(200, 114, 54))
        val mud = t.damage.dirtMudLevel * 0.45f
        val pr = (Color.red(parsed) / 255f) * (1f - mud) + 0.36f * mud
        val pg = (Color.green(parsed) / 255f) * (1f - mud) + 0.28f * mud
        val pb = (Color.blue(parsed) / 255f) * (1f - mud) + 0.18f * mud

        val len = spec.bodyLengthM
        val wid = spec.bodyWidthM
        val hgt = spec.bodyHeightM
        val fwdX = sin(t.headingRad); val fwdZ = cos(t.headingRad)
        val rightX = cos(t.headingRad); val rightZ = -sin(t.headingRad)
        val baseY = t.posY + spec.wheelRadiusM * 0.92f

        // Lower body
        val lowerH = hgt * 0.44f
        drawOrientedBox(t.posX, baseY + lowerH * 0.5f, t.posZ, t.headingRad, wid, lowerH, len, pr, pg, pb)

        // Hood / Grille front trim
        drawOrientedBox(
            t.posX + fwdX * (len * 0.495f), baseY + lowerH * 0.48f, t.posZ + fwdZ * (len * 0.495f),
            t.headingRad, wid * 0.62f, lowerH * 0.55f, 0.10f, 0.12f, 0.13f, 0.16f
        )

        // Upper cabin tailored per vehicle class
        val isPickup = vehicleId in listOf(VehicleId.SAQR, VehicleId.BADIYA, VehicleId.MUNQIDH)
        val isTruck = vehicleId == VehicleId.JABAL
        val isBus = vehicleId == VehicleId.HAFILAT
        val cabH = hgt * 0.42f
        val cabLen = when {
            isBus -> len * 0.90f
            isPickup -> len * 0.42f
            isTruck -> len * 0.34f
            else -> len * 0.56f
        }
        val cabOffset = when {
            isPickup -> len * 0.12f
            isTruck -> len * 0.26f
            else -> -len * 0.05f
        }
        drawOrientedBox(
            t.posX + fwdX * cabOffset, baseY + lowerH + cabH * 0.46f, t.posZ + fwdZ * cabOffset,
            t.headingRad, wid * 0.88f, cabH, cabLen, pr * 0.96f, pg * 0.96f, pb * 0.96f
        )
        // Panoramic cabin glass
        drawOrientedBox(
            t.posX + fwdX * cabOffset, baseY + lowerH + cabH * 0.48f, t.posZ + fwdZ * cabOffset,
            t.headingRad, wid * 0.90f, cabH * 0.64f, cabLen * 0.88f, 0.09f, 0.14f, 0.22f
        )

        // Truck cargo container / Pickup accessories
        if (isTruck) {
            drawOrientedBox(
                t.posX - fwdX * (len * 0.18f), baseY + lowerH + 0.95f, t.posZ - fwdZ * (len * 0.18f),
                t.headingRad, wid * 0.95f, 1.95f, len * 0.56f, 0.90f, 0.88f, 0.82f
            )
        } else if (isPickup && custom.pickupAccessory != PickupAccessoryType.NONE) {
            val accH = if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) cabH else 0.65f
            drawOrientedBox(
                t.posX - fwdX * (len * 0.24f), baseY + lowerH + accH * 0.5f, t.posZ - fwdZ * (len * 0.24f),
                t.headingRad, wid * 0.86f, accH, len * 0.38f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.78f else 0.18f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.70f else 0.19f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.54f else 0.22f
            )
        }

        // Front Steel Bullbar / Expedition Winch
        if (custom.bullbarStyle != BullbarStyle.NONE) {
            drawOrientedBox(
                t.posX + fwdX * (len * 0.53f), baseY + 0.32f, t.posZ + fwdZ * (len * 0.53f),
                t.headingRad, wid * 0.94f, 0.36f, 0.26f, 0.14f, 0.15f, 0.18f
            )
        }

        // Roof Rack & Cargo Box
        if (custom.roofRackInstalled) {
            val roofY = baseY + lowerH + cabH + 0.08f
            drawOrientedBox(
                t.posX + fwdX * cabOffset, roofY, t.posZ + fwdZ * cabOffset,
                t.headingRad, wid * 0.78f, 0.12f, cabLen * 0.84f, 0.16f, 0.17f, 0.20f
            )
            if (custom.roofCargoBoxInstalled) {
                drawOrientedBox(
                    t.posX + fwdX * cabOffset, roofY + 0.24f, t.posZ + fwdZ * cabOffset,
                    t.headingRad, wid * 0.54f, 0.34f, cabLen * 0.62f, 0.82f, 0.62f, 0.26f
                )
            }
        }

        // Emergency Flashing Lightbar on Rescue Vehicle (المنقذ)
        if (vehicleId == VehicleId.MUNQIDH) {
            val roofY = baseY + lowerH + cabH + 0.18f
            val flashA = ((weatherAnimTime * 6f).toInt() % 2 == 0)
            drawOrientedBox(
                t.posX + fwdX * cabOffset - rightX * 0.38f, roofY, t.posZ + fwdZ * cabOffset - rightZ * 0.38f,
                t.headingRad, 0.45f, 0.22f, 0.28f, 1f, 0.55f, 0.05f,
                emR = if (flashA) 1.5f else 0.1f, emG = if (flashA) 0.7f else 0.05f
            )
            drawOrientedBox(
                t.posX + fwdX * cabOffset + rightX * 0.38f, roofY, t.posZ + fwdZ * cabOffset + rightZ * 0.38f,
                t.headingRad, 0.45f, 0.22f, 0.28f, 0.12f, 0.55f, 1f,
                emB = if (!flashA) 1.5f else 0.1f, emG = if (!flashA) 0.6f else 0.05f
            )
        }

        // 4 Detailed Wheels (front wheels steer!)
        val wb = spec.wheelbaseM * 0.5f
        val tr = spec.trackWidthM * 0.5f
        for (sx in intArrayOf(-1, 1)) {
            // Front
            drawWheel(
                t.posX + rightX * (sx * tr) + fwdX * wb,
                t.posY + spec.wheelRadiusM,
                t.posZ + rightZ * (sx * tr) + fwdZ * wb,
                t.headingRad, t.frontSteerRad, t.wheelRotationRad, spec.wheelRadiusM, 0.32f
            )
            // Rear
            drawWheel(
                t.posX + rightX * (sx * tr) - fwdX * wb,
                t.posY + spec.wheelRadiusM,
                t.posZ + rightZ * (sx * tr) - fwdZ * wb,
                t.headingRad, 0f, t.wheelRotationRad, spec.wheelRadiusM, 0.34f
            )
        }

        // Headlights, Brake Lights & Blinkers
        val hlOn = t.headlightsOn || atmo.nightFactor > 0.35f
        val hlEm = if (hlOn) 1.6f else 0.12f
        val blEm = if (t.brake > 0.1f || t.handbrake) 1.6f else 0.25f
        for (sx in intArrayOf(-1, 1)) {
            drawOrientedBox(
                t.posX + fwdX * (len * 0.505f) + rightX * (sx * wid * 0.35f),
                baseY + lowerH * 0.55f,
                t.posZ + fwdZ * (len * 0.505f) + rightZ * (sx * wid * 0.35f),
                t.headingRad, 0.36f, 0.22f, 0.08f, 1f, 0.96f, 0.85f,
                emR = hlEm, emG = hlEm * 0.92f, emB = hlEm * 0.72f
            )
            val isBlinkerLit = blinkerOn && ((sx < 0 && t.leftIndicatorOn) || (sx > 0 && t.rightIndicatorOn))
            drawOrientedBox(
                t.posX - fwdX * (len * 0.505f) + rightX * (sx * wid * 0.35f),
                baseY + lowerH * 0.55f,
                t.posZ - fwdZ * (len * 0.505f) + rightZ * (sx * wid * 0.35f),
                t.headingRad, 0.38f, 0.22f, 0.08f,
                if (isBlinkerLit) 1f else 0.95f, if (isBlinkerLit) 0.65f else 0.10f, 0.06f,
                emR = if (isBlinkerLit) 1.5f else blEm, emG = if (isBlinkerLit) 0.9f else 0.02f
            )
        }
    }

    private fun drawCockpitInterior(t: VehicleTelemetry, custom: VehicleCustomization, atmo: AtmosphereSnapshot) {
        val fwdX = sin(t.headingRad); val fwdZ = cos(t.headingRad)
        val rightX = cos(t.headingRad); val rightZ = -sin(t.headingRad)
        // Hood visible through windshield
        val parsed = runCatching { Color.parseColor(custom.paintColorHex) }.getOrDefault(Color.rgb(200, 114, 54))
        drawOrientedBox(
            t.posX + fwdX * 1.55f, t.posY + 1.05f, t.posZ + fwdZ * 1.55f,
            t.headingRad, 1.65f, 0.24f, 1.55f,
            Color.red(parsed) / 255f, Color.green(parsed) / 255f, Color.blue(parsed) / 255f
        )
        // 3D Dashboard console
        drawOrientedBox(
            t.posX + fwdX * 0.82f, t.posY + 1.08f, t.posZ + fwdZ * 0.82f,
            t.headingRad, 1.72f, 0.36f, 0.44f, 0.10f, 0.11f, 0.13f
        )
        // Glowing Digital Instrument Cluster inside cockpit
        drawOrientedBox(
            t.posX + fwdX * 0.72f - rightX * 0.36f, t.posY + 1.18f, t.posZ + fwdZ * 0.72f - rightZ * 0.36f,
            t.headingRad, 0.42f, 0.14f, 0.05f, 0.15f, 0.78f, 0.95f,
            emR = 0.25f, emG = 0.65f, emB = 0.85f
        )
        // Windshield A-pillars
        for (side in intArrayOf(-1, 1)) {
            drawOrientedBox(
                t.posX + fwdX * 0.85f + rightX * (side * 0.82f), t.posY + 1.55f, t.posZ + fwdZ * 0.85f + rightZ * (side * 0.82f),
                t.headingRad, 0.09f, 0.85f, 0.11f, 0.11f, 0.12f, 0.14f
            )
        }
        // Rotating 3D Steering Wheel inside cockpit
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, t.posX + fwdX * 0.62f - rightX * 0.36f, t.posY + 1.16f, t.posZ + fwdZ * 0.62f - rightZ * 0.36f)
        Matrix.rotateM(modelMatrix, 0, t.headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, -t.steeringWheelAngleDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.34f, 0.08f, 0.06f)
        drawMeshWithCurrentMatrix(cubeMesh, 0.16f, 0.17f, 0.20f, textures.whiteTex, 0f, 1f, 1f, 0.12f * atmo.nightFactor, 0.08f * atmo.nightFactor, 0f)
    }

    private fun drawWeatherParticles(weather: WeatherPreset, particleScale: Float) {
        val isSand = weather == WeatherPreset.SANDSTORM
        val count = ((if (weather == WeatherPreset.HEAVY_RAIN) 55 else 30) * particleScale).toInt()
        val r = if (isSand) 0.88f else 0.78f
        val g = if (isSand) 0.70f else 0.86f
        val b = if (isSand) 0.44f else 0.96f

        for (i in 0 until count) {
            val seed = i * 19.37f
            val px = currentCamX + sin(seed + weatherAnimTime * (if (isSand) 3.4f else 0.3f)) * 16f
            val pz = currentCamZ + cos(seed * 1.3f + weatherAnimTime * 0.4f) * 16f
            val fall = ((weatherAnimTime * (if (isSand) 4f else 18f) + i * 0.7f) % 10f)
            val py = currentCamY + 5f - fall
            drawOrientedBox(
                px, py, pz, 0f,
                if (isSand) 0.16f else 0.03f,
                if (isSand) 0.14f else 0.68f,
                if (isSand) 0.16f else 0.03f,
                r, g, b
            )
        }
    }

    private fun drawWheel(
        x: Float, y: Float, z: Float,
        headingRad: Float, steerRad: Float, spinRad: Float,
        radius: Float, width: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, (headingRad + steerRad) * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, spinRad * 180f / PI.toFloat(), 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, width, radius * 2f, radius * 2f)
        drawMeshWithCurrentMatrix(wheelMesh, 0.12f, 0.13f, 0.15f, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f)

        // Alloy Rim hubcap
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, (headingRad + steerRad) * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, spinRad * 180f / PI.toFloat(), 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, width * 1.04f, radius * 1.05f, radius * 1.05f)
        drawMeshWithCurrentMatrix(wheelMesh, 0.76f, 0.78f, 0.82f, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f)
    }

    private fun drawOrientedBox(
        x: Float, y: Float, z: Float,
        headingRad: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        texId: Int = textures.whiteTex,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f,
        emR: Float = 0f,
        emG: Float = 0f,
        emB: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        val useTex = if (texId != textures.whiteTex) 1f else 0f
        drawMeshWithCurrentMatrix(cubeMesh, r, g, b, texId, useTex, uvScaleX, uvScaleY, emR, emG, emB)
    }

    private fun drawOrientedQuad(
        x: Float, y: Float, z: Float,
        headingRad: Float,
        sx: Float, sz: Float,
        texId: Int
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, 1f, sz)
        drawMeshWithCurrentMatrix(quadMesh, 1f, 1f, 1f, texId, 1f, 1f, 1f, 0f, 0f, 0f)
    }

    private fun drawCone(x: Float, y: Float, z: Float, width: Float, height: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, width, height, width)
        drawMeshWithCurrentMatrix(coneMesh, r, g, b, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f)
    }

    private fun drawMeshWithCurrentMatrix(
        mesh: GpuMesh,
        r: Float, g: Float, b: Float,
        texId: Int,
        useTex: Float,
        uvScaleX: Float,
        uvScaleY: Float,
        emR: Float, emG: Float, emB: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(shaders.uBaseColor, r, g, b, 1f)
        GLES30.glUniform3f(shaders.uEmissive, emR, emG, emB)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1f(shaders.uUseTexture, useTex)
        GLES30.glUniform2f(shaders.uUvScale, uvScaleX, uvScaleY)
        mesh.draw()
    }

    private data class Quad4(val a: Float, val b: Float, val c: Float, val d: Float)
}
