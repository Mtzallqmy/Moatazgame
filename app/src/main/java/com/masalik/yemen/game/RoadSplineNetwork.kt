package com.masalik.yemen.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

object RoadSplineNetwork {

    const val TOTAL_ROAD_LENGTH_M: Float = 22000f
    private const val SAMPLE_STEP_M: Float = 5f

    val REGIONS: List<RegionMeta> = listOf(
        RegionMeta(RegionId.TAIZ_HIGHLANDS, "مرتفعات تعز — جبل صبر", 0f, 2600f, 1480f, SurfaceType.ASPHALT),
        RegionMeta(RegionId.IBB_GREEN_PEAKS, "إب الخضراء — اللواء الأخضر", 2600f, 5200f, 2050f, SurfaceType.ASPHALT),
        RegionMeta(RegionId.WADI_RAYHAN, "وادي ريحان والمدرجات", 5200f, 7600f, 1780f, SurfaceType.DIRT),
        RegionMeta(RegionId.NAQIL_SAHAB, "نقيل سمارة والسحاب", 7600f, 10600f, 2650f, SurfaceType.GRAVEL),
        RegionMeta(RegionId.SANAA_HIGHLANDS, "صنعاء القديمة — باب اليمن", 10600f, 16200f, 2250f, SurfaceType.ASPHALT),
        RegionMeta(RegionId.MASALIK_DESERT, "صحراء مأرب التاريخية", 16200f, 18000f, 1120f, SurfaceType.SAND),
        RegionMeta(RegionId.SHIBAM_HADRAMOUT, "شبام حضرموت — ناطحات الطين", 18000f, 19600f, 720f, SurfaceType.DIRT),
        RegionMeta(RegionId.TIHAMA_PLAINS, "سهول تهامة وزبيد", 19600f, 20800f, 160f, SurfaceType.SAND),
        RegionMeta(RegionId.COASTAL_ROAD, "طريق عدن والمخا الساحلي", 20800f, 22000f, 22f, SurfaceType.ASPHALT)
    )

