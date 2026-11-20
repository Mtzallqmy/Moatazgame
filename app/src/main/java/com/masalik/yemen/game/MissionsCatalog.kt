package com.masalik.yemen.game

import java.time.LocalDate

object MissionsCatalog {

    val STORY_CHAPTERS: List<StoryChapter> = listOf(
        StoryChapter(
            id = 1,
            titleAr = "الفصل الأول: شرارة جبل صبر",
            regionAr = "تعز — المرتفعات",
            narratorNameAr = "العم صالح التعزي",
            narratorRoleAr = "شيخ السائقين في تعز",
            summaryAr = "ابدأ رحلتك في شوارع تعز ومنحدرات جبل صبر، وتعلّم فنون القيادة بين الأسواق والإشارات الضوئية."
        ),
        StoryChapter(
            id = 2,
            titleAr = "الفصل الثاني: قافلة البن الأخضر",
            regionAr = "إب ووادي ريحان",
            narratorNameAr = "المعلم يحيى الإبي",
            narratorRoleAr = "تاجر البن اليمني",
            summaryAr = "انقل محصول البن الفاخر عبر مدرجات إب الممطرة ودروب وادي ريحان الزلقة بحمولة سليمة."
        ),
        StoryChapter(
            id = 3,
            titleAr = "الفصل الثالث: تحدي نقيل سمارة",
            regionAr = "نقيل السحاب",
            narratorNameAr = "الكابتن ناصر الجبلي",
            narratorRoleAr = "قائد فرق الإنقاذ الجبلي",
            summaryAr = "واجه الضباب الكثيف والأنفاق الصخرية في أعلى ممر جبلي بين المحافظات."
        ),
        StoryChapter(
            id = 4,
            titleAr = "الفصل الرابع: أمجاد باب اليمن وسبأ",
            regionAr = "صنعاء القديمة ومأرب",
            narratorNameAr = "الأستاذ عبدالكريم الصنعاني",
            narratorRoleAr = "مؤرخ الدروب اليمنية",
            summaryAr = "قد مركبتك في شوارع صنعاء القديمة المضيئة بالقمريات ثم اقتحم رمال مأرب الذهبية."
        ),
        StoryChapter(
            id = 5,
            titleAr = "الفصل الخامس: من شبام إلى بحر عدن",
            regionAr = "حضرموت وتهامة وعدن",
            narratorNameAr = "النوخذة سالم العدني",
            narratorRoleAr = "ربان طريق الساحل",
            summaryAr = "اختتم الملحمة بسباق القمم من ناطحات شبام الطينية حتى كورنيش عدن الساحلي."
        )
    )

    val ALL_OFFLINE_MISSIONS: List<MissionDefinition> by lazy { buildAll65Missions() }

    fun getDailyChallenges(): List<MissionDefinition> {
        val daySeed = runCatching { LocalDate.now().dayOfYear }.getOrDefault(140)
        val pool = ALL_OFFLINE_MISSIONS.filter { it.chapterId == null }
        val idx1 = daySeed % pool.size
        val idx2 = (daySeed + 11) % pool.size
        val idx3 = (daySeed + 23) % pool.size
        return listOf(pool[idx1], pool[idx2], pool[idx3])
    }

    fun getDriverRank(xp: Int): DriverRankInfo = when {
        xp < 300 -> DriverRankInfo(1, "سائق مبتدئ", 300)
        xp < 800 -> DriverRankInfo(2, "عابر الدروب", 800)
        xp < 1600 -> DriverRankInfo(3, "خبير العقبات", 1600)
        xp < 2800 -> DriverRankInfo(4, "قائد القوافل", 2800)
        xp < 4500 -> DriverRankInfo(5, "فارس الجبال", 4500)
        else -> DriverRankInfo(6, "أسطورة مَسَالِك اليمن", 9999)
    }

