package com.masalik.yemen.ui.screens

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masalik.yemen.game.BullbarStyle
import com.masalik.yemen.game.DamageState
import com.masalik.yemen.game.GearRatioPreset
import com.masalik.yemen.game.PaintFinishType
import com.masalik.yemen.game.PickupAccessoryType
import com.masalik.yemen.game.SaveGameSchema
import com.masalik.yemen.game.TireCompoundType
import com.masalik.yemen.game.VehicleCustomization
import com.masalik.yemen.game.VehicleFleetCatalog
import com.masalik.yemen.game.VehicleId
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
fun GarageShowroomScreen(
    saveState: SaveGameSchema,
    previewVehicleId: VehicleId,
    onSelectPreview: (VehicleId) -> Unit,
    onOrbit: (Float) -> Unit,
    onConfirmEquip: (VehicleId) -> Unit,
    onUnlockVehicle: (VehicleId) -> Unit,
    onUpdateCustomization: (VehicleId, VehicleCustomization) -> Unit,
    onUpgradePart: (VehicleId, String, Int) -> Unit,
    onServiceAction: (String, VehicleId, Int) -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=Specs, 1=Upgrades, 2=Cosmetics, 3=Service
    val spec = VehicleFleetCatalog.specOf(previewVehicleId)
    val custom = saveState.customizations[previewVehicleId]
        ?: VehicleFleetCatalog.defaultCustomizations().getValue(previewVehicleId)
    val isUnlocked = previewVehicleId in saveState.unlockedVehicles
    val isEquipped = saveState.selectedVehicleId == previewVehicleId
    val fuelLiters = saveState.fuelByVehicle[previewVehicleId] ?: spec.fuelTankLiters
    val dmg = saveState.damageByVehicle[previewVehicleId] ?: DamageState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MasalikObsidian.copy(alpha = 0.94f),
                        MasalikObsidian.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Left-side 360° Touch Drag Orbit Zone over the live 3D OpenGL Showroom
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.54f)
                .fillMaxHeight()
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
                            onOrbit(dx * 0.011f)
                            c.consume()
                        }
                    }
                }
        ) {
            Text(
                "اسحب لتدوير المركبة 360°",
                color = MasalikIvory.copy(alpha = 0.72f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MasalikObsidian.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(MasalikSlate.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(20.dp))
                Text("كراج وورشة مَسَالِك ثلاثية الأبعاد (10 مركبات)", color = MasalikAmberSoft, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${saveState.masalikCoins} ر.ي",
                    color = MasalikAmber,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MasalikCard)
                        .border(1.dp, MasalikAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MasalikCard)
                        .border(1.dp, MasalikBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("close_garage_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MasalikIvory, modifier = Modifier.size(16.dp))
                    Text("عودة", color = MasalikIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right Side (RTL Start): Inspector & Customization Panel
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 50.dp, bottom = 78.dp, start = 12.dp)
                .fillMaxWidth(0.45f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(MasalikSlate.copy(alpha = 0.94f))
                .border(1.dp, MasalikBorder, RoundedCornerShape(18.dp))
                .padding(10.dp)
        ) {
            // Vehicle Name & Unlock / Equip Action
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spec.nameAr, color = MasalikIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(spec.categoryAr, color = MasalikAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                when {
                    isEquipped -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MasalikEmerald.copy(alpha = 0.20f))
                                .border(1.dp, MasalikEmerald, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MasalikEmerald, modifier = Modifier.size(15.dp))
                            Text("مجهّزة", color = MasalikEmerald, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    isUnlocked -> {
                        Button(
                            onClick = { onConfirmEquip(previewVehicleId) },
                            colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp).testTag("equip_vehicle_button")
                        ) {
                            Text("تجهيز للقيادة", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    else -> {
                        val canAfford = saveState.masalikCoins >= spec.unlockPriceCoins
                        Button(
                            onClick = { onUnlockVehicle(previewVehicleId) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MasalikEmerald,
                                contentColor = MasalikObsidian,
                                disabledContainerColor = MasalikCard,
                                disabledContentColor = MasalikMuted
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp).testTag("unlock_vehicle_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("شراء ${spec.unlockPriceCoins} ر.ي", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4 Garage Tabs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                GarageTabChip("المواصفات", Icons.Default.Speed, activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
                GarageTabChip("التطويرات", Icons.Default.Upgrade, activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
                GarageTabChip("المظهر", Icons.Default.Palette, activeTab == 2, Modifier.weight(1f)) { activeTab = 2 }
                GarageTabChip("الورشة", Icons.Default.Build, activeTab == 3, Modifier.weight(1f)) { activeTab = 3 }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable Tab Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        Text(spec.subtitleAr, color = MasalikMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        SpecBar("القوة الحصانية", "${(spec.maxPowerHp * (1f + (custom.engineUpgradeLevel - 1) * 0.08f)).roundToInt()} HP", (spec.maxPowerHp / 560f).coerceIn(0f, 1f), MasalikAmber)
                        SpecBar("عزم الدوران", "${spec.maxEngineTorqueNm.roundToInt()} Nm", (spec.maxEngineTorqueNm / 1200f).coerceIn(0f, 1f), MasalikEmerald)
                        SpecBar("قوة المكابح", "${(spec.maxBrakeForceN / 1000f).roundToInt()} kN", (spec.maxBrakeForceN / 38000f).coerceIn(0f, 1f), MasalikSky)
                        SpecBar("سعة الحمولة", "${spec.maxCargoKg.roundToInt()} كجم", (spec.maxCargoKg / 3600f).coerceIn(0f, 1f), MasalikAmberSoft)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الدفع المدعوم: ${spec.supportedDrivetrains.joinToString(" / ") { it.label }}", color = MasalikIvory, fontSize = 11.sp)
                            Text("الخزان: ${spec.fuelTankLiters.roundToInt()} لتر", color = MasalikSky, fontSize = 11.sp)
                        }
                    }

                    1 -> {
                        if (!isUnlocked) {
                            Text("قم بشراء المركبة أولاً لتطوير المحرك والتعليق.", color = MasalikAmber, fontSize = 12.sp)
                        } else {
                            UpgradeRow("ترقية المحرك والتوربو", custom.engineUpgradeLevel, 1800 * custom.engineUpgradeLevel, saveState.masalikCoins) {
                                onUpgradePart(previewVehicleId, "engine", 1800 * custom.engineUpgradeLevel)
                            }
                            UpgradeRow("ناقل الحركة والتروس", custom.transmissionUpgradeLevel, 1500 * custom.transmissionUpgradeLevel, saveState.masalikCoins) {
                                onUpgradePart(previewVehicleId, "transmission", 1500 * custom.transmissionUpgradeLevel)
                            }
                            UpgradeRow("نظام التعليق الجبلي", custom.suspensionUpgradeLevel, 1600 * custom.suspensionUpgradeLevel, saveState.masalikCoins) {
                                onUpgradePart(previewVehicleId, "suspension", 1600 * custom.suspensionUpgradeLevel)
                            }
                            UpgradeRow("مكابح سيراميك جبلية", custom.brakeUpgradeLevel, 1400 * custom.brakeUpgradeLevel, saveState.masalikCoins) {
                                onUpgradePart(previewVehicleId, "brake", 1400 * custom.brakeUpgradeLevel)
                            }

                            Text("نسبة تروس القير", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GearRatioPreset.entries.forEach { preset ->
                                    SmallSelectChip(preset.labelAr, custom.gearPreset == preset) {
                                        onUpdateCustomization(previewVehicleId, custom.copy(gearPreset = preset))
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Text("ألوان يمنية أصيلة", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VehicleFleetCatalog.YEMENI_PAINT_SWATCHES) { sw ->
                                val col = Color(runCatching { AndroidColor.parseColor(sw.hex) }.getOrDefault(AndroidColor.DKGRAY))
                                val sel = custom.paintColorHex.equals(sw.hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(if (sel) 2.5.dp else 1.dp, if (sel) MasalikAmber else MasalikBorder, CircleShape)
                                        .clickable { onUpdateCustomization(previewVehicleId, custom.copy(paintColorHex = sw.hex)) }
                                )
                            }
                        }

                        Text("نوع الطلاء والإطارات", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            PaintFinishType.entries.forEach { f ->
                                SmallSelectChip(f.labelAr, custom.paintFinish == f) {
                                    onUpdateCustomization(previewVehicleId, custom.copy(paintFinish = f))
                                }
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(TireCompoundType.entries) { tire ->
                                SmallSelectChip(tire.labelAr, custom.tireType == tire) {
                                    onUpdateCustomization(previewVehicleId, custom.copy(tireType = tire))
                                }
                            }
                        }

                        Text("الصدام الأمامي والإكسسوارات", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            BullbarStyle.entries.forEach { b ->
                                SmallSelectChip(b.labelAr, custom.bullbarStyle == b) {
                                    onUpdateCustomization(previewVehicleId, custom.copy(bullbarStyle = b))
                                }
                            }
                        }
                        if (previewVehicleId in listOf(VehicleId.SAQR, VehicleId.BADIYA, VehicleId.MUNQIDH)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(PickupAccessoryType.entries) { acc ->
                                    SmallSelectChip(acc.labelAr, custom.pickupAccessory == acc) {
                                        onUpdateCustomization(previewVehicleId, custom.copy(pickupAccessory = acc))
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("سلة السقف الجبلية", color = MasalikIvory, fontSize = 11.sp)
                            Switch(
                                checked = custom.roofRackInstalled,
                                onCheckedChange = { onUpdateCustomization(previewVehicleId, custom.copy(roofRackInstalled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MasalikObsidian, checkedTrackColor = MasalikAmber)
                            )
                        }
                        if (custom.roofRackInstalled) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("صندوق أمتعة علوي", color = MasalikIvory, fontSize = 11.sp)
                                Switch(
                                    checked = custom.roofCargoBoxInstalled,
                                    onCheckedChange = { onUpdateCustomization(previewVehicleId, custom.copy(roofCargoBoxInstalled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MasalikObsidian, checkedTrackColor = MasalikAmber)
                                )
                            }
                        }

                        Text("ارتفاع التعليق: ${(custom.rideHeightOffsetM * 100).roundToInt()} سم", color = MasalikSky, fontSize = 11.sp)
                        Slider(
                            value = custom.rideHeightOffsetM,
                            onValueChange = { onUpdateCustomization(previewVehicleId, custom.copy(rideHeightOffsetM = it)) },
                            valueRange = -0.08f..0.18f,
                            colors = SliderDefaults.colors(thumbColor = MasalikAmber, activeTrackColor = MasalikAmber)
                        )

                        Text("اللوحة اليمنية", color = MasalikAmberSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(VehicleFleetCatalog.YEMENI_LICENSE_PLATES) { plate ->
                                SmallSelectChip(plate, custom.licensePlateCode == plate) {
                                    onUpdateCustomization(previewVehicleId, custom.copy(licensePlateCode = plate))
                                }
                            }
                        }
                    }

                    3 -> {
                        val fuelPct = (fuelLiters / spec.fuelTankLiters.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        SpecBar("مستوى الوقود", "${fuelLiters.roundToInt()} / ${spec.fuelTankLiters.roundToInt()} لتر", fuelPct, if (fuelPct < 0.25f) MasalikCrimson else MasalikEmerald)
                        SpecBar("صحة الهيكل والمحرك", "${dmg.bodyHealthPct.roundToInt()}%", dmg.bodyHealthPct / 100f, MasalikSky)
                        SpecBar("نظافة الطلاء (الطين)", "${((1f - dmg.dirtMudLevel) * 100).roundToInt()}%", 1f - dmg.dirtMudLevel, MasalikAmber)

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ServiceButton("تعبئة وقود (150 ر.ي)", Icons.Default.LocalGasStation, Modifier.weight(1f)) {
                                onServiceAction("fuel", previewVehicleId, 150)
                            }
                            ServiceButton("صيانة شاملة (350 ر.ي)", Icons.Default.Build, Modifier.weight(1f)) {
                                onServiceAction("workshop", previewVehicleId, 350)
                            }
                        }
                        ServiceButton("غسيل وتلميع المركبة وإزالة الطين (100 ر.ي)", Icons.Default.LocalCarWash, Modifier.fillMaxWidth()) {
                            onServiceAction("car_wash", previewVehicleId, 100)
                        }
                    }
                }
            }
        }

        // Bottom Fleet Strip (All 10 Vehicles)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MasalikObsidian.copy(alpha = 0.90f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VehicleFleetCatalog.ALL_VEHICLES) { v ->
                val selected = v.id == previewVehicleId
                val owned = v.id in saveState.unlockedVehicles
                Column(
                    modifier = Modifier
                        .width(126.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MasalikAmber.copy(alpha = 0.22f) else MasalikCard)
                        .border(1.5.dp, if (selected) MasalikAmber else MasalikBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelectPreview(v.id) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("garage_vehicle_${v.id.name.lowercase()}")
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(v.nameAr, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (!owned) Icon(Icons.Default.Lock, contentDescription = null, tint = MasalikAmber, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        if (owned) "${v.maxPowerHp} HP · مملوكة" else "${v.unlockPriceCoins} ر.ي",
                        color = if (owned) MasalikEmerald else MasalikAmberSoft,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GarageTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) MasalikAmber else MasalikCard)
            .border(1.dp, if (active) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (active) MasalikObsidian else MasalikIvory, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = if (active) MasalikObsidian else MasalikIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpecBar(label: String, value: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MasalikMuted, fontSize = 11.sp)
            Text(value, color = MasalikIvory, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MasalikBorder
        )
    }
}

@Composable
private fun UpgradeRow(title: String, level: Int, cost: Int, coins: Int, onUpgrade: () -> Unit) {
    val isMax = level >= 5
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MasalikCard)
            .border(1.dp, MasalikBorder, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("المستوى $level / 5", color = MasalikAmberSoft, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
        }
        Button(
            onClick = onUpgrade,
            enabled = !isMax && coins >= cost,
            colors = ButtonDefaults.buttonColors(containerColor = MasalikAmber, contentColor = MasalikObsidian),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(if (isMax) "أقصى مستوى" else "ترقية $cost ر.ي", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SmallSelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MasalikAmber else MasalikCard)
            .border(1.dp, if (selected) MasalikAmberSoft else MasalikBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (selected) MasalikObsidian else MasalikIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServiceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(MasalikCard)
            .border(1.dp, MasalikEmerald.copy(alpha = 0.65f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = MasalikEmerald, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MasalikIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
