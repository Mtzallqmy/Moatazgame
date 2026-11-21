package com.masalik.yemen.game

data class YemeniPaintSwatch(
    val nameAr: String,
    val hex: String
)

object VehicleFleetCatalog {

    val YEMENI_PAINT_SWATCHES: List<YemeniPaintSwatch> = listOf(
        YemeniPaintSwatch("نحاسي صبر", "#C87236"),
        YemeniPaintSwatch("لؤلؤي صنعاء", "#F2EFE9"),
        YemeniPaintSwatch("رمال مأرب", "#D9B878"),
        YemeniPaintSwatch("أخضر إب الزمردي", "#16835C"),
        YemeniPaintSwatch("أزرق عدن الكهربائي", "#196AD4"),
        YemeniPaintSwatch("قرمزي رياضي", "#D6242C"),
        YemeniPaintSwatch("أوبسيديان ليلي", "#1D2430"),
        YemeniPaintSwatch("ذهبي شبام", "#E5A31E"),
        YemeniPaintSwatch("فضي تيتانيوم", "#A4B2C2"),
        YemeniPaintSwatch("أصفر سباق", "#F5C414"),
        YemeniPaintSwatch("تركواز سقطرى", "#0E9AA7"),
        YemeniPaintSwatch("برتقالي بركاني", "#EA580C"),
        YemeniPaintSwatch("عنابي ملكي", "#7C1830"),
        YemeniPaintSwatch("بنفسجي سماء ليل", "#5B21B6"),
        YemeniPaintSwatch("رمادي ناردو", "#64748B"),
        YemeniPaintSwatch("أخضر عسكري", "#3F5E45"),
        YemeniPaintSwatch("أزرق سماوي معدني", "#0284C7"),
        YemeniPaintSwatch("برونزي صحراوي", "#8C5A32")
    )

    val YEMENI_LICENSE_PLATES: List<String> = listOf(
        "تعز 1-4092",
        "صنعاء 3-8814",
        "عدن 2-5520",
        "إب 4-1190",
        "حضرموت 5-7741",
        "مأرب 6-3028"
    )

