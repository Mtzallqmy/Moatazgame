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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.audio.YemeniRadioCatalog
import com.masalik.yemen.game.AudioMixerSettings
import com.masalik.yemen.game.DrivingRealismMode
import com.masalik.yemen.game.EngineeringTestResult
import com.masalik.yemen.game.GraphicsQuality
import com.masalik.yemen.game.PhoneDatabaseStats
import com.masalik.yemen.game.RadioPlayerSettings
import com.masalik.yemen.game.RadioStationId
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.TimeOfDayPreset
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.game.WeatherPreset
import com.masalik.yemen.ui.theme.JetBrainsMonoFontFamily
import com.masalik.yemen.ui.theme.MasalikAmber
import com.masalik.yemen.ui.theme.MasalikAmberSoft
import com.masalik.yemen.ui.theme.MasalikBorder
import com.masalik.yemen.ui.theme.MasalikCard
import com.masalik.yemen.ui.theme.MasalikCrimson
import com.masalik.yemen.ui.theme.MasalikEmerald
import com.masalik.yemen.ui.theme.MasalikIvory
import com.masalik.yemen.ui.theme.MasalikMuted
import com.masalik.yemen.ui.theme.MasalikObsidian
import com.masalik.yemen.ui.theme.MasalikSky
import com.masalik.yemen.ui.theme.MasalikSlate
import kotlin.math.roundToInt

