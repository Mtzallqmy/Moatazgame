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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.AutomaticShifterState
import com.masalik.yemen.game.PhotoColorFilter
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.TrafficSignalPhase
import com.masalik.yemen.game.WeatherPreset
import com.masalik.yemen.ui.ActiveModal
import com.masalik.yemen.ui.MasalikGameViewModel
import com.masalik.yemen.ui.MasalikUiState
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
import kotlin.math.roundToInt

@Composable
fun DrivingHudOverlay(
    state: MasalikUiState,
    vm: MasalikGameViewModel
) {
    val t = state.telemetry
    val hud = state.saveState.hudSettings
    var toolsDrawerOpen by remember { mutableStateOf(false) }
    var minimapExpanded by remember { mutableStateOf(false) }
    var gasPressed by remember { mutableStateOf(false) }
    var brakePressed by remember { mutableStateOf(false) }
    var handbrakeOn by remember { mutableStateOf(false) }

    // Photo mode color filter tint
    val filterOverlayColor = when (state.photoFilter) {
        PhotoColorFilter.NATURAL -> Color.Transparent
        PhotoColorFilter.GOLDEN_HOUR -> Color(0x28F59E0B)
        PhotoColorFilter.WARM_COPPER -> Color(0x28C87236)
        PhotoColorFilter.HIGHLAND_MIST -> Color(0x2694A3B8)
        PhotoColorFilter.MONOCHROME -> Color(0x42111827)
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
                onClose = { vm.setModal(ActiveModal.DRIVING) }
            )
            return@Box
        }

        // Clean Cinematic Mode Toggle Button (always visible)
        if (state.cleanCinematicHud) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MasalikObsidian.copy(alpha = 0.72f))
                    .border(1.dp, MasalikAmber, CircleShape)
                    .clickable { vm.toggleCleanHud() }
                    .padding(10.dp)
                    .testTag("restore_hud_button")
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
            // Top Right (RTL Start): Pause, Camera, Map, Tools, Clean HUD + Mission Card
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    HudIconPill(Icons.Default.Pause, "إيقاف مؤقت", "hud_pause_button") { vm.setModal(ActiveModal.PAUSE) }
                    HudTextPill(state.cameraMode.labelAr, Icons.Default.CameraAlt, "hud_camera_button") { vm.cycleCameraMode() }
                    HudIconPill(Icons.Default.Map, "خريطة اليمن", "hud_map_button") { vm.setModal(ActiveModal.WORLD_MAP) }
                    HudIconPill(Icons.Default.Radio, "الراديو اليمني", "hud_radio_button", active = state.saveState.radioSettings.isPlaying) { vm.toggleRadioOpen() }
                    HudIconPill(Icons.Default.PhotoCamera, "وضع التصوير", "hud_photo_button") { vm.setModal(ActiveModal.PHOTO) }
                    HudIconPill(Icons.Default.Tune, "أدوات المركبة", "hud_tools_button", active = toolsDrawerOpen) { toolsDrawerOpen = !toolsDrawerOpen }
                    HudIconPill(Icons.Default.VisibilityOff, "إخفاء الواجهة", "hud_clean_button") { vm.toggleCleanHud() }
                }

                // Active Mission Card
                state.activeMission?.let { mission ->
                    val progress = ((t.distanceAlongRoadM - mission.startDistanceM) /
                        (mission.finishDistanceM - mission.startDistanceM).coerceAtLeast(10f)).coerceIn(0f, 1f)
                    val remainingM = (mission.finishDistanceM - t.distanceAlongRoadM).coerceAtLeast(0f).roundToInt()
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MasalikObsidian.copy(alpha = 0.84f))
                            .border(1.dp, MasalikAmber.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .width(235.dp)
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

            // Top Center: Region, 24h Clock, Live Traffic Signal Alert & Violations
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MasalikObsidian.copy(alpha = 0.84f))
                        .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(t.regionNameAr, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("·", color = MasalikMuted)
                    Text("${state.displayClockStr} (${state.activeTimePhase.labelAr})", color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    Text("·", color = MasalikMuted)
                    Text("${t.altitudeM.roundToInt()}م", color = MasalikSky, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
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
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(sigColor))
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

        // ==================== EXPANDABLE VEHICLE TOOLS DRAWER ====================
        if (toolsDrawerOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MasalikObsidian.copy(alpha = 0.92f))
                    .border(1.dp, MasalikAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("أنظمة المركبة والطقس", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("مصابيح", t.headlightsOn) { vm.toggleHeadlights() }
                    ToolChip("مسّاحات", t.wipersOn) { vm.toggleWipers() }
                    ToolChip("إشارة يمين", t.rightIndicatorOn) { vm.toggleRightIndicator() }
                    ToolChip("إشارة يسار", t.leftIndicatorOn) { vm.toggleLeftIndicator() }
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
                    ToolChip("تغيير الطقس", false) {
                        val all = WeatherPreset.entries
                        vm.setWeather(all[(all.indexOf(state.saveState.weather) + 1) % all.size])
                    }
                }
            }
        }

        // ==================== BOTTOM DRIVING CONTROLS ====================
        val steerWidget = hud.widgets[com.masalik.yemen.game.HudWidgetId.STEERING]
        val pedalWidget = hud.widgets[com.masalik.yemen.game.HudWidgetId.PEDALS]

        // Left side physically (in RTL: Alignment.BottomEnd) — Steering Wheel + Horn
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Horn button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MasalikObsidian.copy(alpha = 0.82f))
                    .border(1.5.dp, MasalikAmber.copy(alpha = 0.7f), CircleShape)
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
                scale = steerWidget?.scale ?: hud.buttonScale,
                opacity = steerWidget?.opacity ?: hud.hudOpacity,
                onSteerChanged = { vm.inputSteerNorm = it },
                onCalibrateGyro = { vm.gyroController.calibrateCenter() }
            )
        }

        // Center Bottom — Digital Speedometer, RPM bar, Gear, Fuel & Surface Telemetry
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(18.dp))
                .background(MasalikObsidian.copy(alpha = 0.86f))
                .border(1.dp, MasalikBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${t.speedKmh.roundToInt()}",
                        color = MasalikIvory,
                        fontFamily = ChakraPetchFontFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("كم/س", color = MasalikMuted, fontSize = 9.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MasalikAmber.copy(alpha = 0.20f))
                        .border(1.dp, MasalikAmber, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(t.gear, color = MasalikAmber, fontFamily = ChakraPetchFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("السطح: ${t.surface.labelAr}", color = MasalikSky, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val fuelPct = (t.fuelLiters / t.fuelTankCapacityL.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    Text(
                        "الوقود: ${(fuelPct * 100).roundToInt()}% · هيكل: ${t.damage.bodyHealthPct.roundToInt()}%",
                        color = if (t.lowFuelWarning) MasalikCrimson else MasalikEmerald,
                        fontSize = 10.sp
                    )
                    Text(
                        "مرور: ${t.activeTrafficCount} · مشاة: ${t.activePedestriansCount}",
                        color = MasalikMuted,
                        fontSize = 9.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val rpmFrac = (t.rpm / t.redlineRpm.coerceAtLeast(1000f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { rpmFrac },
                modifier = Modifier.width(210.dp).height(5.dp).clip(RoundedCornerShape(4.dp)),
                color = if (rpmFrac > 0.85f) MasalikCrimson else MasalikAmber,
                trackColor = MasalikBorder
            )
        }

        // Right side physically (in RTL: Alignment.BottomStart) — Shifter + Handbrake + Brake & Gas Pedals
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .alpha(pedalWidget?.opacity ?: hud.hudOpacity),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gas Pedal
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (gasPressed) MasalikEmerald else MasalikObsidian.copy(alpha = 0.84f))
                    .border(2.dp, MasalikEmerald, RoundedCornerShape(16.dp))
                    .testTag("gas_pedal")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            gasPressed = true
                            vm.inputThrottle = 1f
                            while (true) {
                                val ev = awaitPointerEvent()
                                val c = ev.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) {
                                    gasPressed = false
                                    vm.inputThrottle = 0f
                                    break
                                }
                                c.consume()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("بنزين", color = if (gasPressed) MasalikObsidian else MasalikEmerald, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("GAS", color = if (gasPressed) MasalikObsidian else MasalikMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
                }
            }

            // Brake Pedal
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (brakePressed) MasalikCrimson else MasalikObsidian.copy(alpha = 0.84f))
                    .border(2.dp, MasalikCrimson, RoundedCornerShape(16.dp))
                    .testTag("brake_pedal")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            brakePressed = true
                            vm.inputBrake = 1f
                            while (true) {
                                val ev = awaitPointerEvent()
                                val c = ev.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed) {
                                    brakePressed = false
                                    vm.inputBrake = 0f
                                    break
                                }
                                c.consume()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("فرامل", color = if (brakePressed) Color.White else MasalikCrimson, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("BRAKE", color = if (brakePressed) Color.White else MasalikMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
                }
            }

            // P/R/N/D Shifter + Handbrake column
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (t.transmissionMode == "auto") {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        AutomaticShifterState.entries.forEach { st ->
                            val selected = t.autoShifter == st
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MasalikAmber else MasalikObsidian.copy(alpha = 0.84f))
                                    .border(1.dp, if (selected) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(8.dp))
                                    .clickable { vm.setShifter(st) }
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
                        ToolChip("غيار -", false) { vm.manualGearDown() }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (handbrakeOn) MasalikCrimson else MasalikObsidian.copy(alpha = 0.84f))
                        .border(1.5.dp, if (handbrakeOn) Color.White else MasalikCrimson.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable {
                            handbrakeOn = !handbrakeOn
                            vm.inputHandbrake = handbrakeOn
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
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
                    .padding(top = 62.dp)
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
                    .background(MasalikObsidian.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MasalikSlate)
                        .border(1.dp, MasalikAmber.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("إيقاف مؤقت — مَسَالِك", color = MasalikAmberSoft, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Button(
                        onClick = { vm.setModal(ActiveModal.DRIVING) },
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("resume_button"),
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
                        PauseActionChip("الإعدادات", Icons.Default.Settings, Modifier.weight(1f)) { vm.setModal(ActiveModal.SETTINGS) }
                        PauseActionChip("القائمة الرئيسية", Icons.Default.Home, Modifier.weight(1f)) { vm.setModal(ActiveModal.MENU) }
                    }
                }
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
            ToolChip("عودة", true, onClose)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tag: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) MasalikAmber else MasalikObsidian.copy(alpha = 0.84f))
            .border(1.dp, if (active) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(11.dp))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MasalikObsidian.copy(alpha = 0.84f))
            .border(1.dp, MasalikBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MasalikAmber, modifier = Modifier.size(16.dp))
        Text(label, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun PauseActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
