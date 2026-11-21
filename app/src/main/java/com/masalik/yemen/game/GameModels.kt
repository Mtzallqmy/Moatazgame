package com.masalik.yemen.game

import kotlinx.serialization.Serializable

@Serializable
enum class VehicleId {
    SHAHIQ,
    SAQR,
    MADINA,
    ASIL,
    HAFILAT,
    RAHHAL,
    BADIYA,
    MUNQIDH,
    JABAL,
    BARQ
}

@Serializable
enum class RegionId {
    TAIZ_HIGHLANDS,
    IBB_GREEN_PEAKS,
    WADI_RAYHAN,
    NAQIL_SAHAB,
    SANAA_HIGHLANDS,
    MASALIK_DESERT,
    SHIBAM_HADRAMOUT,
    TIHAMA_PLAINS,
    COASTAL_ROAD
}

@Serializable
enum class SurfaceType(val labelAr: String) {
    ASPHALT("إسفلت"),
    DIRT("تراب جبلي"),
    GRAVEL("حصى"),
    ROCK("صخور وعرة"),
    SAND("رمال"),
    MUD("طين ومطر")
}

@Serializable
enum class CameraMode(val labelAr: String, val subtitleAr: String = "") {
    CHASE_NEAR("تتبع قريب", "منظور الشخص الثالث الرياضي"),
    COCKPIT("المقصورة 3D", "قيادة واقعية من داخل المقصورة"),
    HOOD("غطاء المحرك", "منظور منخفض فوق غطاء المحرك"),
    CINEMATIC_ORBIT("مداري 360°", "منظور سينمائي قابل للدوران باللمس"),
    CHASE_FAR("تتبع واسع", "منظور بانورامي بعيد للطريق"),
    CINEMATIC_FRONT("أمامية", "لقطة سينمائية لمقدمة المركبة"),
    FREE_PHOTO("حرة 360°", "زاوية تصوير حرة")
}

@Serializable
enum class TimeOfDayPreset(val labelAr: String, val hours: Float) {
    DAWN("الفجر", 5.25f),
    SUNRISE("الشروق", 6.5f),
    MORNING("الصباح", 9.0f),
    NOON("الظهر", 13.0f),
    AFTERNOON("العصر", 16.0f),
    SUNSET("الغروب", 18.1f),
    NIGHT("الليل", 21.5f)
}

@Serializable
enum class WeatherPreset(val labelAr: String) {
    CLEAR("صحو مشمس"),
    CLOUDY("غائم جزئياً"),
    LIGHT_RAIN("مطر خفيف"),
    HEAVY_RAIN("مطر غزير"),
    MOUNTAIN_FOG("ضباب الجبال"),
    SANDSTORM("عاصفة رملية")
}

@Serializable
enum class DrivingRealismMode(val labelAr: String) {
    ARCADE("سهل (أركيد)"),
    SPORT("رياضي متوازن"),
    SIMULATION("محاكاة واقعية")
}

@Serializable
enum class GraphicsQuality(val labelAr: String) {
    LOW("منخفض"),
    MEDIUM("متوسط"),
    HIGH("عالٍ"),
    ULTRA("فائق 3D")
}

@Serializable
enum class DrivetrainMode(val label: String) {
    TWO_WD("2WD"),
    FOUR_H("4H"),
    FOUR_L("4L"),
    AWD("AWD")
}

@Serializable
enum class AutomaticShifterState(val label: String) {
    P("P"),
    R("R"),
    N("N"),
    D("D")
}

@Serializable
enum class TouchControlMode(val labelAr: String) {
    WHEEL("المقود الذكي"),
    BUTTONS("أزرار يمين/يسار"),
    TILT("الجيروسكوب (إمالة)"),
    JOYSTICK("عصا تحكم")
}

@Serializable
enum class SteeringWheelStyle(
    val labelAr: String,
    val shortLabelAr: String,
    val descAr: String,
    val defaultLockDeg: Int,
    val steeringRatio: Float,
    val returnSpeed: Float
) {
    GULF_YEMENI(
        "شرقي / خليجي فاخر",
        "شرقي 4×4",
        "خشب فاخر وجلد مطرّز بدوران ناعم ومريح للطرق الجبلية والمدن",
        720, 0.96f, 3.1f
    ),
    EUROPEAN_LUXURY(
        "أوروبي فاخر (D-Cut)",
        "أوروبي",
        "مقود رياضي مقطوع من الأسفل مع مبدّلات غيار ودقة ألمانية حادة",
        540, 1.15f, 4.4f
    ),
    JAPANESE_SPORT(
        "ياباني رياضي / درفت",
        "رياضي JDM",
        "قماش ألكانتارا وشريط علوي واستجابة فورية للمنعطفات والانجراف",
        360, 1.28f, 5.4f
    ),
    CLASSIC_HERITAGE(
        "كلاسيكي خشب وكروم",
        "كلاسيكي",
        "مقود خشب جوز لامع وأذرع كروم بإحساس قيادة أصيل وهادئ",
        900, 0.88f, 2.5f
    )
}

