package com.masalik.yemen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.AutomaticShifterState
import com.masalik.yemen.game.HeadlightBeamMode
import com.masalik.yemen.game.VehicleTelemetry
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DashboardClusterMode(val labelAr: String, val tag: String) {
    HYBRID("هجين 3D", "HYBRID"),
    DIGITAL_TURBO("توربو رقمي", "DIGITAL"),
    ANALOG_CLASSIC("كلاسيكي", "ANALOG")
}

/**
 * Realistic Hybrid Digital & Analog Automotive Instrument Cluster.
 *
 * Driven in real time by [VehicleTelemetry]:
 * - Smooth spring-damped analog needles for Speedometer (0–240 km/h) and Tachometer (0–9 x1000 RPM)
 * - High-RPM redline needle micro-tremor & progressive shift-light alert
 * - Central OLED TFT telemetry display (Speed, Gear, Boost PSI, Odometer, Surface & Drift angle)
 * - Live blinking turn indicators, headlights, parking brake, low-fuel, ABS/TCS/ESC & drivetrain telltales
 * - 1-tap mode switch between Hybrid Cockpit, Digital Turbo, and Classic Heritage Analog
 */
@Composable
fun RealisticDashboardCluster(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier,
    initialMode: DashboardClusterMode = DashboardClusterMode.HYBRID
) {
    val haptics = LocalHapticFeedback.current
    var clusterMode by rememberSaveable { mutableStateOf(initialMode) }

    // Smooth 60fps needle & numeric interpolation over the 20Hz simulation telemetry stream
    val smoothSpeedKmh by animateFloatAsState(
        targetValue = telemetry.speedKmh.coerceIn(0f, 240f),
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 460f),
        label = "speedNeedle"
    )
    val smoothRpm by animateFloatAsState(
        targetValue = telemetry.rpm.coerceAtLeast(0f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
        label = "rpmNeedle"
    )
    val smoothBoostPsi by animateFloatAsState(
        targetValue = telemetry.turboBoostPsi.coerceIn(0f, 22f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "boostGauge"
    )

    val redlineRpm = telemetry.redlineRpm.coerceAtLeast(4500f)
    val rpmFrac = (smoothRpm / redlineRpm).coerceIn(0f, 1.06f)
    val maxScaleRpm = (ceil(redlineRpm / 1000f) + 1f).coerceIn(7f, 9f) * 1000f
    val fuelPct = (telemetry.fuelLiters / telemetry.fuelTankCapacityL.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val engineHealthNorm = (telemetry.damage.engineHealthPct / 100f).coerceIn(0f, 1f)
    val targetCoolantNorm = if (!telemetry.engineRunning) {
        0.18f
    } else {
        (0.42f + rpmFrac * 0.22f + (1f - engineHealthNorm) * 0.34f).coerceIn(0.25f, 0.98f)
    }
    val coolantNorm by animateFloatAsState(
        targetValue = targetCoolantNorm,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "coolantTemp"
    )

    // Blinking turn-signal & redline shift-flash clock
    var blinkOn by remember { mutableStateOf(false) }
    val anyIndicator = telemetry.leftIndicatorOn || telemetry.rightIndicatorOn || telemetry.lowFuelWarning
    LaunchedEffect(anyIndicator) {
        if (!anyIndicator) {
            blinkOn = false
        } else {
            while (true) {
                blinkOn = !blinkOn
                delay(420L)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "clusterPulse")
    val redlinePulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 160),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redlinePulse"
    )
    val needleTremorPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 55),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needleTremor"
    )

    val isNearRedline = telemetry.engineRunning && rpmFrac >= 0.87f
    val activeNeedleTremor = if (isNearRedline) needleTremorPhase * 0.75f else 0f
    val isDrifting = abs(telemetry.driftSlipAngleDeg) > 4.2f && telemetry.speedKmh > 18f

    val housingBorderColor by animateColorAsState(
        targetValue = when {
            isNearRedline && redlinePulse > 0.65f -> MasalikCrimson
            isDrifting -> MasalikAmber
            clusterMode == DashboardClusterMode.ANALOG_CLASSIC -> Color(0xFFB4833E)
            else -> Color(0xFF334155)
        },
        animationSpec = tween(160),
        label = "bezelBorder"
    )

    val textMeasurer = rememberTextMeasurer()

    // Lock internal horizontal gauge layout to physical Left-to-Right so Speedometer, Turn Arrows & RPM match real dashboards
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .widthIn(min = 286.dp, max = 342.dp)
                .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        when (clusterMode) {
                            DashboardClusterMode.ANALOG_CLASSIC -> listOf(
                                Color(0xF512100E),
                                Color(0xFA090807)
                            )
                            DashboardClusterMode.DIGITAL_TURBO -> listOf(
                                Color(0xF208101C),
                                Color(0xFA040810)
                            )
                            DashboardClusterMode.HYBRID -> listOf(
                                Color(0xF20D1422),
                                Color(0xFA060911)
                            )
                        }
                    )
                )
                .border(1.4.dp, housingBorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("realistic_dashboard_cluster")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Top Automotive Telltales & Mode Toggle Bar
                DashboardTelltaleStrip(
                    telemetry = telemetry,
                    blinkOn = blinkOn,
                    clusterMode = clusterMode,
                    onCycleMode = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val all = DashboardClusterMode.entries
                        clusterMode = all[(all.indexOf(clusterMode) + 1) % all.size]
                    }
                )

                when (clusterMode) {
                    DashboardClusterMode.HYBRID -> {
                        HybridAnalogDigitalBody(
                            telemetry = telemetry,
                            smoothSpeedKmh = smoothSpeedKmh,
                            smoothRpm = smoothRpm,
                            maxScaleRpm = maxScaleRpm,
                            redlineRpm = redlineRpm,
                            rpmFrac = rpmFrac,
                            smoothBoostPsi = smoothBoostPsi,
                            fuelPct = fuelPct,
                            coolantNorm = coolantNorm,
                            needleTremorDeg = activeNeedleTremor,
                            isNearRedline = isNearRedline,
                            redlinePulse = redlinePulse,
                            isDrifting = isDrifting,
                            textMeasurer = textMeasurer,
                            classicWarmTheme = false
                        )
                    }

                    DashboardClusterMode.ANALOG_CLASSIC -> {
                        HybridAnalogDigitalBody(
                            telemetry = telemetry,
                            smoothSpeedKmh = smoothSpeedKmh,
                            smoothRpm = smoothRpm,
                            maxScaleRpm = maxScaleRpm,
                            redlineRpm = redlineRpm,
                            rpmFrac = rpmFrac,
                            smoothBoostPsi = smoothBoostPsi,
                            fuelPct = fuelPct,
                            coolantNorm = coolantNorm,
                            needleTremorDeg = activeNeedleTremor,
                            isNearRedline = isNearRedline,
                            redlinePulse = redlinePulse,
                            isDrifting = isDrifting,
                            textMeasurer = textMeasurer,
                            classicWarmTheme = true
                        )
                    }

                    DashboardClusterMode.DIGITAL_TURBO -> {
                        DigitalTurboCockpitBody(
                            telemetry = telemetry,
                            smoothSpeedKmh = smoothSpeedKmh,
                            smoothRpm = smoothRpm,
                            rpmFrac = rpmFrac,
                            smoothBoostPsi = smoothBoostPsi,
                            fuelPct = fuelPct,
                            coolantNorm = coolantNorm,
                            isNearRedline = isNearRedline,
                            redlinePulse = redlinePulse,
                            isDrifting = isDrifting
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTelltaleStrip(
    telemetry: VehicleTelemetry,
    blinkOn: Boolean,
    clusterMode: DashboardClusterMode,
    onCycleMode: () -> Unit
) {
    val leftSignalLit = telemetry.leftIndicatorOn && blinkOn
    val rightSignalLit = telemetry.rightIndicatorOn && blinkOn
    val parkBrakeLit = telemetry.handbrake || telemetry.autoShifter == AutomaticShifterState.P
    val checkEngineLit = !telemetry.engineRunning || telemetry.damage.engineHealthPct < 65f
    val lowFuelLit = telemetry.lowFuelWarning && blinkOn
    val beamTint = when (telemetry.headlightBeamMode) {
        HeadlightBeamMode.HIGH_BEAM -> MasalikSky
        HeadlightBeamMode.LOW_BEAM -> MasalikEmerald
        HeadlightBeamMode.OFF -> Color(0xFF334155)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Left Turn Arrow + Headlight + Park Brake + Engine Warning
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (leftSignalLit) MasalikEmerald.copy(alpha = 0.28f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "إشارة يسار",
                    tint = if (leftSignalLit) MasalikEmerald else Color(0xFF283244),
                    modifier = Modifier.size(16.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.Highlight,
                contentDescription = "المصابيح",
                tint = beamTint,
                modifier = Modifier.size(12.dp)
            )

            // (P) Parking Brake Telltale
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (parkBrakeLit) MasalikCrimson.copy(alpha = 0.25f) else Color.Transparent)
                    .border(
                        0.8.dp,
                        if (parkBrakeLit) MasalikCrimson else Color(0xFF283244),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = "P",
                    color = if (parkBrakeLit) MasalikCrimson else Color(0xFF334155),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Engine / Maintenance Telltale
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "فحص المحرك",
                tint = if (checkEngineLit) MasalikAmber else Color(0xFF283244),
                modifier = Modifier.size(11.dp)
            )

            // Low Fuel Telltale
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = "مؤشر الوقود",
                tint = when {
                    lowFuelLit -> MasalikCrimson
                    telemetry.lowFuelWarning -> MasalikAmber
                    else -> Color(0xFF283244)
                },
                modifier = Modifier.size(11.dp)
            )
        }

        // Center: Clickable Dashboard Theme Toggle (Hybrid / Digital / Analog)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141C2B))
                .border(0.8.dp, MasalikAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .clickable(onClick = onCycleMode)
                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                .testTag("dashboard_style_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "تبديل شكل العداد",
                tint = MasalikAmberSoft,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = clusterMode.labelAr,
                color = MasalikAmberSoft,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Right side: Assists (ABS/TCS) + Drivetrain + Cruise + Right Turn Arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (telemetry.cruiseControlEnabled) {
                Text(
                    text = "CRUISE",
                    color = MasalikSky,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = telemetry.drivetrainMode.label,
                color = if (telemetry.diffLockActive) MasalikAmber else MasalikSky,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (telemetry.assists.absEnabled) "ABS" else "—",
                color = if (telemetry.assists.absEnabled) Color(0xFF94A3B8) else MasalikCrimson,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (rightSignalLit) MasalikEmerald.copy(alpha = 0.28f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "إشارة يمين",
                    tint = if (rightSignalLit) MasalikEmerald else Color(0xFF283244),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HybridAnalogDigitalBody(
    telemetry: VehicleTelemetry,
    smoothSpeedKmh: Float,
    smoothRpm: Float,
    maxScaleRpm: Float,
    redlineRpm: Float,
    rpmFrac: Float,
    smoothBoostPsi: Float,
    fuelPct: Float,
    coolantNorm: Float,
    needleTremorDeg: Float,
    isNearRedline: Boolean,
    redlinePulse: Float,
    isDrifting: Boolean,
    textMeasurer: TextMeasurer,
    classicWarmTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LEFT ANALOG DIAL: Precision 0–240 km/h Speedometer + Embedded Fuel Arc
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAnalogSpeedometerDial(
                    speedKmh = smoothSpeedKmh,
                    fuelPct = fuelPct,
                    lowFuel = telemetry.lowFuelWarning,
                    classicWarm = classicWarmTheme,
                    textMeasurer = textMeasurer
                )
            }
            Column(
                modifier = Modifier.padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "كم/س",
                    color = if (classicWarmTheme) Color(0xFFD6B98C) else MasalikMuted,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // CENTER OLED MULTI-FUNCTION TFT SCREEN
        CenterTftMultiFunctionDisplay(
            telemetry = telemetry,
            smoothSpeedKmh = smoothSpeedKmh,
            smoothRpm = smoothRpm,
            rpmFrac = rpmFrac,
            smoothBoostPsi = smoothBoostPsi,
            isNearRedline = isNearRedline,
            redlinePulse = redlinePulse,
            isDrifting = isDrifting,
            classicWarmTheme = classicWarmTheme,
            modifier = Modifier
                .width(98.dp)
                .height(92.dp)
        )

        // RIGHT ANALOG DIAL: Precision Tachometer (x1000 RPM) + Embedded Engine Temp Arc
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAnalogTachometerDial(
                    rpm = smoothRpm,
                    maxScaleRpm = maxScaleRpm,
                    redlineRpm = redlineRpm,
                    coolantNorm = coolantNorm,
                    needleTremorDeg = needleTremorDeg,
                    isNearRedline = isNearRedline,
                    redlinePulse = redlinePulse,
                    classicWarm = classicWarmTheme,
                    textMeasurer = textMeasurer
                )
            }
            Column(
                modifier = Modifier.padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "×1000 د/د",
                    color = if (isNearRedline) MasalikCrimson else if (classicWarmTheme) Color(0xFFD6B98C) else MasalikMuted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CenterTftMultiFunctionDisplay(
    telemetry: VehicleTelemetry,
    smoothSpeedKmh: Float,
    smoothRpm: Float,
    rpmFrac: Float,
    smoothBoostPsi: Float,
    isNearRedline: Boolean,
    redlinePulse: Float,
    isDrifting: Boolean,
    classicWarmTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val gearColor = when {
        telemetry.gear == "R" -> MasalikCrimson
        telemetry.gear == "P" -> MasalikAmber
        telemetry.gear == "N" -> MasalikSky
        isNearRedline -> MasalikCrimson
        else -> if (classicWarmTheme) Color(0xFFFCD34D) else MasalikEmerald
    }
    val odoKm = (telemetry.totalDistanceDrivenM / 1000f).coerceAtLeast(0f)
    val speedInt = smoothSpeedKmh.roundToInt().coerceIn(0, 999)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    if (classicWarmTheme) listOf(Color(0xFF15120D), Color(0xFF0A0806))
                    else listOf(Color(0xFF0B1322), Color(0xFF050810))
                )
            )
            .border(
                1.dp,
                if (isNearRedline && redlinePulse > 0.6f) MasalikCrimson else Color(0xFF263349),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1: Active Gear Badge + Surface / Slope
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(gearColor.copy(alpha = 0.22f))
                    .border(1.dp, gearColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = telemetry.gear,
                    color = gearColor,
                    fontFamily = ChakraPetchFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isDrifting) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(MasalikCrimson)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "انجراف",
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text(
                    text = telemetry.surface.labelAr,
                    color = if (classicWarmTheme) MasalikAmberSoft else MasalikSky,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Row 2: Crisp Digital Speed + RPM Readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedInt.toString().padStart(2, '0'),
                color = if (classicWarmTheme) Color(0xFFFFF7E6) else MasalikIvory,
                fontFamily = ChakraPetchFontFamily,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 27.sp
            )
            Text(
                text = "${"%.1f".format(smoothRpm / 1000f)}k RPM",
                color = if (isNearRedline) MasalikCrimson else MasalikMuted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 7.5.sp
            )
        }

        // Row 3: Live Turbo Boost / Throttle Load Micro-Bar + Odometer
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val boostFrac = (smoothBoostPsi / 18f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1A2233))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(boostFrac.coerceAtLeast(0.04f))
                        .background(
                            Brush.horizontalGradient(
                                if (classicWarmTheme) listOf(MasalikAmber, Color(0xFFEF4444))
                                else listOf(MasalikSky, MasalikEmerald, MasalikAmber)
                            )
                        )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${"%.1f".format(smoothBoostPsi)} PSI",
                    color = if (smoothBoostPsi > 12f) MasalikAmberSoft else MasalikMuted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.sp
                )
                Text(
                    text = "ODO ${"%.1f".format(odoKm)}",
                    color = MasalikMuted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
private fun DigitalTurboCockpitBody(
    telemetry: VehicleTelemetry,
    smoothSpeedKmh: Float,
    smoothRpm: Float,
    rpmFrac: Float,
    smoothBoostPsi: Float,
    fuelPct: Float,
    coolantNorm: Float,
    isNearRedline: Boolean,
    redlinePulse: Float,
    isDrifting: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Sequential 16-LED Motorsport Shift Light Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
        ) {
            val leds = 16
            val gap = 3.dp.toPx()
            val ledW = (size.width - gap * (leds - 1)) / leds
            val litCount = (rpmFrac * leds).roundToInt().coerceIn(0, leds)
            for (i in 0 until leds) {
                val frac = i / leds.toFloat()
                val lit = i < litCount
                val baseColor = when {
                    frac >= 0.75f -> MasalikCrimson
                    frac >= 0.44f -> MasalikAmber
                    else -> MasalikEmerald
                }
                val flashDim = if (isNearRedline && lit && redlinePulse < 0.5f) 0.35f else 1f
                drawRoundRect(
                    color = if (lit) baseColor.copy(alpha = flashDim) else Color(0xFF1C2436),
                    topLeft = Offset(i * (ledW + gap), 1.dp.toPx()),
                    size = Size(ledW, size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }
        }

        // Main Digital Telemetry Row: Left Speed Arc + Center Speed/Gear + Right RPM/Boost Arc
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Vertical Fuel & Throttle Load Bars
            Column(
                modifier = Modifier.width(68.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniDigitalBar("وقود", "${(fuelPct * 100).roundToInt()}%", fuelPct, if (telemetry.lowFuelWarning) MasalikCrimson else MasalikAmber)
                MiniDigitalBar("غاز", "${(telemetry.throttle * 100).roundToInt()}%", telemetry.throttle, MasalikEmerald)
                MiniDigitalBar("فرامل", "${(telemetry.brake * 100).roundToInt()}%", telemetry.brake, MasalikCrimson)
            }

            // Center Digital Readout + Curved Speed/RPM Dual Ring
            Box(
                modifier = Modifier.size(width = 148.dp, height = 74.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 5.5.dp.toPx()
                    val pad = 4.dp.toPx()
                    val speedFrac = (smoothSpeedKmh / 220f).coerceIn(0f, 1f)
                    // Outer left-to-right arc track
                    drawArc(
                        color = Color(0xFF1A2334),
                        startAngle = 155f,
                        sweepAngle = 230f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - pad * 2f, size.width - pad * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Dynamic RPM sweep gradient arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to MasalikSky,
                            0.55f to MasalikEmerald,
                            0.80f to MasalikAmber,
                            1.0f to MasalikCrimson
                        ),
                        startAngle = 155f,
                        sweepAngle = 230f * rpmFrac.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - pad * 2f, size.width - pad * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Inner speed track arc
                    val innerPad = pad + 8.dp.toPx()
                    drawArc(
                        color = Color(0x3338BDF8),
                        startAngle = 165f,
                        sweepAngle = 210f * speedFrac,
                        useCenter = false,
                        topLeft = Offset(innerPad, innerPad),
                        size = Size(size.width - innerPad * 2f, size.width - innerPad * 2f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${smoothSpeedKmh.roundToInt()}",
                            color = MasalikIvory,
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 31.sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MasalikAmber.copy(alpha = 0.22f))
                                .border(1.dp, MasalikAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = telemetry.gear,
                                color = MasalikAmberSoft,
                                fontFamily = ChakraPetchFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "كم/س · ${smoothRpm.roundToInt()} RPM",
                        color = if (isNearRedline) MasalikCrimson else MasalikSky,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 7.5.sp
                    )
                }
            }

            // Right Side: Boost + Coolant Temp + Slip/Slope
            Column(
                modifier = Modifier.width(68.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniDigitalBar("توربو", "${"%.1f".format(smoothBoostPsi)}", (smoothBoostPsi / 18f).coerceIn(0f, 1f), MasalikSky)
                MiniDigitalBar("حرارة", "${(60f + coolantNorm * 55f).roundToInt()}°", coolantNorm, if (coolantNorm > 0.8f) MasalikCrimson else MasalikEmerald)
                Text(
                    text = if (isDrifting) "انجراف ${abs(telemetry.driftSlipAngleDeg).roundToInt()}°" else "ميل ${telemetry.slopeGradePct.roundToInt()}%",
                    color = if (isDrifting) MasalikCrimson else MasalikAmberSoft,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniDigitalBar(label: String, valueText: String, progress: Float, barColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MasalikMuted, fontSize = 7.sp)
            Text(valueText, color = MasalikIvory, fontFamily = JetBrainsMonoFontFamily, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1B2435))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0.03f, 1f))
                    .background(barColor)
            )
        }
    }
}

/**
 * Draws a precision recessed analog Speedometer dial (0–240 km/h) with:
 * - Multi-layer metallic outer bezel & radial dial face gradient
 * - Glowing speed sweep trail
 * - 49 crisp tick marks (major every 20 km/h) + Canvas-aligned JetBrainsMono numerals
 * - Bottom inner curved Fuel gauge arc (E—F)
 * - Tapered glowing automotive needle + brushed metal hub cap
 */
private fun DrawScope.drawAnalogSpeedometerDial(
    speedKmh: Float,
    fuelPct: Float,
    lowFuel: Boolean,
    classicWarm: Boolean,
    textMeasurer: TextMeasurer
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.48f

    drawDialHousing(cx, cy, r, classicWarm)

    val startAngleDeg = 135f
    val sweepRangeDeg = 270f
    val maxSpeed = 240f
    val speedFrac = (speedKmh / maxSpeed).coerceIn(0f, 1f)

    // Glowing active sweep arc behind needle
    if (speedFrac > 0.005f) {
        val trailColor = if (classicWarm) MasalikAmber else MasalikSky
        drawArc(
            brush = Brush.sweepGradient(
                0f to trailColor.copy(alpha = 0.25f),
                0.7f to trailColor.copy(alpha = 0.65f),
                1f to MasalikAmber
            ),
            startAngle = startAngleDeg,
            sweepAngle = sweepRangeDeg * speedFrac,
            useCenter = false,
            topLeft = Offset(cx - r * 0.80f, cy - r * 0.80f),
            size = Size(r * 1.60f, r * 1.60f),
            style = Stroke(width = r * 0.10f, cap = StrokeCap.Round)
        )
    }

    // Major & Minor radial tick marks (0 to 240 in steps of 5 km/h -> 48 intervals)
    val totalSteps = 48
    for (i in 0..totalSteps) {
        val frac = i / totalSteps.toFloat()
        val deg = startAngleDeg + frac * sweepRangeDeg
        val rad = deg * PI.toFloat() / 180f
        val isMajor = (i % 4 == 0) // Every 20 km/h
        val isMed = (i % 2 == 0) && !isMajor
        val outerR = r * 0.88f
        val innerR = when {
            isMajor -> r * 0.72f
            isMed -> r * 0.77f
            else -> r * 0.81f
        }
        val passed = frac <= speedFrac + 0.005f
        val tickColor = when {
            passed && i >= 40 -> MasalikCrimson
            passed -> if (classicWarm) MasalikAmberSoft else Color.White
            classicWarm -> Color(0x66E7D3B0)
            else -> Color(0x55CBD5E1)
        }
        drawLine(
            color = tickColor,
            start = Offset(cx + cos(rad) * innerR, cy + sin(rad) * innerR),
            end = Offset(cx + cos(rad) * outerR, cy + sin(rad) * outerR),
            strokeWidth = if (isMajor) 1.6.dp.toPx() else 0.9.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw numerals every 40 km/h (0, 40, 80, 120, 160, 200, 240) to keep 96dp dial ultra-crisp
        if (i % 8 == 0) {
            val labelVal = i * 5
            val textR = r * 0.56f
            val tx = cx + cos(rad) * textR
            val ty = cy + sin(rad) * textR
            val layout = textMeasurer.measure(
                text = labelVal.toString(),
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        passed && labelVal >= 200 -> MasalikCrimson
                        passed -> if (classicWarm) MasalikAmberSoft else Color.White
                        classicWarm -> Color(0xFFBFA986)
                        else -> Color(0xFF94A3B8)
                    }
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(tx - layout.size.width * 0.5f, ty - layout.size.height * 0.5f)
            )
        }
    }

    // Embedded Bottom Fuel Level Arc (E -> F)
    val fuelTopLeft = Offset(cx - r * 0.62f, cy - r * 0.62f)
    val fuelSize = Size(r * 1.24f, r * 1.24f)
    drawArc(
        color = Color(0x3364748B),
        startAngle = 62f,
        sweepAngle = 56f,
        useCenter = false,
        topLeft = fuelTopLeft,
        size = fuelSize,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = if (lowFuel) MasalikCrimson else MasalikAmber,
        startAngle = 118f - 56f * fuelPct,
        sweepAngle = 56f * fuelPct.coerceAtLeast(0.04f),
        useCenter = false,
        topLeft = fuelTopLeft,
        size = fuelSize,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Physical Tapered Automotive Needle
    val needleDeg = startAngleDeg + speedFrac * sweepRangeDeg
    drawTaperedNeedle(
        cx = cx,
        cy = cy,
        radius = r,
        angleDeg = needleDeg,
        needleColor = if (classicWarm) Color(0xFFFF6B35) else Color(0xFFFF3B30),
        glowColor = if (classicWarm) MasalikAmber else Color(0xFFFB7185)
    )
}

/**
 * Draws a precision recessed analog Tachometer dial (0–8/9 x1000 RPM) with:
 * - Dynamic vehicle-specific Redline & Amber caution zones
 * - Pulsing redline glow at high RPM
 * - Embedded Engine Temperature / Health Arc (C—H)
 * - High-RPM vibrating needle tip & brushed metal hub
 */
private fun DrawScope.drawAnalogTachometerDial(
    rpm: Float,
    maxScaleRpm: Float,
    redlineRpm: Float,
    coolantNorm: Float,
    needleTremorDeg: Float,
    isNearRedline: Boolean,
    redlinePulse: Float,
    classicWarm: Boolean,
    textMeasurer: TextMeasurer
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = size.minDimension * 0.48f

    drawDialHousing(cx, cy, r, classicWarm)

    val startAngleDeg = 135f
    val sweepRangeDeg = 270f
    val rpmScaleFrac = (rpm / maxScaleRpm).coerceIn(0f, 1.01f)
    val redlineStartFrac = (redlineRpm / maxScaleRpm).coerceIn(0.65f, 0.94f)
    val amberStartFrac = (redlineStartFrac - 0.14f).coerceAtLeast(0.50f)

    // Colored caution + redline outer rim band
    drawArc(
        color = MasalikAmber.copy(alpha = 0.50f),
        startAngle = startAngleDeg + amberStartFrac * sweepRangeDeg,
        sweepAngle = (redlineStartFrac - amberStartFrac) * sweepRangeDeg,
        useCenter = false,
        topLeft = Offset(cx - r * 0.85f, cy - r * 0.85f),
        size = Size(r * 1.70f, r * 1.70f),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Butt)
    )
    drawArc(
        color = MasalikCrimson.copy(alpha = if (isNearRedline) 0.55f + 0.45f * redlinePulse else 0.78f),
        startAngle = startAngleDeg + redlineStartFrac * sweepRangeDeg,
        sweepAngle = (1f - redlineStartFrac) * sweepRangeDeg,
        useCenter = false,
        topLeft = Offset(cx - r * 0.85f, cy - r * 0.85f),
        size = Size(r * 1.70f, r * 1.70f),
        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Butt)
    )

    // Glowing live RPM trail behind needle
    if (rpmScaleFrac > 0.01f) {
        val activeColor = when {
            rpmScaleFrac >= redlineStartFrac -> MasalikCrimson
            rpmScaleFrac >= amberStartFrac -> MasalikAmber
            classicWarm -> MasalikAmberSoft
            else -> MasalikEmerald
        }
        drawArc(
            color = activeColor.copy(alpha = 0.48f),
            startAngle = startAngleDeg,
            sweepAngle = sweepRangeDeg * rpmScaleFrac.coerceAtMost(1f),
            useCenter = false,
            topLeft = Offset(cx - r * 0.76f, cy - r * 0.76f),
            size = Size(r * 1.52f, r * 1.52f),
            style = Stroke(width = r * 0.085f, cap = StrokeCap.Round)
        )
    }

    // Major & Minor Ticks (0..maxK in 0.25k steps)
    val maxK = (maxScaleRpm / 1000f).roundToInt().coerceIn(7, 9)
    val totalSteps = maxK * 4
    for (i in 0..totalSteps) {
        val frac = i / totalSteps.toFloat()
        val deg = startAngleDeg + frac * sweepRangeDeg
        val rad = deg * PI.toFloat() / 180f
        val isMajor = (i % 4 == 0)
        val outerR = r * 0.88f
        val innerR = if (isMajor) r * 0.71f else r * 0.80f
        val inRed = frac >= redlineStartFrac
        val passed = frac <= rpmScaleFrac + 0.005f
        val tickColor = when {
            inRed -> MasalikCrimson
            passed -> if (classicWarm) MasalikAmberSoft else Color.White
            classicWarm -> Color(0x66E7D3B0)
            else -> Color(0x55CBD5E1)
        }
        drawLine(
            color = tickColor,
            start = Offset(cx + cos(rad) * innerR, cy + sin(rad) * innerR),
            end = Offset(cx + cos(rad) * outerR, cy + sin(rad) * outerR),
            strokeWidth = if (isMajor) 1.7.dp.toPx() else 0.9.dp.toPx(),
            cap = StrokeCap.Round
        )

        if (isMajor) {
            val kLabel = (i / 4).toString()
            val textR = r * 0.56f
            val tx = cx + cos(rad) * textR
            val ty = cy + sin(rad) * textR
            val layout = textMeasurer.measure(
                text = kLabel,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        inRed -> MasalikCrimson
                        passed -> if (classicWarm) MasalikAmberSoft else Color.White
                        classicWarm -> Color(0xFFBFA986)
                        else -> Color(0xFF94A3B8)
                    }
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(tx - layout.size.width * 0.5f, ty - layout.size.height * 0.5f)
            )
        }
    }

    // Embedded Bottom Coolant Temp Arc (C -> H)
    val tempTopLeft = Offset(cx - r * 0.62f, cy - r * 0.62f)
    val tempSize = Size(r * 1.24f, r * 1.24f)
    drawArc(
        color = Color(0x3364748B),
        startAngle = 62f,
        sweepAngle = 56f,
        useCenter = false,
        topLeft = tempTopLeft,
        size = tempSize,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
    val tempColor = when {
        coolantNorm > 0.80f -> MasalikCrimson
        coolantNorm < 0.25f -> MasalikSky
        else -> MasalikEmerald
    }
    drawArc(
        color = tempColor,
        startAngle = 118f - 56f * coolantNorm,
        sweepAngle = 56f * coolantNorm.coerceAtLeast(0.05f),
        useCenter = false,
        topLeft = tempTopLeft,
        size = tempSize,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Physical Tapered RPM Needle with redline quiver
    val needleDeg = startAngleDeg + rpmScaleFrac.coerceAtMost(1.02f) * sweepRangeDeg + needleTremorDeg
    drawTaperedNeedle(
        cx = cx,
        cy = cy,
        radius = r,
        angleDeg = needleDeg,
        needleColor = if (isNearRedline) Color(0xFFFF1F3D) else if (classicWarm) Color(0xFFFF7A2F) else Color(0xFFFF3B30),
        glowColor = if (isNearRedline) MasalikCrimson else MasalikAmber
    )
}

private fun DrawScope.drawDialHousing(cx: Float, cy: Float, r: Float, classicWarm: Boolean) {
    // Recessed gauge background radial gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (classicWarm) {
                listOf(Color(0xFF1F1912), Color(0xFF0E0B08), Color(0xFF060504))
            } else {
                listOf(Color(0xFF162033), Color(0xFF0A0F1A), Color(0xFF04060B))
            },
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy)
    )

    // Outer metallic chamfered bezel ring
    drawCircle(
        brush = Brush.linearGradient(
            colors = if (classicWarm) {
                listOf(Color(0xFFD4AF37), Color(0xFF473720), Color(0xFF9E7B3B))
            } else {
                listOf(Color(0xFF64748B), Color(0xFF1E293B), Color(0xFF475569))
            },
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2.dp.toPx())
    )

    // Subtle glass top highlight arc
    drawArc(
        color = Color(0x18FFFFFF),
        startAngle = 210f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.92f, cy - r * 0.92f),
        size = Size(r * 1.84f, r * 1.84f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawTaperedNeedle(
    cx: Float,
    cy: Float,
    radius: Float,
    angleDeg: Float,
    needleColor: Color,
    glowColor: Color
) {
    val rad = angleDeg * PI.toFloat() / 180f
    val perp = rad + (PI.toFloat() * 0.5f)
    val tipLen = radius * 0.79f
    val tailLen = radius * 0.19f
    val baseHalfW = radius * 0.048f

    val tip = Offset(cx + cos(rad) * tipLen, cy + sin(rad) * tipLen)
    val tail = Offset(cx - cos(rad) * tailLen, cy - sin(rad) * tailLen)
    val leftBase = Offset(cx + cos(perp) * baseHalfW, cy + sin(perp) * baseHalfW)
    val rightBase = Offset(cx - cos(perp) * baseHalfW, cy - sin(perp) * baseHalfW)

    // Soft ambient neon needle glow
    drawLine(
        color = glowColor.copy(alpha = 0.36f),
        start = tail,
        end = tip,
        strokeWidth = 4.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Sharp 3D tapered needle polygon
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftBase.x, leftBase.y)
        lineTo(tail.x, tail.y)
        lineTo(rightBase.x, rightBase.y)
        close()
    }
    drawPath(path = path, color = needleColor)

    // Bright center highlight spine
    drawLine(
        color = Color(0x99FFFFFF),
        start = Offset(cx, cy),
        end = Offset(cx + cos(rad) * (tipLen * 0.85f), cy + sin(rad) * (tipLen * 0.85f)),
        strokeWidth = 0.9.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Brushed metallic center cap
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF94A3B8), Color(0xFF1E293B), Color(0xFF090D16)),
            center = Offset(cx - radius * 0.02f, cy - radius * 0.02f),
            radius = radius * 0.14f
        ),
        radius = radius * 0.13f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0x88E2E8F0),
        radius = radius * 0.13f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.dp.toPx())
    )
}