    private fun buildAll65Missions(): List<MissionDefinition> {
        val list = mutableListOf<MissionDefinition>()

        // 1) 25 Story Missions (5 per Chapter x 5 Chapters)
        val storyConfigs = listOf(
            // Chapter 1: Taiz
            Triple("انطلاقة شارع جمال", 120f, 920f),
            Triple("توصيل بضائع باب موسى", 600f, 1550f),
            Triple("صعود مطل جبل صبر", 1200f, 2250f),
            Triple("دورية المساء في تعز", 300f, 1450f),
            Triple("سباق قمة صبر الودي", 1500f, 2550f),
            // Chapter 2: Ibb & Wadi Rayhan
            Triple("دخول اللواء الأخضر", 2700f, 3650f),
            Triple("مطر المدرجات الخفيف", 3400f, 4450f),
            Triple("قافلة البن في وادي ريحان", 5250f, 6400f),
            Triple("عبور الجسر المائي", 5900f, 7050f),
            Triple("تحدي طين الوادي", 6400f, 7550f),
            // Chapter 3: Naqil Samara
            Triple("بداية صعود نقيل سمارة", 7650f, 8600f),
            Triple("عبور نفق السحاب الأول", 8450f, 9450f),
            Triple("إنقاذ عالق في الضباب", 8900f, 9850f),
            Triple("منحدرات الممر الشاهق", 9400f, 10450f),
            Triple("سباق السحاب الجبلي", 8100f, 9600f),
            // Chapter 4: Dhamar, Old Sana'a & Marib
            Triple("طريق هضبة ذمار المفتوح", 10700f, 12000f),
            Triple("إيصال ركاب نحو صنعاء", 12600f, 14100f),
            Triple("جولة باب اليمن والقمريات", 14250f, 15750f),
            Triple("اقتحام صحراء مأرب", 16250f, 17450f),
            Triple("العاصفة الرملية عند السد", 16800f, 17950f),
            // Chapter 5: Shibam, Tihama & Aden
            Triple("شوارع ناطحات شبام الطينية", 18050f, 19200f),
            Triple("قافلة التمور الحضرمية", 18400f, 19550f),
            Triple("انحدار سهل تهامة وزبيد", 19650f, 20700f),
            Triple("أضواء كورنيش عدن الليلي", 20850f, 21750f),
            Triple("السباق الكبير لخليج عدن", 20600f, 21920f)
        )

        val chapterVehicles = listOf(
            VehicleId.SHAHIQ, VehicleId.SAQR, VehicleId.MUNQIDH, VehicleId.RAHHAL, VehicleId.BARQ
        )
        val chapterWeathers = listOf(
            WeatherPreset.CLEAR, WeatherPreset.LIGHT_RAIN, WeatherPreset.MOUNTAIN_FOG, WeatherPreset.SANDSTORM, WeatherPreset.CLEAR
        )
        val chapterTimes = listOf(
            TimeOfDayPreset.MORNING, TimeOfDayPreset.AFTERNOON, TimeOfDayPreset.DAWN, TimeOfDayPreset.SUNSET, TimeOfDayPreset.NIGHT
        )

        storyConfigs.forEachIndexed { idx, (title, startM, endM) ->
            val chapId = (idx / 5) + 1
            val isBossRace = (idx % 5 == 4)
            val isCargo = (idx % 5 == 1)
            val region = RoadSplineNetwork.getRegionAt(startM).id
            list.add(
                MissionDefinition(
                    id = "story_${idx + 1}",
                    titleAr = title,
                    descriptionAr = "مهمة قصة في ${RoadSplineNetwork.getRegionAt(startM).nameAr} بمسافة ${((endM - startM) / 1000f).let { "%.1f".format(it) }} كم.",
                    category = GameModeCategory.STORY,
                    chapterId = chapId,
                    regionId = region,
                    startDistanceM = startM,
                    finishDistanceM = endM,
                    targetTimeSec = (((endM - startM) / 11f).toInt()).coerceIn(65, 195),
                    rewardCoins = 1200 + idx * 240,
                    rewardXp = 120 + idx * 25,
                    recommendedVehicleId = chapterVehicles[(chapId - 1) % chapterVehicles.size],
                    weatherPreset = if (idx % 5 == 2 || idx % 5 == 3) chapterWeathers[chapId - 1] else WeatherPreset.CLEAR,
                    timeOfDayPreset = chapterTimes[(idx) % chapterTimes.size],
                    hasAiRival = isBossRace,
                    aiRivalSpeedMs = 16.5f + chapId * 1.1f,
                    cargoRequiredPct = if (isCargo) 75 else 0,
                    dialogueBriefAr = "${STORY_CHAPTERS[chapId - 1].narratorNameAr}: «$title — اثبت مهارتك في دروب اليمن!»"
                )
            )
        }

        // 2) 40 Multi-Mode Offline Missions across Race (8), Cargo (8), Passenger (8), Rescue (8), Challenge (8)
        val modeDefinitions = listOf(
            Triple(GameModeCategory.RACE, "سباق", VehicleId.BARQ),
            Triple(GameModeCategory.CARGO, "شحنة بن وبضائع", VehicleId.JABAL),
            Triple(GameModeCategory.PASSENGER, "خط ركاب", VehicleId.HAFILAT),
            Triple(GameModeCategory.RESCUE, "نداء إنقاذ عاجل", VehicleId.MUNQIDH),
            Triple(GameModeCategory.CHALLENGE, "تحدي احترافي", VehicleId.BADIYA)
        )

        val sectorStarts = floatArrayOf(200f, 3100f, 5600f, 8000f, 11200f, 14300f, 16500f, 18400f)
        val sectorNames = listOf("تعز", "إب الخضراء", "وادي ريحان", "نقيل سمارة", "ذمار", "صنعاء القديمة", "مأرب", "شبام وعدن")

        var mCounter = 1
        for ((cat, prefix, recVeh) in modeDefinitions) {
            for (s in 0 until 8) {
                val startM = (sectorStarts[s] + mCounter * 35f).coerceAtMost(20700f)
                val spanM = 850f + (s % 4) * 260f
                val endM = (startM + spanM).coerceAtMost(21940f)
                val reg = RoadSplineNetwork.getRegionAt(startM).id
                val weather = when (cat) {
                    GameModeCategory.RESCUE -> if (s % 2 == 0) WeatherPreset.HEAVY_RAIN else WeatherPreset.MOUNTAIN_FOG
                    GameModeCategory.CHALLENGE -> if (s >= 6) WeatherPreset.SANDSTORM else WeatherPreset.LIGHT_RAIN
                    else -> if (s % 3 == 0) WeatherPreset.CLOUDY else WeatherPreset.CLEAR
                }
                val time = TimeOfDayPreset.entries[(s + mCounter) % TimeOfDayPreset.entries.size]

                list.add(
                    MissionDefinition(
                        id = "mode_${cat.name.lowercase()}_${s + 1}",
                        titleAr = "$prefix — ${sectorNames[s]} #${s + 1}",
                        descriptionAr = "انطلق في مهمة ${cat.labelAr} عبر ${RoadSplineNetwork.getRegionAt(startM).nameAr} بطول ${"%.1f".format((endM - startM) / 1000f)} كم.",
                        category = cat,
                        chapterId = null,
                        regionId = reg,
                        startDistanceM = startM,
                        finishDistanceM = endM,
                        targetTimeSec = (((endM - startM) / 12f).toInt()).coerceIn(60, 180),
                        rewardCoins = 1400 + s * 320 + mCounter * 40,
                        rewardXp = 140 + s * 35,
                        recommendedVehicleId = if (cat == GameModeCategory.PASSENGER && s % 2 == 1) VehicleId.MADINA else recVeh,
                        weatherPreset = weather,
                        timeOfDayPreset = time,
                        hasAiRival = (cat == GameModeCategory.RACE),
                        aiRivalSpeedMs = 17.5f + s * 0.7f,
                        cargoRequiredPct = if (cat == GameModeCategory.CARGO) 80 else 0
                    )
                )
                mCounter++
            }
        }

        return list
    }
}