    val ALL_VEHICLES: List<VehicleSpec> = listOf(
        VehicleSpec(
            id = VehicleId.SHAHIQ,
            nameAr = "الشاهق 4x4",
            subtitleAr = "دفع رباعي جبلي أسطوري لمنحدرات جبل صبر ونقيل سمارة",
            categoryAr = "دفع رباعي جبلي",
            unlockPriceCoins = 0,
            maxPowerHp = 285,
            maxEngineTorqueNm = 520f,
            baseMassKg = 2280f,
            maxCargoKg = 650f,
            fuelTankLiters = 95f,
            baseFuelBurnLPerMin = 0.58f,
            idleRpm = 780f,
            redlineRpm = 6000f,
            engineBaseFreqHz = 48f,
            forwardGearsCount = 6,
            maxBrakeForceN = 19500f,
            dragCoefficient = 0.44f,
            rollingResistance = 360f,
            maxSteerAngleDeg = 35f,
            wheelbaseM = 2.86f,
            trackWidthM = 1.84f,
            wheelRadiusM = 0.43f,
            bodyLengthM = 4.92f,
            bodyWidthM = 1.98f,
            bodyHeightM = 1.92f,
            defaultDrivetrain = DrivetrainMode.FOUR_H,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#C87236"
        ),
        VehicleSpec(
            id = VehicleId.SAQR,
            nameAr = "الصقر بيك أب",
            subtitleAr = "شاص وبيك أب يمني أصيل لحمل البن والمحاصيل بين القرى والمدن",
            categoryAr = "بيك أب نقل جبلي",
            unlockPriceCoins = 0,
            maxPowerHp = 240,
            maxEngineTorqueNm = 480f,
            baseMassKg = 2050f,
            maxCargoKg = 1100f,
            fuelTankLiters = 110f,
            baseFuelBurnLPerMin = 0.52f,
            idleRpm = 750f,
            redlineRpm = 5600f,
            engineBaseFreqHz = 44f,
            forwardGearsCount = 5,
            maxBrakeForceN = 18200f,
            dragCoefficient = 0.46f,
            rollingResistance = 350f,
            maxSteerAngleDeg = 36f,
            wheelbaseM = 3.08f,
            trackWidthM = 1.82f,
            wheelRadiusM = 0.42f,
            bodyLengthM = 5.22f,
            bodyWidthM = 1.90f,
            bodyHeightM = 1.86f,
            defaultDrivetrain = DrivetrainMode.FOUR_H,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#EAE6DC"
        ),
        VehicleSpec(
            id = VehicleId.MADINA,
            nameAr = "المدينة سيدان",
            subtitleAr = "سيارة مدن اقتصادية رشيقة لشوارع صنعاء القديمة وتعز وعدن",
            categoryAr = "سيدان مدنية",
            unlockPriceCoins = 0,
            maxPowerHp = 195,
            maxEngineTorqueNm = 310f,
            baseMassKg = 1480f,
            maxCargoKg = 260f,
            fuelTankLiters = 62f,
            baseFuelBurnLPerMin = 0.36f,
            idleRpm = 800f,
            redlineRpm = 6600f,
            engineBaseFreqHz = 56f,
            forwardGearsCount = 6,
            maxBrakeForceN = 16500f,
            dragCoefficient = 0.30f,
            rollingResistance = 270f,
            maxSteerAngleDeg = 38f,
            wheelbaseM = 2.72f,
            trackWidthM = 1.74f,
            wheelRadiusM = 0.34f,
            bodyLengthM = 4.64f,
            bodyWidthM = 1.82f,
            bodyHeightM = 1.46f,
            defaultDrivetrain = DrivetrainMode.TWO_WD,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.AWD),
            defaultColorHex = "#1E5B94"
        ),
        VehicleSpec(
            id = VehicleId.ASIL,
            nameAr = "الأصيل كروس أوفر",
            subtitleAr = "راحة عائلية متطورة مع ثبات ممتاز في العقبات الممطرة",
            categoryAr = "كروس أوفر عائلي",
            unlockPriceCoins = 6800,
            maxPowerHp = 265,
            maxEngineTorqueNm = 410f,
            baseMassKg = 1860f,
            maxCargoKg = 450f,
            fuelTankLiters = 75f,
            baseFuelBurnLPerMin = 0.44f,
            idleRpm = 780f,
            redlineRpm = 6400f,
            engineBaseFreqHz = 52f,
            forwardGearsCount = 7,
            maxBrakeForceN = 18800f,
            dragCoefficient = 0.36f,
            rollingResistance = 310f,
            maxSteerAngleDeg = 37f,
            wheelbaseM = 2.82f,
            trackWidthM = 1.84f,
            wheelRadiusM = 0.39f,
            bodyLengthM = 4.78f,
            bodyWidthM = 1.92f,
            bodyHeightM = 1.72f,
            defaultDrivetrain = DrivetrainMode.AWD,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.AWD),
            defaultColorHex = "#1F6F50"
        ),
        VehicleSpec(
            id = VehicleId.HAFILAT,
            nameAr = "الحافلة الجبلية",
            subtitleAr = "حافلة نقل ركاب بين المحافظات اليمنية بمحرك ديزل قوي ومكابح متينة",
            categoryAr = "نقل ركاب",
            unlockPriceCoins = 11500,
            maxPowerHp = 310,
            maxEngineTorqueNm = 760f,
            baseMassKg = 3450f,
            maxCargoKg = 1400f,
            fuelTankLiters = 145f,
            baseFuelBurnLPerMin = 0.68f,
            idleRpm = 680f,
            redlineRpm = 4500f,
            engineBaseFreqHz = 36f,
            forwardGearsCount = 6,
            maxBrakeForceN = 26500f,
            dragCoefficient = 0.56f,
            rollingResistance = 490f,
            maxSteerAngleDeg = 33f,
            wheelbaseM = 3.55f,
            trackWidthM = 2.02f,
            wheelRadiusM = 0.45f,
            bodyLengthM = 6.10f,
            bodyWidthM = 2.12f,
            bodyHeightM = 2.48f,
            defaultDrivetrain = DrivetrainMode.TWO_WD,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H),
            defaultColorHex = "#EAB308"
        ),
        VehicleSpec(
            id = VehicleId.RAHHAL,
            nameAr = "الرحّال إكسبيدشن",
            subtitleAr = "سيارة رحلات واستكشاف مجهزة لقطع 22 كم من الجبال والصحاري",
            categoryAr = "استكشاف ورحلات",
            unlockPriceCoins = 14800,
            maxPowerHp = 340,
            maxEngineTorqueNm = 620f,
            baseMassKg = 2520f,
            maxCargoKg = 820f,
            fuelTankLiters = 130f,
            baseFuelBurnLPerMin = 0.60f,
            idleRpm = 760f,
            redlineRpm = 6100f,
            engineBaseFreqHz = 45f,
            forwardGearsCount = 6,
            maxBrakeForceN = 21500f,
            dragCoefficient = 0.45f,
            rollingResistance = 380f,
            maxSteerAngleDeg = 35f,
            wheelbaseM = 2.96f,
            trackWidthM = 1.92f,
            wheelRadiusM = 0.46f,
            bodyLengthM = 5.14f,
            bodyWidthM = 2.04f,
            bodyHeightM = 2.04f,
            defaultDrivetrain = DrivetrainMode.FOUR_H,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#D4B475"
        ),
        VehicleSpec(
            id = VehicleId.BADIYA,
            nameAr = "البادية الصحراوي",
            subtitleAr = "وحش رمال مأرب ووادي حضرموت بنظام تعليق طويل المدى",
            categoryAr = "صحراوي 4x4",
            unlockPriceCoins = 18500,
            maxPowerHp = 395,
            maxEngineTorqueNm = 680f,
            baseMassKg = 2380f,
            maxCargoKg = 950f,
            fuelTankLiters = 125f,
            baseFuelBurnLPerMin = 0.64f,
            idleRpm = 800f,
            redlineRpm = 6400f,
            engineBaseFreqHz = 43f,
            forwardGearsCount = 7,
            maxBrakeForceN = 22400f,
            dragCoefficient = 0.47f,
            rollingResistance = 370f,
            maxSteerAngleDeg = 36f,
            wheelbaseM = 3.12f,
            trackWidthM = 1.98f,
            wheelRadiusM = 0.47f,
            bodyLengthM = 5.36f,
            bodyWidthM = 2.06f,
            bodyHeightM = 1.98f,
            defaultDrivetrain = DrivetrainMode.FOUR_H,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#D99B26"
        ),
        VehicleSpec(
            id = VehicleId.MUNQIDH,
            nameAr = "المنقذ — طوارئ",
            subtitleAr = "مركبة إسعاف وإنقاذ جبلي بمصابيح طوارئ مزدوجة ومعدات سحب",
            categoryAr = "إنقاذ وطوارئ",
            unlockPriceCoins = 22000,
            maxPowerHp = 360,
            maxEngineTorqueNm = 650f,
            baseMassKg = 2590f,
            maxCargoKg = 900f,
            fuelTankLiters = 120f,
            baseFuelBurnLPerMin = 0.62f,
            idleRpm = 780f,
            redlineRpm = 6000f,
            engineBaseFreqHz = 46f,
            forwardGearsCount = 6,
            maxBrakeForceN = 23000f,
            dragCoefficient = 0.46f,
            rollingResistance = 385f,
            maxSteerAngleDeg = 36f,
            wheelbaseM = 3.04f,
            trackWidthM = 1.94f,
            wheelRadiusM = 0.45f,
            bodyLengthM = 5.26f,
            bodyWidthM = 2.02f,
            bodyHeightM = 2.08f,
            defaultDrivetrain = DrivetrainMode.FOUR_H,
            supportedDrivetrains = listOf(DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#A62626"
        ),
        VehicleSpec(
            id = VehicleId.JABAL,
            nameAr = "الجبل — شاحنة قوافل",
            subtitleAr = "شاحنة نقل ثقيلة لقوافل البن والحبوب عبر العقبات الجبلية الشاهقة",
            categoryAr = "شاحنة نقل ثقيل",
            unlockPriceCoins = 28000,
            maxPowerHp = 450,
            maxEngineTorqueNm = 1150f,
            baseMassKg = 4800f,
            maxCargoKg = 3500f,
            fuelTankLiters = 220f,
            baseFuelBurnLPerMin = 0.85f,
            idleRpm = 620f,
            redlineRpm = 3900f,
            engineBaseFreqHz = 31f,
            forwardGearsCount = 8,
            maxBrakeForceN = 36000f,
            dragCoefficient = 0.62f,
            rollingResistance = 640f,
            maxSteerAngleDeg = 32f,
            wheelbaseM = 3.90f,
            trackWidthM = 2.16f,
            wheelRadiusM = 0.50f,
            bodyLengthM = 6.75f,
            bodyWidthM = 2.28f,
            bodyHeightM = 2.75f,
            defaultDrivetrain = DrivetrainMode.FOUR_L,
            supportedDrivetrains = listOf(DrivetrainMode.TWO_WD, DrivetrainMode.FOUR_H, DrivetrainMode.FOUR_L),
            defaultColorHex = "#27303D"
        ),
        VehicleSpec(
            id = VehicleId.BARQ,
            nameAr = "البرق GT رالي",
            subtitleAr = "سيارة رالي وسباقات جبلية خارقة بتوربو مزدوج ودفع رباعي مستمر",
            categoryAr = "رالي وسباقات",
            unlockPriceCoins = 36000,
            maxPowerHp = 540,
            maxEngineTorqueNm = 720f,
            baseMassKg = 1520f,
            maxCargoKg = 150f,
            fuelTankLiters = 78f,
            baseFuelBurnLPerMin = 0.68f,
            idleRpm = 950f,
            redlineRpm = 7800f,
            engineBaseFreqHz = 62f,
            forwardGearsCount = 7,
            maxBrakeForceN = 24500f,
            dragCoefficient = 0.28f,
            rollingResistance = 265f,
            maxSteerAngleDeg = 38f,
            wheelbaseM = 2.74f,
            trackWidthM = 1.88f,
            wheelRadiusM = 0.37f,
            bodyLengthM = 4.68f,
            bodyWidthM = 1.94f,
            bodyHeightM = 1.38f,
            defaultDrivetrain = DrivetrainMode.AWD,
            supportedDrivetrains = listOf(DrivetrainMode.AWD),
            defaultColorHex = "#EAB308"
        )
    )

    private val specMap = ALL_VEHICLES.associateBy { it.id }

    fun specOf(id: VehicleId): VehicleSpec = specMap[id] ?: ALL_VEHICLES.first()

    fun gripFor(tire: TireCompoundType, surface: SurfaceType): Float = when (tire) {
        TireCompoundType.HIGHWAY_SPORT -> when (surface) {
            SurfaceType.ASPHALT -> 1.18f
            SurfaceType.DIRT -> 0.80f
            SurfaceType.GRAVEL -> 0.78f
            SurfaceType.ROCK -> 0.74f
            SurfaceType.SAND -> 0.58f
            SurfaceType.MUD -> 0.46f
        }
        TireCompoundType.ALL_TERRAIN -> when (surface) {
            SurfaceType.ASPHALT -> 1.04f
            SurfaceType.DIRT -> 1.02f
            SurfaceType.GRAVEL -> 1.00f
            SurfaceType.ROCK -> 0.96f
            SurfaceType.SAND -> 0.88f
            SurfaceType.MUD -> 0.82f
        }
        TireCompoundType.MUD_ROCK -> when (surface) {
            SurfaceType.ASPHALT -> 0.92f
            SurfaceType.DIRT -> 1.12f
            SurfaceType.GRAVEL -> 1.10f
            SurfaceType.ROCK -> 1.18f
            SurfaceType.SAND -> 0.94f
            SurfaceType.MUD -> 1.16f
        }
        TireCompoundType.DESERT_SAND -> when (surface) {
            SurfaceType.ASPHALT -> 0.94f
            SurfaceType.DIRT -> 1.05f
            SurfaceType.GRAVEL -> 0.96f
            SurfaceType.ROCK -> 0.86f
            SurfaceType.SAND -> 1.22f
            SurfaceType.MUD -> 0.78f
        }
    }

    fun defaultCustomizations(): Map<VehicleId, VehicleCustomization> = ALL_VEHICLES.associate { spec ->
        spec.id to VehicleCustomization(
            paintColorHex = spec.defaultColorHex,
            bullbarStyle = if (spec.id in listOf(VehicleId.SHAHIQ, VehicleId.RAHHAL, VehicleId.BADIYA, VehicleId.MUNQIDH)) {
                BullbarStyle.STEEL_DESERT
            } else BullbarStyle.NONE,
            roofRackInstalled = spec.id in listOf(VehicleId.SHAHIQ, VehicleId.RAHHAL),
            pickupAccessory = if (spec.id == VehicleId.SAQR) PickupAccessoryType.ROLL_BAR else PickupAccessoryType.NONE,
            tireType = when (spec.id) {
                VehicleId.MADINA, VehicleId.BARQ -> TireCompoundType.HIGHWAY_SPORT
                VehicleId.BADIYA -> TireCompoundType.DESERT_SAND
                else -> TireCompoundType.ALL_TERRAIN
            }
        )
    }

    fun defaultFuelMap(): Map<VehicleId, Float> = ALL_VEHICLES.associate { it.id to it.fuelTankLiters }

    fun defaultDamageMap(): Map<VehicleId, DamageState> = ALL_VEHICLES.associate { it.id to DamageState() }
}
