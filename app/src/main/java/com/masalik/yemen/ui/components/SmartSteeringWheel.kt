package com.masalik.yemen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.SmartWheelSettings
import com.masalik.yemen.game.SteeringWheelStyle
import com.masalik.yemen.game.TouchControlMode
import com.masalik.yemen.ui.theme.JetBrainsMonoFontFamily
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikAmberSoft
import com.masalik.yemen.ui.theme.MasalikCrimson
import com.masalik.yemen.ui.theme.MasalikEmerald
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikSky
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

@Composable
fun SteeringControlWidget(
    controlMode: TouchControlMode,
    settings: SmartWheelSettings,
    currentWheelDeg: Float,
    speedKmh: Float = 0f,
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    onCalibrateGyro: () -> Unit,
    onShiftUp: () -> Unit = {},
    onShiftDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (controlMode) {
        TouchControlMode.WHEEL -> SmartSteeringWheel(
            settings = settings,
            wheelDeg = currentWheelDeg,
            speedKmh = speedKmh,
            scale = scale,
            opacity = opacity,
            onSteerChanged = onSteerChanged,
            onShiftUp = onShiftUp,
            onShiftDown = onShiftDown,
            modifier = modifier
        )
        TouchControlMode.BUTTONS -> DirectionalSteerButtons(
            scale = scale,
            opacity = opacity,
            onSteerChanged = onSteerChanged,
            modifier = modifier
        )
        TouchControlMode.JOYSTICK -> VirtualSteerJoystick(
            scale = scale,
            opacity = opacity,
            onSteerChanged = onSteerChanged,
            modifier = modifier
        )
        TouchControlMode.TILT -> {
            Box(
                modifier = modifier
                    .alpha(opacity)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MasalikObsidian.copy(alpha = 0.84f))
                    .border(1.5.dp, MasalikAmber.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onCalibrateGyro()
                        }
                    }
                    .size(width = 160.dp, height = 56.dp)
                    .testTag("gyro_calibrate_badge"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "جيروسكوب نشط · اضغط للمعايرة",
                    color = MasalikAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmartSteeringWheel(
    settings: SmartWheelSettings,
    wheelDeg: Float,
    speedKmh: Float,
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    onShiftUp: () -> Unit,
    onShiftDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDp = (152f * scale.coerceIn(0.75f, 1.38f)).dp
    var localAngleDeg by remember { mutableFloatStateOf(0f) }
    var isGrabbed by remember { mutableStateOf(false) }
    var hitLockEdge by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val halfLock = (settings.rotationRangeDeg * 0.5f).coerceAtLeast(90f)
    val style = settings.wheelStyle
    val hasPaddles = style == SteeringWheelStyle.EUROPEAN_LUXURY || style == SteeringWheelStyle.JAPANESE_SPORT

    // Realistic speed-proportional spring auto-centering when the driver releases the wheel
    LaunchedEffect(isGrabbed, settings.autoCenterEnabled, style, halfLock) {
        if (!isGrabbed && settings.autoCenterEnabled && abs(localAngleDeg) > 0.25f) {
            var lastNanos = 0L
            while (!isGrabbed && abs(localAngleDeg) > 0.45f) {
                withFrameNanos { now ->
                    if (lastNanos == 0L) {
                        lastNanos = now
                        return@withFrameNanos
                    }
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastNanos = now
                    // Caster self-aligning torque strengthens with vehicle speed
                    val speedBoost = 0.65f + (speedKmh / 55f).coerceIn(0f, 1.55f)
                    val decay = (style.returnSpeed * speedBoost * 2.15f * dt).coerceAtMost(0.42f)
                    val next = localAngleDeg * (1f - decay)
                    localAngleDeg = if (abs(next) < 0.7f) 0f else next
                    val norm = if (abs(localAngleDeg) < settings.deadZoneDeg) 0f else (localAngleDeg / halfLock).coerceIn(-1f, 1f)
                    onSteerChanged(norm)
                }
            }
            if (!isGrabbed) {
                localAngleDeg = 0f
                onSteerChanged(0f)
            }
        }
    }

    val displayDeg = if (isGrabbed || abs(localAngleDeg) > 0.4f) localAngleDeg else wheelDeg
    val lockFrac = (displayDeg / halfLock).coerceIn(-1f, 1f)

    Box(
        modifier = modifier.alpha(opacity),
        contentAlignment = Alignment.Center
    ) {
        // Optional Sport/European Paddle Shifters positioned behind upper rim
        if (hasPaddles) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .size(width = sizeDp + 18.dp, height = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note RTL: Start is Right (+ Upshift), End is Left (- Downshift)
                PaddleShiftButton(
                    symbol = "+",
                    desc = "غيار أعلى",
                    tag = "paddle_shift_up",
                    accent = if (style == SteeringWheelStyle.JAPANESE_SPORT) MasalikCrimson else MasalikSky
                ) {
                    if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShiftUp()
                }
                PaddleShiftButton(
                    symbol = "−",
                    desc = "غيار أدنى",
                    tag = "paddle_shift_down",
                    accent = if (style == SteeringWheelStyle.JAPANESE_SPORT) MasalikCrimson else MasalikSky
                ) {
                    if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShiftDown()
                }
            }
        }

        // Main Interactive Steering Wheel Surface
        Box(
            modifier = Modifier
                .size(sizeDp)
                .testTag("smart_steering_wheel")
                .pointerInput(settings) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        var prevAngle = atan2(down.position.y - cy, down.position.x - cx)
                        isGrabbed = true
                        hitLockEdge = false
                        var accumDeg = localAngleDeg
                        if (settings.hapticFeedback) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                isGrabbed = false
                                hitLockEdge = false
                                break
                            }
                            val currAngle = atan2(change.position.y - cy, change.position.x - cx)
                            var delta = currAngle - prevAngle
                            if (delta > PI) delta -= (2.0 * PI).toFloat()
                            if (delta < -PI) delta += (2.0 * PI).toFloat()
                            prevAngle = currAngle

                            val rawNext = accumDeg + delta * (180f / PI.toFloat()) * settings.sensitivity * style.steeringRatio
                            val clamped = rawNext.coerceIn(-halfLock, halfLock)

                            // Haptic thud when hitting full steering lock
                            val atLock = abs(clamped) >= halfLock - 1.5f
                            if (atLock && !hitLockEdge && settings.hapticFeedback) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                hitLockEdge = true
                            } else if (!atLock) {
                                hitLockEdge = false
                            }

                            // Subtle center detent haptic when passing 0°
                            if (sign(accumDeg) != 0f && sign(clamped) != 0f && sign(accumDeg) != sign(clamped)) {
                                if (settings.hapticFeedback) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }

                            accumDeg = clamped
                            localAngleDeg = accumDeg
                            val effective = if (abs(accumDeg) < settings.deadZoneDeg) 0f else accumDeg / halfLock
                            onSteerChanged(effective.coerceIn(-1f, 1f))
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val r = size.minDimension * 0.415f
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val arcRadius = r + 11.dp.toPx()

                // Soft dark cockpit halo backdrop
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xD6080C15), Color(0x9A080C15), Color.Transparent),
                        center = center,
                        radius = arcRadius + 6.dp.toPx()
                    ),
                    radius = arcRadius + 6.dp.toPx(),
                    center = center
                )

                // Outer live steering-lock progress track
                drawArc(
                    color = Color(0x45334155),
                    startAngle = -150f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
                if (abs(lockFrac) > 0.02f) {
                    val arcColor = when {
                        abs(lockFrac) > 0.85f -> MasalikCrimson
                        style == SteeringWheelStyle.EUROPEAN_LUXURY -> MasalikSky
                        style == SteeringWheelStyle.JAPANESE_SPORT -> MasalikCrimson
                        else -> MasalikAmber
                    }
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = lockFrac * 60f,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw the authentic rotating steering wheel style
                rotate(degrees = displayDeg, pivot = center) {
                    when (style) {
                        SteeringWheelStyle.GULF_YEMENI -> drawGulfYemeniWheel(center, r, isGrabbed)
                        SteeringWheelStyle.EUROPEAN_LUXURY -> drawEuropeanLuxuryWheel(center, r, isGrabbed)
                        SteeringWheelStyle.JAPANESE_SPORT -> drawJapaneseSportWheel(center, r, isGrabbed)
                        SteeringWheelStyle.CLASSIC_HERITAGE -> drawClassicHeritageWheel(center, r, isGrabbed)
                    }
                }
            }

            // Crisp style + live steering angle pill anchored cleanly at bottom rim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MasalikObsidian.copy(alpha = 0.88f))
                    .border(
                        1.dp,
                        if (isGrabbed) MasalikAmber else Color(0xFF334155),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                val degLabel = if (abs(displayDeg) >= 2f) "${displayDeg.roundToInt()}°" else style.shortLabelAr
                Text(
                    text = degLabel,
                    color = if (isGrabbed) MasalikAmberSoft else MasalikIvory,
                    fontFamily = if (abs(displayDeg) >= 2f) JetBrainsMonoFontFamily else null,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PaddleShiftButton(
    symbol: String,
    desc: String,
    tag: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF283142), Color(0xFF141A24))
                    )
                )
                .border(1.2.dp, accent.copy(alpha = 0.78f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/** 1. Gulf / Yemeni Luxury 4x4 Wheel: Glossy Walnut Wood upper/lower + Stitched Charcoal Leather + 4-Spoke Gold Boss */
private fun DrawScope.drawGulfYemeniWheel(center: Offset, r: Float, isGrabbed: Boolean) {
    val rimW = 14.dp.toPx()

    // Base charcoal stitched leather rim
    drawCircle(
        color = Color(0xFF1B202A),
        radius = r,
        center = center,
        style = Stroke(width = rimW)
    )
    // Glossy walnut mahogany wood inlays at top & bottom arcs
    val rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    drawArc(
        brush = Brush.linearGradient(listOf(Color(0xFF5A2A14), Color(0xFF9C552B), Color(0xFF4F220F))),
        startAngle = -138f,
        sweepAngle = 96f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = rimW * 0.94f, cap = StrokeCap.Butt)
    )
    drawArc(
        brush = Brush.linearGradient(listOf(Color(0xFF4F220F), Color(0xFF8C4A22), Color(0xFF5A2A14))),
        startAngle = 48f,
        sweepAngle = 84f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = rimW * 0.94f, cap = StrokeCap.Butt)
    )
    // Specular sheen highlight on top wood arc
    drawArc(
        color = Color(0x55FFD8A8),
        startAngle = -126f,
        sweepAngle = 72f,
        useCenter = false,
        topLeft = Offset(center.x - r + 2.dp.toPx(), center.y - r + 2.dp.toPx()),
        size = Size((r - 2.dp.toPx()) * 2f, (r - 2.dp.toPx()) * 2f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    // Subtle outer stitch border
    drawCircle(
        color = if (isGrabbed) MasalikAmber else Color(0xFF6B4E35),
        radius = r + rimW * 0.45f,
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
    )

    // 4 Luxury SUV brushed champagne-silver spokes
    val spokeAngles = listOf(18f, 162f, 65f, 115f)
    for (deg in spokeAngles) {
        val rad = deg * PI.toFloat() / 180f
        val end = Offset(center.x + cos(rad) * (r - 4.dp.toPx()), center.y + sin(rad) * (r - 4.dp.toPx()))
        drawLine(
            color = Color(0xFF2B3442),
            start = center,
            end = end,
            strokeWidth = 11.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFC8B08A),
            start = center,
            end = end,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Center leather airbag boss + golden desert emblem
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF282F3D), Color(0xFF0E131D)), center, 22.dp.toPx()),
        radius = 22.dp.toPx(),
        center = center
    )
    drawCircle(color = MasalikAmber, radius = 22.dp.toPx(), center = center, style = Stroke(width = 2.2.dp.toPx()))
    // Golden diamond Yemeni crest inside boss
    val d = 7.dp.toPx()
    val crest = Path().apply {
        moveTo(center.x, center.y - d)
        lineTo(center.x + d * 0.85f, center.y)
        lineTo(center.x, center.y + d)
        lineTo(center.x - d * 0.85f, center.y)
        close()
    }
    drawPath(crest, color = MasalikAmber)
}

