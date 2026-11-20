package com.masalik.yemen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.HudCustomizationSettings
import com.masalik.yemen.game.SmartWheelSettings
import com.masalik.yemen.game.TouchControlMode
import com.masalik.yemen.ui.theme.JetBrainsMonoFontFamily
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikAmberSoft
import com.masalik.yemen.ui.theme.MasalikBorder
import com.masalik.yemen.ui.theme.MasalikCard
import com.masalik.yemen.ui.theme.MasalikEmerald
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikMuted
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikSky
import com.masalik.yemen.ui.theme.MasalikSlate
import kotlin.math.roundToInt

private data class HudLayoutPreset(
    val nameAr: String,
    val descAr: String,
    val mode: TouchControlMode,
    val wheelDeg: Int,
    val scale: Float,
    val opacity: Float,
    val mirror: Boolean
)

@Composable
fun ControlStudioScreen(
    hudSettings: HudCustomizationSettings,
    onUpdateHud: (HudCustomizationSettings) -> Unit,
    onCalibrateGyro: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=Mode, 1=Smart Wheel, 2=Presets

    val presets = remember {
        listOf(
            HudLayoutPreset("الوضع المتوازن (افتراضي)", "مقود ذكي 270° بحجم مريح ووضوح عالٍ", TouchControlMode.WHEEL, 270, 1.0f, 0.92f, false),
            HudLayoutPreset("محاكاة احترافية 900°", "دوران مقود واقعي كامل 900° لمرتفعات اليمن", TouchControlMode.WHEEL, 900, 1.15f, 0.88f, false),
            HudLayoutPreset("قيادة بالأزرار السريعة", "أزرار يمين/يسار دقيقة للمنعطفات الحادة", TouchControlMode.BUTTONS, 270, 1.10f, 0.95f, false),
            HudLayoutPreset("توجيه الجيروسكوب (إمالة)", "إمالة الهاتف للتوجيه السلس في الوضع الأفقي", TouchControlMode.TILT, 360, 1.0f, 0.84f, false),
            HudLayoutPreset("عصا تحكم رياضية", "عصا تحكم أفقية ناعمة للسباقات السريعة", TouchControlMode.JOYSTICK, 180, 1.05f, 0.90f, false),
            HudLayoutPreset("وضع اليد اليسرى المعكوس", "تبديل أماكن التوجيه والدواسات لمستخدمي اليسار", TouchControlMode.WHEEL, 360, 1.0f, 0.92f, true)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MasalikObsidian.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MasalikSlate)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(22.dp))
                Column {
                    Text("استوديو التحكم والمقود الذكي", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("اختر أسلوب القيادة، اضبط زاوية دوران المقود (180°–900°)، وخصّص الشفافية", color = MasalikMuted, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MasalikCard)
                    .border(1.dp, MasalikBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("close_control_studio_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                Text("حفظ وإغلاق", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioTabChip("أسلوب التحكم والواجهة", Icons.Default.Gamepad, activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
            StudioTabChip("ضبط المقود الذكي", Icons.Default.Tune, activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
            StudioTabChip("قوالب جاهزة (6)", Icons.Default.GridView, activeTab == 2, Modifier.weight(1f)) { activeTab = 2 }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                0 -> {
                    Text("اختر نظام التوجيه الأساسي", color = MasalikAmberSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TouchControlMode.entries.forEach { mode ->
                            val sel = hudSettings.controlMode == mode
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (sel) MasalikAmber.copy(alpha = 0.22f) else MasalikCard)
                                    .border(1.5.dp, if (sel) MasalikAmber else MasalikBorder, RoundedCornerShape(14.dp))
                                    .clickable { onUpdateHud(hudSettings.copy(controlMode = mode)) }
                                    .padding(12.dp)
                                    .testTag("control_mode_${mode.name.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(mode.labelAr, color = if (sel) MasalikAmberSoft else MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    if (hudSettings.controlMode == TouchControlMode.TILT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MasalikEmerald.copy(alpha = 0.16f))
                                .border(1.dp, MasalikEmerald, RoundedCornerShape(12.dp))
                                .clickable(onClick = onCalibrateGyro)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حساس الجيروسكوب الأفقي مفعّل — اضغط لمعايرة نقطة المنتصف الآن", color = MasalikEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("معايرة", color = MasalikIvory, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MasalikCard)
                                .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text("حجم أزرار المقود والدواسات: ${(hudSettings.buttonScale * 100).roundToInt()}%", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = hudSettings.buttonScale,
                                onValueChange = { onUpdateHud(hudSettings.copy(buttonScale = it)) },
                                valueRange = 0.8f..1.35f,
                                colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MasalikCard)
                                .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text("شفافية لوحة القيادة: ${(hudSettings.hudOpacity * 100).roundToInt()}%", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = hudSettings.hudOpacity,
                                onValueChange = { onUpdateHud(hudSettings.copy(hudOpacity = it)) },
                                valueRange = 0.45f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToggleCard("إظهار الخريطة المصغرة GPS", hudSettings.showMinimap, Modifier.weight(1f)) {
                            onUpdateHud(hudSettings.copy(showMinimap = it))
                        }
                        ToggleCard("عكس اتجاه الأزرار (يمين / يسار)", hudSettings.mirrorControls, Modifier.weight(1f)) {
                            onUpdateHud(hudSettings.copy(mirrorControls = it))
                        }
                    }
                }

                1 -> {
                    val sw = hudSettings.smartWheel
                    Text("زاوية دوران المقود الذكي (من قفل إلى قفل)", color = MasalikAmberSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(180, 270, 360, 540, 900).forEach { deg ->
                            val sel = sw.rotationRangeDeg == deg
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) MasalikAmber else MasalikCard)
                                    .border(1.dp, if (sel) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(12.dp))
                                    .clickable { onUpdateHud(hudSettings.copy(smartWheel = sw.copy(rotationRangeDeg = deg))) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${deg}°", color = if (sel) MasalikObsidian else MasalikIvory, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MasalikCard)
                                .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text("حساسية استجابة المقود: ${"%.2f".format(sw.sensitivity)}x", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = sw.sensitivity,
                                onValueChange = { onUpdateHud(hudSettings.copy(smartWheel = sw.copy(sensitivity = it))) },
                                valueRange = 0.6f..1.8f,
                                colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MasalikCard)
                                .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text("منطقة الثبات الوسطى (Deadzone): ${sw.deadZoneDeg.roundToInt()}°", color = MasalikSky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = sw.deadZoneDeg,
                                onValueChange = { onUpdateHud(hudSettings.copy(smartWheel = sw.copy(deadZoneDeg = it))) },
                                valueRange = 0f..14f,
                                colors = SliderDefaults.colors(thumbColor = MasalikSky, activeTrackColor = MasalikSky)
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToggleCard("إرجاع المقود تلقائياً للمنتصف", sw.autoCenterEnabled, Modifier.weight(1f)) {
                            onUpdateHud(hudSettings.copy(smartWheel = sw.copy(autoCenterEnabled = it)))
                        }
                        ToggleCard("مساعد ثبات المسار في المنعطفات", sw.steeringAssist, Modifier.weight(1f)) {
                            onUpdateHud(hudSettings.copy(smartWheel = sw.copy(steeringAssist = it)))
                        }
                    }
                }

                else -> {
                    presets.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { preset ->
                                val active = hudSettings.layoutPresetName == preset.nameAr
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (active) MasalikAmber.copy(alpha = 0.18f) else MasalikCard)
                                        .border(1.5.dp, if (active) MasalikAmber else MasalikBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            onUpdateHud(
                                                hudSettings.copy(
                                                    controlMode = preset.mode,
                                                    layoutPresetName = preset.nameAr,
                                                    buttonScale = preset.scale,
                                                    hudOpacity = preset.opacity,
                                                    mirrorControls = preset.mirror,
                                                    smartWheel = SmartWheelSettings(rotationRangeDeg = preset.wheelDeg)
                                                )
                                            )
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(preset.nameAr, color = if (active) MasalikAmberSoft else MasalikIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.height(3.dp))
                                    Text(preset.descAr, color = MasalikMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MasalikAmber else MasalikCard)
            .border(1.dp, if (active) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (active) MasalikObsidian else MasalikAmber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) MasalikObsidian else MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ToggleCard(label: String, checked: Boolean, modifier: Modifier, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MasalikCard)
            .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = MasalikObsidian, checkedTrackColor = MasalikAmber)
        )
    }
}