    val YEMEN_CITY_HUBS: List<YemenCityHub> = listOf(
        YemenCityHub(
            id = "city_taiz_sabir",
            nameAr = "تعز — شارع جمال وجبل صبر",
            governorateAr = "محافظة تعز",
            subtitleAr = "شوارع حجرية حيوية، أسواق شعبية ومطلات جبل صبر الشاهق",
            regionId = RegionId.TAIZ_HIGHLANDS,
            distanceM = 180f,
            altitudeM = 1480,
            mapX = 27f,
            mapY = 74f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.MORNING,
            highlightsAr = listOf("شارع جمال", "جبل صبر", "سوق الباب الكبير")
        ),
        YemenCityHub(
            id = "city_ibb_green",
            nameAr = "إب — المدينة القديمة والمدرجات",
            governorateAr = "محافظة إب",
            subtitleAr = "طبيعة خضراء وأمطار خفيفة ومنعطفات ضبابية ساحرة",
            regionId = RegionId.IBB_GREEN_PEAKS,
            distanceM = 3400f,
            altitudeM = 2050,
            mapX = 29f,
            mapY = 63f,
            defaultWeather = WeatherPreset.LIGHT_RAIN,
            defaultTimeOfDay = TimeOfDayPreset.AFTERNOON,
            highlightsAr = listOf("المدرجات الخضراء", "إشارات المدينة", "الجامع الكبير")
        ),
        YemenCityHub(
            id = "city_wadi_rayhan",
            nameAr = "وادي ريحان — مزارع البن اليمني",
            governorateAr = "وسط المرتفعات",
            subtitleAr = "دروب ترابية وحصوية بين مزارع البن والمياه الجارية",
            regionId = RegionId.WADI_RAYHAN,
            distanceM = 6100f,
            altitudeM = 1780,
            mapX = 33f,
            mapY = 55f,
            defaultWeather = WeatherPreset.CLOUDY,
            defaultTimeOfDay = TimeOfDayPreset.MORNING,
            highlightsAr = listOf("مدرجات البن", "سوق الوادي", "جسر ريحان")
        ),
        YemenCityHub(
            id = "city_naqil_samara",
            nameAr = "نقيل سمارة — ممر السحاب",
            governorateAr = "إب — ذمار",
            subtitleAr = "منحدرات شديدة الارتفاع، أنفاق صخرية وضباب جبلي كثيف",
            regionId = RegionId.NAQIL_SAHAB,
            distanceM = 8900f,
            altitudeM = 2650,
            mapX = 31f,
            mapY = 47f,
            defaultWeather = WeatherPreset.MOUNTAIN_FOG,
            defaultTimeOfDay = TimeOfDayPreset.DAWN,
            highlightsAr = listOf("عقبة سمارة", "نفق السحاب", "مطل القمة")
        ),
        YemenCityHub(
            id = "city_dhamar_highland",
            nameAr = "ذمار — ساحة المدينة",
            governorateAr = "محافظة ذمار",
            subtitleAr = "هضبة مفتوحة بطرق واسعة وحركة مرورية نشطة",
            regionId = RegionId.SANAA_HIGHLANDS,
            distanceM = 11400f,
            altitudeM = 2400,
            mapX = 34f,
            mapY = 42f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.NOON,
            highlightsAr = listOf("شارع رداع", "السوق الشعبي", "تقاطع ذمار")
        ),
        YemenCityHub(
            id = "city_sanaa_old",
            nameAr = "صنعاء القديمة — باب اليمن",
            governorateAr = "أمانة العاصمة",
            subtitleAr = "أبراج الطوب والقمريات المضيئة وسوق الملح العريق وباب اليمن",
            regionId = RegionId.SANAA_HIGHLANDS,
            distanceM = 15250f,
            altitudeM = 2250,
            mapX = 32f,
            mapY = 33f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.SUNSET,
            highlightsAr = listOf("باب اليمن", "القمريات", "شارع الزبيري")
        ),
        YemenCityHub(
            id = "city_marib_desert",
            nameAr = "مأرب — طريق السد والصحراء",
            governorateAr = "محافظة مأرب",
            subtitleAr = "كثبان رملية ذهبية وطرق صحراوية مفتوحة ومعالم سبأ",
            regionId = RegionId.MASALIK_DESERT,
            distanceM = 16900f,
            altitudeM = 1120,
            mapX = 48f,
            mapY = 34f,
            defaultWeather = WeatherPreset.SANDSTORM,
            defaultTimeOfDay = TimeOfDayPreset.AFTERNOON,
            highlightsAr = listOf("رمال سبأ", "سد مأرب", "واحة النخيل")
        ),
        YemenCityHub(
            id = "city_shibam_hadramout",
            nameAr = "شبام حضرموت — ناطحات الطين",
            governorateAr = "محافظة حضرموت",
            subtitleAr = "أقدم ناطحات سحاب طينية في العالم تحيط بشوارع الوادي",
            regionId = RegionId.SHIBAM_HADRAMOUT,
            distanceM = 18650f,
            altitudeM = 720,
            mapX = 72f,
            mapY = 43f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.SUNRISE,
            highlightsAr = listOf("أبراج شبام", "وادي حضرموت", "بساتين النخيل")
        ),
        YemenCityHub(
            id = "city_zabid_tihama",
            nameAr = "زبيد وسهل تهامة",
            governorateAr = "محافظة الحديدة",
            subtitleAr = "سهول تهامية دافئة وأسواق تقليدية مفتوحة على الساحل",
            regionId = RegionId.TIHAMA_PLAINS,
            distanceM = 20100f,
            altitudeM = 160,
            mapX = 18f,
            mapY = 58f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.NOON,
            highlightsAr = listOf("مدينة زبيد", "طريق النخيل", "السوق التهامي")
        ),
        YemenCityHub(
            id = "city_aden_mokha_coast",
            nameAr = "عدن والمخا — الكورنيش الساحلي",
            governorateAr = "محافظة عدن",
            subtitleAr = "كورنيش ساحلي مضاء ليلاً بمحاذاة البحر الأزرق وجبال صيرة",
            regionId = RegionId.COASTAL_ROAD,
            distanceM = 21350f,
            altitudeM = 22,
            mapX = 36f,
            mapY = 83f,
            defaultWeather = WeatherPreset.CLEAR,
            defaultTimeOfDay = TimeOfDayPreset.NIGHT,
            highlightsAr = listOf("كورنيش عدن", "ميناء المخا", "طريق البحر")
        )
    )

