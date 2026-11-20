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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.GameModeCategory
import com.masalik.yemen.game.MissionDefinition
import com.masalik.yemen.game.MissionsCatalog
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.VehicleFleetCatalog
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

@Composable
fun MissionsAndStoryScreen(
    saveState: SaveGameSchema,
    onStartMission: (MissionDefinition) -> Unit,
    onStartFreeRoam: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=Story, 1=All 65, 2=Daily
    var selectedChapterId by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf(GameModeCategory.ALL) }
    val dailyChallenges = remember { MissionsCatalog.getDailyChallenges() }
    val completedCount = saveState.missionCompletions.values.count { it > 0 }

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
                Icon(Icons.Default.Flag, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(22.dp))
                Column {
                    Text("طور القصة والمهمات (65 مهمة · بدون إنترنت)", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("منجز: $completedCount / 65 مهمة · رصيدك: ${saveState.masalikCoins} ر.ي", color = MasalikMuted, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onStartFreeRoam,
                    colors = ButtonDefaults.buttonColors(containerColor = MasalikEmerald, contentColor = MasalikObsidian),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp).testTag("missions_free_roam_button")
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("تجوال حر مفتوح", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MasalikCard)
                        .border(1.dp, MasalikBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("close_missions_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                    Text("إغلاق", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Mode Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MissionTabPill("طور القصة (5 فصول)", Icons.Default.AutoStories, activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
            MissionTabPill("جميع المهمات الـ 65", Icons.Default.Flag, activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
            MissionTabPill("تحديات اليوم", Icons.Default.Today, activeTab == 2, Modifier.weight(1f)) { activeTab = 2 }
        }

        // Body
        when (activeTab) {
            0 -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chapter Selector Column
                    LazyColumn(
                        modifier = Modifier.weight(0.95f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MissionsCatalog.STORY_CHAPTERS) { ch ->
                            val selected = ch.id == selectedChapterId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) MasalikAmber.copy(alpha = 0.18f) else MasalikCard)
                                    .border(1.5.dp, if (selected) MasalikAmber else MasalikBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedChapterId = ch.id }
                                    .padding(11.dp)
                            ) {
                                Text(ch.titleAr, color = if (selected) MasalikAmberSoft else MasalikIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text("${ch.narratorNameAr} — ${ch.narratorRoleAr}", color = MasalikEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(ch.summaryAr, color = MasalikMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    // Chapter Missions
                    val chapterMissions = remember(selectedChapterId) {
                        MissionsCatalog.ALL_OFFLINE_MISSIONS.filter { it.chapterId == selectedChapterId }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1.25f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chapterMissions) { m ->
                            MissionRowCard(
                                mission = m,
                                completedCount = saveState.missionCompletions[m.id] ?: 0,
                                bestTimeSec = saveState.missionBestTimesSec[m.id],
                                onPlay = { onStartMission(m) }
                            )
                        }
                    }
                }
            }

            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GameModeCategory.entries.filter { it != GameModeCategory.FREE_ROAM }) { cat ->
                            val sel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) MasalikAmber else MasalikCard)
                                    .border(1.dp, if (sel) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat.labelAr, color = if (sel) MasalikObsidian else MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val filtered = remember(selectedCategory) {
                        if (selectedCategory == GameModeCategory.ALL) MissionsCatalog.ALL_OFFLINE_MISSIONS
                        else MissionsCatalog.ALL_OFFLINE_MISSIONS.filter { it.category == selectedCategory }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.id }) { m ->
                            MissionRowCard(
                                mission = m,
                                completedCount = saveState.missionCompletions[m.id] ?: 0,
                                bestTimeSec = saveState.missionBestTimesSec[m.id],
                                onPlay = { onStartMission(m) }
                            )
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "تحديات اليوم المحلية — تتجدد تلقائياً كل يوم دون حاجة إلى الإنترنت",
                        color = MasalikEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dailyChallenges) { m ->
                            MissionRowCard(
                                mission = m,
                                completedCount = saveState.missionCompletions[m.id] ?: 0,
                                bestTimeSec = saveState.missionBestTimesSec[m.id],
                                onPlay = { onStartMission(m) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionTabPill(
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
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (active) MasalikObsidian else MasalikAmber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) MasalikObsidian else MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MissionRowCard(
    mission: MissionDefinition,
    completedCount: Int,
    bestTimeSec: Float?,
    onPlay: () -> Unit
) {
    val recSpec = VehicleFleetCatalog.specOf(mission.recommendedVehicleId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MasalikCard)
            .border(1.dp, if (completedCount > 0) MasalikEmerald.copy(alpha = 0.6f) else MasalikBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (completedCount > 0) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "منجزة", tint = MasalikEmerald, modifier = Modifier.size(16.dp))
                }
                Text(mission.titleAr, color = MasalikIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(
                    mission.category.labelAr,
                    color = MasalikAmberSoft,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MasalikSlate)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(mission.descriptionAr, color = MasalikMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المكافأة: +${mission.rewardCoins} ر.ي", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("الطقس: ${mission.weatherPreset.labelAr}", color = MasalikSky, fontSize = 11.sp)
                Text("المقترحة: ${recSpec.nameAr}", color = MasalikEmerald, fontSize = 11.sp)
                if (bestTimeSec != null && bestTimeSec > 0f) {
                    Text("أفضل زمن: ${bestTimeSec.toInt()}ث", color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.height(38.dp).testTag("start_mission_${mission.id}")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("ابدأ", fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}
