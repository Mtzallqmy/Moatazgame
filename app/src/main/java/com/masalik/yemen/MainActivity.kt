package com.masalik.yemen

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masalik.yemen.renderer.MasalikGLRenderer
import com.masalik.yemen.ui.ActiveModal
import com.masalik.yemen.ui.MasalikGameViewModel
import com.masalik.yemen.ui.screens.ControlStudioScreen
import com.masalik.yemen.ui.screens.DrivingHudOverlay
import com.masalik.yemen.ui.screens.GarageShowroomScreen
import com.masalik.yemen.ui.screens.MainMenuLobbyScreen
import com.masalik.yemen.ui.screens.MissionsAndStoryScreen
import com.masalik.yemen.ui.screens.SettingsAndSaveScreen
import com.masalik.yemen.ui.screens.YemenWorldMapScreen
import com.masalik.yemen.ui.screens.YemeniRadioOverlay
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MasalikGameViewModel by viewModels()
    private var glSurfaceViewRef: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBarsImmersive()

        setContent {
            MasalikTheme {
                MasalikRootScreen(
                    vm = viewModel,
                    onBindGLView = { glSurfaceViewRef = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBarsImmersive()
        glSurfaceViewRef?.onResume()
        viewModel.onResumeLifecycle()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceViewRef?.onPause()
        viewModel.onPauseLifecycle()
    }

    private fun hideSystemBarsImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun MasalikRootScreen(
    vm: MasalikGameViewModel,
    onBindGLView: (GLSurfaceView?) -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SAF JSON Export / Import launchers for 100% offline local save backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val jsonPayload = vm.exportSaveJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    vm.importSaveJson(reader.readText())
                }
            }
        }
    }

    // Auto-dismiss transient toast messages after 3.2s
    LaunchedEffect(state.toastMessageAr) {
        if (state.toastMessageAr != null) {
            delay(3200L)
            vm.dismissToast()
        }
    }

    // Handle Android Back button cleanly across every modal/sub-screen
    BackHandler(enabled = state.activeModal != ActiveModal.MENU) {
        when (state.activeModal) {
            ActiveModal.DRIVING -> vm.setModal(ActiveModal.PAUSE)
            ActiveModal.PAUSE, ActiveModal.PHOTO -> vm.setModal(ActiveModal.DRIVING)
            ActiveModal.WORLD_MAP,
            ActiveModal.GARAGE,
            ActiveModal.MISSIONS,
            ActiveModal.CONTROL_STUDIO,
            ActiveModal.SETTINGS -> vm.setModal(ActiveModal.MENU)
            ActiveModal.MENU -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { onBindGLView(null) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MasalikObsidian)) {
        // 1. Live Native 60fps OpenGL ES 3.0 3D Yemen World & Vehicle Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3)
                    preserveEGLContextOnPause = true
                    setRenderer(MasalikGLRenderer { dtSec -> vm.stepSimulationFrame(dtSec) })
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    onBindGLView(this)
                }
            }
        )

        // 2. Active Screen Overlay
        when (state.activeModal) {
            ActiveModal.MENU -> {
                MainMenuLobbyScreen(
                    saveState = state.saveState,
                    onStartDriving = { vm.setModal(ActiveModal.DRIVING) },
                    onOpenWorldMap = { vm.setModal(ActiveModal.WORLD_MAP) },
                    onOpenMissions = { vm.setModal(ActiveModal.MISSIONS) },
                    onOpenGarage = { vm.setModal(ActiveModal.GARAGE) },
                    onOpenControlStudio = { vm.setModal(ActiveModal.CONTROL_STUDIO) },
                    onOpenSettings = { vm.setModal(ActiveModal.SETTINGS) },
                    onToggleRadio = { vm.toggleRadioOpen() }
                )
            }

            ActiveModal.DRIVING, ActiveModal.PAUSE, ActiveModal.PHOTO -> {
                DrivingHudOverlay(state = state, vm = vm)
            }

            ActiveModal.WORLD_MAP -> {
                YemenWorldMapScreen(
                    currentDistanceM = state.telemetry.distanceAlongRoadM,
                    visitedCities = state.saveState.visitedCities,
                    onFastTravel = { vm.fastTravelToCity(it) },
                    onClose = { vm.setModal(ActiveModal.DRIVING) }
                )
            }

            ActiveModal.GARAGE -> {
                GarageShowroomScreen(
                    saveState = state.saveState,
                    previewVehicleId = state.previewVehicleId,
                    onSelectPreview = { vm.selectPreviewVehicle(it) },
                    onOrbit = { vm.orbitShowroom(it) },
                    onConfirmEquip = { vm.confirmSelectVehicle(it) },
                    onUnlockVehicle = { vm.unlockVehicle(it) },
                    onUpdateCustomization = { id, c -> vm.updateVehicleCustomization(id, c) },
                    onUpgradePart = { id, part, cost -> vm.upgradeMechanicalPart(id, part, cost) },
                    onServiceAction = { type, id, cost -> vm.serviceAction(type, id, cost) },
                    onClose = { vm.setModal(ActiveModal.MENU) }
                )
            }

            ActiveModal.MISSIONS -> {
                MissionsAndStoryScreen(
                    saveState = state.saveState,
                    onStartMission = { vm.startMission(it) },
                    onStartFreeRoam = { vm.startFreeRoam() },
                    onClose = { vm.setModal(ActiveModal.MENU) }
                )
            }

            ActiveModal.CONTROL_STUDIO -> {
                ControlStudioScreen(
                    hudSettings = state.saveState.hudSettings,
                    onUpdateHud = { vm.updateHudSettings(it) },
                    onCalibrateGyro = { vm.gyroController.calibrateCenter() },
                    onClose = { vm.setModal(ActiveModal.MENU) }
                )
            }

            ActiveModal.SETTINGS -> {
                SettingsAndSaveScreen(
                    saveState = state.saveState,
                    dbStats = state.dbStats,
                    diagnostics = state.diagnosticResults,
                    onSetGraphics = { vm.setGraphicsQuality(it) },
                    onSetRealism = { vm.setDrivingRealism(it) },
                    onSetWeather = { vm.setWeather(it) },
                    onSetTimeOfDay = { vm.setTimeOfDay(it) },
                    onToggleAutoCycle = { vm.toggleAutoTimeCycle() },
                    onUpdateAudio = { vm.updateAudioMixer(it) },
                    onExportJson = { exportLauncher.launch("masalik_yemen_backup.masalik.json") },
                    onImportJson = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    onResetProgress = { vm.resetAllProgress() },
                    onRunDiagnostics = { vm.runDiagnostics() },
                    onClose = { vm.setModal(ActiveModal.MENU) }
                )
            }
        }

        // 3. Floating Yemeni Lo-Fi Radio Widget
        if (state.radioOpen) {
            YemeniRadioOverlay(
                radio = state.saveState.radioSettings,
                onTogglePlay = { vm.toggleRadioPlaying() },
                onSelectStation = { vm.selectRadioStation(it) },
                onVolumeChange = { vm.setRadioVolume(it) },
                onToggleVinyl = { vm.toggleRadioVinyl() },
                onClose = { vm.toggleRadioOpen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 54.dp, end = 14.dp)
            )
        }

        // 4. Transient Toast Pill
        state.toastMessageAr?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MasalikObsidian.copy(alpha = 0.94f))
                    .border(1.5.dp, MasalikAmber, RoundedCornerShape(14.dp))
                    .clickable { vm.dismissToast() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(msg, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