    val TRAFFIC_INTERSECTIONS: List<TrafficIntersectionDef> = listOf(
        TrafficIntersectionDef("sig_taiz_1", "تقاطع شارع جمال — تعز", 280f, 0f),
        TrafficIntersectionDef("sig_taiz_2", "جولة التحرير — تعز", 1320f, 7f),
        TrafficIntersectionDef("sig_ibb_1", "تقاطع العدين — إب", 3320f, 14f),
        TrafficIntersectionDef("sig_ibb_2", "مفرق جبلة — إب", 4480f, 4f),
        TrafficIntersectionDef("sig_dhamar", "تقاطع ذمار المركزي", 11460f, 11f),
        TrafficIntersectionDef("sig_sanaa_1", "جولة الزبيري — صنعاء", 14200f, 18f),
        TrafficIntersectionDef("sig_sanaa_bab", "إشارة باب اليمن — صنعاء القديمة", 15380f, 3f),
        TrafficIntersectionDef("sig_shibam", "مدخل شبام حضرموت", 18720f, 9f),
        TrafficIntersectionDef("sig_zabid", "تقاطع زبيد — تهامة", 20160f, 16f),
        TrafficIntersectionDef("sig_aden", "إشارة كورنيش عدن", 21420f, 6f)
    )

    val WORLD_POIS: List<WorldPoi> = listOf(
        WorldPoi("poi_fuel_taiz", "محطة صبر للوقود", "fuel", 520f, 9.5f, RegionId.TAIZ_HIGHLANDS),
        WorldPoi("poi_work_taiz", "ورشة تعز للصيانة", "workshop", 1620f, -9.5f, RegionId.TAIZ_HIGHLANDS),
        WorldPoi("poi_wash_ibb", "مغسلة إب الخضراء", "car_wash", 3650f, 9.5f, RegionId.IBB_GREEN_PEAKS),
        WorldPoi("poi_fuel_ibb", "محطة اللواء الأخضر", "fuel", 4820f, -9.5f, RegionId.IBB_GREEN_PEAKS),
        WorldPoi("poi_work_wadi", "ورشة وادي ريحان", "workshop", 6380f, 9.5f, RegionId.WADI_RAYHAN),
        WorldPoi("poi_fuel_naqil", "محطة نقيل سمارة", "fuel", 8400f, 9.5f, RegionId.NAQIL_SAHAB),
        WorldPoi("poi_wash_naqil", "مغسلة السحاب", "car_wash", 9950f, -9.5f, RegionId.NAQIL_SAHAB),
        WorldPoi("poi_fuel_dhamar", "محطة ذمار", "fuel", 11650f, 9.5f, RegionId.SANAA_HIGHLANDS),
        WorldPoi("poi_work_sanaa", "ورشة صنعاء التراثية", "workshop", 13650f, -9.5f, RegionId.SANAA_HIGHLANDS),
        WorldPoi("poi_fuel_sanaa", "محطة باب اليمن", "fuel", 14950f, 9.5f, RegionId.SANAA_HIGHLANDS),
        WorldPoi("poi_landmark_bab", "بوابة باب اليمن التاريخية", "landmark", 15450f, 0f, RegionId.SANAA_HIGHLANDS),
        WorldPoi("poi_wash_sanaa", "مغسلة القمرية", "car_wash", 15850f, -9.5f, RegionId.SANAA_HIGHLANDS),
        WorldPoi("poi_fuel_marib", "محطة مأرب الصحراوية", "fuel", 17150f, 9.5f, RegionId.MASALIK_DESERT),
        WorldPoi("poi_work_shibam", "ورشة شبام حضرموت", "workshop", 18480f, -9.5f, RegionId.SHIBAM_HADRAMOUT),
        WorldPoi("poi_fuel_shibam", "محطة وادي حضرموت", "fuel", 19150f, 9.5f, RegionId.SHIBAM_HADRAMOUT),
        WorldPoi("poi_wash_tihama", "مغسلة زبيد", "car_wash", 20320f, 9.5f, RegionId.TIHAMA_PLAINS),
        WorldPoi("poi_fuel_aden", "محطة كورنيش عدن", "fuel", 21120f, -9.5f, RegionId.COASTAL_ROAD),
        WorldPoi("poi_work_aden", "ورشة ميناء عدن", "workshop", 21700f, 9.5f, RegionId.COASTAL_ROAD)
    )

    // Precomputed 4401 samples every 5m along 22,000m for ultra-fast O(1) lookup on the 60fps GL thread
    private val cachedSamples: Array<RoadSample> by lazy { buildSplineSamples() }

    fun getAllSamples(): Array<RoadSample> = cachedSamples

    fun getRegionAt(distanceM: Float): RegionMeta {
        val d = distanceM.coerceIn(0f, TOTAL_ROAD_LENGTH_M - 1f)
        return REGIONS.firstOrNull { d >= it.startM && d < it.endM } ?: REGIONS.last()
    }

