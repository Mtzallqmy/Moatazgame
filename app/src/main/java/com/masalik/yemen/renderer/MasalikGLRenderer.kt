package com.masalik.yemen.renderer

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.masalik.yemen.game.AtmosphereSnapshot
import com.masalik.yemen.game.BullbarStyle
import com.masalik.yemen.game.CameraMode
import com.masalik.yemen.game.DriverSeatSide
import com.masalik.yemen.game.HeadlightBeamMode
import com.masalik.yemen.game.LivingWorldSimulation
import com.masalik.yemen.game.PaintFinishType
import com.masalik.yemen.game.PedestrianAgent
import com.masalik.yemen.game.PedestrianStyle
import com.masalik.yemen.game.PickupAccessoryType
import com.masalik.yemen.game.RegionId
import com.masalik.yemen.game.ReplayCameraAngle
import com.masalik.yemen.game.RoadSample
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.SteeringWheelStyle
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
        val photoFovDeg: Float,
        val worldTimeHours: Float,
        val weather: WeatherPreset,
        val livingWorld: LivingWorldSimulation,
        val finishDistanceM: Float?,
        val blinkerOn: Boolean,
        val wheelStyle: SteeringWheelStyle = SteeringWheelStyle.EUROPEAN_LUXURY,
        val seatSide: DriverSeatSide = DriverSeatSide.LEFT_HAND,
        val rearMirrorEnabled: Boolean = true,
        val isReplayMode: Boolean = false,
        val replayCameraAngle: ReplayCameraAngle = ReplayCameraAngle.DRONE_ORBIT,
        val orbitYawRad: Float = 0.42f,
        val orbitPitchRad: Float = 0.24f
    )

    private lateinit var shaders: ShaderLibrary
    private lateinit var textures: ProceduralTextures
    private lateinit var cubeMesh: GpuMesh
    private lateinit var slopedBodyMesh: GpuMesh
    private lateinit var cabinMesh: GpuMesh
    private lateinit var wheelMesh: GpuMesh
    private lateinit var quadMesh: GpuMesh
    private lateinit var coneMesh: GpuMesh
    private lateinit var steerRingRound: GpuMesh
    private lateinit var steerRingFlat: GpuMesh

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
    private var screenWidth = 1920
    private var screenHeight = 1080
    private var lastFrameNs = System.nanoTime()
    private var lastStreamCenterDistM = -9999f
    private var weatherAnimTime = 0f

    private val fxEngine = ParticleAndMirrorEngine()

    private var currentCamX = 0f
    private var currentCamY = 4f
    private var currentCamZ = -8f
    private var smoothCamX = 0f
    private var smoothCamY = 4f
    private var smoothCamZ = -8f
    private var camInitialized = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shaders = ShaderLibrary()
        textures = ProceduralTextures()
        cubeMesh = MeshPrimitives.createUnitCube()
        slopedBodyMesh = MeshPrimitives.createSlopedBody()
        cabinMesh = MeshPrimitives.createTrapezoidCabin()
        wheelMesh = MeshPrimitives.createWheelCylinder(24)
        quadMesh = MeshPrimitives.createUnitQuadXZ()
        coneMesh = MeshPrimitives.createConePyramid(8)
        steerRingRound = MeshPrimitives.createSteeringRing(24, flatBottom = false)
        steerRingFlat = MeshPrimitives.createSteeringRing(24, flatBottom = true)

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

        fxEngine.initGlResources()
        lastFrameNs = System.nanoTime()
        lastStreamCenterDistM = -9999f
        camInitialized = false
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
        lastFrameNs = now
        weatherAnimTime += dt

        val snap = onStepFrame(dt)
        val atmo = LivingWorldSimulation.sampleAtmosphere(snap.worldTimeHours, snap.weather)
        val t = snap.telemetry

        val centerDist = t.distanceAlongRoadM
        if (abs(centerDist - lastStreamCenterDistM) > 55f) {
            rebuildStreamingCorridor(centerDist)
            lastStreamCenterDistM = centerDist
        }

        // Update 3D Drift Smoke, Desert Dust, Rain Spray, Exhaust Backfire Flames & Asphalt Skidmarks
        val playerSpec = VehicleFleetCatalog.specOf(t.vehicleId)
        fxEngine.stepParticlesAndSkids(dt, t, playerSpec, snap.weather, !snap.isLobbyOrGarage)

        // Pass A: Live Rear-View Mirror Render-to-Texture FBO Pass (Backwards-facing 3D camera!)
        val showLiveMirror = !snap.isLobbyOrGarage && !snap.isReplayMode && snap.rearMirrorEnabled &&
            (snap.cameraMode == CameraMode.COCKPIT || snap.cameraMode == CameraMode.HOOD || snap.cameraMode == CameraMode.CHASE_NEAR)
        if (showLiveMirror && fxEngine.beginRearMirrorPass()) {
            GLES30.glClearColor(atmo.skyR, atmo.skyG, atmo.skyB, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            drawSkyBackdrop(atmo)

            val fwdX = sin(t.headingRad); val fwdZ = cos(t.headingRad)
            val mEyeX = t.posX - fwdX * 0.45f
            val mEyeY = t.posY + 1.55f
            val mEyeZ = t.posZ - fwdZ * 0.45f
            Matrix.perspectiveM(projMatrix, 0, 56f, fxEngine.mirrorWidth.toFloat() / fxEngine.mirrorHeight.toFloat(), 0.2f, 260f)
            Matrix.setLookAtM(viewMatrix, 0, mEyeX, mEyeY, mEyeZ, mEyeX - fwdX * 32f, mEyeY - 0.8f, mEyeZ - fwdZ * 32f, 0f, 1f, 0f)
            Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
            currentCamX = mEyeX; currentCamY = mEyeY; currentCamZ = mEyeZ

            renderWorldScenePass(snap, atmo, centerDist, isMirrorPass = true)
            fxEngine.endRearMirrorPass(screenWidth, screenHeight)
        }

        // Pass B: Primary Full-Resolution 3D Viewport Pass
        GLES30.glClearColor(atmo.skyR, atmo.skyG, atmo.skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        drawSkyBackdrop(atmo)

        val speedFovBoost = (t.speedKmh / 140f).coerceIn(0f, 1f) * 13f
        val fov = when {
            snap.isLobbyOrGarage -> 44f
            snap.isReplayMode && snap.replayCameraAngle == ReplayCameraAngle.DRONE_ORBIT -> 52f
            snap.isReplayMode && snap.replayCameraAngle == ReplayCameraAngle.TRACKSIDE -> 46f
            snap.cameraMode == CameraMode.FREE_PHOTO -> snap.photoFovDeg
            snap.cameraMode == CameraMode.CINEMATIC_ORBIT -> 54f + speedFovBoost * 0.55f
            snap.cameraMode == CameraMode.HOOD -> 64f + speedFovBoost * 0.65f
            snap.cameraMode == CameraMode.COCKPIT || (snap.isReplayMode && snap.replayCameraAngle == ReplayCameraAngle.COCKPIT) -> 68f + speedFovBoost * 0.45f
            else -> 58f + speedFovBoost
        }
        Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.16f, 440f)
        updateCameraMatrix(snap, dt)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        renderWorldScenePass(snap, atmo, centerDist, isMirrorPass = false)

        // Composite Live Chrome-Framed Rear-View Mirror HUD directly on top of the 3D scene
        if (showLiveMirror) {
            fxEngine.drawMirrorOverlay(shaders, skyVboId, isCockpit = snap.cameraMode == CameraMode.COCKPIT)
        }
    }

    private fun renderWorldScenePass(
        snap: RenderFrameSnapshot,
        atmo: AtmosphereSnapshot,
        centerDist: Float,
        isMirrorPass: Boolean
    ) {
        val t = snap.telemetry
        GLES30.glUseProgram(shaders.worldProgram)
        GLES30.glUniform3f(shaders.uSunDir, atmo.sunDirX, atmo.sunDirY, atmo.sunDirZ)
        GLES30.glUniform3f(shaders.uSunColor, atmo.sunR, atmo.sunG, atmo.sunB)
        GLES30.glUniform1f(shaders.uSunIntensity, atmo.sunIntensity)
        GLES30.glUniform1f(shaders.uAmbientIntensity, atmo.ambientIntensity)
        GLES30.glUniform3f(shaders.uFogColor, atmo.horizonR, atmo.horizonG, atmo.horizonB)
        GLES30.glUniform1f(shaders.uFogDensity, atmo.fogDensity)
        GLES30.glUniform3f(shaders.uCameraPos, currentCamX, currentCamY, currentCamZ)
        GLES30.glUniform1f(shaders.uNightFactor, atmo.nightFactor)

        val fwdX = sin(t.headingRad)
        val fwdZ = cos(t.headingRad)
        val beamPower = when {
            t.headlightBeamMode == HeadlightBeamMode.HIGH_BEAM -> 1.85f
            t.headlightBeamMode == HeadlightBeamMode.LOW_BEAM -> 1.10f
            atmo.nightFactor > 0.35f || t.inTunnel -> 1.05f
            else -> 0f
        }
        val activeHeadlightStrength = if (snap.isLobbyOrGarage || isMirrorPass) 0f else beamPower * (0.35f + 0.65f * atmo.nightFactor + if (t.inTunnel) 0.7f else 0f)
        GLES30.glUniform3f(shaders.uHeadlightPos, t.posX + fwdX * 2.1f, t.posY + 0.92f, t.posZ + fwdZ * 2.1f)
        GLES30.glUniform3f(shaders.uHeadlightDir, fwdX, -0.075f, fwdZ)
        GLES30.glUniform1f(shaders.uHeadlightStrength, activeHeadlightStrength.coerceAtMost(2.2f))

        val roadWetness = when (snap.weather) {
            WeatherPreset.HEAVY_RAIN -> 0.85f
            WeatherPreset.LIGHT_RAIN -> 0.55f
            WeatherPreset.MOUNTAIN_FOG -> 0.25f
            else -> 0.04f
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(shaders.uTexture, 0)

        val curSample = RoadSplineNetwork.sampleAt(centerDist)
        drawCorridorGeometry(t.regionId, t.surface, curSample.isModernBoulevard, roadWetness)

        // Persistent 3D Asphalt Skidmarks directly over road surface
        if (!isMirrorPass) {
            fxEngine.renderSkidmarks { sx, sy, sz, hRad, w, l, alpha ->
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, sx, sy, sz)
                Matrix.rotateM(modelMatrix, 0, hRad * 180f / PI.toFloat(), 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, w, 1f, l)
                drawMeshWithCurrentMatrix(quadMesh, 0.06f, 0.06f, 0.07f, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0.95f, alpha = alpha)
            }
        }

        drawYemeniCityEnvironment(centerDist, snap, atmo)
        drawRoundaboutsAndCrossStreets(centerDist, atmo)
        drawSpeedRadars(centerDist, atmo)
        drawParkedCars(centerDist, snap.livingWorld)
        if (!isMirrorPass) drawYemeniPedestrians(centerDist, snap.livingWorld)
        drawAiTrafficAndRival(centerDist, snap, atmo)

        if (!isMirrorPass) {
            val drawVehicleId = if (snap.isLobbyOrGarage) snap.previewVehicleId else t.vehicleId
            val drawCustom = if (snap.isLobbyOrGarage) snap.previewCustomization else snap.customization
            val showCockpit = !snap.isLobbyOrGarage &&
                ((!snap.isReplayMode && snap.cameraMode == CameraMode.COCKPIT) ||
                    (snap.isReplayMode && snap.replayCameraAngle == ReplayCameraAngle.COCKPIT))
            if (showCockpit) {
                drawCockpitInterior(t, drawCustom, snap.wheelStyle, snap.seatSide, atmo)
            } else {
                drawDetailedPlayerVehicle(t, drawVehicleId, drawCustom, snap.blinkerOn, atmo)
            }

            // 3D Drift Smoke, Desert Dust, Rain Spray & Exhaust Backfire Flames
            fxEngine.renderParticles { px, py, pz, sz, r, g, b, alpha, emR, emG, emB ->
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, px, py, pz)
                Matrix.scaleM(modelMatrix, 0, sz, sz, sz)
                drawMeshWithCurrentMatrix(cubeMesh, r, g, b, textures.whiteTex, 0f, 1f, 1f, emR, emG, emB, 0f, 0.9f, alpha = alpha)
            }

            if (snap.weather in listOf(WeatherPreset.LIGHT_RAIN, WeatherPreset.HEAVY_RAIN, WeatherPreset.SANDSTORM)) {
                drawWeatherParticles(snap.weather)
            }
        }
    }

    private fun drawSkyBackdrop(atmo: AtmosphereSnapshot) {
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(shaders.skyProgram)
        GLES30.glUniform3f(shaders.uSkyTopColor, atmo.skyR, atmo.skyG, atmo.skyB)
        GLES30.glUniform3f(shaders.uSkyHorizonColor, atmo.horizonR, atmo.horizonG, atmo.horizonB)
        GLES30.glUniform3f(shaders.uSkySunDir, atmo.sunDirX, atmo.sunDirY, atmo.sunDirZ)
        GLES30.glUniform1f(shaders.uSkyNightFactor, atmo.nightFactor)
        GLES30.glUniform1f(shaders.uSkyTime, weatherAnimTime)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, skyVboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDepthMask(true)
    }

    private fun updateCameraMatrix(snap: RenderFrameSnapshot, dt: Float) {
        val t = snap.telemetry
        val fwdX = sin(t.headingRad)
        val fwdZ = cos(t.headingRad)
        val rightX = cos(t.headingRad)
        val rightZ = -sin(t.headingRad)
        val steerApexLook = t.steeringInputNorm * 2.1f

        var eyeX: Float
        var eyeY: Float
        var eyeZ: Float
        var targetX: Float
        var targetY: Float
        var targetZ: Float
        var upX = 0f
        var upY = 1f
        var upZ = 0f

        if (snap.isLobbyOrGarage) {
            val radius = 6.6f
            val angle = t.headingRad + snap.showroomOrbitRad
            eyeX = t.posX + sin(angle) * radius
            eyeY = t.posY + 2.15f
            eyeZ = t.posZ + cos(angle) * radius
            targetX = t.posX
            targetY = t.posY + 1.12f
            targetZ = t.posZ
            camInitialized = false
        } else if (snap.isReplayMode) {
            when (snap.replayCameraAngle) {
                ReplayCameraAngle.DRONE_ORBIT -> {
                    val ang = t.headingRad + weatherAnimTime * 0.55f + snap.showroomOrbitRad
                    val dist = 9.2f + sin(weatherAnimTime * 0.7f) * 1.8f
                    eyeX = t.posX + sin(ang) * dist
                    eyeY = t.posY + 4.4f + cos(weatherAnimTime * 0.6f) * 1.1f
                    eyeZ = t.posZ + cos(ang) * dist
                    targetX = t.posX + fwdX * 1.8f
                    targetY = t.posY + 1.1f
                    targetZ = t.posZ + fwdZ * 1.8f
                }
                ReplayCameraAngle.LOW_CHASE -> {
                    eyeX = t.posX - fwdX * 5.6f + rightX * 1.15f
                    eyeY = t.posY + 1.15f
                    eyeZ = t.posZ - fwdZ * 5.6f + rightZ * 1.15f
                    targetX = t.posX + fwdX * 12f
                    targetY = t.posY + 0.95f
                    targetZ = t.posZ + fwdZ * 12f
                }
                ReplayCameraAngle.TRACKSIDE -> {
                    // Anchored roadside television broadcast camera panning smoothly with the car!
                    val anchorDist = ((t.distanceAlongRoadM / 38f).toInt() * 38f + 16f)
                    val s = RoadSplineNetwork.sampleAt(anchorDist)
                    eyeX = s.x + s.rightX * (s.widthM * 0.5f + 4.5f)
                    eyeY = s.y + 2.6f
                    eyeZ = s.z + s.rightZ * (s.widthM * 0.5f + 4.5f)
                    targetX = t.posX
                    targetY = t.posY + 1.05f
                    targetZ = t.posZ
                }
                ReplayCameraAngle.COCKPIT -> {
                    val seatSign = if (snap.seatSide == DriverSeatSide.LEFT_HAND) -1f else 1f
                    eyeX = t.posX + fwdX * 0.16f + rightX * (seatSign * 0.38f)
                    eyeY = t.posY + 1.40f
                    eyeZ = t.posZ + fwdZ * 0.16f + rightZ * (seatSign * 0.38f)
                    targetX = eyeX + fwdX * 26f
                    targetY = eyeY - 0.38f
                    targetZ = eyeZ + fwdZ * 26f
                }
            }
            camInitialized = false
        } else {
            when (snap.cameraMode) {
                CameraMode.CHASE_NEAR -> {
                    val speedBack = (t.speedKmh / 160f).coerceIn(0f, 1f) * 0.9f
                    val desiredX = t.posX - fwdX * (6.9f + speedBack) - rightX * (t.driftSlipAngleDeg * 0.035f)
                    val desiredY = t.posY + 2.65f
                    val desiredZ = t.posZ - fwdZ * (6.9f + speedBack) - rightZ * (t.driftSlipAngleDeg * 0.035f)
                    if (!camInitialized) {
                        smoothCamX = desiredX; smoothCamY = desiredY; smoothCamZ = desiredZ; camInitialized = true
                    } else {
                        val k = (dt * 11f).coerceAtMost(1f)
                        smoothCamX += (desiredX - smoothCamX) * k
                        smoothCamY += (desiredY - smoothCamY) * k
                        smoothCamZ += (desiredZ - smoothCamZ) * k
                    }
                    eyeX = smoothCamX; eyeY = smoothCamY; eyeZ = smoothCamZ
                    targetX = t.posX + fwdX * 16f + rightX * steerApexLook
                    targetY = t.posY + 1.25f
                    targetZ = t.posZ + fwdZ * 16f + rightZ * steerApexLook
                    // Subtle dynamic cornering camera horizon tilt
                    val bankTilt = (t.rollDeg * 0.0045f).coerceIn(-0.045f, 0.045f)
                    upX = rightX * bankTilt; upZ = rightZ * bankTilt
                }
                CameraMode.CHASE_FAR -> {
                    eyeX = t.posX - fwdX * 10.6f
                    eyeY = t.posY + 4.1f
                    eyeZ = t.posZ - fwdZ * 10.6f
                    targetX = t.posX + fwdX * 20f + rightX * steerApexLook
                    targetY = t.posY + 1.35f
                    targetZ = t.posZ + fwdZ * 20f + rightZ * steerApexLook
                    camInitialized = false
                }
                CameraMode.COCKPIT -> {
                    // Authentic Left-Hand Drive vs Right-Hand Drive driver eye position with G-force shift!
                    val seatSign = if (snap.seatSide == DriverSeatSide.LEFT_HAND) -1f else 1f
                    val gLongShift = (-t.throttle * 0.055f + t.brake * 0.065f).coerceIn(-0.07f, 0.08f)
                    eyeX = t.posX + fwdX * (0.16f + gLongShift) + rightX * (seatSign * 0.38f)
                    eyeY = t.posY + 1.40f + t.suspensionBounceM * 0.72f
                    eyeZ = t.posZ + fwdZ * (0.16f + gLongShift) + rightZ * (seatSign * 0.38f)
                    targetX = eyeX + fwdX * 28f + rightX * (steerApexLook * 1.45f)
                    targetY = eyeY - 0.40f + t.pitchDeg * 0.032f
                    targetZ = eyeZ + fwdZ * 28f + rightZ * (steerApexLook * 1.45f)
                    val cockpitRoll = (t.rollDeg * 0.0085f).coerceIn(-0.075f, 0.075f)
                    upX = rightX * cockpitRoll; upZ = rightZ * cockpitRoll
                    camInitialized = false
                }
                CameraMode.HOOD -> {
                    // Sculpted Hood-Mounted Sports Camera showing the metallic hood & dynamic pitch/roll!
                    eyeX = t.posX + fwdX * 0.62f
                    eyeY = t.posY + 1.42f + t.suspensionBounceM * 0.85f
                    eyeZ = t.posZ + fwdZ * 0.62f
                    targetX = eyeX + fwdX * 26f + rightX * (steerApexLook * 1.1f)
                    targetY = eyeY - 0.62f + t.pitchDeg * 0.035f
                    targetZ = eyeZ + fwdZ * 26f + rightZ * (steerApexLook * 1.1f)
                    val hoodRoll = (t.rollDeg * 0.006f).coerceIn(-0.055f, 0.055f)
                    upX = rightX * hoodRoll; upZ = rightZ * hoodRoll
                    camInitialized = false
                }
                CameraMode.CINEMATIC_ORBIT -> {
                    // Smooth 360° Touch-Draggable & Auto-Sweeping Cinematic Third-Person Orbit View!
                    val speedPull = (t.speedKmh / 160f).coerceIn(0f, 1f) * 1.45f
                    val radius = 7.25f + speedPull
                    val pitch = snap.orbitPitchRad.coerceIn(0.10f, 0.72f)
                    val horizR = radius * cos(pitch)
                    // Offset by PI so orbitYawRad = 0 sits cleanly behind the car
                    val worldAng = t.headingRad + PI.toFloat() + snap.orbitYawRad
                    val desiredX = t.posX + sin(worldAng) * horizR
                    val desiredY = t.posY + 1.18f + radius * sin(pitch)
                    val desiredZ = t.posZ + cos(worldAng) * horizR
                    if (!camInitialized) {
                        smoothCamX = desiredX; smoothCamY = desiredY; smoothCamZ = desiredZ; camInitialized = true
                    } else {
                        val k = (dt * 9.5f).coerceAtMost(1f)
                        smoothCamX += (desiredX - smoothCamX) * k
                        smoothCamY += (desiredY - smoothCamY) * k
                        smoothCamZ += (desiredZ - smoothCamZ) * k
                    }
                    eyeX = smoothCamX; eyeY = smoothCamY; eyeZ = smoothCamZ
                    targetX = t.posX + fwdX * 1.6f
                    targetY = t.posY + 1.10f
                    targetZ = t.posZ + fwdZ * 1.6f
                    val orbitTilt = (t.rollDeg * 0.0035f).coerceIn(-0.035f, 0.035f)
                    upX = rightX * orbitTilt; upZ = rightZ * orbitTilt
                }
                CameraMode.CINEMATIC_FRONT -> {
                    eyeX = t.posX + fwdX * 7.8f + rightX * 2.2f
                    eyeY = t.posY + 2.0f
                    eyeZ = t.posZ + fwdZ * 7.8f + rightZ * 2.2f
                    targetX = t.posX
                    targetY = t.posY + 1.05f
                    targetZ = t.posZ
                    camInitialized = false
                }
                CameraMode.FREE_PHOTO -> {
                    val r = 8.0f
                    val pitch = snap.orbitPitchRad.coerceIn(0.08f, 0.75f)
                    val horizR = r * cos(pitch)
                    val ang = t.headingRad + snap.showroomOrbitRad
                    eyeX = t.posX + sin(ang) * horizR
                    eyeY = t.posY + 1.15f + r * sin(pitch)
                    eyeZ = t.posZ + cos(ang) * horizR
                    targetX = t.posX
                    targetY = t.posY + 1.12f
                    targetZ = t.posZ
                    camInitialized = false
                }
            }
        }

        currentCamX = eyeX
        currentCamY = eyeY
        currentCamZ = eyeZ
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX, upY, upZ)
    }

    private fun rebuildStreamingCorridor(centerDistM: Float) {
        val startM = (centerDistM - 150f).coerceAtLeast(0f)
        val endM = (centerDistM + 340f).coerceAtMost(RoadSplineNetwork.TOTAL_ROAD_LENGTH_M)
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

            val l0x = s0.x - s0.rightX * hw0; val l0z = s0.z - s0.rightZ * hw0
            val r0x = s0.x + s0.rightX * hw0; val r0z = s0.z + s0.rightZ * hw0
            val l1x = s1.x - s1.rightX * hw1; val l1z = s1.z - s1.rightZ * hw1
            val r1x = s1.x + s1.rightX * hw1; val r1z = s1.z + s1.rightZ * hw1

            pushTri(roadVerts, l0x, s0.y + 0.03f, l0z, 0f, v0, r0x, s0.y + 0.03f, r0z, 1f, v0, r1x, s1.y + 0.03f, r1z, 1f, v1, 0f, 1f, 0f)
            pushTri(roadVerts, l0x, s0.y + 0.03f, l0z, 0f, v0, r1x, s1.y + 0.03f, r1z, 1f, v1, l1x, s1.y + 0.03f, l1z, 0f, v1, 0f, 1f, 0f)

            val swLift0 = if (s0.isUrbanStreet) 0.19f else 0.02f
            val swLift1 = if (s1.isUrbanStreet) 0.19f else 0.02f
            val swW = 3.6f
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

            val offsets = floatArrayOf(hw0 + swW, hw0 + 18f, hw0 + 46f, hw0 + 88f)
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

    private fun drawCorridorGeometry(regionId: RegionId, surface: SurfaceType, isModernBoulevard: Boolean, wetness: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform3f(shaders.uEmissive, 0f, 0f, 0f)

        val (rr, rg, rb, useTex) = when (surface) {
            SurfaceType.ASPHALT -> Quad4(0.96f, 0.96f, 0.99f, 1f)
            SurfaceType.DIRT -> Quad4(0.62f, 0.48f, 0.34f, 0.32f)
            SurfaceType.GRAVEL -> Quad4(0.56f, 0.54f, 0.50f, 0.38f)
            SurfaceType.SAND -> Quad4(0.86f, 0.72f, 0.44f, 0.24f)
            SurfaceType.MUD -> Quad4(0.34f, 0.24f, 0.17f, 0.24f)
            SurfaceType.ROCK -> Quad4(0.48f, 0.45f, 0.42f, 0.34f)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (isModernBoulevard) textures.modernBoulevardTex else textures.roadAsphaltTex)
        GLES30.glUniform1f(shaders.uUseTexture, useTex)
        GLES30.glUniform2f(shaders.uUvScale, 1f, 1f)
        GLES30.glUniform1f(shaders.uMetallic, 0.08f)
        GLES30.glUniform1f(shaders.uRoughness, if (surface == SurfaceType.ASPHALT) 0.52f else 0.88f)
        GLES30.glUniform1f(shaders.uWetness, wetness)
        GLES30.glUniform4f(shaders.uBaseColor, rr, rg, rb, 1f)
        roadDynamicMesh.draw()

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.sidewalkStoneTex)
        GLES30.glUniform1f(shaders.uUseTexture, 1f)
        GLES30.glUniform2f(shaders.uUvScale, 1f, 1.5f)
        GLES30.glUniform1f(shaders.uMetallic, 0.04f)
        GLES30.glUniform1f(shaders.uRoughness, 0.78f)
        GLES30.glUniform1f(shaders.uWetness, wetness * 0.5f)
        GLES30.glUniform4f(shaders.uBaseColor, 0.94f, 0.92f, 0.88f, 1f)
        sidewalkDynamicMesh.draw()

        val (tr, tg, tb) = when (regionId) {
            RegionId.TAIZ_HIGHLANDS -> Triple(0.46f, 0.42f, 0.28f)
            RegionId.IBB_GREEN_PEAKS, RegionId.WADI_RAYHAN -> Triple(0.24f, 0.47f, 0.23f)
            RegionId.NAQIL_SAHAB, RegionId.SANAA_HIGHLANDS -> Triple(0.50f, 0.42f, 0.34f)
            RegionId.MASALIK_DESERT -> Triple(0.84f, 0.68f, 0.41f)
            RegionId.SHIBAM_HADRAMOUT -> Triple(0.76f, 0.60f, 0.38f)
            RegionId.TIHAMA_PLAINS -> Triple(0.72f, 0.62f, 0.38f)
            RegionId.COASTAL_ROAD -> Triple(0.76f, 0.68f, 0.46f)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.whiteTex)
        GLES30.glUniform1f(shaders.uUseTexture, 0f)
        GLES30.glUniform1f(shaders.uMetallic, 0f)
        GLES30.glUniform1f(shaders.uRoughness, 0.94f)
        GLES30.glUniform1f(shaders.uWetness, 0f)
        GLES30.glUniform4f(shaders.uBaseColor, tr, tg, tb, 1f)
        terrainDynamicMesh.draw()
    }

    private fun drawYemeniCityEnvironment(
        centerDistM: Float,
        snap: RenderFrameSnapshot,
        atmo: AtmosphereSnapshot
    ) {
        val minD = ((centerDistM - 110f) / 22f).toInt().coerceAtLeast(1) * 22
        val maxD = ((centerDistM + 250f) / 22f).toInt() * 22
        val nightGlow = atmo.nightFactor

        var dInt = minD
        while (dInt <= maxD) {
            val d = dInt.toFloat()
            if (d >= RoadSplineNetwork.TOTAL_ROAD_LENGTH_M - 20f) break
            val sample = RoadSplineNetwork.sampleAt(d)
            val hash = ((dInt * 2654435761L) ushr 16).toInt() and 0x7FFFFFFF

            if (sample.hasTunnel) {
                drawOrientedBox(sample.x, sample.y + 6.8f, sample.z, sample.headingRad, sample.widthM + 3.5f, 1.8f, 22.5f, 0.30f, 0.28f, 0.26f)
                for (side in intArrayOf(-1, 1)) {
                    val lat = side * (sample.widthM * 0.5f + 1.2f)
                    drawOrientedBox(sample.x + sample.rightX * lat, sample.y + 3.4f, sample.z + sample.rightZ * lat, sample.headingRad, 1.8f, 6.8f, 22.5f, 0.34f, 0.31f, 0.28f)
                }
                // Warm overhead tunnel LED ceiling strip
                drawOrientedBox(sample.x, sample.y + 5.85f, sample.z, sample.headingRad, 0.9f, 0.12f, 5.5f, 1f, 0.92f, 0.72f, emR = 1.2f, emG = 0.95f, emB = 0.55f)
            } else if (sample.isUrbanStreet) {
                val isModern = sample.isModernBoulevard
                for (side in intArrayOf(-1, 1)) {
                    val bHash = hash + (if (side > 0) 37 else 11)
                    val buildingW = if (isModern) 10.5f + (bHash % 5) else 8.5f + (bHash % 5)
                    val buildingD = 12.0f + (bHash % 6)
                    val floors = when {
                        isModern -> 5 + (bHash % 5)                  // Sleek 5-9 story modern Yemeni commercial towers
                        sample.regionId == RegionId.SHIBAM_HADRAMOUT -> 5 + (bHash % 4)
                        sample.regionId == RegionId.SANAA_HIGHLANDS -> 4 + (bHash % 3)
                        else -> 3 + (bHash % 3)
                    }
                    val buildingH = floors * 3.5f
                    val lat = side * (sample.widthM * 0.5f + 3.9f + buildingW * 0.52f)
                    val bx = sample.x + sample.rightX * lat
                    val bz = sample.z + sample.rightZ * lat
                    val by = sample.y + buildingH * 0.5f

                    val isCoastal = sample.regionId == RegionId.COASTAL_ROAD || sample.regionId == RegionId.TIHAMA_PLAINS
                    val isMosqueLandmark = (dInt % 264 == 0) && (side == -1)
                    val facadeTex = when {
                        isMosqueLandmark -> textures.heritageMosqueTex
                        isCoastal && isModern -> textures.adenCoastalTowerTex
                        isModern && (bHash % 3 != 0) -> textures.modernTowerTex
                        sample.regionId == RegionId.SANAA_HIGHLANDS -> textures.sanaaFacadeTex
                        sample.regionId == RegionId.SHIBAM_HADRAMOUT -> textures.shibamMudTex
                        else -> if (bHash % 3 == 0) textures.sanaaFacadeTex else textures.taizStoneTex
                    }
                    val isGlassTower = facadeTex == textures.modernTowerTex || facadeTex == textures.adenCoastalTowerTex
                    val winGlow = nightGlow * (if (isGlassTower) 0.42f else if (isMosqueLandmark) 0.38f else 0.28f)
                    val tintR = if (isGlassTower) 0.98f else if (bHash % 4 == 0) 0.99f else 0.94f
                    val tintG = if (isGlassTower) 0.98f else if (bHash % 4 == 1) 0.95f else 0.92f
                    val tintB = if (isGlassTower) 1.00f else if (bHash % 4 == 2) 0.90f else 0.88f

                    drawOrientedBox(
                        bx, by, bz, sample.headingRad,
                        buildingW, buildingH, buildingD,
                        tintR, tintG, tintB,
                        texId = facadeTex,
                        uvScaleX = 1.2f, uvScaleY = floors * 0.52f,
                        emR = winGlow * 0.95f, emG = winGlow * 0.80f, emB = winGlow * (if (isGlassTower) 0.62f else 0.34f),
                        metallic = if (isGlassTower) 0.62f else 0.06f,
                        roughness = if (isGlassTower) 0.16f else 0.74f
                    )

                    if (isMosqueLandmark) {
                        // Iconic Yemeni Mosque White Dome + Tall Illuminated Minaret with Golden Finial!
                        val domeBaseY = sample.y + buildingH
                        drawOrientedBox(bx, domeBaseY + 1.2f, bz, sample.headingRad, buildingW * 0.68f, 2.4f, buildingD * 0.68f, 0.96f, 0.95f, 0.91f, emR = nightGlow * 0.22f, emG = nightGlow * 0.20f, emB = nightGlow * 0.14f)
                        drawCone(bx, domeBaseY + 3.8f, bz, buildingW * 0.58f, 3.6f, 0.97f, 0.96f, 0.92f)
                        // Slender Yemeni Minaret Tower on corner
                        val minX = bx + sample.rightX * (side * -buildingW * 0.35f) + sin(sample.headingRad) * (buildingD * 0.35f)
                        val minZ = bz + sample.rightZ * (side * -buildingW * 0.35f) + cos(sample.headingRad) * (buildingD * 0.35f)
                        drawOrientedBox(minX, sample.y + buildingH + 5.5f, minZ, sample.headingRad, 2.1f, 11.5f, 2.1f, 0.95f, 0.93f, 0.88f, texId = textures.heritageMosqueTex, uvScaleX = 0.5f, uvScaleY = 2.0f, emR = nightGlow * 0.30f, emG = nightGlow * 0.26f, emB = nightGlow * 0.15f)
                        drawOrientedBox(minX, sample.y + buildingH + 10.6f, minZ, sample.headingRad, 2.75f, 0.55f, 2.75f, 0.12f, 0.62f, 0.44f, emR = nightGlow * 0.14f, emG = nightGlow * 0.65f, emB = nightGlow * 0.42f)
                        drawCone(minX, sample.y + buildingH + 13.0f, minZ, 1.9f, 3.6f, 0.98f, 0.97f, 0.93f)
                    } else {
                        // Rooftop architectural crown / modern stepped penthouse & LED cornice OR Sana'a parapet
                        drawOrientedBox(
                            bx, sample.y + buildingH + 0.55f, bz, sample.headingRad,
                            buildingW + 0.5f, 0.95f, buildingD + 0.5f,
                            if (isGlassTower) 0.16f else 0.95f,
                            if (isGlassTower) 0.21f else 0.93f,
                            if (isGlassTower) 0.28f else 0.88f,
                            emR = if (isGlassTower) nightGlow * 0.50f else 0f,
                            emG = if (isGlassTower) nightGlow * 0.36f else 0f,
                            emB = if (isGlassTower) nightGlow * (if (isCoastal) 0.48f else 0.12f) else 0f
                        )
                        // Stepped upper architectural tower crown for skyline variety
                        if (isGlassTower && bHash % 2 == 0) {
                            drawOrientedBox(
                                bx, sample.y + buildingH + 2.4f, bz, sample.headingRad,
                                buildingW * 0.62f, 3.2f, buildingD * 0.62f,
                                0.94f, 0.96f, 1.0f,
                                texId = facadeTex, uvScaleX = 0.8f, uvScaleY = 0.5f,
                                emR = winGlow * 0.9f, emG = winGlow * 0.8f, emB = winGlow * 0.65f,
                                metallic = 0.68f, roughness = 0.14f
                            )
                        } else if (!isGlassTower && bHash % 2 == 1) {
                            // Authentic Carved Wooden Yemeni Mashrabiya Balcony projecting toward street
                            val balLat = side * (sample.widthM * 0.5f + 4.05f)
                            drawOrientedBox(
                                sample.x + sample.rightX * balLat, sample.y + buildingH * 0.58f, sample.z + sample.rightZ * balLat,
                                sample.headingRad, 1.15f, 2.2f, buildingD * 0.42f,
                                0.42f, 0.25f, 0.14f, metallic = 0.08f, roughness = 0.72f
                            )
                        }
                    }

                    // Ground-level Modern Mall/Café Storefront OR Authentic Striped Souk Awning + Colorful Neon Sign Accent
                    if (bHash % 2 == 0 && !isMosqueLandmark) {
                        val shopLat = side * (sample.widthM * 0.5f + 3.65f)
                        drawOrientedBox(
                            sample.x + sample.rightX * shopLat, sample.y + 1.95f, sample.z + sample.rightZ * shopLat,
                            sample.headingRad, 1.4f, 3.5f, buildingD * 0.86f,
                            1f, 1f, 1f,
                            texId = if (isModern) textures.modernMallTex else textures.soukShopTex,
                            uvScaleX = 1f, uvScaleY = 1f,
                            emR = nightGlow * 0.48f, emG = nightGlow * 0.38f, emB = nightGlow * 0.18f,
                            metallic = if (isModern) 0.38f else 0.05f,
                            roughness = if (isModern) 0.22f else 0.72f
                        )
                        if (isModern) {
                            val (nr, ng, nb) = when (bHash % 4) {
                                0 -> Triple(0.98f, 0.68f, 0.12f)
                                1 -> Triple(0.08f, 0.82f, 0.68f)
                                2 -> Triple(0.22f, 0.64f, 0.98f)
                                else -> Triple(0.94f, 0.26f, 0.32f)
                            }
                            drawOrientedBox(
                                sample.x + sample.rightX * (shopLat - side * 0.72f), sample.y + 3.65f, sample.z + sample.rightZ * (shopLat - side * 0.72f),
                                sample.headingRad, 0.14f, 0.42f, buildingD * 0.76f,
                                nr, ng, nb, emR = nr * (0.35f + nightGlow * 0.85f), emG = ng * (0.35f + nightGlow * 0.85f), emB = nb * (0.35f + nightGlow * 0.85f)
                            )
                        }
                    }

                    // Sleek Curved-Arm Modern Boulevard Streetlamps on BOTH sides every 44m
                    if (dInt % 44 == 0 && (isModern || side == 1)) {
                        val poleLat = side * (sample.widthM * 0.5f + 0.85f)
                        val px = sample.x + sample.rightX * poleLat
                        val pz = sample.z + sample.rightZ * poleLat
                        drawOrientedBox(px, sample.y + 3.6f, pz, sample.headingRad, 0.18f, 7.2f, 0.18f, 0.26f, 0.30f, 0.35f, metallic = 0.55f, roughness = 0.32f)
                        drawOrientedBox(
                            px - sample.rightX * (side * 0.95f), sample.y + 7.15f, pz - sample.rightZ * (side * 0.95f),
                            sample.headingRad, 1.9f, 0.14f, 0.22f, 0.32f, 0.36f, 0.42f, metallic = 0.55f, roughness = 0.32f
                        )
                        val lampEm = 0.25f + nightGlow * 1.45f
                        drawOrientedBox(
                            px - sample.rightX * (side * 1.65f), sample.y + 6.95f, pz - sample.rightZ * (side * 1.65f),
                            sample.headingRad, 0.55f, 0.20f, 0.48f,
                            if (isModern) 0.95f else 1.0f, if (isModern) 0.96f else 0.88f, if (isModern) 1.0f else 0.58f,
                            emR = lampEm, emG = lampEm * (if (isModern) 0.92f else 0.78f), emB = lampEm * (if (isModern) 0.78f else 0.38f)
                        )
                    }

                    // Multi-Frond Ornamental Boulevard Palm Trees + Stone Planter Box lining Modern Yemeni Sidewalks
                    if (isModern && dInt % 44 == 22) {
                        val palmLat = side * (sample.widthM * 0.5f + 2.25f)
                        val px = sample.x + sample.rightX * palmLat
                        val pz = sample.z + sample.rightZ * palmLat
                        // Decorative curb planter + green hedge
                        drawOrientedBox(px, sample.y + 0.32f, pz, sample.headingRad, 1.25f, 0.48f, 1.25f, 0.76f, 0.72f, 0.65f)
                        drawOrientedBox(px, sample.y + 2.65f, pz, 0f, 0.34f, 5.1f, 0.34f, 0.46f, 0.33f, 0.20f)
                        // Lush layered palm fronds radiating in 4 directions
                        drawCone(px, sample.y + 5.55f, pz, 3.6f, 1.65f, 0.14f, 0.54f, 0.24f)
                        drawOrientedBox(px, sample.y + 5.25f, pz, sample.headingRad + 0.35f, 4.4f, 0.24f, 0.85f, 0.16f, 0.58f, 0.26f)
                        drawOrientedBox(px, sample.y + 5.25f, pz, sample.headingRad + 1.92f, 4.4f, 0.24f, 0.85f, 0.12f, 0.50f, 0.22f)
                    }
                }

                // Overhead Modern Green Highway Directional Gantry every 330m in modern boulevards
                if (isModern && dInt % 330 == 110) {
                    val hw = sample.widthM * 0.5f + 1.1f
                    for (side in intArrayOf(-1, 1)) {
                        drawOrientedBox(
                            sample.x + sample.rightX * (side * hw), sample.y + 3.6f, sample.z + sample.rightZ * (side * hw),
                            sample.headingRad, 0.34f, 7.2f, 0.34f, 0.62f, 0.66f, 0.70f, metallic = 0.6f, roughness = 0.3f
                        )
                    }
                    drawOrientedBox(
                        sample.x, sample.y + 6.5f, sample.z, sample.headingRad,
                        sample.widthM + 2.6f, 1.35f, 0.32f,
                        0.06f, 0.44f, 0.26f,
                        emR = 0.04f, emG = 0.24f + nightGlow * 0.22f, emB = 0.14f + nightGlow * 0.12f
                    )
                }
            } else {
                // Mountain / Wadi / Desert Scenery + Reflective Curve Chevron Warning Signs on sharp bends!
                val side = if (hash % 2 == 0) 1 else -1
                val lat = side * (sample.widthM * 0.5f + 5.5f + (hash % 7))
                val tx = sample.x + sample.rightX * lat
                val tz = sample.z + sample.rightZ * lat
                val ty = RoadSplineNetwork.terrainHeightAt(tx, tz, sample, lat)

                if (sample.regionId in listOf(RegionId.MASALIK_DESERT, RegionId.SHIBAM_HADRAMOUT, RegionId.TIHAMA_PLAINS, RegionId.COASTAL_ROAD)) {
                    drawOrientedBox(tx, ty + 3.1f, tz, 0f, 0.42f, 6.2f, 0.42f, 0.48f, 0.34f, 0.22f)
                    drawCone(tx, ty + 6.4f, tz, 4.6f, 2.2f, 0.18f, 0.48f, 0.20f)
                } else {
                    drawOrientedBox(tx, ty + 1.5f, tz, 0f, 0.38f, 3.0f, 0.38f, 0.40f, 0.28f, 0.18f)
                    drawCone(tx, ty + 4.2f, tz, 3.4f, 4.6f, 0.15f, 0.40f, 0.18f)
                }

                // Reflective Curve Chevron Signs (>>>) on outside of sharp mountain switchbacks!
                if (abs(sample.curvatureRadPerM) > 0.0046f) {
                    val outerSide = if (sample.curvatureRadPerM > 0f) -1 else 1
                    val cLat = outerSide * (sample.widthM * 0.5f + 1.05f)
                    val cx = sample.x + sample.rightX * cLat
                    val cz = sample.z + sample.rightZ * cLat
                    drawOrientedBox(cx, sample.y + 0.8f, cz, sample.headingRad, 0.10f, 1.6f, 0.10f, 0.55f, 0.58f, 0.62f)
                    drawOrientedBox(
                        cx, sample.y + 1.65f, cz, sample.headingRad,
                        0.95f, 0.85f, 0.10f, 1f, 0.94f, 0.80f,
                        texId = textures.curveChevronTex,
                        emR = 0.28f + nightGlow * 0.42f, emG = 0.22f + nightGlow * 0.32f, emB = 0.02f
                    )
                }

                // Steel guardrail posts with amber night reflectors
                for (gSide in intArrayOf(-1, 1)) {
                    val gLat = gSide * (sample.widthM * 0.5f + 0.60f)
                    drawOrientedBox(
                        sample.x + sample.rightX * gLat, sample.y + 0.46f, sample.z + sample.rightZ * gLat,
                        sample.headingRad, 0.24f, 0.86f, 1.4f, 0.86f, 0.84f, 0.80f,
                        emR = nightGlow * 0.25f, emG = nightGlow * 0.15f
                    )
                }
            }

            if (sample.regionId == RegionId.COASTAL_ROAD) {
                val seaLat = sample.widthM * 0.5f + 46f
                drawOrientedBox(
                    sample.x + sample.rightX * seaLat, sample.y - 2.6f, sample.z + sample.rightZ * seaLat,
                    sample.headingRad, 80f, 0.4f, 24f,
                    0.06f, 0.38f, 0.64f, metallic = 0.72f, roughness = 0.12f
                )
            }
            dInt += 22
        }

        // Iconic Bab al-Yemen Archway Landmark in Old Sana'a (15450m)
        if (abs(15450f - centerDistM) < 230f) {
            val s = RoadSplineNetwork.sampleAt(15450f)
            for (side in intArrayOf(-1, 1)) {
                val lat = side * (s.widthM * 0.5f + 3.2f)
                drawOrientedBox(
                    s.x + s.rightX * lat, s.y + 6.5f, s.z + s.rightZ * lat,
                    s.headingRad, 4.8f, 13f, 5.2f, 0.90f, 0.76f, 0.60f,
                    texId = textures.sanaaFacadeTex, uvScaleX = 1f, uvScaleY = 2f,
                    emR = nightGlow * 0.24f, emG = nightGlow * 0.16f, emB = nightGlow * 0.05f
                )
            }
            drawOrientedBox(
                s.x, s.y + 9.2f, s.z, s.headingRad,
                s.widthM + 6.8f, 3.4f, 4.4f, 0.92f, 0.82f, 0.68f,
                texId = textures.sanaaFacadeTex, uvScaleX = 2f, uvScaleY = 0.7f,
                emR = nightGlow * 0.24f, emG = nightGlow * 0.16f, emB = nightGlow * 0.05f
            )
        }

        // Service Stations (Fuel Canopies, Workshops & Car Washes)
        for (poi in RoadSplineNetwork.WORLD_POIS) {
            if (abs(poi.distanceM - centerDistM) > 220f) continue
            if (poi.type !in listOf("fuel", "workshop", "car_wash")) continue
            val s = RoadSplineNetwork.sampleAt(poi.distanceM)
            val px = s.x + s.rightX * poi.lateralOffsetM
            val pz = s.z + s.rightZ * poi.lateralOffsetM
            val (ar, ag, ab) = when (poi.type) {
                "fuel" -> Triple(0.95f, 0.60f, 0.10f)
                "workshop" -> Triple(0.18f, 0.56f, 0.88f)
                else -> Triple(0.10f, 0.72f, 0.52f)
            }
            drawOrientedBox(px, s.y + 4.6f, pz, s.headingRad, 6.2f, 0.7f, 9.5f, ar, ag, ab, emR = ar * 0.45f, emG = ag * 0.45f, emB = ab * 0.45f)
            drawOrientedBox(px, s.y + 2.2f, pz + 3.2f, s.headingRad, 0.5f, 4.4f, 0.5f, 0.85f, 0.85f, 0.88f)
            drawOrientedBox(px, s.y + 2.2f, pz - 3.2f, s.headingRad, 0.5f, 4.4f, 0.5f, 0.85f, 0.85f, 0.88f)
        }

        // Live Traffic Signal Gantries & Zebra Crosswalks
        for (sig in snap.livingWorld.trafficSignals) {
            if (abs(sig.def.distanceM - centerDistM) > 200f) continue
            val s = RoadSplineNetwork.sampleAt(sig.def.distanceM)

            val cwSample = RoadSplineNetwork.sampleAt(sig.def.distanceM + 2.5f)
            drawOrientedQuad(cwSample.x, cwSample.y + 0.06f, cwSample.z, cwSample.headingRad, cwSample.widthM - 0.6f, 3.8f, textures.crosswalkTex)

            val poleLat = s.widthM * 0.5f + 0.9f
            val px = s.x + s.rightX * poleLat
            val pz = s.z + s.rightZ * poleLat
            drawOrientedBox(px, s.y + 3.2f, pz, s.headingRad, 0.22f, 6.4f, 0.22f, 0.22f, 0.24f, 0.28f, metallic = 0.5f, roughness = 0.35f)
            drawOrientedBox(px - s.rightX * 2.4f, s.y + 6.1f, pz - s.rightZ * 2.4f, s.headingRad, 4.9f, 0.22f, 0.22f, 0.22f, 0.24f, 0.28f, metallic = 0.5f, roughness = 0.35f)

            val bx = px - s.rightX * 3.6f
            val bz = pz - s.rightZ * 3.6f
            drawOrientedBox(bx, s.y + 5.7f, bz, s.headingRad, 0.65f, 1.75f, 0.55f, 0.10f, 0.11f, 0.14f)

            val redOn = sig.phase == TrafficSignalPhase.RED
            val yelOn = sig.phase == TrafficSignalPhase.YELLOW
            val grnOn = sig.phase == TrafficSignalPhase.GREEN
            drawOrientedBox(bx, s.y + 6.22f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, if (redOn) 1f else 0.22f, 0.06f, 0.06f, emR = if (redOn) 1.8f else 0f)
            drawOrientedBox(bx, s.y + 5.72f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, if (yelOn) 1f else 0.22f, if (yelOn) 0.85f else 0.18f, 0.05f, emR = if (yelOn) 1.5f else 0f, emG = if (yelOn) 1.15f else 0f)
            drawOrientedBox(bx, s.y + 5.22f, bz, s.headingRad, 0.42f, 0.38f, 0.62f, 0.06f, if (grnOn) 1f else 0.20f, 0.20f, emG = if (grnOn) 1.8f else 0f)
        }

        // Mission Finish Archway Banner
        val finishM = snap.finishDistanceM
        if (finishM != null && abs(finishM - centerDistM) < 230f) {
            val s = RoadSplineNetwork.sampleAt(finishM)
            for (side in intArrayOf(-1, 1)) {
                val lat = side * (s.widthM * 0.5f + 0.8f)
                drawOrientedBox(s.x + s.rightX * lat, s.y + 3.5f, s.z + s.rightZ * lat, s.headingRad, 0.5f, 7f, 0.5f, 0.96f, 0.62f, 0.06f, emR = 0.55f, emG = 0.32f)
            }
            drawOrientedBox(s.x, s.y + 6.6f, s.z, s.headingRad, s.widthM + 2.2f, 1.1f, 0.7f, 0.96f, 0.62f, 0.06f, emR = 0.65f, emG = 0.40f)
        }
    }

    /** Iconic Yemeni Roundabouts (جولة القصر، العدين، المصباحي، كالتكس) with Central Monuments & Branching 4-Way Cross-Streets! */
    private fun drawRoundaboutsAndCrossStreets(centerDistM: Float, atmo: AtmosphereSnapshot) {
        val nightGlow = atmo.nightFactor
        for (rb in RoadSplineNetwork.ROUNDABOUTS) {
            if (abs(rb.distanceM - centerDistM) > 220f) continue
            val s = RoadSplineNetwork.sampleAt(rb.distanceM)
            val crossHeading = s.headingRad + (PI.toFloat() * 0.5f)

            // Branching 4-Way Perpendicular Asphalt Cross-Street extending ±48m left & right!
            drawOrientedQuad(s.x, s.y + 0.045f, s.z, crossHeading, 11.4f, 96f, textures.modernBoulevardTex)
            // Flanking sidewalks along the branching cross-street
            for (side in intArrayOf(-1, 1)) {
                val off = side * 7.4f
                val fwdX = sin(s.headingRad); val fwdZ = cos(s.headingRad)
                drawOrientedBox(
                    s.x + fwdX * off, s.y + 0.14f, s.z + fwdZ * off,
                    crossHeading, 2.8f, 0.24f, 82f, 0.88f, 0.86f, 0.82f, texId = textures.sidewalkStoneTex, uvScaleX = 1f, uvScaleY = 12f
                )
            }
            // Crosswalks at the 4 mouths of the roundabout
            for (dir in intArrayOf(-1, 1)) {
                drawOrientedQuad(s.x + s.rightX * (dir * 13.5f), s.y + 0.065f, s.z + s.rightZ * (dir * 13.5f), crossHeading, 10.6f, 3.2f, textures.crosswalkTex)
            }

            // Central Landscaped Roundabout Stone Island & Glowing Fountain/Monument
            drawOrientedBox(s.x, s.y + 0.36f, s.z, s.headingRad, 6.2f, 0.62f, 6.2f, 0.82f, 0.78f, 0.72f, texId = textures.sidewalkStoneTex)
            drawOrientedBox(s.x, s.y + 0.68f, s.z, s.headingRad, 5.2f, 0.18f, 5.2f, 0.14f, 0.48f, 0.22f) // Green turf garden bed

            // Illuminated fountain/arch monument centerpiece
            val glow = 0.25f + nightGlow * 1.1f
            when (rb.monumentStyle) {
                0 -> { // Illuminated Yemeni Arch & Water Fountain
                    drawOrientedBox(s.x, s.y + 0.95f, s.z, s.headingRad, 3.2f, 0.45f, 3.2f, 0.12f, 0.62f, 0.88f, emR = 0.05f, emG = glow * 0.45f, emB = glow * 0.85f, metallic = 0.7f, roughness = 0.12f)
                    drawOrientedBox(s.x, s.y + 3.2f, s.z, s.headingRad, 0.85f, 4.4f, 0.85f, 0.92f, 0.84f, 0.68f, texId = textures.sanaaFacadeTex)
                    drawOrientedBox(s.x, s.y + 5.6f, s.z, s.headingRad, 1.45f, 0.55f, 1.45f, 0.96f, 0.72f, 0.18f, emR = glow, emG = glow * 0.72f, emB = glow * 0.15f)
                }
                1 -> { // Modern Palm Plaza & Illuminated Obelisk
                    drawCone(s.x, s.y + 3.6f, s.z, 2.2f, 5.8f, 0.86f, 0.88f, 0.92f)
                    drawOrientedBox(s.x, s.y + 6.5f, s.z, s.headingRad, 0.75f, 0.75f, 0.75f, 0.20f, 0.82f, 1.0f, emR = 0.1f, emG = glow * 0.85f, emB = glow * 1.15f)
                }
                else -> { // Sana'a Heritage Qamariya Clock Tower Monument
                    drawOrientedBox(s.x, s.y + 3.4f, s.z, s.headingRad, 2.4f, 5.8f, 2.4f, 0.92f, 0.82f, 0.66f, texId = textures.sanaaFacadeTex, emR = nightGlow * 0.35f, emG = nightGlow * 0.24f, emB = nightGlow * 0.08f)
                    drawOrientedBox(s.x, s.y + 6.5f, s.z, s.headingRad, 2.7f, 0.45f, 2.7f, 0.98f, 0.96f, 0.92f, emR = glow * 0.5f, emG = glow * 0.4f, emB = glow * 0.18f)
                }
            }
        }
    }

    /** 3D Roadside Speed Radar Cameras (رادار ساهر) with Overhead Speed-Limit Sign & Glowing Status Lens! */
    private fun drawSpeedRadars(centerDistM: Float, atmo: AtmosphereSnapshot) {
        val nightGlow = atmo.nightFactor
        for (rad in RoadSplineNetwork.SPEED_RADARS) {
            if (abs(rad.distanceM - centerDistM) > 190f) continue
            val s = RoadSplineNetwork.sampleAt(rad.distanceM)
            val poleLat = s.widthM * 0.5f + 1.15f
            val rx = s.x + s.rightX * poleLat
            val rz = s.z + s.rightZ * poleLat

            // Metallic radar pole + dual-lens Saher camera box
            drawOrientedBox(rx, s.y + 2.3f, rz, s.headingRad, 0.22f, 4.6f, 0.22f, 0.72f, 0.75f, 0.80f, metallic = 0.7f, roughness = 0.25f)
            drawOrientedBox(rx, s.y + 4.45f, rz, s.headingRad, 0.65f, 0.92f, 0.58f, 0.90f, 0.92f, 0.95f, metallic = 0.4f, roughness = 0.3f)
            // Radar flash lens + pulsing crimson/amber active sensor LED
            val pulse = if (((weatherAnimTime * 3.5f).toInt() % 2) == 0) 1.6f else 0.35f
            drawOrientedBox(rx - sin(s.headingRad) * 0.31f, s.y + 4.62f, rz - cos(s.headingRad) * 0.31f, s.headingRad, 0.32f, 0.26f, 0.06f, 0.12f, 0.14f, 0.18f, emR = pulse, emG = pulse * 0.15f)
            drawOrientedBox(rx - sin(s.headingRad) * 0.31f, s.y + 4.24f, rz - cos(s.headingRad) * 0.31f, s.headingRad, 0.36f, 0.20f, 0.06f, 1.0f, 0.96f, 0.88f, emR = 0.35f + nightGlow * 0.5f, emG = 0.35f + nightGlow * 0.5f, emB = 0.35f + nightGlow * 0.5f)
        }
    }

    private fun drawParkedCars(centerDistM: Float, livingWorld: LivingWorldSimulation) {
        for (car in livingWorld.parkedCars) {
            if (abs(car.distanceM - centerDistM) > 175f) continue
            drawTrafficVehicleBody(
                car.worldX, car.worldY, car.worldZ, car.headingRad,
                car.type, car.colorR, car.colorG, car.colorB,
                braking = false, headlightsOn = false, wheelSpin = 0f
            )
        }
    }

    private fun drawYemeniPedestrians(centerDistM: Float, livingWorld: LivingWorldSimulation) {
        for (ped in livingWorld.pedestrians) {
            if (abs(ped.distanceM - centerDistM) > 160f) continue
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
        val fwdX = sin(ped.headingRad); val fwdZ = cos(ped.headingRad)
        val rightX = cos(ped.headingRad); val rightZ = -sin(ped.headingRad)
        val (legR, legG, legB) = if (ped.style == PedestrianStyle.MAAWIS_COLORFUL) {
            Triple(ped.accentR, ped.accentG, ped.accentB)
        } else Triple(robeR * 0.9f, robeG * 0.9f, robeB * 0.9f)

        drawOrientedBox(ped.worldX - rightX * 0.11f + fwdX * swing, ped.worldY + 0.36f, ped.worldZ - rightZ * 0.11f + fwdZ * swing, ped.headingRad, 0.16f, 0.72f, 0.18f, legR, legG, legB)
        drawOrientedBox(ped.worldX + rightX * 0.11f - fwdX * swing, ped.worldY + 0.36f, ped.worldZ + rightZ * 0.11f - fwdZ * swing, ped.headingRad, 0.16f, 0.72f, 0.18f, legR, legG, legB)
        drawOrientedBox(ped.worldX, ped.worldY + 1.02f, ped.worldZ, ped.headingRad, 0.44f, 0.64f, 0.26f, robeR, robeG, robeB)
        if (ped.style != PedestrianStyle.ABAYA_CLASSIC) {
            drawOrientedBox(ped.worldX, ped.worldY + 0.82f, ped.worldZ, ped.headingRad, 0.46f, 0.11f, 0.28f, ped.accentR, ped.accentG, ped.accentB)
        }
        drawOrientedBox(ped.worldX - rightX * 0.28f - fwdX * swing * 0.8f, ped.worldY + 1.0f, ped.worldZ - rightZ * 0.28f - fwdZ * swing * 0.8f, ped.headingRad, 0.12f, 0.54f, 0.13f, robeR * 0.95f, robeG * 0.95f, robeB * 0.95f)
        drawOrientedBox(ped.worldX + rightX * 0.28f + fwdX * swing * 0.8f, ped.worldY + 1.0f, ped.worldZ + rightZ * 0.28f + fwdZ * swing * 0.8f, ped.headingRad, 0.12f, 0.54f, 0.13f, robeR * 0.95f, robeG * 0.95f, robeB * 0.95f)
        drawOrientedBox(ped.worldX, ped.worldY + 1.50f, ped.worldZ, ped.headingRad, 0.25f, 0.26f, 0.25f, 0.76f, 0.56f, 0.42f)
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
            if (abs(car.distanceM - centerDistM) > 225f) continue
            drawTrafficVehicleBody(
                x = car.worldX,
                y = car.worldY,
                z = car.worldZ,
                heading = car.headingRad,
                type = car.type,
                r = car.colorR,
                g = car.colorG,
                b = car.colorB,
                braking = car.braking,
                headlightsOn = car.headlightsOn,
                wheelSpin = car.wheelSpinRad,
                turnSignalDir = if (snap.blinkerOn) car.turnSignalDir else 0,
                steerAngleRad = car.steerAngleRad,
                pitchDeg = car.bodyPitchDeg,
                rollDeg = car.bodyRollDeg
            )
        }

        val rivalDist = snap.telemetry.aiRivalDistanceM
        if (rivalDist != null && abs(rivalDist - centerDistM) < 230f) {
            val rs = RoadSplineNetwork.sampleAt(rivalDist)
            val rx = rs.x - rs.rightX * 2.2f
            val rz = rs.z - rs.rightZ * 2.2f
            drawTrafficVehicleBody(
                x = rx, y = rs.y, z = rz, heading = rs.headingRad,
                type = TrafficVehicleType.SPORTS_COUPE,
                r = 0.96f, g = 0.68f, b = 0.08f,
                braking = false, headlightsOn = atmo.nightFactor > 0.25f,
                wheelSpin = weatherAnimTime * 22f,
                turnSignalDir = 0,
                steerAngleRad = (rs.curvatureRadPerM * 25f).coerceIn(-0.4f, 0.4f)
            )
        }
    }

    private fun drawTrafficVehicleBody(
        x: Float, y: Float, z: Float, heading: Float,
        type: TrafficVehicleType,
        r: Float, g: Float, b: Float,
        braking: Boolean,
        headlightsOn: Boolean,
        wheelSpin: Float,
        turnSignalDir: Int = 0,
        steerAngleRad: Float = 0f,
        pitchDeg: Float = 0f,
        rollDeg: Float = 0f
    ) {
        val (len, wid, bodyH, cabH) = when (type) {
            TrafficVehicleType.SEDAN -> Quad4(4.55f, 1.84f, 0.64f, 0.54f)
            TrafficVehicleType.SPORTS_COUPE -> Quad4(4.48f, 1.96f, 0.54f, 0.44f)
            TrafficVehicleType.SUV_LUXURY -> Quad4(4.98f, 2.02f, 0.88f, 0.72f)
            TrafficVehicleType.TAXI_YEMEN -> Quad4(4.50f, 1.82f, 0.64f, 0.55f)
            TrafficVehicleType.HATCHBACK -> Quad4(3.92f, 1.78f, 0.66f, 0.56f)
            TrafficVehicleType.PICKUP -> Quad4(5.08f, 1.92f, 0.78f, 0.68f)
            TrafficVehicleType.MINIBUS -> Quad4(5.40f, 2.00f, 1.12f, 0.84f)
            TrafficVehicleType.TRUCK -> Quad4(6.35f, 2.24f, 1.45f, 1.12f)
            TrafficVehicleType.POLICE -> Quad4(4.68f, 1.88f, 0.66f, 0.56f)
            TrafficVehicleType.AMBULANCE -> Quad4(5.55f, 2.04f, 1.16f, 0.86f)
        }
        val wheelRad = when (type) {
            TrafficVehicleType.SUV_LUXURY, TrafficVehicleType.PICKUP, TrafficVehicleType.TRUCK -> 0.43f
            TrafficVehicleType.SPORTS_COUPE -> 0.36f
            else -> 0.38f
        }
        val fwdX = sin(heading); val fwdZ = cos(heading)
        val rightX = cos(heading); val rightZ = -sin(heading)

        // Soft Ambient Occlusion Contact Shadow grounding the car onto the street
        drawContactShadow(x, y + 0.055f, z, heading, wid * 1.20f, len * 1.10f)

        val paintMet = when (type) {
            TrafficVehicleType.SPORTS_COUPE, TrafficVehicleType.SUV_LUXURY -> 0.76f
            TrafficVehicleType.TRUCK -> 0.32f
            else -> 0.64f
        }
        val paintRough = when (type) {
            TrafficVehicleType.SPORTS_COUPE, TrafficVehicleType.SUV_LUXURY -> 0.14f
            TrafficVehicleType.TRUCK -> 0.42f
            else -> 0.18f
        }

        // Sculpted metallic lower chassis with real dynamic suspension pitch & cornering body roll!
        val lowerY = y + wheelRad * 0.92f + bodyH * 0.5f
        val useSloped = type in listOf(
            TrafficVehicleType.SEDAN,
            TrafficVehicleType.SPORTS_COUPE,
            TrafficVehicleType.TAXI_YEMEN,
            TrafficVehicleType.HATCHBACK,
            TrafficVehicleType.POLICE
        )
        drawOrientedMesh(
            if (useSloped) slopedBodyMesh else cubeMesh,
            x, lowerY, z, heading, pitchDeg, rollDeg,
            wid, bodyH, len, r, g, b, metallic = paintMet, roughness = paintRough
        )
        // Lower dark bumper / side-skirt trim + front grille
        drawOrientedMesh(
            cubeMesh,
            x, y + wheelRad * 0.92f + 0.10f, z, heading, pitchDeg, rollDeg,
            wid * 1.015f, 0.18f, len * 0.97f, 0.11f, 0.12f, 0.15f, metallic = 0.42f, roughness = 0.44f
        )
        drawOrientedMesh(
            cubeMesh,
            x + fwdX * (len * 0.498f), lowerY, z + fwdZ * (len * 0.498f), heading, pitchDeg, rollDeg,
            wid * 0.54f, bodyH * 0.44f, 0.08f, 0.08f, 0.09f, 0.12f, metallic = 0.72f, roughness = 0.22f
        )

        val cabZOff = when (type) {
            TrafficVehicleType.PICKUP -> 0.56f
            TrafficVehicleType.TRUCK -> 1.70f
            TrafficVehicleType.HATCHBACK -> -0.26f
            TrafficVehicleType.SPORTS_COUPE -> -0.22f
            TrafficVehicleType.SUV_LUXURY -> -0.28f
            else -> -0.14f
        }
        val cabLen = when (type) {
            TrafficVehicleType.PICKUP -> 1.88f
            TrafficVehicleType.MINIBUS, TrafficVehicleType.AMBULANCE -> len * 0.88f
            TrafficVehicleType.SUV_LUXURY -> len * 0.68f
            TrafficVehicleType.HATCHBACK -> len * 0.66f
            TrafficVehicleType.SPORTS_COUPE -> len * 0.48f
            TrafficVehicleType.TRUCK -> 1.82f
            else -> len * 0.56f
        }
        // Aerodynamic sloped cabin glasshouse
        drawOrientedMesh(
            if (type == TrafficVehicleType.TRUCK) cubeMesh else cabinMesh,
            x + fwdX * cabZOff, y + wheelRad * 0.92f + bodyH + cabH * 0.44f, z + fwdZ * cabZOff,
            heading, pitchDeg, rollDeg, wid * 0.90f, cabH, cabLen,
            0.07f, 0.13f, 0.21f, metallic = 0.90f, roughness = 0.07f
        )
        // Painted or contrasting gloss-black roof cap
        val roofY = y + wheelRad * 0.92f + bodyH + cabH * 0.90f
        val darkRoof = type == TrafficVehicleType.SPORTS_COUPE || type == TrafficVehicleType.HATCHBACK
        val whiteRoof = type == TrafficVehicleType.MINIBUS || type == TrafficVehicleType.AMBULANCE
        drawOrientedMesh(
            cubeMesh,
            x + fwdX * (cabZOff - 0.06f), roofY, z + fwdZ * (cabZOff - 0.06f),
            heading, pitchDeg, rollDeg, wid * 0.72f, 0.09f, cabLen * 0.64f,
            if (whiteRoof) 0.96f else if (darkRoof) 0.09f else r,
            if (whiteRoof) 0.96f else if (darkRoof) 0.10f else g,
            if (whiteRoof) 0.98f else if (darkRoof) 0.13f else b,
            metallic = paintMet, roughness = paintRough
        )

        // Distinct Vehicle-Class Signature Details!
        when (type) {
            TrafficVehicleType.SPORTS_COUPE -> {
                // Carbon-Fiber GT Rear Wing & Twin Chrome Exhaust Tips
                drawOrientedMesh(
                    cubeMesh,
                    x - fwdX * (len * 0.44f), y + wheelRad * 0.92f + bodyH + 0.26f, z - fwdZ * (len * 0.44f),
                    heading, pitchDeg, rollDeg, wid * 0.94f, 0.07f, 0.26f,
                    0.10f, 0.11f, 0.14f, metallic = 0.72f, roughness = 0.18f
                )
                for (sx in intArrayOf(-1, 1)) {
                    drawOrientedMesh(
                        cubeMesh,
                        x - fwdX * (len * 0.44f) + rightX * (sx * wid * 0.30f), y + wheelRad * 0.92f + bodyH + 0.12f, z - fwdZ * (len * 0.44f) + rightZ * (sx * wid * 0.30f),
                        heading, pitchDeg, rollDeg, 0.07f, 0.24f, 0.16f, 0.12f, 0.13f, 0.16f
                    )
                }
            }
            TrafficVehicleType.SUV_LUXURY -> {
                // Chrome Roof Rails + Rear Door Mounted Spare Wheel
                for (sx in intArrayOf(-1, 1)) {
                    drawOrientedMesh(
                        cubeMesh,
                        x + fwdX * (cabZOff - 0.08f) + rightX * (sx * wid * 0.33f), roofY + 0.09f, z + fwdZ * (cabZOff - 0.08f) + rightZ * (sx * wid * 0.33f),
                        heading, pitchDeg, rollDeg, 0.07f, 0.07f, cabLen * 0.62f,
                        0.84f, 0.87f, 0.92f, metallic = 0.90f, roughness = 0.14f
                    )
                }
                drawOrientedMesh(
                    cubeMesh,
                    x - fwdX * (len * 0.52f), lowerY + 0.22f, z - fwdZ * (len * 0.52f),
                    heading, pitchDeg, rollDeg, 0.68f, 0.68f, 0.26f,
                    0.14f, 0.16f, 0.20f, metallic = 0.35f, roughness = 0.55f
                )
            }
            TrafficVehicleType.TAXI_YEMEN -> {
                // Authentic Yemeni Taxi Dark Side Checkered Trim + Illuminated Glowing Amber Roof Taxi Sign
                drawOrientedMesh(
                    cubeMesh,
                    x, lowerY + 0.04f, z, heading, pitchDeg, rollDeg,
                    wid * 1.016f, 0.12f, len * 0.86f, 0.12f, 0.13f, 0.16f
                )
                drawOrientedMesh(
                    cubeMesh,
                    x + fwdX * (cabZOff + 0.08f), roofY + 0.13f, z + fwdZ * (cabZOff + 0.08f),
                    heading, pitchDeg, rollDeg, 0.58f, 0.18f, 0.24f,
                    1.0f, 0.88f, 0.22f, emR = 1.45f, emG = 1.10f, emB = 0.18f
                )
            }
            TrafficVehicleType.PICKUP -> {
                // Steel Bed Roll-Bar & Timber Mountain Cargo Crate
                drawOrientedMesh(
                    cubeMesh,
                    x - fwdX * (len * 0.10f), y + wheelRad * 0.92f + bodyH + 0.42f, z - fwdZ * (len * 0.10f),
                    heading, pitchDeg, rollDeg, wid * 0.88f, 0.10f, 0.12f, 0.14f, 0.15f, 0.18f, metallic = 0.7f
                )
                drawOrientedMesh(
                    cubeMesh,
                    x - fwdX * (len * 0.26f), y + wheelRad * 0.92f + bodyH + 0.22f, z - fwdZ * (len * 0.26f),
                    heading, pitchDeg, rollDeg, wid * 0.78f, 0.44f, len * 0.32f, 0.62f, 0.44f, 0.26f, roughness = 0.75f
                )
            }
            TrafficVehicleType.MINIBUS -> {
                // Classic Yemeni Passenger Dabab Teal/Emerald Side Stripe
                drawOrientedMesh(
                    cubeMesh,
                    x, lowerY + 0.10f, z, heading, pitchDeg, rollDeg,
                    wid * 1.018f, 0.22f, len * 0.90f, 0.08f, 0.58f, 0.48f, metallic = 0.45f, roughness = 0.28f
                )
            }
            TrafficVehicleType.TRUCK -> {
                drawOrientedMesh(
                    cubeMesh,
                    x - fwdX * 1.1f, y + wheelRad * 0.92f + bodyH + 0.85f, z - fwdZ * 1.1f,
                    heading, pitchDeg, rollDeg, wid * 0.96f, 1.85f, 3.65f, 0.92f, 0.90f, 0.86f
                )
                // Glowing amber truck cab roof marker lights
                for (sx in intArrayOf(-1, 0, 1)) {
                    drawOrientedBox(
                        x + fwdX * (cabZOff + 0.85f) + rightX * (sx * 0.55f), roofY + 0.10f, z + fwdZ * (cabZOff + 0.85f) + rightZ * (sx * 0.55f),
                        heading, 0.14f, 0.10f, 0.10f, 1f, 0.72f, 0.10f, emR = 1.4f, emG = 0.85f, emB = 0.08f
                    )
                }
            }
            TrafficVehicleType.POLICE, TrafficVehicleType.AMBULANCE -> {
                val isPolice = type == TrafficVehicleType.POLICE
                val stripeR = if (isPolice) 0.06f else 0.92f
                val stripeG = if (isPolice) 0.18f else 0.10f
                val stripeB = if (isPolice) 0.52f else 0.12f
                drawOrientedMesh(
                    cubeMesh,
                    x, lowerY + 0.06f, z,
                    heading, pitchDeg, rollDeg, wid * 1.018f, 0.18f, len * 0.86f,
                    stripeR, stripeG, stripeB, metallic = 0.35f, roughness = 0.30f
                )
                val strobeLeftRed = ((weatherAnimTime * 7.2f).toInt() % 2) == 0
                val barY = roofY + 0.14f
                val barZ = cabZOff + (if (isPolice) 0.02f else cabLen * 0.26f)
                drawOrientedBox(
                    x + fwdX * barZ, barY - 0.03f, z + fwdZ * barZ,
                    heading, wid * 0.66f, 0.06f, 0.24f,
                    0.14f, 0.16f, 0.20f, metallic = 0.7f, roughness = 0.25f
                )
                val redEm = if (strobeLeftRed) 2.6f else 0.22f
                drawOrientedBox(
                    x + fwdX * barZ - rightX * (wid * 0.22f), barY + 0.05f, z + fwdZ * barZ - rightZ * (wid * 0.22f),
                    heading, wid * 0.26f, 0.12f, 0.20f,
                    1.0f, 0.08f, 0.10f, emR = redEm, emG = 0.04f, emB = 0.06f
                )
                val blueEm = if (!strobeLeftRed) 2.6f else 0.22f
                drawOrientedBox(
                    x + fwdX * barZ + rightX * (wid * 0.22f), barY + 0.05f, z + fwdZ * barZ + rightZ * (wid * 0.22f),
                    heading, wid * 0.26f, 0.12f, 0.20f,
                    0.08f, 0.42f, 1.0f, emR = 0.06f, emG = blueEm * 0.35f, emB = blueEm
                )
            }
            else -> Unit
        }

        // 4 High-Res Alloy Wheels — Front wheels visibly steer into curves & lane changes!
        val wb = len * 0.32f
        val tr = wid * 0.48f
        for (sx in intArrayOf(-1, 1)) {
            drawWheel(
                x + rightX * (sx * tr) + fwdX * wb,
                y + wheelRad,
                z + rightZ * (sx * tr) + fwdZ * wb,
                heading, steerAngleRad, wheelSpin, wheelRad, 0.27f
            )
            drawWheel(
                x + rightX * (sx * tr) - fwdX * wb,
                y + wheelRad,
                z + rightZ * (sx * tr) - fwdZ * wb,
                heading, 0f, wheelSpin, wheelRad, 0.28f
            )
        }

        // Crisp LED Headlights, Tail/Brake Lights & AI Blinking Amber Turn Signals
        val hlEm = if (headlightsOn) 1.75f else 0.18f
        val blEm = if (braking) 2.0f else 0.32f
        val lightY = y + wheelRad * 0.92f + bodyH * 0.44f
        for (sx in intArrayOf(-1, 1)) {
            drawOrientedBox(
                x + fwdX * (len * 0.50f) + rightX * (sx * wid * 0.35f), lightY, z + fwdZ * (len * 0.50f) + rightZ * (sx * wid * 0.35f),
                heading, 0.38f, 0.15f, 0.08f, 1f, 0.97f, 0.90f, emR = hlEm, emG = hlEm * 0.95f, emB = hlEm * 0.82f
            )
            drawOrientedBox(
                x - fwdX * (len * 0.50f) + rightX * (sx * wid * 0.35f), lightY + 0.02f, z - fwdZ * (len * 0.50f) + rightZ * (sx * wid * 0.35f),
                heading, 0.42f, 0.15f, 0.08f, 0.98f, 0.10f, 0.08f, emR = blEm, emG = 0.02f, emB = 0.02f
            )
            if (turnSignalDir != 0 && sx == turnSignalDir) {
                drawOrientedBox(
                    x + fwdX * (len * 0.495f) + rightX * (sx * wid * 0.46f), lightY, z + fwdZ * (len * 0.495f) + rightZ * (sx * wid * 0.46f),
                    heading, 0.16f, 0.15f, 0.12f, 1f, 0.66f, 0.04f, emR = 2.0f, emG = 1.25f, emB = 0.05f
                )
                drawOrientedBox(
                    x - fwdX * (len * 0.495f) + rightX * (sx * wid * 0.46f), lightY + 0.02f, z - fwdZ * (len * 0.495f) + rightZ * (sx * wid * 0.46f),
                    heading, 0.16f, 0.15f, 0.12f, 1f, 0.66f, 0.04f, emR = 2.0f, emG = 1.25f, emB = 0.05f
                )
            }
        }
        // Full-width sports LED tail light bar on Sports Coupe & Luxury SUV
        if (type == TrafficVehicleType.SPORTS_COUPE || type == TrafficVehicleType.SUV_LUXURY) {
            drawOrientedBox(
                x - fwdX * (len * 0.502f), lightY + 0.02f, z - fwdZ * (len * 0.502f),
                heading, wid * 0.82f, 0.06f, 0.06f, 0.98f, 0.08f, 0.08f, emR = blEm * 0.85f, emG = 0.02f, emB = 0.02f
            )
        }
    }

    /** Sculpted Photorealistic Player Vehicle with Metallic Clearcoat, Sloped Windshield, Side Mirrors, DRLs, Exhausts & Full 3D Suspension Pitch/Roll. */
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

        val (paintMet, paintRough) = when (custom.paintFinish) {
            PaintFinishType.METALLIC -> 0.72f to 0.16f
            PaintFinishType.PEARL -> 0.52f to 0.10f
            PaintFinishType.MATTE -> 0.12f to 0.70f
        }

        val len = spec.bodyLengthM
        val wid = spec.bodyWidthM
        val hgt = spec.bodyHeightM
        // Include subtle drift slip yaw so cornering looks authentically dynamic!
        val visualHeading = t.headingRad + (t.driftSlipAngleDeg * PI.toFloat() / 180f) * 0.35f
        val pitchDeg = t.pitchDeg
        val rollDeg = t.rollDeg
        val fwdX = sin(visualHeading); val fwdZ = cos(visualHeading)
        val rightX = cos(visualHeading); val rightZ = -sin(visualHeading)
        val baseY = t.posY + spec.wheelRadiusM * 0.90f + t.suspensionBounceM

        // 1. Soft Ground Contact Shadow
        drawContactShadow(t.posX, t.posY + 0.06f, t.posZ, visualHeading, wid * 1.24f, len * 1.12f)

        // 2. Sculpted Aerodynamic Lower Body + Dark Metallic Lower Valance/Skirt
        val lowerH = hgt * 0.43f
        val isPickup = vehicleId in listOf(VehicleId.SAQR, VehicleId.BADIYA, VehicleId.MUNQIDH)
        val isTruck = vehicleId == VehicleId.JABAL
        val isBus = vehicleId == VehicleId.HAFILAT
        val bodyMesh = if (isTruck || isBus) cubeMesh else slopedBodyMesh

        drawOrientedMesh(
            bodyMesh,
            t.posX, baseY + lowerH * 0.52f, t.posZ,
            visualHeading, pitchDeg, rollDeg,
            wid, lowerH, len, pr, pg, pb,
            metallic = paintMet, roughness = paintRough
        )
        // Lower dark carbon/steel rock-slider & rear diffuser skirt
        drawOrientedMesh(
            cubeMesh,
            t.posX, baseY + 0.12f, t.posZ,
            visualHeading, pitchDeg, rollDeg,
            wid * 1.02f, 0.22f, len * 0.98f, 0.10f, 0.11f, 0.13f,
            metallic = 0.38f, roughness = 0.45f
        )

        // 3. Sculpted Front Chrome/Dark Grille & Hood Crease
        drawOrientedMesh(
            cubeMesh,
            t.posX + fwdX * (len * 0.495f), baseY + lowerH * 0.46f, t.posZ + fwdZ * (len * 0.495f),
            visualHeading, pitchDeg, rollDeg,
            wid * 0.64f, lowerH * 0.50f, 0.12f, 0.08f, 0.09f, 0.11f,
            metallic = 0.65f, roughness = 0.24f
        )
        // Chrome Grille Horizontal Slats + Emblem Badge
        drawOrientedMesh(
            cubeMesh,
            t.posX + fwdX * (len * 0.505f), baseY + lowerH * 0.50f, t.posZ + fwdZ * (len * 0.505f),
            visualHeading, pitchDeg, rollDeg,
            wid * 0.58f, 0.06f, 0.06f, 0.85f, 0.88f, 0.92f,
            metallic = 0.92f, roughness = 0.10f
        )
        drawOrientedMesh(
            cubeMesh,
            t.posX + fwdX * (len * 0.512f), baseY + lowerH * 0.50f, t.posZ + fwdZ * (len * 0.512f),
            visualHeading, pitchDeg, rollDeg,
            0.22f, 0.16f, 0.06f, 0.96f, 0.72f, 0.16f,
            metallic = 0.90f, roughness = 0.12f
        )

        // 4. Sculpted Sloped Panoramic Cabin Greenhouse + Roof Cap + Side Wing Mirrors
        val cabH = hgt * 0.43f
        val cabLen = when {
            isBus -> len * 0.90f
            isPickup -> len * 0.42f
            isTruck -> len * 0.34f
            else -> len * 0.58f
        }
        val cabOffset = when {
            isPickup -> len * 0.12f
            isTruck -> len * 0.26f
            else -> -len * 0.07f
        }
        // Reflective Smoked Panoramic Glass Greenhouse
        drawOrientedMesh(
            if (isTruck) cubeMesh else cabinMesh,
            t.posX + fwdX * cabOffset, baseY + lowerH + cabH * 0.44f, t.posZ + fwdZ * cabOffset,
            visualHeading, pitchDeg, rollDeg,
            wid * 0.90f, cabH, cabLen,
            0.07f, 0.12f, 0.19f,
            metallic = 0.92f, roughness = 0.06f
        )
        // Sculpted Metallic Roof Panel
        drawOrientedMesh(
            cubeMesh,
            t.posX + fwdX * (cabOffset - 0.06f), baseY + lowerH + cabH * 0.91f, t.posZ + fwdZ * (cabOffset - 0.06f),
            visualHeading, pitchDeg, rollDeg,
            wid * 0.72f, 0.10f, cabLen * 0.62f,
            pr, pg, pb, metallic = paintMet, roughness = paintRough
        )
        // Left & Right Aerodynamic Side Mirrors
        for (sx in intArrayOf(-1, 1)) {
            drawOrientedMesh(
                cubeMesh,
                t.posX + fwdX * (cabOffset + cabLen * 0.28f) + rightX * (sx * (wid * 0.53f)),
                baseY + lowerH + 0.16f,
                t.posZ + fwdZ * (cabOffset + cabLen * 0.28f) + rightZ * (sx * (wid * 0.53f)),
                visualHeading, pitchDeg, rollDeg,
                0.24f, 0.14f, 0.18f, pr, pg, pb, metallic = paintMet, roughness = paintRough
            )
        }

        // 5. Dual Chrome Rear Exhaust Tips
        for (sx in intArrayOf(-1, 1)) {
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.505f) + rightX * (sx * wid * 0.30f),
                baseY + 0.14f,
                t.posZ - fwdZ * (len * 0.505f) + rightZ * (sx * wid * 0.30f),
                visualHeading, pitchDeg, rollDeg,
                0.15f, 0.11f, 0.18f, 0.80f, 0.84f, 0.88f, metallic = 0.92f, roughness = 0.12f
            )
        }

        // 6. Truck Cargo / Pickup Accessories / Bullbar / Roof Rack / Rescue Lightbar
        if (isTruck) {
            drawOrientedMesh(cubeMesh, t.posX - fwdX * (len * 0.18f), baseY + lowerH + 0.95f, t.posZ - fwdZ * (len * 0.18f), visualHeading, pitchDeg, rollDeg, wid * 0.95f, 1.95f, len * 0.56f, 0.90f, 0.88f, 0.82f)
        } else if (isPickup && custom.pickupAccessory != PickupAccessoryType.NONE) {
            val accH = if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) cabH else 0.65f
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.24f), baseY + lowerH + accH * 0.5f, t.posZ - fwdZ * (len * 0.24f),
                visualHeading, pitchDeg, rollDeg, wid * 0.86f, accH, len * 0.38f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.78f else 0.18f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.70f else 0.19f,
                if (custom.pickupAccessory == PickupAccessoryType.CANOPY_TENT) 0.54f else 0.22f
            )
        }
        if (custom.bullbarStyle != BullbarStyle.NONE) {
            drawOrientedMesh(cubeMesh, t.posX + fwdX * (len * 0.53f), baseY + 0.32f, t.posZ + fwdZ * (len * 0.53f), visualHeading, pitchDeg, rollDeg, wid * 0.94f, 0.34f, 0.24f, 0.14f, 0.15f, 0.18f, metallic = 0.72f, roughness = 0.25f)
        }
        if (custom.roofRackInstalled) {
            val roofY = baseY + lowerH + cabH + 0.04f
            drawOrientedMesh(cubeMesh, t.posX + fwdX * cabOffset, roofY, t.posZ + fwdZ * cabOffset, visualHeading, pitchDeg, rollDeg, wid * 0.70f, 0.10f, cabLen * 0.62f, 0.15f, 0.16f, 0.20f, metallic = 0.6f, roughness = 0.3f)
            if (custom.roofCargoBoxInstalled) {
                drawOrientedMesh(cabinMesh, t.posX + fwdX * cabOffset, roofY + 0.22f, t.posZ + fwdZ * cabOffset, visualHeading, pitchDeg, rollDeg, wid * 0.50f, 0.32f, cabLen * 0.54f, 0.14f, 0.16f, 0.20f, metallic = 0.7f, roughness = 0.18f)
            }
        }
        if (vehicleId == VehicleId.BARQ) {
            // Signature Carbon-Fiber GT Rear Wing & Twin Aerodynamic Struts for the Sports Car!
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.44f), baseY + lowerH + 0.30f, t.posZ - fwdZ * (len * 0.44f),
                visualHeading, pitchDeg, rollDeg, wid * 0.96f, 0.07f, 0.30f,
                0.09f, 0.10f, 0.13f, metallic = 0.78f, roughness = 0.16f
            )
            for (sx in intArrayOf(-1, 1)) {
                drawOrientedMesh(
                    cubeMesh,
                    t.posX - fwdX * (len * 0.44f) + rightX * (sx * wid * 0.31f), baseY + lowerH + 0.15f, t.posZ - fwdZ * (len * 0.44f) + rightZ * (sx * wid * 0.31f),
                    visualHeading, pitchDeg, rollDeg, 0.07f, 0.28f, 0.16f, 0.11f, 0.12f, 0.15f
                )
            }
        } else if (vehicleId in listOf(VehicleId.SHAHIQ, VehicleId.RAHHAL)) {
            // Rear-Door Mounted Full-Size 4x4 Spare Tire + Chrome Side Beltline Trim
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.515f), baseY + lowerH * 0.78f, t.posZ - fwdZ * (len * 0.515f),
                visualHeading, pitchDeg, rollDeg, 0.74f, 0.74f, 0.28f,
                0.13f, 0.15f, 0.18f, metallic = 0.35f, roughness = 0.55f
            )
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.525f), baseY + lowerH * 0.78f, t.posZ - fwdZ * (len * 0.525f),
                visualHeading, pitchDeg, rollDeg, 0.46f, 0.46f, 0.28f,
                pr, pg, pb, metallic = paintMet, roughness = paintRough
            )
        } else if (vehicleId in listOf(VehicleId.MADINA, VehicleId.ASIL)) {
            // Sleek Brushed-Chrome Beltline Accent Molding
            drawOrientedMesh(
                cubeMesh,
                t.posX, baseY + lowerH * 0.92f, t.posZ,
                visualHeading, pitchDeg, rollDeg, wid * 1.015f, 0.05f, len * 0.84f,
                0.86f, 0.89f, 0.94f, metallic = 0.94f, roughness = 0.10f
            )
        }
        if (vehicleId == VehicleId.MUNQIDH) {
            val roofY = baseY + lowerH + cabH + 0.16f
            val flashA = ((weatherAnimTime * 7f).toInt() % 2 == 0)
            drawOrientedBox(t.posX + fwdX * cabOffset - rightX * 0.38f, roofY, t.posZ + fwdZ * cabOffset - rightZ * 0.38f, visualHeading, 0.45f, 0.20f, 0.26f, 1f, 0.55f, 0.05f, emR = if (flashA) 1.9f else 0.1f, emG = if (flashA) 0.8f else 0.05f)
            drawOrientedBox(t.posX + fwdX * cabOffset + rightX * 0.38f, roofY, t.posZ + fwdZ * cabOffset + rightZ * 0.38f, visualHeading, 0.45f, 0.20f, 0.26f, 0.12f, 0.55f, 1f, emB = if (!flashA) 1.9f else 0.1f, emG = if (!flashA) 0.7f else 0.05f)
        }

        // 7. 4 High-Detail 24-Segment Alloy Wheels with Red Brake Calipers & Steering Angle
        val wb = spec.wheelbaseM * 0.5f
        val tr = spec.trackWidthM * 0.5f
        for (sx in intArrayOf(-1, 1)) {
            drawWheel(
                t.posX + rightX * (sx * tr) + fwdX * wb,
                t.posY + spec.wheelRadiusM,
                t.posZ + rightZ * (sx * tr) + fwdZ * wb,
                visualHeading, t.frontSteerRad, t.wheelRotationRad, spec.wheelRadiusM, 0.33f
            )
            drawWheel(
                t.posX + rightX * (sx * tr) - fwdX * wb,
                t.posY + spec.wheelRadiusM,
                t.posZ + rightZ * (sx * tr) - fwdZ * wb,
                visualHeading, 0f, t.wheelRotationRad, spec.wheelRadiusM, 0.35f
            )
        }

        // 8. Signature LED DRLs, High/Low Beam Headlights, Full-Width Rear LED Taillight Bar & Blinkers
        val hlOn = t.headlightsOn || atmo.nightFactor > 0.32f || t.inTunnel
        val hlEm = when {
            t.headlightBeamMode == HeadlightBeamMode.HIGH_BEAM -> 2.4f
            hlOn -> 1.65f
            else -> 0.55f // Modern crisp white DRL signature always visible!
        }
        val blEm = if (t.brake > 0.08f || t.handbrake) 2.2f else 0.45f

        // Rear full-width luxury LED light bar
        drawOrientedMesh(
            cubeMesh,
            t.posX - fwdX * (len * 0.502f), baseY + lowerH * 0.58f, t.posZ - fwdZ * (len * 0.502f),
            visualHeading, pitchDeg, rollDeg,
            wid * 0.88f, 0.08f, 0.07f, 0.95f, 0.08f, 0.06f,
            emR = blEm * 0.72f, emG = 0.02f, emB = 0.02f
        )

        for (sx in intArrayOf(-1, 1)) {
            drawOrientedMesh(
                cubeMesh,
                t.posX + fwdX * (len * 0.505f) + rightX * (sx * wid * 0.36f),
                baseY + lowerH * 0.54f,
                t.posZ + fwdZ * (len * 0.505f) + rightZ * (sx * wid * 0.36f),
                visualHeading, pitchDeg, rollDeg,
                0.42f, 0.16f, 0.09f, 0.96f, 0.98f, 1.0f,
                emR = hlEm, emG = hlEm * 0.96f, emB = hlEm * 0.88f
            )
            val isBlinkerLit = blinkerOn && ((sx < 0 && t.leftIndicatorOn) || (sx > 0 && t.rightIndicatorOn))
            drawOrientedMesh(
                cubeMesh,
                t.posX - fwdX * (len * 0.506f) + rightX * (sx * wid * 0.36f),
                baseY + lowerH * 0.56f,
                t.posZ - fwdZ * (len * 0.506f) + rightZ * (sx * wid * 0.36f),
                visualHeading, pitchDeg, rollDeg,
                0.42f, 0.18f, 0.09f,
                if (isBlinkerLit) 1f else 0.98f, if (isBlinkerLit) 0.65f else 0.08f, 0.04f,
                emR = if (isBlinkerLit) 2.0f else blEm, emG = if (isBlinkerLit) 1.15f else 0.02f
            )
        }
    }

    /** Hyper-Detailed 3D Cockpit Interior: Supports LHD & RHD seating, Eastern/European/Sport/Classic 3D Steering Wheel & Sweeping Wipers! */
    private fun drawCockpitInterior(
        t: VehicleTelemetry,
        custom: VehicleCustomization,
        wheelStyle: SteeringWheelStyle,
        seatSide: DriverSeatSide,
        atmo: AtmosphereSnapshot
    ) {
        val fwdX = sin(t.headingRad); val fwdZ = cos(t.headingRad)
        val rightX = cos(t.headingRad); val rightZ = -sin(t.headingRad)
        val seatSign = if (seatSide == DriverSeatSide.LEFT_HAND) -1f else 1f
        val parsed = runCatching { Color.parseColor(custom.paintColorHex) }.getOrDefault(Color.rgb(200, 114, 54))

        // 1. Sculpted Metallic Hood visible through windshield
        drawOrientedMesh(
            slopedBodyMesh,
            t.posX + fwdX * 1.65f, t.posY + 1.02f, t.posZ + fwdZ * 1.65f,
            t.headingRad, 0f, 0f, 1.74f, 0.24f, 1.65f,
            Color.red(parsed) / 255f, Color.green(parsed) / 255f, Color.blue(parsed) / 255f,
            metallic = 0.72f, roughness = 0.16f
        )

        // 2. Sculpted Soft-Touch Leather Dashboard + Wood/Carbon Trim Strip
        drawOrientedBox(t.posX + fwdX * 0.84f, t.posY + 1.04f, t.posZ + fwdZ * 0.84f, t.headingRad, 1.80f, 0.38f, 0.48f, 0.09f, 0.10f, 0.12f, metallic = 0.15f, roughness = 0.65f)
        val (trimR, trimG, trimB) = when (wheelStyle) {
            SteeringWheelStyle.GULF_YEMENI, SteeringWheelStyle.CLASSIC_HERITAGE -> Triple(0.42f, 0.22f, 0.10f) // Polished Walnut Wood
            SteeringWheelStyle.EUROPEAN_LUXURY -> Triple(0.72f, 0.76f, 0.82f) // Brushed Aluminum
            SteeringWheelStyle.JAPANESE_SPORT -> Triple(0.75f, 0.12f, 0.14f)  // Racing Red Carbon Accent
        }
        drawOrientedBox(
            t.posX + fwdX * 0.64f, t.posY + 1.12f, t.posZ + fwdZ * 0.64f,
            t.headingRad, 1.72f, 0.055f, 0.06f, trimR, trimG, trimB, metallic = 0.65f, roughness = 0.20f,
            emR = if (wheelStyle == SteeringWheelStyle.EUROPEAN_LUXURY) 0.15f else 0f,
            emG = if (wheelStyle == SteeringWheelStyle.EUROPEAN_LUXURY) 0.35f else 0f,
            emB = if (wheelStyle == SteeringWheelStyle.EUROPEAN_LUXURY) 0.55f else 0f
        )

        // 3. Glowing Virtual Cockpit Digital Cluster (directly ahead of LHD/RHD driver) + Center Infotainment Tablet
        val clusterX = t.posX + fwdX * 0.72f + rightX * (seatSign * 0.38f)
        val clusterZ = t.posZ + fwdZ * 0.72f + rightZ * (seatSign * 0.38f)
        drawOrientedBox(clusterX, t.posY + 1.18f, clusterZ, t.headingRad, 0.46f, 0.15f, 0.05f, 0.12f, 0.74f, 0.96f, emR = 0.22f, emG = 0.64f, emB = 0.90f)
        drawOrientedBox(t.posX + fwdX * 0.74f, t.posY + 1.22f, t.posZ + fwdZ * 0.74f, t.headingRad, 0.34f, 0.20f, 0.04f, 0.12f, 0.20f, 0.28f, emR = 0.38f, emG = 0.26f, emB = 0.08f)

        // 4. Windshield A-Pillars & Top Rear-View Mirror
        for (side in intArrayOf(-1, 1)) {
            drawOrientedBox(
                t.posX + fwdX * 0.86f + rightX * (side * 0.84f), t.posY + 1.56f, t.posZ + fwdZ * 0.86f + rightZ * (side * 0.84f),
                t.headingRad, 0.09f, 0.90f, 0.11f, 0.11f, 0.12f, 0.14f
            )
        }
        drawOrientedBox(t.posX + fwdX * 0.78f, t.posY + 1.78f, t.posZ + fwdZ * 0.78f, t.headingRad, 0.30f, 0.08f, 0.05f, 0.14f, 0.18f, 0.24f, metallic = 0.9f, roughness = 0.05f)

        // 5. Animated Sweeping Windshield Wipers!
        if (t.wipersOn || abs(t.wiperPhaseRad) > 0.02f) {
            val sweepDeg = 12f + (sin(t.wiperPhaseRad) * 0.5f + 0.5f) * 68f
            for (wOff in floatArrayOf(-0.32f, 0.22f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, t.posX + fwdX * 1.02f + rightX * wOff, t.posY + 1.22f, t.posZ + fwdZ * 1.02f + rightZ * wOff)
                Matrix.rotateM(modelMatrix, 0, t.headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
                Matrix.rotateM(modelMatrix, 0, -sweepDeg, 0f, 0f, 1f)
                Matrix.translateM(modelMatrix, 0, 0f, 0.28f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.025f, 0.56f, 0.025f)
                drawMeshWithCurrentMatrix(cubeMesh, 0.08f, 0.09f, 0.10f, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f, 0.2f, 0.5f)
            }
        }

        // 6. Sculpted 3D Rotating Steering Wheel matching Selected Style (Eastern / European / Sport / Classic)
        val swX = t.posX + fwdX * 0.58f + rightX * (seatSign * 0.38f)
        val swY = t.posY + 1.15f
        val swZ = t.posZ + fwdZ * 0.58f + rightZ * (seatSign * 0.38f)
        val ringMesh = if (wheelStyle == SteeringWheelStyle.EUROPEAN_LUXURY) steerRingFlat else steerRingRound
        val (rimR, rimG, rimB) = when (wheelStyle) {
            SteeringWheelStyle.GULF_YEMENI -> Triple(0.46f, 0.24f, 0.11f)
            SteeringWheelStyle.EUROPEAN_LUXURY -> Triple(0.14f, 0.16f, 0.20f)
            SteeringWheelStyle.JAPANESE_SPORT -> Triple(0.12f, 0.13f, 0.16f)
            SteeringWheelStyle.CLASSIC_HERITAGE -> Triple(0.56f, 0.30f, 0.12f)
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, swX, swY, swZ)
        Matrix.rotateM(modelMatrix, 0, t.headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 16f, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, -t.steeringWheelAngleDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.38f, 0.38f, 0.38f)
        drawMeshWithCurrentMatrix(ringMesh, rimR, rimG, rimB, textures.whiteTex, 0f, 1f, 1f, 0.05f * atmo.nightFactor, 0.04f * atmo.nightFactor, 0f, 0.55f, 0.22f)

        // Spokes & Center Boss
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, swX, swY, swZ)
        Matrix.rotateM(modelMatrix, 0, t.headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 16f, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, -t.steeringWheelAngleDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.32f, 0.06f, 0.05f)
        drawMeshWithCurrentMatrix(cubeMesh, trimR, trimG, trimB, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f, 0.72f, 0.22f)
    }

    private fun drawWeatherParticles(weather: WeatherPreset) {
        val isSand = weather == WeatherPreset.SANDSTORM
        val count = if (weather == WeatherPreset.HEAVY_RAIN) 68 else 36
        val r = if (isSand) 0.88f else 0.80f
        val g = if (isSand) 0.70f else 0.88f
        val b = if (isSand) 0.44f else 0.98f

        for (i in 0 until count) {
            val seed = i * 19.37f
            val px = currentCamX + sin(seed + weatherAnimTime * (if (isSand) 3.6f else 0.3f)) * 16f
            val pz = currentCamZ + cos(seed * 1.3f + weatherAnimTime * 0.4f) * 16f
            val fall = ((weatherAnimTime * (if (isSand) 4.2f else 20f) + i * 0.7f) % 10f)
            val py = currentCamY + 5f - fall
            drawOrientedBox(
                px, py, pz, 0f,
                if (isSand) 0.16f else 0.025f,
                if (isSand) 0.14f else 0.78f,
                if (isSand) 0.16f else 0.025f,
                r, g, b
            )
        }
    }

    private fun drawContactShadow(x: Float, y: Float, z: Float, headingRad: Float, sx: Float, sz: Float) {
        GLES30.glDepthMask(false)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, 1f, sz)
        drawMeshWithCurrentMatrix(quadMesh, 1f, 1f, 1f, textures.contactShadowTex, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, alpha = 0.85f)
        GLES30.glDepthMask(true)
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
        drawMeshWithCurrentMatrix(wheelMesh, 1f, 1f, 1f, textures.alloyRimTex, 1f, 1f, 1f, 0f, 0f, 0f, 0.52f, 0.28f)
    }

    private fun drawOrientedMesh(
        mesh: GpuMesh,
        x: Float, y: Float, z: Float,
        headingRad: Float,
        pitchDeg: Float,
        rollDeg: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        texId: Int = textures.whiteTex,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f,
        emR: Float = 0f,
        emG: Float = 0f,
        emB: Float = 0f,
        metallic: Float = 0.05f,
        roughness: Float = 0.72f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        if (abs(pitchDeg) > 0.01f) Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        if (abs(rollDeg) > 0.01f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        val useTex = if (texId != textures.whiteTex) 1f else 0f
        drawMeshWithCurrentMatrix(mesh, r, g, b, texId, useTex, uvScaleX, uvScaleY, emR, emG, emB, metallic, roughness)
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
        emB: Float = 0f,
        metallic: Float = 0.05f,
        roughness: Float = 0.74f
    ) {
        drawOrientedMesh(cubeMesh, x, y, z, headingRad, 0f, 0f, sx, sy, sz, r, g, b, texId, uvScaleX, uvScaleY, emR, emG, emB, metallic, roughness)
    }

    private fun drawOrientedQuad(x: Float, y: Float, z: Float, headingRad: Float, sx: Float, sz: Float, texId: Int) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, headingRad * 180f / PI.toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, 1f, sz)
        drawMeshWithCurrentMatrix(quadMesh, 1f, 1f, 1f, texId, 1f, 1f, 1f, 0f, 0f, 0f, 0.04f, 0.65f)
    }

    private fun drawCone(x: Float, y: Float, z: Float, width: Float, height: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, width, height, width)
        drawMeshWithCurrentMatrix(coneMesh, r, g, b, textures.whiteTex, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0.85f)
    }

    private fun drawMeshWithCurrentMatrix(
        mesh: GpuMesh,
        r: Float, g: Float, b: Float,
        texId: Int,
        useTex: Float,
        uvScaleX: Float,
        uvScaleY: Float,
        emR: Float, emG: Float, emB: Float,
        metallic: Float,
        roughness: Float,
        alpha: Float = 1f
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shaders.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(shaders.uBaseColor, r, g, b, alpha)
        GLES30.glUniform3f(shaders.uEmissive, emR, emG, emB)
        GLES30.glUniform1f(shaders.uMetallic, metallic)
        GLES30.glUniform1f(shaders.uRoughness, roughness)
        GLES30.glUniform1f(shaders.uWetness, 0f)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1f(shaders.uUseTexture, useTex)
        GLES30.glUniform2f(shaders.uUvScale, uvScaleX, uvScaleY)
        mesh.draw()
    }

    private data class Quad4(val a: Float, val b: Float, val c: Float, val d: Float)
}
