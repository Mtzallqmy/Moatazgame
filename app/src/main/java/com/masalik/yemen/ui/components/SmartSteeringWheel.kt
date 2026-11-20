package com.masalik.yemen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.SmartWheelSettings
import com.masalik.yemen.game.TouchControlMode
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikObsidian
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun SteeringControlWidget(
    controlMode: TouchControlMode,
    settings: SmartWheelSettings,
    currentWheelDeg: Float,
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    onCalibrateGyro: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (controlMode) {
        TouchControlMode.WHEEL -> SmartSteeringWheel(
            settings = settings,
            wheelDeg = currentWheelDeg,
            scale = scale,
            opacity = opacity,
            onSteerChanged = onSteerChanged,
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
                    .background(MasalikObsidian.copy(alpha = 0.82f))
                    .border(1.5.dp, MasalikAmber.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onCalibrateGyro()
                        }
                    }
                    .size(width = 148.dp, height = 56.dp)
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
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDp = (138f * scale.coerceIn(0.75f, 1.4f)).dp
    var localAngleDeg by remember { mutableFloatStateOf(0f) }
    var isGrabbed by remember { mutableStateOf(false) }

    val displayDeg = if (isGrabbed) localAngleDeg else wheelDeg
    val halfLock = (settings.rotationRangeDeg * 0.5f).coerceAtLeast(90f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .alpha(opacity)
            .testTag("smart_steering_wheel")
            .pointerInput(settings) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    var prevAngle = atan2(down.position.y - cy, down.position.x - cx)
                    isGrabbed = true
                    var accumDeg = localAngleDeg

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            isGrabbed = false
                            if (settings.autoCenterEnabled) {
                                localAngleDeg = 0f
                                onSteerChanged(0f)
                            }
                            break
                        }
                        val currAngle = atan2(change.position.y - cy, change.position.x - cx)
                        var delta = currAngle - prevAngle
                        if (delta > PI) delta -= (2.0 * PI).toFloat()
                        if (delta < -PI) delta += (2.0 * PI).toFloat()
                        prevAngle = currAngle

                        accumDeg = (accumDeg + delta * (180f / PI.toFloat()) * settings.sensitivity)
                            .coerceIn(-halfLock, halfLock)
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
            val r = size.minDimension * 0.44f
            val center = Offset(size.width * 0.5f, size.height * 0.5f)

            // Outer ambient ring
            drawCircle(
                color = Color(0xCC090D16),
                radius = r + 6.dp.toPx(),
                center = center
            )

            rotate(degrees = displayDeg, pivot = center) {
                // Leather rim
                drawCircle(
                    color = Color(0xFF1F2937),
                    radius = r,
                    center = center,
                    style = Stroke(width = 15.dp.toPx())
                )
                drawCircle(
                    color = if (isGrabbed) MasalikAmber else Color(0xFF475569),
                    radius = r,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Top amber center alignment stripe
                drawLine(
                    color = MasalikAmber,
                    start = Offset(center.x, center.y - r - 6.dp.toPx()),
                    end = Offset(center.x, center.y - r + 7.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3-Spoke steering wheel arms
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(center.x - r + 6.dp.toPx(), center.y),
                    end = Offset(center.x + r - 6.dp.toPx(), center.y),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF334155),
                    start = center,
                    end = Offset(center.x, center.y + r - 6.dp.toPx()),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center Yemeni emblem hub
                drawCircle(color = Color(0xFF0F172A), radius = 19.dp.toPx(), center = center)
                drawCircle(
                    color = MasalikAmber,
                    radius = 19.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(color = MasalikAmber, radius = 5.dp.toPx(), center = center)
            }
        }
    }
}

@Composable
private fun DirectionalSteerButtons(
    scale: Float,
    opacity: Float,
    onSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = (66f * scale.coerceIn(0.8f, 1.35f)).dp
    var leftDown by remember { mutableStateOf(false) }
    var rightDown by remember { mutableStateOf(false) }

    fun emit() {
        val s = (if (rightDown) 1f else 0f) - (if (leftDown) 1f else 0f)
        onSteerChanged(s)
    }

    // Notice RTL: Left arrow sits physically on left, Right arrow sits physically on right
    Row(
        modifier = modifier.alpha(opacity),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(RoundedCornerShape(18.dp))
                .background(if (rightDown) MasalikAmber else MasalikObsidian.copy(alpha = 0.82f))
                .border(1.5.dp, MasalikAmber.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
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
                .background(if (leftDown) MasalikAmber else MasalikObsidian.copy(alpha = 0.82f))
                .border(1.5.dp, MasalikAmber.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
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
    val sizeDp = (128f * scale.coerceIn(0.8f, 1.35f)).dp
    var knobNormX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(MasalikObsidian.copy(alpha = 0.78f))
            .border(1.5.dp, MasalikAmber.copy(alpha = 0.65f), CircleShape)
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
            drawCircle(color = MasalikAmber, radius = 22.dp.toPx(), center = Offset(cx, cy))
        }
    }
}