@Serializable
enum class DriverSeatSide(val labelAr: String, val shortLabelAr: String) {
    LEFT_HAND("مقود يسار (خليجي / أوروبي)", "يسار LHD"),
    RIGHT_HAND("مقود يمين (شرقي / ياباني)", "يمين RHD")
}

@Serializable
enum class HeadlightBeamMode(val labelAr: String) {
    OFF("مطفأة"),
    LOW_BEAM("واطي"),
    HIGH_BEAM("عالي LED")
}

@Serializable
enum class TrafficSignalPhase(val labelAr: String) {
    RED("توقف — أحمر"),
    YELLOW("استعد — أصفر"),
    GREEN("انطلق — أخضر")
}

@Serializable
enum class PhotoColorFilter(val labelAr: String) {
    NATURAL("طبيعي"),
    GOLDEN_HOUR("ذهبي"),
    WARM_COPPER("نحاسي"),
    HIGHLAND_MIST("ضبابي"),
    MONOCHROME("أبيض وأسود")
}

@Serializable
enum class ReplayCameraAngle(val labelAr: String) {
    DRONE_ORBIT("درون طائرة 360°"),
    LOW_CHASE("تتبع رياضي منخفض"),
    TRACKSIDE("كاميرا المنعطف الثابتة"),
    COCKPIT("داخل المقصورة 3D")
}

@Serializable
enum class TireCompoundType(val labelAr: String) {
    HIGHWAY_SPORT("طرق سريعة H/T"),
    ALL_TERRAIN("متعدد التضاريس A/T"),
    MUD_ROCK("طين وصخور M/T"),
    DESERT_SAND("رمال الصحراء")
}

@Serializable
enum class PaintFinishType(val labelAr: String) {
    METALLIC("معدني لامع"),
    MATTE("مطفي صحراوي"),
    PEARL("لؤلؤي فاخر")
}

@Serializable
enum class BullbarStyle(val labelAr: String) {
    NONE("بدون صدام"),
    STEEL_DESERT("صدام صحراوي فولاذي"),
    EXPEDITION_WINCH("صدام رحلات + ونش")
}

@Serializable
enum class PickupAccessoryType(val labelAr: String) {
    NONE("حوض مفتوح"),
    ROLL_BAR("قضبان حماية فولاذية"),
    CANOPY_TENT("خيمة رحلات يمنية"),
    CARGO_CRATES("صناديق بضائع")
}

@Serializable
enum class GearRatioPreset(val labelAr: String) {
    CRAWLER("تسلق جبال (عزم)"),
    BALANCED("متوازن"),
    HIGHWAY("طرق سريعة (سرعة)")
}

@Serializable
enum class HudWidgetId {
    STEERING,
    PEDALS,
    SHIFTER,
    HANDBRAKE,
    MINIMAP,
    TOOLS
}

@Serializable
enum class RadioStationId {
    TAIZ_OUD_LOFI,
    SANAA_QAMARIYA_CHILL,
    IBB_RAIN_OUD,
    ADEN_COAST_BREEZE,
    HADRAMOUT_DAN_LOFI
}

@Serializable
enum class GameModeCategory(val labelAr: String) {
    ALL("الكل"),
    STORY("طور القصة"),
    RACE("سباقات القمم"),
    CARGO("نقل البضائع"),
    PASSENGER("نقل الركاب"),
    RESCUE("الإنقاذ والطوارئ"),
    CHALLENGE("تحديات جبلية"),
    FREE_ROAM("تجوال حر")
}

@Serializable
data class SmartWheelSettings(
    val rotationRangeDeg: Int = 540,
    val sensitivity: Float = 1.05f,
    val autoCenterEnabled: Boolean = true,
    val deadZoneDeg: Float = 2.5f,
    val steeringAssist: Boolean = true,
    val wheelStyle: SteeringWheelStyle = SteeringWheelStyle.EUROPEAN_LUXURY,
    val seatSide: DriverSeatSide = DriverSeatSide.LEFT_HAND,
    val hapticFeedback: Boolean = true
)