/** 2. European Luxury Sports Wheel: Flat-Bottom D-Cut Leather + Brushed Aluminum Y-Spokes + Ambient Glow */
private fun DrawScope.drawEuropeanLuxuryWheel(center: Offset, r: Float, isGrabbed: Boolean) {
    val rimW = 13.5.dp.toPx()
    val dCutPath = Path().apply {
        val steps = 48
        for (i in 0..steps) {
            val a = -PI.toFloat() + (2f * PI.toFloat() * i / steps)
            val rawY = sin(a) * r
            val flatY = rawY.coerceAtMost(r * 0.78f)
            val px = center.x + cos(a) * r
            val py = center.y + flatY
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    // Thick sculpted perforated leather D-Cut rim
    drawPath(
        path = dCutPath,
        color = Color(0xFF171C26),
        style = Stroke(width = rimW, join = StrokeJoin.Round)
    )
    // Metallic inner accent trim
    drawPath(
        path = dCutPath,
        color = if (isGrabbed) MasalikSky else Color(0xFF54637A),
        style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
    )

    // Subtle top silver-sky alignment marker
    drawLine(
        color = MasalikSky,
        start = Offset(center.x, center.y - r - 6.dp.toPx()),
        end = Offset(center.x, center.y - r + 6.dp.toPx()),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Brushed aluminum European Y-spokes (left, right, split bottom)
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color(0xFF8A97AA), Color(0xFFD6DEE9), Color(0xFF8A97AA))),
        start = Offset(center.x - r + 6.dp.toPx(), center.y - 2.dp.toPx()),
        end = Offset(center.x + r - 6.dp.toPx(), center.y - 2.dp.toPx()),
        strokeWidth = 9.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Split twin lower aluminum bars down to the flat D-Cut base
    val botY = center.y + r * 0.78f - 4.dp.toPx()
    drawLine(
        color = Color(0xFFB5C2D4),
        start = Offset(center.x - 6.dp.toPx(), center.y + 8.dp.toPx()),
        end = Offset(center.x - 10.dp.toPx(), botY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFB5C2D4),
        start = Offset(center.x + 6.dp.toPx(), center.y + 8.dp.toPx()),
        end = Offset(center.x + 10.dp.toPx(), botY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Compact round German-style sports hub + glowing ambient ring
    drawCircle(color = Color(0xFF0D121B), radius = 19.dp.toPx(), center = center)
    drawCircle(
        color = if (isGrabbed) MasalikSky else Color(0xFFBBD0EB),
        radius = 19.dp.toPx(),
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    // 3-point star emblem
    for (k in 0..2) {
        val a = (-90f + k * 120f) * PI.toFloat() / 180f
        drawLine(
            color = Color(0xFFE2E8F0),
            start = center,
            end = Offset(center.x + cos(a) * 11.dp.toPx(), center.y + sin(a) * 11.dp.toPx()),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** 3. Japanese Sport / Drift JDM Wheel: Matte Alcantara + Crimson 12-o'clock Stripe + Drilled Racing Spokes */
private fun DrawScope.drawJapaneseSportWheel(center: Offset, r: Float, isGrabbed: Boolean) {
    val rimW = 14.5.dp.toPx()

    // Deep matte Alcantara suede rim
    drawCircle(
        color = Color(0xFF14171F),
        radius = r,
        center = center,
        style = Stroke(width = rimW)
    )
    // Crimson inner racing contrast stitching ring
    drawCircle(
        color = if (isGrabbed) MasalikCrimson else Color(0xAAEF4444),
        radius = r - rimW * 0.34f,
        center = center,
        style = Stroke(width = 1.6.dp.toPx())
    )

    // Glowing Crimson 12-o'clock drift reference stripe
    drawLine(
        color = MasalikCrimson,
        start = Offset(center.x, center.y - r - 7.dp.toPx()),
        end = Offset(center.x, center.y - r + 7.dp.toPx()),
        strokeWidth = 6.5.dp.toPx(),
        cap = StrokeCap.Butt
    )

    // 3 Drilled anodized gunmetal racing spokes
    val spokeDirs = listOf(
        Offset(-1f, 0.08f),
        Offset(1f, 0.08f),
        Offset(0f, 1f)
    )
    for (dir in spokeDirs) {
        val end = Offset(center.x + dir.x * (r - 5.dp.toPx()), center.y + dir.y * (r - 5.dp.toPx()))
        drawLine(
            color = Color(0xFF394150),
            start = center,
            end = end,
            strokeWidth = 11.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Weight-reduction drilled racing holes along each spoke
        for (frac in listOf(0.48f, 0.72f)) {
            drawCircle(
                color = Color(0xFF0A0D14),
                radius = 2.6.dp.toPx(),
                center = Offset(
                    center.x + dir.x * (r - 5.dp.toPx()) * frac,
                    center.y + dir.y * (r - 5.dp.toPx()) * frac
                )
            )
        }
    }

    // Compact motorsport horn button
    drawCircle(color = Color(0xFF11151E), radius = 16.dp.toPx(), center = center)
    drawCircle(color = MasalikCrimson, radius = 16.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
    drawCircle(color = MasalikCrimson, radius = 5.dp.toPx(), center = center)
}

/** 4. Classic Heritage Wood & Chrome Wheel: Glossy Mahogany Rim with Rivets + 3 Mirror-Chrome Slotted Spokes */
private fun DrawScope.drawClassicHeritageWheel(center: Offset, r: Float, isGrabbed: Boolean) {
    val rimW = 10.5.dp.toPx()

    // Glossy classic mahogany/teak rim
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(Color(0xFF6B3418), Color(0xFFA65A2E), Color(0xFF5D2910), Color(0xFF9C5127), Color(0xFF6B3418)),
            center = center
        ),
        radius = r,
        center = center,
        style = Stroke(width = rimW)
    )
    // Chrome inner rim bead
    drawCircle(
        color = if (isGrabbed) MasalikAmberSoft else Color(0xFFDDE5EF),
        radius = r - rimW * 0.45f,
        center = center,
        style = Stroke(width = 1.6.dp.toPx())
    )
    // Vintage brass rivets around the wooden rim
    for (i in 0 until 12) {
        val a = (i * 30f) * PI.toFloat() / 180f
        drawCircle(
            color = Color(0xFFE6C288),
            radius = 1.8.dp.toPx(),
            center = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
        )
    }

    // 3 Mirror-Chrome slotted heritage spokes
    val spokeDegs = listOf(15f, 165f, 90f)
    for (deg in spokeDegs) {
        val rad = deg * PI.toFloat() / 180f
        val end = Offset(center.x + cos(rad) * (r - 4.dp.toPx()), center.y + sin(rad) * (r - 4.dp.toPx()))
        drawLine(
            color = Color(0xFFE2E8F0),
            start = center,
            end = end,
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Elongated vintage slot cutout
        drawLine(
            color = Color(0xFF0E131E),
            start = Offset(center.x + cos(rad) * (r * 0.38f), center.y + sin(rad) * (r * 0.38f)),
            end = Offset(center.x + cos(rad) * (r * 0.72f), center.y + sin(rad) * (r * 0.72f)),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Vintage chrome horn cap
    drawCircle(color = Color(0xFF1F2430), radius = 17.dp.toPx(), center = center)
    drawCircle(color = Color(0xFFE2E8F0), radius = 17.dp.toPx(), center = center, style = Stroke(width = 2.8.dp.toPx()))
    drawCircle(color = MasalikAmber, radius = 7.dp.toPx(), center = center, style = Stroke(width = 1.8.dp.toPx()))
}

@Composable
private fun DirectionalSteerButtons(
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = (68f * scale.coerceIn(0.8f, 1.35f)).dp
    var leftDown by remember { mutableStateOf(false) }
    var rightDown by remember { mutableStateOf(false) }

    fun emit() {
        val s = (if (rightDown) 1f else 0f) - (if (leftDown) 1f else 0f)
        onSteerChanged(s)
    }

    // Notice RTL: Start is physically right, End is physically left
    Row(
        modifier = modifier.alpha(opacity),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(RoundedCornerShape(18.dp))
                .background(if (rightDown) MasalikAmber else MasalikObsidian.copy(alpha = 0.84f))
                .border(1.5.dp, MasalikAmber.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                .testTag("steer_right_button")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        rightDown = true; emit()
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) { rightDown = false; emit(); break }
                            c.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "توجيه يمين",
                tint = if (rightDown) MasalikObsidian else MasalikIvory,
                modifier = Modifier.size(34.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(RoundedCornerShape(18.dp))
                .background(if (leftDown) MasalikAmber else MasalikObsidian.copy(alpha = 0.84f))
                .border(1.5.dp, MasalikAmber.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                .testTag("steer_left_button")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        leftDown = true; emit()
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) { leftDown = false; emit(); break }
                            c.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "توجيه يسار",
                tint = if (leftDown) MasalikObsidian else MasalikIvory,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun VirtualSteerJoystick(
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDp = (132f * scale.coerceIn(0.8f, 1.35f)).dp
    var knobNormX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(MasalikObsidian.copy(alpha = 0.80f))
            .border(1.5.dp, MasalikAmber.copy(alpha = 0.70f), CircleShape)
            .testTag("steer_joystick")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width * 0.5f
                    val maxR = size.width * 0.36f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) {
                            knobNormX = 0f
                            onSteerChanged(0f)
                            break
                        }
                        val norm = ((c.position.x - cx) / maxR).coerceIn(-1f, 1f)
                        knobNormX = norm
                        onSteerChanged(norm)
                        c.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val cy = size.height * 0.5f
            val cx = size.width * 0.5f + knobNormX * size.width * 0.30f
            drawLine(
                color = Color(0x5594A3B8),
                start = Offset(size.width * 0.16f, cy),
                end = Offset(size.width * 0.84f, cy),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = MasalikAmber, radius = 22.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = MasalikObsidian, radius = 8.dp.toPx(), center = Offset(cx, cy))
        }
    }
}
