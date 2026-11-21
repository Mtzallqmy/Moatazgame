package com.masalik.yemen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.AutomaticShifterState
import com.masalik.yemen.game.CameraMode
import com.masalik.yemen.game.HeadlightBeamMode
import com.masalik.yemen.game.HudWidgetId
import com.masalik.yemen.game.PhotoColorFilter
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.TimeOfDayPreset
import com.masalik.yemen.game.TrafficSignalPhase
import com.masalik.yemen.game.VehicleTelemetry
import com.masalik.yemen.game.WeatherPreset
import com.masalik.yemen.ui.ActiveModal
import com.masalik.yemen.ui.MasalikGameViewModel
import com.masalik.yemen.ui.MasalikUiState
import com.masalik.yemen.ui.components.RealisticDashboardCluster
import com.masalik.yemen.ui.components.SteeringControlWidget
import com.masalik.yemen.ui.theme.ChakraPetchFontFamily
import com.masalik.yemen.ui.theme.JetBrainsMonoFontFamily
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikAmberSoft
import com.masalik.yemen.ui.theme.MasalikBorder
import com.masalik.yemen.ui.theme.MasalikCrimson
import com.masalik.yemen.ui.theme.MasalikEmerald
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikMuted
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikSky
import com.masalik.yemen.ui.theme.MasalikSlate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DrivingHudOverlay(
    state: MasalikUiState,
    vm: MasalikGameViewModel,
    onCaptureScreenshot: () -> Unit = {}
) {
    val t = state.telemetry
    val hud = state.saveState.hudSettings
    val haptics = LocalHapticFeedback.current
    var toolsDrawerOpen by remember { mutableStateOf(false) }
    var cameraPickerOpen by remember { mutableStateOf(false) }
    var minimapExpanded by remember { mutableStateOf(false) }
    var gasLevel by remember { mutableFloatStateOf(0f) }
    var brakeLevel by remember { mutableFloatStateOf(0f) }
    var handbrakeOn by remember { mutableStateOf(false) }

    val filterOverlayColor = when (state.photoFilter) {
        PhotoColorFilter.NATURAL -> Color.Transparent
        PhotoColorFilter.GOLDEN_HOUR -> Color(0x28F59E0B)
        PhotoColorFilter.WARM_COPPER -> Color(0x28C87236)
        PhotoColorFilter.HIGHLAND_MIST -> Color(0x2694A3B8)
        PhotoColorFilter.MONOCHROME -> Color(0x42111827)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Live 360° Touch-Drag Orbit Surface for Cinematic Third-Person Orbit View (sits behind HUD buttons & pedals)
        if ((state.cameraMode == CameraMode.CINEMATIC_ORBIT || state.cameraMode == CameraMode.FREE_PHOTO) &&
            state.activeModal == ActiveModal.DRIVING
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.cameraMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var lastX = down.position.x
                            var lastY = down.position.y
                            while (true) {
                                val ev = awaitPointerEvent()
                                val c = ev.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) break
                                if (!c.isConsumed) {
                                    val dx = c.position.x - lastX
                                    val dy = c.position.y - lastY
                                    lastX = c.position.x
                                    lastY = c.position.y
                                    if (state.cameraMode == CameraMode.CINEMATIC_ORBIT) {
                                        vm.orbitDrivingCamera(dx * 0.0085f, dy * 0.0055f)
                                    } else {
                                        vm.orbitShowroom(dx * 0.009f)
                                    }
                                    c.consume()
                                }
                            }
                        }
                    }
            )
        }

        // Cockpit 3D Windshield Raindrops + Live Wiper Sweeping Layer (under HUD controls)
        val isRaining = state.saveState.weather == WeatherPreset.LIGHT_RAIN || state.saveState.weather == WeatherPreset.HEAVY_RAIN
        if (state.cameraMode == CameraMode.COCKPIT && isRaining && state.activeModal == ActiveModal.DRIVING) {
            CockpitRainOverlay(
                heavyRain = state.saveState.weather == WeatherPreset.HEAVY_RAIN,
                wipersOn = t.wipersOn,
                wiperPhaseRad = t.wiperPhaseRad,
                speedKmh = t.speedKmh
            )
        }

        // Speed Radar (ساهر) Strobe White Camera Flash Overlay
        if (state.radarFlashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = (state.radarFlashAlpha * 0.85f).coerceIn(0f, 0.85f)))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(filterOverlayColor)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Photo mode minimal UI
            if (state.activeModal == ActiveModal.PHOTO) {
                PhotoModeOverlay(
                    state = state,
                    onOrbit = { vm.orbitShowroom(it) },
                    onFilter = { vm.setPhotoFilter(it) },
                    onFov = { vm.setPhotoFov(it) },
                    onCaptureScreenshot = onCaptureScreenshot,
                    onClose = { vm.setModal(ActiveModal.DRIVING) }
                )
                return@Box
            }

        // Clean Cinematic Mode Toggle Button
        if (state.cleanCinematicHud) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MasalikObsidian.copy(alpha = 0.76f))
                    .border(1.2.dp, MasalikAmber, CircleShape)
                    .clickable { vm.toggleCleanHud() }
                    .testTag("restore_hud_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "إظهار لوحة القيادة", tint = MasalikAmber)
            }
            return@Box
        }

        // ==================== TOP BAR ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top Right (RTL Start): Pause, Camera, Quick Day/Night, Headlights, Wheel Style, Map, Tools
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudIconPill(Icons.Default.Pause, "إيقاف مؤقت", "hud_pause_button") { vm.setModal(ActiveModal.PAUSE) }
                    HudTextPill(state.cameraMode.labelAr, Icons.Default.CameraAlt, "hud_camera_button") { vm.cycleCameraMode() }

                    // 1-Tap Day / Sunset / Night Cycle Pill
                    val timeIcon = when (state.activeTimePhase) {
                        TimeOfDayPreset.NIGHT -> Icons.Default.NightsStay
                        TimeOfDayPreset.SUNSET, TimeOfDayPreset.DAWN, TimeOfDayPreset.SUNRISE -> Icons.Default.WbTwilight
                        else -> Icons.Default.WbSunny
                    }
                    HudTextPill(
                        label = state.activeTimePhase.labelAr,
                        icon = timeIcon,
                        tag = "hud_day_night_button",
                        accentTint = if (state.activeTimePhase == TimeOfDayPreset.NIGHT) MasalikSky else MasalikAmber
                    ) { vm.cycleQuickDayNight() }

                    // 1-Tap 3-Stage Headlights Pill (OFF -> LOW -> HIGH LED)
                    val beamTint = when (t.headlightBeamMode) {
                        HeadlightBeamMode.HIGH_BEAM -> MasalikSky
                        HeadlightBeamMode.LOW_BEAM -> MasalikAmber
                        HeadlightBeamMode.OFF -> MasalikMuted
                    }
                    HudIconPill(
                        icon = Icons.Default.Highlight,
                        desc = "المصابيح الأمامية: ${t.headlightBeamMode.labelAr}",
                        tag = "hud_headlights_button",
                        active = t.headlightBeamMode != HeadlightBeamMode.OFF,
                        activeColor = beamTint
                    ) { vm.toggleHeadlights() }

                    // 1-Tap Steering Style Switcher Pill (Gulf / European / JDM / Classic)
                    HudTextPill(
                        label = hud.smartWheel.wheelStyle.shortLabelAr,
                        icon = Icons.Default.DirectionsCar,
                        tag = "hud_wheel_style_button"
                    ) { vm.cycleSteeringWheelStyle() }

                    HudIconPill(Icons.Default.Map, "خريطة اليمن", "hud_map_button") { vm.setModal(ActiveModal.WORLD_MAP) }
                    HudIconPill(Icons.Default.Radio, "الراديو اليمني", "hud_radio_button", active = state.saveState.radioSettings.isPlaying) { vm.toggleRadioOpen() }
                    HudIconPill(Icons.Default.SlowMotionVideo, "إعادة سينمائية 20ث", "hud_replay_button") { vm.enterReplayMode() }
                    HudIconPill(Icons.Default.PhotoCamera, "وضع التصوير", "hud_photo_button") { vm.setModal(ActiveModal.PHOTO) }
                    HudIconPill(Icons.Default.Tune, "أدوات المركبة", "hud_tools_button", active = toolsDrawerOpen) { toolsDrawerOpen = !toolsDrawerOpen }
                    HudIconPill(Icons.Default.VisibilityOff, "إخفاء الواجهة", "hud_clean_button") { vm.toggleCleanHud() }
                }

                // Active Mission Progress Card
                state.activeMission?.let { mission ->
                    val progress = ((t.distanceAlongRoadM - mission.startDistanceM) /
                        (mission.finishDistanceM - mission.startDistanceM).coerceAtLeast(10f)).coerceIn(0f, 1f)
                    val remainingM = (mission.finishDistanceM - t.distanceAlongRoadM).coerceAtLeast(0f).roundToInt()
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MasalikObsidian.copy(alpha = 0.85f))
                            .border(1.dp, MasalikAmber.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .width(240.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mission.titleAr, color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${state.missionTimeRemainingSec}ث", color = MasalikSky, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height(4.dp).clip(RoundedCornerShape(4.dp)),
                            color = MasalikAmber,
                            trackColor = MasalikBorder
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المتبقي: ${remainingM}م", color = MasalikIvory, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
                            if (mission.hasAiRival) {
                                Text("المركز: #${t.playerRaceRank}", color = MasalikEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else if (mission.cargoRequiredPct > 0) {
                                Text("سلامة الحمولة: ${t.damage.cargoIntegrityPct.roundToInt()}%", color = MasalikEmerald, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Top Center: Region, Live 24h Clock, Altitude, Traffic Signal & Service Prompts
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MasalikObsidian.copy(alpha = 0.84f))
                        .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(t.regionNameAr, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("·", color = MasalikMuted)
                    Text(state.displayClockStr, color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("·", color = MasalikMuted)
                    Text("${t.altitudeM.roundToInt()}م", color = MasalikSky, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
                    Text("·", color = MasalikMuted)
                    Text("${state.fps} FPS", color = MasalikEmerald, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
                }

                // Live Traffic Signal & Pedestrian Crosswalk Alert
                state.nearbySignal?.let { sig ->
                    val sigColor = when (sig.phase) {
                        TrafficSignalPhase.RED -> MasalikCrimson
                        TrafficSignalPhase.YELLOW -> MasalikAmber
                        TrafficSignalPhase.GREEN -> MasalikEmerald
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MasalikObsidian.copy(alpha = 0.90f))
                            .border(1.5.dp, sigColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.size(11.dp).clip(CircleShape).background(sigColor))
                        Text(
                            "${sig.nameAr} (${sig.distanceAheadM.roundToInt()}م) · ${sig.phase.labelAr} (${sig.secondsRemaining}ث)",
                            color = MasalikIvory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (sig.pedestriansCrossing) {
                            Text("عبور مشاة!", color = MasalikAmber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Speed Radar (رادار ساهر) Upcoming Speed Limit Sign Alert
                state.nearbyRadar?.let { rad ->
                    val overspeed = t.speedKmh > rad.speedLimitKmh + 4f
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MasalikObsidian.copy(alpha = 0.92f))
                            .border(1.5.dp, if (overspeed) MasalikCrimson else MasalikAmber, RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, MasalikCrimson, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${rad.speedLimitKmh}",
                                color = Color.Black,
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            "${rad.nameAr} (${rad.distanceAheadM.roundToInt()}م) ${if (overspeed) "· خفف السرعة!" else "· رصد آلي"}",
                            color = if (overspeed) MasalikCrimson else MasalikAmberSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Red-light traffic violation warning
                t.trafficViolationWarningAr?.let { warn ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MasalikCrimson.copy(alpha = 0.92f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(warn, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Service Station Nearby Action Prompt
                if (t.nearServiceStation != null) {
                    val actionLabel = when (t.nearServiceStation) {
                        "fuel" -> "تعبئة وقود مجانية — ${t.nearServiceStationNameAr}"
                        "workshop" -> "إصلاح وصيانة فورية — ${t.nearServiceStationNameAr}"
                        else -> "غسيل وتلميع المركبة — ${t.nearServiceStationNameAr}"
                    }
                    Button(
                        onClick = { vm.serviceAction(t.nearServiceStation) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MasalikEmerald, contentColor = MasalikObsidian),
                        modifier = Modifier.height(34.dp).testTag("service_station_action")
                    ) {
                        Icon(
                            if (t.nearServiceStation == "fuel") Icons.Default.LocalGasStation else Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Top Left (RTL End): Expandable GPS Minimap
            if (hud.showMinimap) {
                GpsMinimapCard(
                    distanceM = t.distanceAlongRoadM,
                    expanded = minimapExpanded,
                    onToggleExpand = { minimapExpanded = !minimapExpanded }
                )
            }
        }

        // ==================== EXPANDABLE VEHICLE & COCKPIT TOOLS DRAWER ====================
        if (toolsDrawerOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MasalikObsidian.copy(alpha = 0.94f))
                    .border(1.dp, MasalikAmber.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("أنظمة المركبة والمقصورة", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("مصابيح: ${t.headlightBeamMode.labelAr}", t.headlightsOn) { vm.toggleHeadlights() }
                    ToolChip("مسّاحات", t.wipersOn) { vm.toggleWipers() }
                    ToolChip("إشارة يمين", t.rightIndicatorOn) { vm.toggleRightIndicator() }
                    ToolChip("إشارة يسار", t.leftIndicatorOn) { vm.toggleLeftIndicator() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("مقود: ${hud.smartWheel.seatSide.shortLabelAr}", true) { vm.toggleDriverSeatSide() }
                    ToolChip("طراز: ${hud.smartWheel.wheelStyle.shortLabelAr}", true) { vm.cycleSteeringWheelStyle() }
                    ToolChip("مرآة خلفية", hud.rearMirrorEnabled) { vm.toggleRearMirror() }
                    ToolChip("الوقت: ${state.activeTimePhase.labelAr}", false) { vm.cycleQuickDayNight() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("دفع ${t.drivetrainMode.label}", true) { vm.cycleDrivetrain() }
                    ToolChip("قفل دفرنس", t.diffLockActive) { vm.toggleDiffLock() }
                    ToolChip("مثبّت سرعة", t.cruiseControlEnabled) { vm.toggleCruiseControl() }
                    ToolChip("المحرك", t.engineRunning) { vm.toggleEngine() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("ABS", t.assists.absEnabled) { vm.toggleAssist("abs") }
                    ToolChip("TCS", t.assists.tcsEnabled) { vm.toggleAssist("tcs") }
                    ToolChip("ESC", t.assists.escEnabled) { vm.toggleAssist("esc") }
                    ToolChip("HDC", t.assists.hdcEnabled) { vm.toggleAssist("hdc") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("إعادة للطريق", false) { vm.recoverVehicleOnRoad() }
                    ToolChip("قير ${if (t.transmissionMode == "auto") "تلقائي" else "يدوي"}", t.transmissionMode == "manual") { vm.toggleTransmissionMode() }
                    ToolChip("الطقس: ${state.saveState.weather.labelAr}", false) {
                        val all = WeatherPreset.entries
                        vm.setWeather(all[(all.indexOf(state.saveState.weather) + 1) % all.size])
                    }
                }
            }
        }

        // ==================== DEDICATED CAMERA TOGGLE & VIEW SWITCHER ====================
        CameraViewSwitcherPanel(
            currentMode = state.cameraMode,
            expanded = cameraPickerOpen,
            onQuickToggle = {
                if (hud.smartWheel.hapticFeedback) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                vm.toggleImmersiveCameraView()
            },
            onToggleExpand = { cameraPickerOpen = !cameraPickerOpen },
            onSelectMode = { mode ->
                if (hud.smartWheel.hapticFeedback) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                vm.setCameraMode(mode)
                cameraPickerOpen = false
            },
            onResetOrbit = { vm.resetDrivingOrbitCamera() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 44.dp)
        )

        // ==================== BOTTOM DRIVING CONTROLS ====================
        val steerWidget = hud.widgets[HudWidgetId.STEERING]
        val pedalWidget = hud.widgets[HudWidgetId.PEDALS]
        val steerAlignment = if (hud.mirrorControls) Alignment.BottomStart else Alignment.BottomEnd
        val pedalAlignment = if (hud.mirrorControls) Alignment.BottomEnd else Alignment.BottomStart

        // Steering Group (Physically Left by default; swaps when mirrored)
        Row(
            modifier = Modifier.align(steerAlignment),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tactile Horn Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MasalikObsidian.copy(alpha = 0.85f))
                    .border(1.5.dp, MasalikAmber.copy(alpha = 0.72f), CircleShape)
                    .testTag("horn_button")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            vm.setHornActive(true)
                            while (true) {
                                val ev = awaitPointerEvent()
                                val c = ev.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) { vm.setHornActive(false); break }
                                c.consume()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "البوق", tint = MasalikAmber, modifier = Modifier.size(22.dp))
            }

            SteeringControlWidget(
                controlMode = hud.controlMode,
                settings = hud.smartWheel,
                currentWheelDeg = t.steeringWheelAngleDeg,
                speedKmh = t.speedKmh,
                scale = steerWidget?.scale ?: hud.buttonScale,
                opacity = steerWidget?.opacity ?: hud.hudOpacity,
                onSteerChanged = { vm.inputSteerNorm = it },
                onCalibrateGyro = { vm.gyroController.calibrateCenter() },
                onShiftUp = { vm.manualGearUp() },
                onShiftDown = { vm.manualGearDown() }
            )
        }

        // Center Bottom — Realistic Hybrid Digital/Analog Speedometer & Tachometer Dashboard
        RealisticDashboardCluster(
            telemetry = t,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Pedals & Shifter Group (Physically Right by default; swaps when mirrored)
        Row(
            modifier = Modifier
                .align(pedalAlignment)
                .alpha(pedalWidget?.opacity ?: hud.hudOpacity),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pedalScale = (pedalWidget?.scale ?: hud.buttonScale).coerceIn(0.85f, 1.3f)

            // Progressive Analog Gas Pedal (Vertical slide modulates 28%..100% throttle!)
            AnalogAluminumPedal(
                titleAr = "بنزين",
                codeEn = "GAS",
                level = gasLevel,
                accentColor = MasalikEmerald,
                width = (62f * pedalScale).dp,
                height = (118f * pedalScale).dp,
                tag = "gas_pedal",
                hapticEnabled = hud.smartWheel.hapticFeedback,
                onLevelChanged = { lv ->
                    if (lv > 0f && gasLevel == 0f && hud.smartWheel.hapticFeedback) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    gasLevel = lv
                    vm.inputThrottle = lv
                }
            )

            // Progressive Analog Brake Pedal (Vertical slide modulates 30%..100% braking!)
            AnalogAluminumPedal(
                titleAr = "فرامل",
                codeEn = "BRAKE",
                level = brakeLevel,
                accentColor = MasalikCrimson,
                width = (58f * pedalScale).dp,
                height = (88f * pedalScale).dp,
                tag = "brake_pedal",
                hapticEnabled = hud.smartWheel.hapticFeedback,
                onLevelChanged = { lv ->
                    if (lv > 0f && brakeLevel == 0f && hud.smartWheel.hapticFeedback) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    brakeLevel = lv
                    vm.inputBrake = lv
                }
            )

            // P/R/N/D Shifter + Handbrake column
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (t.transmissionMode == "auto") {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        AutomaticShifterState.entries.forEach { st ->
                            val selected = t.autoShifter == st
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MasalikAmber else MasalikObsidian.copy(alpha = 0.86f))
                                    .border(1.dp, if (selected) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (hud.smartWheel.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        vm.setShifter(st)
                                    }
                                    .testTag("shifter_${st.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(st.label, color = if (selected) MasalikObsidian else MasalikIvory, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ToolChip("غيار +", false) { vm.manualGearUp() }
                        ToolChip("غيار −", false) { vm.manualGearDown() }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (handbrakeOn) MasalikCrimson else MasalikObsidian.copy(alpha = 0.86f))
                        .border(1.5.dp, if (handbrakeOn) Color.White else MasalikCrimson.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                        .clickable {
                            handbrakeOn = !handbrakeOn
                            vm.inputHandbrake = handbrakeOn
                            if (hud.smartWheel.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("handbrake_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (handbrakeOn) "جلنط (مفعّل)" else "جلنط اليد (P)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mission Completion / Dialogue Banner
        state.missionBannerAr?.let { msg ->
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MasalikObsidian.copy(alpha = 0.94f))
                    .border(1.5.dp, MasalikAmber, RoundedCornerShape(14.dp))
                    .clickable { vm.dismissMissionBanner() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = MasalikAmber)
                Text(msg, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Pause Overlay
        if (state.activeModal == ActiveModal.PAUSE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MasalikObsidian.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MasalikSlate)
                        .border(1.dp, MasalikAmber.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("إيقاف مؤقت — مَسَالِك", color = MasalikAmberSoft, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Button(
                        onClick = { vm.setModal(ActiveModal.DRIVING) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("استئناف القيادة", fontWeight = FontWeight.Black)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseActionChip("خريطة اليمن", Icons.Default.Map, Modifier.weight(1f)) { vm.setModal(ActiveModal.WORLD_MAP) }
                        PauseActionChip("الكراج 3D", Icons.Default.DirectionsCar, Modifier.weight(1f)) { vm.setModal(ActiveModal.GARAGE) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseActionChip("المهمات", Icons.Default.Flag, Modifier.weight(1f)) { vm.setModal(ActiveModal.MISSIONS) }
                        PauseActionChip("التحكم", Icons.Default.Tune, Modifier.weight(1f)) { vm.setModal(ActiveModal.CONTROL_STUDIO) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseActionChip("إعادة 20ث", Icons.Default.SlowMotionVideo, Modifier.weight(1f)) { vm.enterReplayMode() }
                        PauseActionChip("الإعدادات", Icons.Default.Settings, Modifier.weight(1f)) { vm.setModal(ActiveModal.SETTINGS) }
                        PauseActionChip("الرئيسية", Icons.Default.Home, Modifier.weight(1f)) { vm.setModal(ActiveModal.MENU) }
                    }
                }
            }
        }
        }
    }
}

/**
 * Progressive CNC-Aluminum Gas/Brake Pedal:
 * Touching and sliding upward modulates pedal pressure smoothly from 30% to 100%,
 * giving realistic city-cruising modulation as well as full-throttle highway launches.
 */
@Composable
private fun AnalogAluminumPedal(
    titleAr: String,
    codeEn: String,
    level: Float,
    accentColor: Color,
    width: Dp,
    height: Dp,
    tag: String,
    hapticEnabled: Boolean,
    onLevelChanged: (Float) -> Unit
) {
    val active = level > 0.01f
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE81C2434), Color(0xF00B101A))
                )
            )
            .border(
                width = if (active) 2.2.dp else 1.5.dp,
                color = if (active) accentColor else accentColor.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag(tag)
            .pointerInput(hapticEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun calcLevel(yPx: Float): Float {
                        val fracFromBottom = (1f - (yPx / size.height.coerceAtLeast(1))).coerceIn(0f, 1f)
                        return (0.35f + fracFromBottom * 0.65f).coerceIn(0.35f, 1f)
                    }
                    onLevelChanged(calcLevel(down.position.y))
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) {
                            onLevelChanged(0f)
                            break
                        }
                        onLevelChanged(calcLevel(c.position.y))
                        c.consume()
                    }
                }
            }
    ) {
        // Live upward pressure glow fill bar
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(level.coerceIn(0.15f, 1f))
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.78f), accentColor.copy(alpha = 0.36f))
                        )
                    )
            )
        }

        // Machined aluminum anti-slip traction grooves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grooves = 5
            for (i in 1..grooves) {
                val y = size.height * (0.16f + i * 0.13f)
                drawLine(
                    color = if (active) Color(0x55FFFFFF) else Color(0x30CBD5E1),
                    start = Offset(size.width * 0.22f, y),
                    end = Offset(size.width * 0.78f, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleAr,
                color = if (active) Color.White else accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
            Text(
                text = if (active) "${(level * 100).roundToInt()}%" else codeEn,
                color = if (active) Color.White else MasalikMuted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
private fun CockpitRainOverlay(
    heavyRain: Boolean,
    wipersOn: Boolean,
    wiperPhaseRad: Float,
    speedKmh: Float
) {
    var animTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(33L)
            animTick++
        }
    }
    val dropCount = if (heavyRain) 62 else 36
    val seeds = remember(dropCount) {
        List(dropCount) { i ->
            val h1 = ((i * 73856093) xor 0x5A17) and 0xFFFF
            val h2 = ((i * 19349663) xor 0x2B91) and 0xFFFF
            val h3 = ((i * 83492791) xor 0x1E3D) and 0xFFFF
            Triple((h1 % 1000) / 1000f, (h2 % 1000) / 1000f, 0.55f + (h3 % 100) / 120f)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tSec = animTick * 0.033f
        val speedStretch = 1f + (speedKmh / 95f).coerceIn(0f, 1.8f)

        // Two wiper pivot points along lower windshield edge
        val p1 = Offset(w * 0.30f, h * 0.96f)
        val p2 = Offset(w * 0.68f, h * 0.96f)
        val bladeLen = h * 0.72f
        val sweepNorm = if (wipersOn) (0.5f + 0.5f * sin(wiperPhaseRad)) else 0.05f
        val bladeAngle = (-PI * 0.86 + sweepNorm * PI * 0.66).toFloat()

        for ((sx, sy, sz) in seeds) {
            val dx = sx * w
            val dy = ((sy + tSec * (0.14f * sz * speedStretch)) % 1.0f) * h

            // Check if this raindrop sits inside the freshly-wiped arc when wipers are running
            var wipedClear = false
            if (wipersOn) {
                for (pivot in listOf(p1, p2)) {
                    val vx = dx - pivot.x
                    val vy = dy - pivot.y
                    val dist = kotlin.math.hypot(vx.toDouble(), vy.toDouble()).toFloat()
                    if (dist < bladeLen && dy < pivot.y) {
                        val ang = atan2(vy.toDouble(), vx.toDouble()).toFloat()
                        if (abs(ang - bladeAngle) < 0.38f) {
                            wipedClear = true
                            break
                        }
                    }
                }
            }
            if (wipedClear) continue

            val radius = (2.2f + sz * 2.6f).dp.toPx()
            val streakLen = radius * (2.0f + speedStretch * 1.8f)
            // Translucent water bead + bright specular highlight
            drawLine(
                color = Color(0x55CBE7FF),
                start = Offset(dx, dy - streakLen),
                end = Offset(dx, dy),
                strokeWidth = radius * 0.95f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0x88E8F6FF),
                radius = radius * 0.78f,
                center = Offset(dx, dy)
            )
            drawCircle(
                color = Color(0xCCFFFFFF),
                radius = radius * 0.28f,
                center = Offset(dx - radius * 0.22f, dy - radius * 0.22f)
            )
        }

        // Draw animated realistic rubber windshield wiper blades when active
        if (wipersOn) {
            for (pivot in listOf(p1, p2)) {
                val tip = Offset(
                    pivot.x + cos(bladeAngle) * bladeLen,
                    pivot.y + sin(bladeAngle) * bladeLen
                )
                drawLine(
                    color = Color(0xD90E131D),
                    start = pivot,
                    end = tip,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0x6694A3B8),
                    start = pivot,
                    end = tip,
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun PhotoModeOverlay(
    state: MasalikUiState,
    onOrbit: (Float) -> Unit,
    onFilter: (PhotoColorFilter) -> Unit,
    onFov: (Float) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastX = down.position.x
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) break
                        val dx = c.position.x - lastX
                        lastX = c.position.x
                        onOrbit(dx * 0.01f)
                        c.consume()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(18.dp))
                .background(MasalikObsidian.copy(alpha = 0.88f))
                .border(1.dp, MasalikAmber.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("وضع التصوير 360°", color = MasalikAmberSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            PhotoColorFilter.entries.forEach { f ->
                ToolChip(f.labelAr, state.photoFilter == f) { onFilter(f) }
            }
            Text("عدسة ${state.photoFovDeg.roundToInt()}°", color = MasalikSky, fontSize = 11.sp)
            Slider(
                value = state.photoFovDeg,
                onValueChange = onFov,
                valueRange = 34f..78f,
                modifier = Modifier.width(100.dp),
                colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
            )
            ToolChip("حفظ HD", true, onCaptureScreenshot)
            ToolChip("عودة", false, onClose)
        }
    }
}

@Composable
private fun GpsMinimapCard(distanceM: Float, expanded: Boolean, onToggleExpand: () -> Unit) {
    val sizeDp = if (expanded) 160.dp else 98.dp
    Column(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(MasalikObsidian.copy(alpha = 0.85f))
            .border(1.5.dp, MasalikAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .padding(6.dp)
            .testTag("gps_minimap")
    ) {
        Text(
            "GPS · ${"%.1f".format(distanceM / 1000f)} كم",
            color = MasalikAmberSoft,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerSample = RoadSplineNetwork.sampleAt(distanceM)
            val scale = if (expanded) 0.22f else 0.34f
            val path = Path()
            var first = true
            var d = (distanceM - 160f).coerceAtLeast(0f)
            val maxD = (distanceM + 260f).coerceAtMost(RoadSplineNetwork.TOTAL_ROAD_LENGTH_M)
            while (d <= maxD) {
                val s = RoadSplineNetwork.sampleAt(d)
                val px = w * 0.5f + (s.x - centerSample.x) * scale
                val py = h * 0.65f - (s.z - centerSample.z) * scale
                if (first) { path.moveTo(px, py); first = false } else path.lineTo(px, py)
                d += 15f
            }
            drawPath(path, color = MasalikAmber, style = Stroke(width = 4f))
            drawCircle(color = MasalikSky, radius = 6f, center = Offset(w * 0.5f, h * 0.65f))
        }
    }
}

@Composable
private fun HudIconPill(
    icon: ImageVector,
    desc: String,
    tag: String,
    active: Boolean = false,
    activeColor: Color = MasalikAmber,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) activeColor else MasalikObsidian.copy(alpha = 0.84f))
            .border(1.dp, if (active) Color.White.copy(alpha = 0.7f) else MasalikBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = if (active) MasalikObsidian else MasalikIvory, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HudTextPill(
    label: String,
    icon: ImageVector,
    tag: String,
    accentTint: Color = MasalikAmber,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MasalikObsidian.copy(alpha = 0.84f))
            .border(1.dp, MasalikBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accentTint, modifier = Modifier.size(15.dp))
        Text(label, color = MasalikIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) MasalikAmber else MasalikSlate)
            .border(1.dp, if (active) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (active) MasalikObsidian else MasalikIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CameraViewSwitcherPanel(
    currentMode: CameraMode,
    expanded: Boolean,
    onQuickToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onSelectMode: (CameraMode) -> Unit,
    onResetOrbit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIcon = when (currentMode) {
        CameraMode.CINEMATIC_ORBIT, CameraMode.FREE_PHOTO -> Icons.Default.Autorenew
        CameraMode.COCKPIT -> Icons.Default.DirectionsCar
        else -> Icons.Default.Videocam
    }
    val selectableModes = remember {
        listOf(
            CameraMode.COCKPIT,
            CameraMode.HOOD,
            CameraMode.CINEMATIC_ORBIT,
            CameraMode.CHASE_NEAR,
            CameraMode.CHASE_FAR,
            CameraMode.CINEMATIC_FRONT
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Expandable Direct View Selector Drawer
            if (expanded) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MasalikObsidian.copy(alpha = 0.92f))
                        .border(1.2.dp, MasalikAmber.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectableModes.forEach { mode ->
                        val selected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MasalikAmber else MasalikSlate)
                                .border(
                                    1.dp,
                                    if (selected) MasalikAmberSoft else MasalikBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectMode(mode) }
                                .padding(horizontal = 9.dp, vertical = 8.dp)
                                .testTag("camera_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.labelAr,
                                color = if (selected) MasalikObsidian else MasalikIvory,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Secondary Expand/Collapse All-Views Button (48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (expanded) MasalikAmber else MasalikObsidian.copy(alpha = 0.86f))
                    .border(
                        1.2.dp,
                        if (expanded) Color.White.copy(alpha = 0.7f) else MasalikBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onToggleExpand)
                    .testTag("camera_picker_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "قائمة زوايا الكاميرا",
                    tint = if (expanded) MasalikObsidian else MasalikAmberSoft,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Primary Camera Toggle Button: 1-tap switches between Cockpit, Hood, and Cinematic 360° Orbit!
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(MasalikObsidian.copy(alpha = 0.92f), MasalikSlate.copy(alpha = 0.94f))
                        )
                    )
                    .border(1.5.dp, MasalikAmber, RoundedCornerShape(14.dp))
                    .clickable(onClick = onQuickToggle)
                    .padding(horizontal = 12.dp)
                    .testTag("camera_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = "تبديل منظور الكاميرا",
                    tint = MasalikAmber,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = currentMode.labelAr,
                        color = MasalikIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 12.sp
                    )
                    Text(
                        text = "تبديل الكاميرا",
                        color = MasalikAmberSoft,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        // Live 360° Orbit Controls Pill when Cinematic Orbit view is active
        if (currentMode == CameraMode.CINEMATIC_ORBIT) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MasalikObsidian.copy(alpha = 0.86f))
                    .border(1.dp, MasalikSky.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onResetOrbit)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("reset_orbit_camera_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة زاوية المدار",
                    tint = MasalikSky,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "اسحب للدوران 360° · إعادة الزاوية",
                    color = MasalikSky,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PauseActionChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MasalikObsidian)
            .border(1.dp, MasalikBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = MasalikAmber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
