package com.masalik.yemen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.RoadSplineNetwork
import com.masalik.yemen.game.YemenCityHub
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
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun YemenWorldMapScreen(
    currentDistanceM: Float,
    visitedCities: List<String>,
    onFastTravel: (YemenCityHub) -> Unit,
    onClose: () -> Unit
) {
    val nearestCity = remember(currentDistanceM) {
        RoadSplineNetwork.YEMEN_CITY_HUBS.minBy { abs(it.distanceM - currentDistanceM) }
    }
    var selectedCity by remember { mutableStateOf(nearestCity) }

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
                Icon(Icons.Default.Explore, contentDescription = null, tint = MasalikEmerald, modifier = Modifier.size(22.dp))
                Column {
                    Text("خريطة دروب ومدن اليمن المفتوحة (22 كم · 9 مناطق)", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("اضغط على أي مدينة يمنية للانتقال السريع فورًا واستكشاف شوارعها وسكانها", color = MasalikMuted, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MasalikCard)
                    .border(1.dp, MasalikBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("close_map_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                Text("إغلاق", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Split Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Topographic Yemen Canvas Map
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A1324))
                    .border(1.dp, MasalikBorder, RoundedCornerShape(18.dp))
                    .padding(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("خريطة الجمهورية اليمنية — شبكة مسالك", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("موقعك: ${"%.2f".format(currentDistanceM / 1000f)} كم", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val hit = RoadSplineNetwork.YEMEN_CITY_HUBS.minByOrNull { c ->
                                        hypot(c.mapX / 100f * w - offset.x, c.mapY / 100f * h - offset.y)
                                    }
                                    if (hit != null) selectedCity = hit
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        // Grid lines
                        for (i in 1..9) {
                            drawLine(Color(0xFF1E293B), Offset(w * i / 10f, 0f), Offset(w * i / 10f, h), strokeWidth = 1f)
                            drawLine(Color(0xFF1E293B), Offset(0f, h * i / 10f), Offset(w, h * i / 10f), strokeWidth = 1f)
                        }
                        // Yemen landmass silhouette
                        val land = Path().apply {
                            moveTo(w * 0.12f, h * 0.22f)
                            lineTo(w * 0.42f, h * 0.18f)
                            lineTo(w * 0.88f, h * 0.18f)
                            lineTo(w * 0.93f, h * 0.48f)
                            lineTo(w * 0.76f, h * 0.62f)
                            lineTo(w * 0.56f, h * 0.72f)
                            lineTo(w * 0.38f, h * 0.89f)
                            lineTo(w * 0.19f, h * 0.88f)
                            lineTo(w * 0.13f, h * 0.62f)
                            lineTo(w * 0.10f, h * 0.42f)
                            close()
                        }
                        drawPath(land, color = Color(0xFF152238))
                        drawPath(land, color = Color(0xFF334155), style = Stroke(width = 2.5f))

                        // Highway polyline connecting all 10 cities
                        val route = Path()
                        RoadSplineNetwork.YEMEN_CITY_HUBS.forEachIndexed { idx, c ->
                            val pt = Offset(c.mapX / 100f * w, c.mapY / 100f * h)
                            if (idx == 0) route.moveTo(pt.x, pt.y) else route.lineTo(pt.x, pt.y)
                        }
                        drawPath(
                            route,
                            color = MasalikAmber,
                            style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                        )

                        // City pins
                        RoadSplineNetwork.YEMEN_CITY_HUBS.forEach { c ->
                            val pt = Offset(c.mapX / 100f * w, c.mapY / 100f * h)
                            val isSel = c.id == selectedCity.id
                            val isVis = c.id in visitedCities
                            if (isSel) {
                                drawCircle(MasalikAmber.copy(alpha = 0.28f), radius = 22f, center = pt)
                            }
                            drawCircle(
                                color = when {
                                    c.id == nearestCity.id -> MasalikSky
                                    isSel -> MasalikAmber
                                    isVis -> MasalikEmerald
                                    else -> MasalikMuted
                                },
                                radius = if (isSel) 11f else 8f,
                                center = pt
                            )
                        }
                    }
                    // Sea labels
                    Text("البحر الأحمر", color = MasalikSky.copy(alpha = 0.55f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                    Text("خليج عدن والبحر العربي", color = MasalikSky.copy(alpha = 0.55f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp))
                }
            }

            // Right Detail & City Selector List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Selected City Info Card + Fast Travel Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MasalikSlate)
                        .border(1.dp, MasalikAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedCity.nameAr, color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("${selectedCity.altitudeM}م", color = MasalikSky, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${selectedCity.governorateAr} · الطقس: ${selectedCity.defaultWeather.labelAr}", color = MasalikEmerald, fontSize = 11.sp)
                    Text(selectedCity.subtitleAr, color = MasalikMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedCity.highlightsAr.forEach { hl ->
                            Text(
                                hl,
                                color = MasalikIvory,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MasalikCard)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { onFastTravel(selectedCity) },
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("fast_travel_city_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("انتقال سريع والقيادة في ${selectedCity.nameAr}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // All 10 Cities List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(RoadSplineNetwork.YEMEN_CITY_HUBS) { city ->
                        val isSel = city.id == selectedCity.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) MasalikAmber.copy(alpha = 0.16f) else MasalikCard)
                                .border(1.dp, if (isSel) MasalikAmber else MasalikBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedCity = city }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (city.id in visitedCities) MasalikEmerald else MasalikMuted, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(city.nameAr, color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(city.governorateAr, color = MasalikMuted, fontSize = 10.sp)
                                }
                            }
                            Text("${"%.1f".format(city.distanceM / 1000f)} كم", color = MasalikAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