@Serializable
data class HudWidgetLayout(
    val scale: Float = 1.0f,
    val opacity: Float = 0.92f,
    val visible: Boolean = true
)

@Serializable
data class HudCustomizationSettings(
    val controlMode: TouchControlMode = TouchControlMode.WHEEL,
    val layoutPresetName: String = "الوضع المتوازن",
    val mirrorControls: Boolean = false,
    val buttonScale: Float = 1.0f,
    val hudOpacity: Float = 0.92f,
    val showMinimap: Boolean = true,
    val rearMirrorEnabled: Boolean = true,
    val smartWheel: SmartWheelSettings = SmartWheelSettings(),
    val widgets: Map<HudWidgetId, HudWidgetLayout> = HudWidgetId.entries.associateWith { HudWidgetLayout() }
)

@Serializable
data class AudioMixerSettings(
    val muted: Boolean = false,
    val musicEnabled: Boolean = true,
    val masterVolume: Float = 0.85f,
    val engineVolume: Float = 0.78f,
    val environmentVolume: Float = 0.65f,
    val musicVolume: Float = 0.72f,
    val sfxVolume: Float = 0.80f,
    val uiVolume: Float = 0.70f
)

@Serializable
data class RadioPlayerSettings(
    val isOpen: Boolean = false,
    val isPlaying: Boolean = false,
    val stationId: RadioStationId = RadioStationId.TAIZ_OUD_LOFI,
    val volume: Float = 0.75f,
    val lofiVinylEnabled: Boolean = true
)

data class RadioStationDefinition(
    val id: RadioStationId,
    val frequencyLabel: String,
    val titleAr: String,
    val subtitleAr: String,
    val cityAr: String,
    val maqamAr: String,
    val bpm: Int,
    val accentHex: String
)

@Serializable
data class ElectronicAssistsState(
    val absEnabled: Boolean = true,
    val tcsEnabled: Boolean = true,
    val escEnabled: Boolean = true,
    val hdcEnabled: Boolean = false
)

@Serializable
data class DamageState(
    val engineHealthPct: Float = 100f,
    val tiresHealthPct: Float = 100f,
    val suspensionHealthPct: Float = 100f,
    val bodyHealthPct: Float = 100f,
    val cargoIntegrityPct: Float = 100f,
    val dirtMudLevel: Float = 0f,
    val steeringPullBias: Float = 0f
)

@Serializable
data class VehicleCustomization(
    val paintColorHex: String = "#C87236",
    val paintFinish: PaintFinishType = PaintFinishType.METALLIC,
    val tireType: TireCompoundType = TireCompoundType.ALL_TERRAIN,
    val bullbarStyle: BullbarStyle = BullbarStyle.STEEL_DESERT,
    val roofRackInstalled: Boolean = true,
    val roofCargoBoxInstalled: Boolean = false,
    val pickupAccessory: PickupAccessoryType = PickupAccessoryType.NONE,
    val licensePlateCode: String = "تعز 1-4092",
    val yemenDecalEnabled: Boolean = true,
    val gearPreset: GearRatioPreset = GearRatioPreset.BALANCED,
    val rideHeightOffsetM: Float = 0.0f,
    val cargoLoadPct: Float = 0f,
    val engineUpgradeLevel: Int = 1,
    val transmissionUpgradeLevel: Int = 1,
    val suspensionUpgradeLevel: Int = 1,
    val brakeUpgradeLevel: Int = 1
)

data class VehicleSpec(
    val id: VehicleId,
    val nameAr: String,
    val subtitleAr: String,
    val categoryAr: String,
    val unlockPriceCoins: Int,
    val maxPowerHp: Int,
    val maxEngineTorqueNm: Float,
    val baseMassKg: Float,
    val maxCargoKg: Float,
    val fuelTankLiters: Float,
    val baseFuelBurnLPerMin: Float,
    val idleRpm: Float,
    val redlineRpm: Float,
    val engineBaseFreqHz: Float,
    val forwardGearsCount: Int,
    val maxBrakeForceN: Float,
    val dragCoefficient: Float,
    val rollingResistance: Float,
    val maxSteerAngleDeg: Float,
    val wheelbaseM: Float,
    val trackWidthM: Float,
    val wheelRadiusM: Float,
    val bodyLengthM: Float,
    val bodyWidthM: Float,
    val bodyHeightM: Float,
    val defaultDrivetrain: DrivetrainMode,
    val supportedDrivetrains: List<DrivetrainMode>,
    val defaultColorHex: String
)

