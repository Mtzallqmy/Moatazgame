package com.masalik.yemen.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.ReplayCameraAngle
import com.masalik.yemen.ui.MasalikUiState
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

/**
 * Cinematic 20-Second Instant Replay Studio Overlay:
 * Supports Drone 360° Touch Orbit, 0.25x/0.5x/1.0x Slow-Motion playback,
 * timeline scrubbing, 4 cinematic camera angles, and zero-permission HD Gallery capture.
 */
@Composable
fun ReplayStudioOverlay(
    state: MasalikUiState,
    onOrbitDrone: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onScrub: (Float) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectCamera: (ReplayCameraAngle) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onExitReplay: () -> Unit
) {
    val t = state.telemetry
    val totalSec = state.replayDurationSec.coerceAtLeast(1f)
    val currentSec = (state.replayProgress * totalSec)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.replayCameraAngle) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastX = down.position.x
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) break
                        val dx = c.position.x - lastX
                        lastX = c.position.x
                        onOrbitDrone(dx * 0.011f)
                        c.consume()
                    }
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top Cinema Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MasalikObsidian.copy(alpha = 0.86f))
                    .border(1.2.dp, MasalikCrimson.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.replayPlaying) MasalikCrimson else MasalikAmber)
                )
                Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(18.dp))
                Text("استوديو الإعادة السينمائية 20ث — مَسَالِك", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text("·", color = MasalikMuted)
                Text(
                    "${t.speedKmh.roundToInt()} كم/س · ${t.gear}",
                    color = MasalikSky,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // HD Gallery Capture Button (Zero-permission PixelCopy + MediaStore)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(MasalikAmber, Color(0xFFF59E0B)))
                        )
                        .clickable(onClick = onCaptureScreenshot)
                        .padding(horizontal = 13.dp, vertical = 9.dp)
                        .testTag("replay_save_hd_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "حفظ لقطة HD", tint = MasalikObsidian, modifier = Modifier.size(17.dp))
                    Text("حفظ لقطة HD", color = MasalikObsidian, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                // Exit Replay -> Return to Live Driving
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MasalikObsidian.copy(alpha = 0.90f))
                        .border(1.2.dp, MasalikBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onExitReplay)
                        .padding(horizontal = 13.dp, vertical = 9.dp)
                        .testTag("exit_replay_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "العودة للقيادة", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                    Text("العودة للقيادة", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom Studio Deck (Timeline Scrubber + Slow-Motion Chips + Camera Selector)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(20.dp))
                .background(MasalikObsidian.copy(alpha = 0.90f))
                .border(1.2.dp, MasalikAmber.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Play/Pause + Scrubber + Timecode + Slow-Motion Speeds
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MasalikAmber)
                        .clickable(onClick = onTogglePlay)
                        .testTag("replay_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.replayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.replayPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = MasalikObsidian,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "${"%.1f".format(currentSec)}s / ${"%.1f".format(totalSec)}s",
                    color = MasalikIvory,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = state.replayProgress,
                    onValueChange = onScrub,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).testTag("replay_timeline_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MasalikAmber,
                        activeTrackColor = MasalikAmber,
                        inactiveTrackColor = MasalikSlate
                    )
                )

                // Slow-Motion Speed Selector (0.25x, 0.5x, 1.0x)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(0.25f to "0.25x", 0.5f to "0.5x", 1.0f to "1.0x").forEach { (spd, lbl) ->
                        val active = kotlin.math.abs(state.replaySpeed - spd) < 0.05f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MasalikSky else MasalikSlate)
                                .border(1.dp, if (active) Color.White else MasalikBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectSpeed(spd) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("replay_speed_$lbl")
                        ) {
                            Text(
                                lbl,
                                color = if (active) MasalikObsidian else MasalikIvory,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Row 2: 4 Cinematic Camera Angles + Orbit hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MasalikEmerald, modifier = Modifier.size(16.dp))
                    ReplayCameraAngle.entries.forEach { angle ->
                        val sel = state.replayCameraAngle == angle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) MasalikAmber else MasalikSlate)
                                .border(1.dp, if (sel) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectCamera(angle) }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                                .testTag("replay_cam_${angle.name.lowercase()}")
                        ) {
                            Text(
                                angle.labelAr,
                                color = if (sel) MasalikObsidian else MasalikIvory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (state.replayCameraAngle == ReplayCameraAngle.DRONE_ORBIT) {
                    Text(
                        "اسحب الشاشة لتدوير كاميرا الدرون 360°",
                        color = MasalikMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