@Composable
fun SettingsAndSaveScreen(
    saveState: SaveGameSchema,
    dbStats: PhoneDatabaseStats,
    diagnostics: List<EngineeringTestResult>,
    onSetGraphics: (GraphicsQuality) -> Unit,
    onSetRealism: (DrivingRealismMode) -> Unit,
    onSetWeather: (WeatherPreset) -> Unit,
    onSetTimeOfDay: (TimeOfDayPreset) -> Unit,
    onToggleAutoCycle: () -> Unit,
    onUpdateAudio: (AudioMixerSettings) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onResetProgress: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=World&Graphics, 1=Audio, 2=Room DB, 3=Diagnostics
    var confirmResetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        if (activeTab == 3 && diagnostics.isEmpty()) onRunDiagnostics()
    }

    if (confirmResetOpen) {
        AlertDialog(
            onDismissRequest = { confirmResetOpen = false },
            containerColor = MasalikSlate,
            titleContentColor = MasalikCrimson,
            textContentColor = MasalikIvory,
            title = { Text("تصفير بيانات الحفظ المحلية؟", fontWeight = FontWeight.Black) },
            text = { Text("سيتم حذف جميع الرحلات والمركبات المفتوحة والمهمات من قاعدة بيانات Room SQLite على هذا الهاتف.") },
            confirmButton = {
                TextButton(onClick = { confirmResetOpen = false; onResetProgress() }) {
                    Text("نعم، تصفير الكل", color = MasalikCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetOpen = false }) {
                    Text("إلغاء", color = MasalikIvory)
                }
            }
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
                Icon(Icons.Default.Settings, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(22.dp))
                Column {
                    Text("الإعدادات، الطقس، الصوت وقاعدة بيانات الهاتف", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("تعمل بالكامل بدون إنترنت · Room SQLite Local Database", color = MasalikEmerald, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MasalikCard)
                    .border(1.dp, MasalikBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("close_settings_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                Text("إغلاق", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsTabPill("الرسوم والطقس والوقت", Icons.Default.Settings, activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
            SettingsTabPill("ميكسر الصوت (6 قنوات)", Icons.Default.VolumeUp, activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
            SettingsTabPill("قاعدة بيانات الهاتف", Icons.Default.Storage, activeTab == 2, Modifier.weight(1f)) { activeTab = 2 }
            SettingsTabPill("الفحص الهندسي (10)", Icons.Default.Memory, activeTab == 3, Modifier.weight(1f)) { activeTab = 3 }
        }

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("واقعية الفيزياء والقيادة", color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrivingRealismMode.entries.forEach { m ->
                            OptionPill(m.labelAr, saveState.drivingRealism == m, Modifier.weight(1f)) { onSetRealism(m) }
                        }
                    }

                    Text("جودة الرسوميات OpenGL ES 3.0", color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsQuality.entries.forEach { q ->
                            OptionPill(q.labelAr, saveState.graphicsQuality == q, Modifier.weight(1f)) { onSetGraphics(q) }
                        }
                    }

                    Text("الطقس الديناميكي في اليمن", color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(WeatherPreset.entries) { w ->
                            OptionPill(w.labelAr, saveState.weather == w, Modifier) { onSetWeather(w) }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("وقت اليوم ودورة الـ 24 ساعة", color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("دورة تلقائية 24 ساعة", color = MasalikIvory, fontSize = 11.sp)
                            Switch(
                                checked = saveState.autoTimeCycleEnabled,
                                onCheckedChange = { onToggleAutoCycle() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MasalikObsidian, checkedTrackColor = MasalikAmber)
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(TimeOfDayPreset.entries) { tod ->
                            OptionPill(tod.labelAr, saveState.timeOfDay == tod, Modifier) { onSetTimeOfDay(tod) }
                        }
                    }
                }
            }

            1 -> {
                val m = saveState.audioMixer
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MasalikCard).border(1.dp, MasalikBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("كتم جميع الأصوات", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = m.muted, onCheckedChange = { onUpdateAudio(m.copy(muted = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = MasalikCrimson))
                        }
                        Row(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MasalikCard).border(1.dp, MasalikBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل راديو Lo-Fi اليمني", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = m.musicEnabled, onCheckedChange = { onUpdateAudio(m.copy(musicEnabled = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = MasalikAmber))
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AudioSliderCard("الصوت الرئيسي (Master)", m.masterVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(masterVolume = it)) }
                        AudioSliderCard("صوت المحرك والعادم", m.engineVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(engineVolume = it)) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AudioSliderCard("أصوات الطريق والطقس", m.environmentVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(environmentVolume = it)) }
                        AudioSliderCard("صوت الموسيقى والراديو", m.musicVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(musicVolume = it)) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AudioSliderCard("البوق والتصادم (SFX)", m.sfxVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(sfxVolume = it)) }
                        AudioSliderCard("أصوات القوائم (UI)", m.uiVolume, Modifier.weight(1f)) { onUpdateAudio(m.copy(uiVolume = it)) }
                    }
                }
            }

            2 -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left: Stats & Backup/Restore/Reset
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MasalikCard)
                            .border(1.dp, MasalikBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(dbStats.engineLabelAr, color = MasalikEmerald, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("ملف القاعدة: ${dbStats.dbName} · الإصدار v${dbStats.schemaVersion}", color = MasalikMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DbStatBox("الحجم", "${dbStats.storageUsedKb} KB", Modifier.weight(1f))
                            Spacer(Modifier.width(6.dp))
                            DbStatBox("الرحلات", "${dbStats.totalTripsCount}", Modifier.weight(1f))
                            Spacer(Modifier.width(6.dp))
                            DbStatBox("المركبات", "${dbStats.unlockedVehiclesCount}/10", Modifier.weight(1f))
                        }

                        Button(
                            onClick = onExportJson,
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("export_save_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تصدير نسخة احتياطية (.masalik.json)", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onImportJson,
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("import_save_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MasalikSky, contentColor = MasalikObsidian),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("استيراد نسخة محفوظة من الهاتف", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { confirmResetOpen = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("reset_save_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MasalikCrimson, contentColor = Color.White),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تصفير التقدم بالكامل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Right: Recent Driving Trip Logs
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MasalikCard)
                            .border(1.dp, MasalikBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text("سجل أحدث الرحلات المحفوظة في Room SQLite", color = MasalikAmberSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (dbStats.recentTrips.isEmpty()) {
                            Text("انطلق بالقيادة في شوارع ومدن اليمن ليتم تسجيل رحلاتك تلقائياً هنا.", color = MasalikMuted, fontSize = 11.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(dbStats.recentTrips) { trip ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MasalikSlate)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(trip.regionNameAr, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(VehicleFleetCatalog.specOf(trip.vehicleId).nameAr, color = MasalikMuted, fontSize = 10.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${"%.2f".format(trip.distanceKm)} كم", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("أقصى سرعة ${trip.maxSpeedKmh.roundToInt()} كم/س", color = MasalikSky, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("نتائج الفحص الهندسي المباشر لأنظمة اللعبة الـ 10", color = MasalikEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onRunDiagnostics,
                            colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp).testTag("run_diagnostics_button")
                        ) {
                            Text("إعادة الفحص الآن", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(diagnostics) { d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MasalikCard)
                                    .border(1.dp, if (d.passed) MasalikEmerald.copy(alpha = 0.55f) else MasalikCrimson, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (d.passed) MasalikEmerald else MasalikCrimson, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(d.titleAr, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(d.detailsAr, color = MasalikMuted, fontSize = 10.sp)
                                    }
                                }
                                Text(d.metricLabel, color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YemeniRadioOverlay(
    radio: RadioPlayerSettings,
    onTogglePlay: () -> Unit,
    onSelectStation: (RadioStationId) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleVinyl: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeStation = YemeniRadioCatalog.stationOf(radio.stationId)

    Column(
        modifier = modifier
            .width(340.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MasalikObsidian.copy(alpha = 0.95f))
            .border(1.5.dp, MasalikAmber.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Radio, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(18.dp))
                Text("راديو مَسَالِك — Lo-Fi يمني", color = MasalikAmberSoft, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MasalikCard)
                    .clickable(onClick = onClose)
                    .testTag("close_radio_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق الراديو", tint = MasalikIvory, modifier = Modifier.size(15.dp))
            }
        }

        // Active Station Card + Play/Pause
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MasalikCard)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${activeStation.frequencyLabel} · ${activeStation.titleAr}", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("${activeStation.cityAr} · ${activeStation.maqamAr}", color = MasalikEmerald, fontSize = 10.sp)
                Text(activeStation.subtitleAr, color = MasalikMuted, fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MasalikAmber)
                    .clickable(onClick = onTogglePlay)
                    .testTag("radio_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (radio.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (radio.isPlaying) "إيقاف" else "تشغيل",
                    tint = MasalikObsidian,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 5 Yemeni Stations Selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(YemeniRadioCatalog.STATIONS) { st ->
                val sel = st.id == radio.stationId
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) MasalikAmber else MasalikCard)
                        .border(1.dp, if (sel) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectStation(st.id) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(st.frequencyLabel, color = if (sel) MasalikObsidian else MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(st.titleAr, color = if (sel) MasalikObsidian else MasalikIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Volume & Vinyl Crackle
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("الصوت", color = MasalikMuted, fontSize = 10.sp)
            Slider(
                value = radio.volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (radio.lofiVinylEnabled) MasalikAmber.copy(alpha = 0.25f) else MasalikCard)
                    .border(1.dp, if (radio.lofiVinylEnabled) MasalikAmber else MasalikBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleVinyl)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text("فينيل Lo-Fi", color = if (radio.lofiVinylEnabled) MasalikAmberSoft else MasalikMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) MasalikAmber else MasalikCard)
            .border(1.dp, if (active) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (active) MasalikObsidian else MasalikAmber, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) MasalikObsidian else MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun OptionPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) MasalikAmber else MasalikCard)
            .border(1.dp, if (selected) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) MasalikObsidian else MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AudioSliderCard(label: String, value: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MasalikCard)
            .border(1.dp, MasalikBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).roundToInt()}%", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
        )
    }
}

@Composable
private fun DbStatBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MasalikSlate)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(label, color = MasalikMuted, fontSize = 10.sp)
    }
}