    fun sampleAt(distanceM: Float): RoadSample {
        val clamped = distanceM.coerceIn(0f, TOTAL_ROAD_LENGTH_M)
        val exactIdx = clamped / SAMPLE_STEP_M
        val i0 = floor(exactIdx).toInt().coerceIn(0, cachedSamples.lastIndex)
        val i1 = (i0 + 1).coerceAtMost(cachedSamples.lastIndex)
        val t = (exactIdx - i0).coerceIn(0f, 1f)
        if (i0 == i1 || t < 0.001f) return cachedSamples[i0]
        val a = cachedSamples[i0]
        val b = cachedSamples[i1]
        var dh = b.headingRad - a.headingRad
        if (dh > PI) dh -= (2.0 * PI).toFloat()
        if (dh < -PI) dh += (2.0 * PI).toFloat()
        val heading = a.headingRad + dh * t
        return a.copy(
            distanceM = clamped,
            x = a.x + (b.x - a.x) * t,
            y = a.y + (b.y - a.y) * t,
            z = a.z + (b.z - a.z) * t,
            headingRad = heading,
            rightX = cos(heading),
            rightZ = -sin(heading),
            widthM = a.widthM + (b.widthM - a.widthM) * t,
            gradePct = a.gradePct + (b.gradePct - a.gradePct) * t
        )
    }

    fun terrainHeightAt(worldX: Float, worldZ: Float, roadSample: RoadSample, lateralOffsetM: Float): Float {
        val absLat = abs(lateralOffsetM)
        val roadHalf = roadSample.widthM * 0.5f + 3.4f
        if (absLat <= roadHalf + 0.5f) return roadSample.y

        val distOut = (absLat - roadHalf).coerceAtLeast(0f)
        val blend = (distOut / 14f).coerceIn(0f, 1f)
        val hillWave = sin(worldX * 0.017f) * cos(worldZ * 0.015f) * 14f +
            sin(worldX * 0.043f + worldZ * 0.031f) * 4.5f

        val profileDelta = when (roadSample.regionId) {
            RegionId.TAIZ_HIGHLANDS, RegionId.IBB_GREEN_PEAKS -> {
                // Dramatic Yemeni terraced mountain slopes (rise on one side, valley drop on other)
                val terrace = if (lateralOffsetM < 0f) distOut * 0.36f else -distOut * 0.14f
                terrace + hillWave * 0.45f
            }
            RegionId.NAQIL_SAHAB -> {
                val cliff = if (lateralOffsetM < 0f) distOut * 0.48f else -distOut * 0.28f
                cliff + hillWave * 0.55f
            }
            RegionId.WADI_RAYHAN -> {
                distOut * 0.18f + hillWave * 0.35f
            }
            RegionId.SANAA_HIGHLANDS, RegionId.SHIBAM_HADRAMOUT -> {
                if (roadSample.isUrbanStreet) 0.1f else distOut * 0.16f + hillWave * 0.25f
            }
            RegionId.MASALIK_DESERT, RegionId.TIHAMA_PLAINS -> {
                // Flowing sand dunes
                sin(worldX * 0.025f + worldZ * 0.02f) * 5.5f + distOut * 0.04f
            }
            RegionId.COASTAL_ROAD -> {
                if (lateralOffsetM > 0f) -distOut * 0.12f else distOut * 0.26f
            }
        }
        return roadSample.y + profileDelta * blend
    }

    private fun isUrbanCorridor(d: Float): Boolean {
        return d in 0f..1750f ||
            d in 3050f..4850f ||
            d in 11100f..12050f ||
            d in 13500f..16100f ||
            d in 18250f..19400f ||
            d in 19850f..20550f ||
            d in 20950f..21900f
    }