data class VehicleTelemetry(
    val vehicleId: VehicleId = VehicleId.SHAHIQ,
    val speedKmh: Float = 0f,
    val forwardSpeedMs: Float = 0f,
    val rpm: Float = 800f,
    val redlineRpm: Float = 6200f,
    val gear: String = "D1",
    val autoShifter: AutomaticShifterState = AutomaticShifterState.D,
    val transmissionMode: String = "auto",
    val drivetrainMode: DrivetrainMode = DrivetrainMode.FOUR_H,
    val diffLockActive: Boolean = false,
    val engineRunning: Boolean = true,
    val headlightsOn: Boolean = false,
    val headlightBeamMode: HeadlightBeamMode = HeadlightBeamMode.OFF,
    val wipersOn: Boolean = false,
    val wiperPhaseRad: Float = 0f,
    val leftIndicatorOn: Boolean = false,
    val rightIndicatorOn: Boolean = false,
    val cruiseControlEnabled: Boolean = false,
    val cruiseTargetKmh: Float = 60f,
    val realismMode: DrivingRealismMode = DrivingRealismMode.SPORT,
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val handbrake: Boolean = false,
    val steeringInputNorm: Float = 0f,
    val steeringWheelAngleDeg: Float = 0f,
    val driftSlipAngleDeg: Float = 0f,
    val suspensionBounceM: Float = 0f,
    val roadCurvatureRadPerM: Float = 0f,
    val altitudeM: Float = 1420f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val headingDeg: Float = 0f,
    val surface: SurfaceType = SurfaceType.ASPHALT,
    val regionId: RegionId = RegionId.TAIZ_HIGHLANDS,
    val regionNameAr: String = "مرتفعات تعز — جبل صبر",
    val activeChunkId: Int = 0,
    val distanceAlongRoadM: Float = 60f,
    val totalDistanceDrivenM: Float = 0f,
    val lateralOffsetM: Float = 2.55f,
    val slopeGradePct: Float = 0f,
    val inTunnel: Boolean = false,
    val onBridge: Boolean = false,
    val fuelLiters: Float = 90f,
    val fuelTankCapacityL: Float = 90f,
    val lowFuelWarning: Boolean = false,
    val damage: DamageState = DamageState(),
    val assists: ElectronicAssistsState = ElectronicAssistsState(),
    val nearServiceStation: String? = null,
    val nearServiceStationNameAr: String? = null,
    val discoveredPoiId: String? = null,
    val aiRivalDistanceM: Float? = null,
    val playerRaceRank: Int = 1,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val headingRad: Float = 0f,
    val wheelRotationRad: Float = 0f,
    val frontSteerRad: Float = 0f,
    val activeTrafficCount: Int = 26,
    val activePedestriansCount: Int = 74,
    val trafficViolationWarningAr: String? = null,
    val tireScreechIntensity: Float = 0f,
    val turboBoostPsi: Float = 0f,
    val exhaustPopIntensity: Float = 0f
)

data class RoadSample(
    val distanceM: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val headingRad: Float,
    val rightX: Float,
    val rightZ: Float,
    val widthM: Float,
    val gradePct: Float,
    val surface: SurfaceType,
    val regionId: RegionId,
    val regionNameAr: String,
    val chunkId: Int,
    val isUrbanStreet: Boolean,
    val isModernBoulevard: Boolean = false,
    val curvatureRadPerM: Float = 0f,
    val bankAngleRad: Float = 0f,
    val hasTunnel: Boolean,
    val hasBridge: Boolean
)

data class RegionMeta(
    val id: RegionId,
    val nameAr: String,
    val startM: Float,
    val endM: Float,
    val baseAltitudeM: Float,
    val defaultSurface: SurfaceType
)

data class YemenCityHub(
    val id: String,
    val nameAr: String,
    val governorateAr: String,
    val subtitleAr: String,
    val regionId: RegionId,
    val distanceM: Float,
    val altitudeM: Int,
    val mapX: Float,
    val mapY: Float,
    val defaultWeather: WeatherPreset,
    val defaultTimeOfDay: TimeOfDayPreset,
    val highlightsAr: List<String>
)

data class WorldPoi(
    val id: String,
    val nameAr: String,
    val type: String, // "fuel", "workshop", "car_wash", "landmark"
    val distanceM: Float,
    val lateralOffsetM: Float,
    val regionId: RegionId
)

data class TrafficIntersectionDef(
    val id: String,
    val nameAr: String,
    val distanceM: Float,
    val cycleOffsetSec: Float
)

data class NearbyTrafficSignalInfo(
    val id: String,
    val nameAr: String,
    val distanceAheadM: Float,
    val phase: TrafficSignalPhase,
    val secondsRemaining: Int,
    val pedestriansCrossing: Boolean
)

