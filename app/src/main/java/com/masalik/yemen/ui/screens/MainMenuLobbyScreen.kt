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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.MissionsCatalog
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.TimeOfDayPreset
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.ui.theme.JetBrainsMonoFontFamily
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikAmberSoft
import com.masalik.yemen.ui.theme.MasalikBorder
import com.masalik.yemen.ui.theme.MasalikEmerald
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikMuted
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikSky

@Composable
fun MainMenuLobbyScreen(
    saveState: SaveGameSchema,
    activeTimePhase: TimeOfDayPreset,
    onStartDriving: () -> Unit,
    onCycleDayNight: () -> Unit,
    onOpenWorldMap: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenControlStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleRadio: () -> Unit
) {
    val rank = MissionsCatalog.getDriverRank(saveState.driverXp)
    val vehicle = VehicleFleetCatalog.specOf(saveState.selectedVehicleId)
    val completedMissions = saveState.missionCompletions.values.count { it > 0 }
    val wheelCfg = saveState.hudSettings.smartWheel

    val timeIcon = when (activeTimePhase) {
        TimeOfDayPreset.NIGHT -> Icons.Default.NightsStay
        TimeOfDayPreset.SUNSET, TimeOfDayPreset.DAWN, TimeOfDayPreset.SUNRISE -> Icons.Default.WbTwilight
        else -> Icons.Default.WbSunny
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MasalikObsidian.copy(alpha = 0.88f),
                        MasalikObsidian.copy(alpha = 0.44f),
                        MasalikObsidian.copy(alpha = 0.12f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Top Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MasalikObsidian.copy(alpha = 0.86f))
                    .border(1.dp, MasalikBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Explore, contentDescription = "رتبة السائق", tint = MasalikAmber, modifier = Modifier.size(16.dp))
                Text(rank.rankTitleAr, color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("·", color = MasalikMuted)
                Text(
                    "${saveState.masalikCoins} ر.ي",
                    color = MasalikAmber,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text("·", color = MasalikMuted)
                Text(
                    "${"%.1f".format(saveState.totalCareerDistanceM / 1000f)} كم",
                    color = MasalikIvory,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp
                )
                Text("·", color = MasalikMuted)
                Text("100% محلي", color = MasalikEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Right Controls: Quick Live Day/Night Showroom Switcher + Yemeni Radio
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MasalikObsidian.copy(alpha = 0.86f))
                        .border(1.dp, if (activeTimePhase == TimeOfDayPreset.NIGHT) MasalikSky else MasalikAmber.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onCycleDayNight)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("lobby_day_night_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        timeIcon,
                        contentDescription = "تبديل إضاءة النهار والليل",
                        tint = if (activeTimePhase == TimeOfDayPreset.NIGHT) MasalikSky else MasalikAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "إضاءة: ${activeTimePhase.labelAr}",
                        color = MasalikIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (saveState.radioSettings.isPlaying) MasalikAmber else MasalikObsidian.copy(alpha = 0.86f))
                        .border(1.dp, if (saveState.radioSettings.isPlaying) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onToggleRadio)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("lobby_radio_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = "راديو Lo-Fi اليمني",
                        tint = if (saveState.radioSettings.isPlaying) MasalikObsidian else MasalikAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (saveState.radioSettings.isPlaying) "الراديو يعمل" else "راديو يمني",
                        color = if (saveState.radioSettings.isPlaying) MasalikObsidian else MasalikIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Right-aligned (RTL Start) Editorial Menu Panel leaving the 3D Showroom open on the left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(max = 395.dp)
                .fillMaxHeight()
                .padding(top = 46.dp, bottom = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "MASALIK · 3D YEMEN DRIVING SIM",
                color = MasalikAmber,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "مَسَالِك",
                style = MaterialTheme.typography.displayLarge,
                color = MasalikIvory
            )
            Text(
                "محاكاة قيادة واقعية — مدن وجبال اليمن",
                color = MasalikAmberSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "بوليفاردات حديثة وأبراج زجاجية إلى جانب تعز وإب وصنعاء القديمة وعدن وشبام — بإضاءة ليلية حقيقية ودركسون شرقي وأوروبي.",
                color = MasalikMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(9.dp))

            // Active vehicle + cockpit steering card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MasalikObsidian.copy(alpha = 0.86f))
                    .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${vehicle.nameAr} · ${vehicle.categoryAr}", color = MasalikIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("الدركسون: ${wheelCfg.wheelStyle.shortLabelAr} (${wheelCfg.seatSide.shortLabelAr})", color = MasalikSky, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${vehicle.maxPowerHp} HP", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${saveState.unlockedVehicles.size}/10 مركبات", color = MasalikMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onStartDriving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_driving_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("انطلق للقيادة الآن", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LobbyNavCard(
                    title = "خريطة مدن اليمن",
                    subtitle = "${saveState.visitedCities.size}/${RoadSplineNetwork.YEMEN_CITY_HUBS.size} مدن · انتقال سريع",
                    icon = Icons.Default.Map,
                    accent = MasalikEmerald,
                    tag = "lobby_world_map_button",
                    onClick = onOpenWorldMap,
                    modifier = Modifier.weight(1f)
                )
                LobbyNavCard(
                    title = "القصة والمهمات",
                    subtitle = "$completedMissions/65 مهمة · 5 فصول",
                    icon = Icons.Default.Flag,
                    accent = MasalikAmber,
                    tag = "lobby_missions_button",
                    onClick = onOpenMissions,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LobbyNavCard(
                    title = "الكراج والورشة 3D",
                    subtitle = "10 مركبات وتعديلات كاملة",
                    icon = Icons.Default.DirectionsCar,
                    accent = MasalikSky,
                    tag = "lobby_garage_button",
                    onClick = onOpenGarage,
                    modifier = Modifier.weight(1f)
                )
                LobbyNavCard(
                    title = "استوديو الدركسون",
                    subtitle = "شرقي · أوروبي · رياضي · RHD/LHD",
                    icon = Icons.Default.Tune,
                    accent = Color(0xFFFB7185),
                    tag = "lobby_control_studio_button",
                    onClick = onOpenControlStudio,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MasalikObsidian.copy(alpha = 0.85f))
                    .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenSettings)
                    .padding(vertical = 10.dp, horizontal = 12.dp)
                    .testTag("lobby_settings_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = MasalikAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("الإعدادات، الطقس، الميكسر الصوتي وقاعدة بيانات الهاتف", color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom credits footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م", color = MasalikMuted, fontSize = 10.sp)
            Text("مَسَالِك · OPENGLES 3.0 PBR · ROOM SQLITE", color = MasalikMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LobbyNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MasalikObsidian.copy(alpha = 0.86f))
            .border(1.dp, MasalikBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MasalikMuted, fontSize = 10.sp, maxLines = 1)
    }
}