    private fun buildSplineSamples(): Array<RoadSample> {
        val count = (TOTAL_ROAD_LENGTH_M / SAMPLE_STEP_M).toInt() + 1
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val zs = FloatArray(count)

        var curX = 0f
        var curZ = 0f
        var heading = 0f

        for (i in 0 until count) {
            val d = i * SAMPLE_STEP_M
            val urban = isUrbanCorridor(d)
            val region = getRegionAt(d)

            // Smooth authentic mountain switchbacks outside cities, straighter realistic avenues inside cities
            val curvature = when {
                urban -> sin(d * 0.0022f) * 0.0014f
                region.id == RegionId.NAQIL_SAHAB -> sin(d * 0.0085f) * 0.0072f + cos(d * 0.0038f) * 0.0035f
                region.id in listOf(RegionId.TAIZ_HIGHLANDS, RegionId.IBB_GREEN_PEAKS, RegionId.WADI_RAYHAN) ->
                    sin(d * 0.0055f) * 0.0046f + cos(d * 0.0024f) * 0.0020f
                else -> sin(d * 0.0030f) * 0.0022f
            }
            heading += curvature * SAMPLE_STEP_M
            heading = heading.coerceIn(-1.15f, 1.15f)

            xs[i] = curX
            zs[i] = curZ
            curX += sin(heading) * SAMPLE_STEP_M
            curZ += cos(heading) * SAMPLE_STEP_M

            // Elevation curve mimicking Taiz -> Ibb -> Naqil Samara (peak) -> Sana'a -> Marib -> Shibam -> Aden sea level
            val y = when {
                d < 2600f -> 60f + (d / 2600f) * 28f + sin(d * 0.004f) * 3.5f
                d < 5200f -> 88f + ((d - 2600f) / 2600f) * 34f + sin(d * 0.005f) * 4.0f
                d < 7600f -> 122f - ((d - 5200f) / 2400f) * 18f + cos(d * 0.006f) * 3.0f
                d < 10600f -> 104f + ((d - 7600f) / 3000f) * 78f + sin(d * 0.007f) * 4.5f
                d < 16200f -> 182f - ((d - 10600f) / 5600f) * 36f + sin(d * 0.0025f) * 2.2f
                d < 18000f -> 146f - ((d - 16200f) / 1800f) * 62f
                d < 19600f -> 84f - ((d - 18000f) / 1600f) * 34f
                d < 20800f -> 50f - ((d - 19600f) / 1200f) * 38f
                else -> 12f - ((d - 20800f) / 1200f) * 6f
            }
            ys[i] = y
        }

        // Blend adjacent elevation profiles over 120 m so region boundaries
        // don't introduce ramps that are steeper than the physics model.
        for (region in REGIONS.drop(1)) {
            val center = (region.startM / SAMPLE_STEP_M).toInt()
            val first = center - 12
            val last = center + 12
            val span = (last - first).toFloat()
            val startY = ys[first]
            val endY = ys[last]
            val startSlope = (ys[first] - ys[first - 1]) * span
            val endSlope = (ys[last + 1] - ys[last]) * span
            for (i in first + 1 until last) {
                val t = (i - first) / span
                val t2 = t * t
                val t3 = t2 * t
                ys[i] = (2f * t3 - 3f * t2 + 1f) * startY +
                    (t3 - 2f * t2 + t) * startSlope +
                    (-2f * t3 + 3f * t2) * endY +
                    (t3 - t2) * endSlope
            }
        }

        return Array(count) { i ->
            val d = i * SAMPLE_STEP_M
            val nextIdx = (i + 1).coerceAtMost(count - 1)
            val prevIdx = (i - 1).coerceAtLeast(0)
            val dx = xs[nextIdx] - xs[prevIdx]
            val dz = zs[nextIdx] - zs[prevIdx]
            val dy = ys[nextIdx] - ys[prevIdx]
            val horiz = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
            val hRad = atan2(dx, dz)
            val gradePct = ((dy / horiz) * 100f).coerceIn(-16f, 16f)

            val region = getRegionAt(d)
            val urban = isUrbanCorridor(d)
            val hasTunnel = d in 8620f..8760f || d in 9640f..9770f
            val hasBridge = d in 6020f..6160f || d in 17420f..17560f

            val surface = when {
                urban || hasTunnel || hasBridge -> SurfaceType.ASPHALT
                region.id == RegionId.WADI_RAYHAN && (d % 400f) > 260f -> SurfaceType.MUD
                region.id == RegionId.NAQIL_SAHAB && (d % 500f) > 360f -> SurfaceType.ROCK
                else -> region.defaultSurface
            }

            RoadSample(
                distanceM = d,
                x = xs[i],
                y = ys[i],
                z = zs[i],
                headingRad = hRad,
                rightX = cos(hRad),
                rightZ = -sin(hRad),
                widthM = if (urban) 11.4f else 9.6f,
                gradePct = gradePct,
                surface = surface,
                regionId = region.id,
                regionNameAr = region.nameAr,
                chunkId = (d / 1000f).toInt(),
                isUrbanStreet = urban,
                hasTunnel = hasTunnel,
                hasBridge = hasBridge
            )
        }
    }
}