data class SpeedRadarDef(
    val id: String,
    val nameAr: String,
    val distanceM: Float,
    val speedLimitKmh: Int
)

data class RadarAlertInfo(
    val id: String,
    val nameAr: String,
    val distanceAheadM: Float,
    val speedLimitKmh: Int
)

data class RoundaboutDef(
    val id: String,
    val nameAr: String,
    val distanceM: Float,
    val radiusM: Float,
    val monumentStyle: Int // 0=fountain/arch, 1=palm_obelisk, 2=heritage_tower
)

data class StoryChapter(
    val id: Int,
    val titleAr: String,
    val regionAr: String,
    val narratorNameAr: String,
    val narratorRoleAr: String,
    val summaryAr: String
)

data class MissionDefinition(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val category: GameModeCategory,
    val chapterId: Int? = null,
    val regionId: RegionId,
    val startDistanceM: Float,
    val finishDistanceM: Float,
    val targetTimeSec: Int,
    val rewardCoins: Int,
    val rewardXp: Int,
    val recommendedVehicleId: VehicleId,
    val weatherPreset: WeatherPreset = WeatherPreset.CLEAR,
    val timeOfDayPreset: TimeOfDayPreset = TimeOfDayPreset.MORNING,
    val hasAiRival: Boolean = false,
    val aiRivalSpeedMs: Float = 18.5f,
    val cargoRequiredPct: Int = 0,
    val dialogueBriefAr: String? = null
)

data class DriverRankInfo(
    val level: Int,
    val rankTitleAr: String,
    val nextLevelXp: Int
)

data class DrivingTripLog(
    val id: Long = 0,
    val timestampIso: String,
    val vehicleId: VehicleId,
    val regionId: RegionId,
    val regionNameAr: String,
    val distanceKm: Float,
    val maxSpeedKmh: Float,
    val durationSec: Int
)

data class PhoneDatabaseStats(
    val dbName: String = "masalik_yemen.db",
    val schemaVersion: Int = 2,
    val engineLabelAr: String = "Room SQLite (محلي 100% على الهاتف)",
    val storageUsedKb: Long = 16L,
    val totalTripsCount: Int = 0,
    val completedMissionsCount: Int = 0,
    val unlockedVehiclesCount: Int = 1,
    val recentTrips: List<DrivingTripLog> = emptyList(),
    val lastSavedIso: String = ""
)

data class EngineeringTestResult(
    val id: String,
    val category: String,
    val titleAr: String,
    val passed: Boolean,
    val metricLabel: String,
    val detailsAr: String
)

@Serializable
data class SaveGameSchema(
    val schemaVersion: Int = 2,
    val timestampIso: String = "",
    val playerNameAr: String = "سائق مَسَالِك",
    val selectedVehicleId: VehicleId = VehicleId.SHAHIQ,
    val unlockedVehicles: List<VehicleId> = listOf(VehicleId.SHAHIQ, VehicleId.SAQR, VehicleId.MADINA),
    val masalikCoins: Int = 12500,
    val driverXp: Int = 150,
    val drivingRealism: DrivingRealismMode = DrivingRealismMode.SPORT,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val hudSettings: HudCustomizationSettings = HudCustomizationSettings(),
    val audioMixer: AudioMixerSettings = AudioMixerSettings(),
    val radioSettings: RadioPlayerSettings = RadioPlayerSettings(),
    val timeOfDay: TimeOfDayPreset = TimeOfDayPreset.MORNING,
    val autoTimeCycleEnabled: Boolean = true,
    val worldTimeHours: Float = 9.0f,
    val weather: WeatherPreset = WeatherPreset.CLEAR,
    val playerDistanceM: Float = 60f,
    val lastRegionId: RegionId = RegionId.TAIZ_HIGHLANDS,
    val visitedCities: List<String> = listOf("city_taiz_sabir"),
    val fuelByVehicle: Map<VehicleId, Float> = VehicleFleetCatalog.defaultFuelMap(),
    val damageByVehicle: Map<VehicleId, DamageState> = VehicleFleetCatalog.defaultDamageMap(),
    val customizations: Map<VehicleId, VehicleCustomization> = VehicleFleetCatalog.defaultCustomizations(),
    val discoveredPois: List<String> = emptyList(),
    val missionBestTimesSec: Map<String, Float> = emptyMap(),
    val missionCompletions: Map<String, Int> = emptyMap(),
    val totalCareerDistanceM: Float = 0f
)
