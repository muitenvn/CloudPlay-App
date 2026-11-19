package com.muitenvn.cloudplay.pad

import android.content.SharedPreferences
import com.muitenvn.cloudplay.protocol.RemotePadButtons
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow

enum class PadPressMode(val id: String) {
    NORMAL("normal"),
    TOGGLE_HOLD("toggle"),
    TURBO("turbo");

    fun next(): PadPressMode = when (this) {
        NORMAL -> TOGGLE_HOLD
        TOGGLE_HOLD -> TURBO
        TURBO -> NORMAL
    }

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        NORMAL -> PadStrings.tr(lang, "Bấm thả", "Normal")
        TOGGLE_HOLD -> PadStrings.tr(lang, "Chạm khóa", "Toggle Hold")
        TURBO -> PadStrings.tr(lang, "Nhấp nhanh (Turbo)", "Turbo")
    }
}

enum class PadLayoutPreset(
    val id: String,
    val shoulderBonusScale: Float,
    val abxyBonusScale: Float,
    val stickBonusScale: Float,
    val rightStickBonusScale: Float,
    val autoSprintL3: Boolean,
    val swapSwitchAbxy: Boolean
) {
    STANDARD("standard", 1.0f, 1.0f, 1.0f, 1.0f, true, false),
    FPS_PRO("fps", 1.08f, 1.04f, 1.05f, 1.15f, true, false),
    ACTION("action", 1.04f, 1.12f, 1.04f, 1.06f, true, false),
    RACING("racing", 1.14f, 0.96f, 1.12f, 1.0f, false, false),
    SWITCH_SWAP("switch", 1.0f, 1.0f, 1.0f, 1.0f, true, true);

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        STANDARD -> PadStrings.tr(lang, "Bố cục: Chuẩn", "Layout: Standard")
        FPS_PRO -> PadStrings.tr(lang, "Bố cục: Bắn súng", "Layout: FPS Pro")
        ACTION -> PadStrings.tr(lang, "Bố cục: Hành động", "Layout: Action")
        RACING -> PadStrings.tr(lang, "Bố cục: Đua xe", "Layout: Racing")
        SWITCH_SWAP -> PadStrings.tr(lang, "Bố cục: Đảo nút", "Layout: Swapped")
    }

    fun shortTitle(lang: PadLanguage): String = when (this) {
        STANDARD -> PadStrings.tr(lang, "Chuẩn", "Standard")
        FPS_PRO -> PadStrings.tr(lang, "Bắn súng", "FPS Pro")
        ACTION -> PadStrings.tr(lang, "Hành động", "Action")
        RACING -> PadStrings.tr(lang, "Đua xe", "Racing")
        SWITCH_SWAP -> PadStrings.tr(lang, "Đảo nút", "Swapped")
    }
}

enum class PadLayoutScope(val id: String) {
    GLOBAL("global"),
    PER_GAME("per_game");

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        GLOBAL -> PadStrings.tr(lang, "Chung mọi game", "Global")
        PER_GAME -> PadStrings.tr(lang, "Riêng từng game", "Per-game")
    }
}

enum class PadHapticLevel(val id: Int, val scale: Float) {
    OFF(0, 0f),
    LIGHT(1, 0.45f),
    MEDIUM(2, 0.75f),
    STRONG(3, 1.0f);

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        OFF -> PadStrings.tr(lang, "Tắt", "Off")
        LIGHT -> PadStrings.tr(lang, "Nhẹ", "Light")
        MEDIUM -> PadStrings.tr(lang, "Vừa", "Medium")
        STRONG -> PadStrings.tr(lang, "Mạnh", "Strong")
    }

    companion object {
        fun fromId(id: Int): PadHapticLevel = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

enum class PadStickCurve(val id: String, val exponent: Float) {
    LINEAR("linear", 1.0f),
    PRECISION("precision", 1.45f),
    AGGRESSIVE("aggressive", 0.72f);

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        LINEAR -> PadStrings.tr(lang, "Tuyến tính", "Linear")
        PRECISION -> PadStrings.tr(lang, "Nhạy tâm", "Precision")
        AGGRESSIVE -> PadStrings.tr(lang, "Nhạy rìa", "Fast")
    }

    fun applyCurve(v: Float): Float {
        val sign = if (v < 0f) -1f else 1f
        val mag = kotlin.math.abs(v).coerceIn(0f, 1f)
        return (sign * mag.pow(exponent)).coerceIn(-1f, 1f)
    }

    companion object {
        fun fromId(id: String): PadStickCurve = entries.firstOrNull { it.id == id } ?: LINEAR
    }
}

enum class PadCustomBtnShape(val id: String) {
    ROUND("round"),
    PILL("pill"),
    CUSTOM_RECT("rect");

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        ROUND -> PadStrings.tr(lang, "Tròn", "Round")
        PILL -> PadStrings.tr(lang, "Viên thuốc", "Pill")
        CUSTOM_RECT -> PadStrings.tr(lang, "Chữ nhật W×H", "Rect W×H")
    }

    companion object {
        fun fromId(id: String): PadCustomBtnShape = entries.firstOrNull { it.id == id } ?: PILL
    }
}

data class PadCustomButtonSpec(
    val id: String,
    val label: String,
    val shape: PadCustomBtnShape = PadCustomBtnShape.PILL,
    val assignedMask: Int = RemotePadButtons.A
)

data class PadNodeTuning(
    val dxNorm: Float = 0f,
    val dyNorm: Float = 0f,
    val scaleMult: Float = 1.0f,
    val widthMult: Float = 1.0f,
    val heightMult: Float = 1.0f,
    val opacity: Float = 0.85f,
    val directionalSwipeEnabled: Boolean = false,
    val swipeBit: Int = 0,
    val swipeUpBit: Int = 0,
    val swipeDownBit: Int = 0,
    val swipeLeftBit: Int = 0,
    val swipeRightBit: Int = 0,
    val swipeIncludeShoulder: Boolean = true,
    val comboMask: Int = 0,
    val analogTriggerDrag: Int = -1, // -1 = theo preset, 0 = tắt, 1 = bật
    val hapticOverride: Int = -1,    // -1 = theo cài đặt chung, 0..3 = Tắt..Mạnh
    val hidden: Boolean = false,
    val pressMode: PadPressMode = PadPressMode.NORMAL,
    val turboHz: Int = 10
) {
    fun hasCustomDimensions(): Boolean =
        kotlin.math.abs(widthMult - 1.0f) >= 0.02f || kotlin.math.abs(heightMult - 1.0f) >= 0.02f
}

enum class PadGyroMode(val id: String) {
    FPS_VR_360("fps_vr"),
    RACING_STEER("racing"),
    LEFT_STICK_360("left_360");

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        FPS_VR_360 -> PadStrings.tr(lang, "Bắn súng / VR — Mọi góc (Cần phải)", "FPS / VR — All Angles (Right Stick)")
        RACING_STEER -> PadStrings.tr(lang, "Đua xe — Chỉ trái/phải (Cần trái)", "Racing — Left/Right Only (Left Stick)")
        LEFT_STICK_360 -> PadStrings.tr(lang, "Di chuyển 360° — Mọi góc (Cần trái)", "360° Move — All Angles (Left Stick)")
    }

    fun shortTitle(lang: PadLanguage): String = when (this) {
        FPS_VR_360 -> PadStrings.tr(lang, "Bắn súng / VR (Mọi góc)", "FPS / VR (All Angles)")
        RACING_STEER -> PadStrings.tr(lang, "Đua xe (Chỉ trái/phải)", "Racing (Left/Right)")
        LEFT_STICK_360 -> PadStrings.tr(lang, "Cần trái (Mọi góc)", "Left Stick (All Angles)")
    }

    companion object {
        fun fromId(id: String): PadGyroMode = entries.firstOrNull { it.id == id } ?: FPS_VR_360
    }
}

data class PadGlobalControllerSettings(
    val globalScale: Float = 1.0f,
    val globalOpacity: Float = 0.90f,
    val layoutScope: PadLayoutScope = PadLayoutScope.PER_GAME,
    val buttonHapticLevel: PadHapticLevel = PadHapticLevel.MEDIUM,
    val gameRumbleLevel: PadHapticLevel = PadHapticLevel.STRONG,
    val leftStickSens: Float = 1.0f,
    val rightStickSens: Float = 1.0f,
    val leftStickCurve: PadStickCurve = PadStickCurve.LINEAR,
    val rightStickCurve: PadStickCurve = PadStickCurve.LINEAR,
    val innerDeadzone: Float = 0.06f,
    val outerDeadzone: Float = 0.96f,
    val rightInnerDeadzone: Float = 0.06f,
    val rightOuterDeadzone: Float = 0.96f,
    val autoSprintL3Override: Int = -1, // -1 = theo preset, 0 = tắt, 1 = bật
    val dpadSplitMode: Boolean = false,
    val abxySplitMode: Boolean = false,
    val isPositionLocked: Boolean = false,
    val showEditGrid: Boolean = true,
    val gyroEnabled: Boolean = false, // Mặc định TẮT hoàn toàn
    val gyroMode: PadGyroMode = PadGyroMode.FPS_VR_360,
    val gyroSensX: Int = 100,
    val gyroSensY: Int = 100,
    val gyroDeadzone: Int = 2,
    val gyroInvertX: Boolean = false,
    val gyroInvertY: Boolean = false,
    val gyroHoldLtOnly: Boolean = false
)

internal object PadControllerStorage {
    fun loadGlobalSettings(prefs: SharedPreferences): PadGlobalControllerSettings {
        val legacyHaptic = prefs.getBoolean("pad_touch_haptic", true)
        val defaultBtnHaptic = if (legacyHaptic) PadHapticLevel.MEDIUM.id else PadHapticLevel.OFF.id
        val inDz = prefs.getFloat("pad_inner_deadzone", 0.06f).coerceIn(0.00f, 0.30f)
        val outDz = prefs.getFloat("pad_outer_deadzone", 0.96f).coerceIn(0.70f, 1.0f)
        return PadGlobalControllerSettings(
            globalScale = prefs.getFloat("pad_global_scale", 1.0f).coerceIn(0.60f, 1.65f),
            globalOpacity = prefs.getFloat("pad_global_opacity", 0.90f).coerceIn(0.05f, 1.0f),
            layoutScope = if (prefs.getString("pad_layout_scope", "per_game") == "global") {
                PadLayoutScope.GLOBAL
            } else {
                PadLayoutScope.PER_GAME
            },
            buttonHapticLevel = PadHapticLevel.fromId(prefs.getInt("pad_btn_haptic_level", defaultBtnHaptic)),
            gameRumbleLevel = PadHapticLevel.fromId(prefs.getInt("pad_game_rumble_level", PadHapticLevel.STRONG.id)),
            leftStickSens = prefs.getFloat("pad_left_stick_sens", 1.0f).coerceIn(0.60f, 1.60f),
            rightStickSens = prefs.getFloat("pad_right_stick_sens", 1.0f).coerceIn(0.60f, 1.60f),
            leftStickCurve = PadStickCurve.fromId(prefs.getString("pad_left_stick_curve", "linear").orEmpty()),
            rightStickCurve = PadStickCurve.fromId(prefs.getString("pad_right_stick_curve", "linear").orEmpty()),
            innerDeadzone = inDz,
            outerDeadzone = outDz,
            rightInnerDeadzone = prefs.getFloat("pad_right_inner_deadzone", inDz).coerceIn(0.00f, 0.30f),
            rightOuterDeadzone = prefs.getFloat("pad_right_outer_deadzone", outDz).coerceIn(0.70f, 1.0f),
            autoSprintL3Override = prefs.getInt("pad_auto_sprint_override", -1),
            dpadSplitMode = prefs.getBoolean("pad_dpad_split_mode", false),
            abxySplitMode = prefs.getBoolean("pad_abxy_split_mode", false),
            isPositionLocked = prefs.getBoolean("pad_position_locked", false),
            showEditGrid = prefs.getBoolean("pad_show_edit_grid", true),
            gyroEnabled = prefs.getBoolean("pad_gyro_enabled", false),
            gyroMode = PadGyroMode.fromId(prefs.getString("pad_gyro_mode", PadGyroMode.FPS_VR_360.id).orEmpty()),
            gyroSensX = prefs.getInt("pad_gyro_sens_x", 100).coerceIn(10, 300),
            gyroSensY = prefs.getInt("pad_gyro_sens_y", 100).coerceIn(10, 300),
            gyroDeadzone = prefs.getInt("pad_gyro_deadzone", 2).coerceIn(0, 20),
            gyroInvertX = prefs.getBoolean("pad_gyro_invert_x", false),
            gyroInvertY = prefs.getBoolean("pad_gyro_invert_y", false),
            gyroHoldLtOnly = prefs.getBoolean("pad_gyro_hold_lt_only", false)
        )
    }

    fun saveGlobalSettings(prefs: SharedPreferences, s: PadGlobalControllerSettings) {
        prefs.edit()
            .putFloat("pad_global_scale", s.globalScale.coerceIn(0.60f, 1.65f))
            .putFloat("pad_global_opacity", s.globalOpacity.coerceIn(0.05f, 1.0f))
            .putString("pad_layout_scope", s.layoutScope.id)
            .putInt("pad_btn_haptic_level", s.buttonHapticLevel.id)
            .putBoolean("pad_touch_haptic", s.buttonHapticLevel != PadHapticLevel.OFF)
            .putInt("pad_game_rumble_level", s.gameRumbleLevel.id)
            .putFloat("pad_left_stick_sens", s.leftStickSens.coerceIn(0.60f, 1.60f))
            .putFloat("pad_right_stick_sens", s.rightStickSens.coerceIn(0.60f, 1.60f))
            .putString("pad_left_stick_curve", s.leftStickCurve.id)
            .putString("pad_right_stick_curve", s.rightStickCurve.id)
            .putFloat("pad_inner_deadzone", s.innerDeadzone.coerceIn(0.00f, 0.30f))
            .putFloat("pad_outer_deadzone", s.outerDeadzone.coerceIn(0.70f, 1.0f))
            .putFloat("pad_right_inner_deadzone", s.rightInnerDeadzone.coerceIn(0.00f, 0.30f))
            .putFloat("pad_right_outer_deadzone", s.rightOuterDeadzone.coerceIn(0.70f, 1.0f))
            .putInt("pad_auto_sprint_override", s.autoSprintL3Override)
            .putBoolean("pad_dpad_split_mode", s.dpadSplitMode)
            .putBoolean("pad_abxy_split_mode", s.abxySplitMode)
            .putBoolean("pad_position_locked", s.isPositionLocked)
            .putBoolean("pad_show_edit_grid", s.showEditGrid)
            .putBoolean("pad_gyro_enabled", s.gyroEnabled)
            .putString("pad_gyro_mode", s.gyroMode.id)
            .putInt("pad_gyro_sens_x", s.gyroSensX.coerceIn(10, 300))
            .putInt("pad_gyro_sens_y", s.gyroSensY.coerceIn(10, 300))
            .putInt("pad_gyro_deadzone", s.gyroDeadzone.coerceIn(0, 20))
            .putBoolean("pad_gyro_invert_x", s.gyroInvertX)
            .putBoolean("pad_gyro_invert_y", s.gyroInvertY)
            .putBoolean("pad_gyro_hold_lt_only", s.gyroHoldLtOnly)
            .apply()
    }

    fun loadPresetTunings(
        prefs: SharedPreferences,
        presetId: String
    ): Map<String, PadNodeTuning> {
        return try {
            val raw = prefs.getString("tunings_$presetId", "{}").orEmpty()
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, PadNodeTuning>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = root.optJSONObject(k) ?: continue
                val modeId = obj.optString("pm", PadPressMode.NORMAL.id)
                val mode = PadPressMode.entries.firstOrNull { it.id == modeId } ?: PadPressMode.NORMAL
                val swU = obj.optInt("swU", 0)
                val swD = obj.optInt("swD", 0)
                val swL = obj.optInt("swL", 0)
                val swR = obj.optInt("swR", 0)
                val swLegacy = obj.optInt("sw", 0)
                val hasAnySwipe = swU != 0 || swD != 0 || swL != 0 || swR != 0 || swLegacy != 0
                out[k] = PadNodeTuning(
                    dxNorm = obj.optDouble("dx", 0.0).toFloat().coerceIn(-0.42f, 0.42f),
                    dyNorm = obj.optDouble("dy", 0.0).toFloat().coerceIn(-0.42f, 0.42f),
                    scaleMult = obj.optDouble("sc", 1.0).toFloat().coerceIn(0.55f, 1.75f),
                    widthMult = obj.optDouble("wm", 1.0).toFloat().coerceIn(0.50f, 2.50f),
                    heightMult = obj.optDouble("hm", 1.0).toFloat().coerceIn(0.50f, 2.50f),
                    opacity = obj.optDouble("op", 0.85).toFloat().coerceIn(0.05f, 1.0f),
                    directionalSwipeEnabled = obj.optBoolean("ds", hasAnySwipe),
                    swipeBit = swLegacy,
                    swipeUpBit = swU,
                    swipeDownBit = swD,
                    swipeLeftBit = swL,
                    swipeRightBit = swR,
                    swipeIncludeShoulder = obj.optBoolean("swInc", true),
                    comboMask = obj.optInt("cb", 0),
                    analogTriggerDrag = obj.optInt("atd", -1),
                    hapticOverride = obj.optInt("hp", -1),
                    hidden = obj.optBoolean("hid", false),
                    pressMode = mode,
                    turboHz = obj.optInt("hz", 10).coerceIn(5, 20)
                )
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun savePresetTunings(
        prefs: SharedPreferences,
        presetId: String,
        map: Map<String, PadNodeTuning>
    ) {
        try {
            val root = JSONObject()
            for ((k, v) in map) {
                root.put(
                    k,
                    JSONObject().apply {
                        put("dx", v.dxNorm.toDouble())
                        put("dy", v.dyNorm.toDouble())
                        put("sc", v.scaleMult.toDouble())
                        put("wm", v.widthMult.toDouble())
                        put("hm", v.heightMult.toDouble())
                        put("op", v.opacity.toDouble())
                        put("ds", v.directionalSwipeEnabled)
                        put("sw", v.swipeBit)
                        put("swU", v.swipeUpBit)
                        put("swD", v.swipeDownBit)
                        put("swL", v.swipeLeftBit)
                        put("swR", v.swipeRightBit)
                        put("swInc", v.swipeIncludeShoulder)
                        put("cb", v.comboMask)
                        put("atd", v.analogTriggerDrag)
                        put("hp", v.hapticOverride)
                        put("hid", v.hidden)
                        put("pm", v.pressMode.id)
                        put("hz", v.turboHz)
                    }
                )
            }
            prefs.edit().putString("tunings_$presetId", root.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun loadCustomButtons(prefs: SharedPreferences, storageKey: String): List<PadCustomButtonSpec> {
        return try {
            val raw = prefs.getString("custom_btns_$storageKey", "[]").orEmpty()
            val arr = JSONArray(raw)
            val list = ArrayList<PadCustomButtonSpec>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                if (id.isEmpty()) continue
                list.add(
                    PadCustomButtonSpec(
                        id = id,
                        label = obj.optString("label", "M1").take(8).ifBlank { "M1" },
                        shape = PadCustomBtnShape.fromId(obj.optString("shape", "pill")),
                        assignedMask = obj.optInt("mask", RemotePadButtons.A)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomButtons(
        prefs: SharedPreferences,
        storageKey: String,
        list: List<PadCustomButtonSpec>
    ) {
        try {
            val arr = JSONArray()
            for (item in list) {
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("label", item.label)
                        put("shape", item.shape.id)
                        put("mask", item.assignedMask)
                    }
                )
            }
            prefs.edit().putString("custom_btns_$storageKey", arr.toString()).apply()
        } catch (_: Exception) {
        }
    }
}

internal const val PAD_SYNTH_BIT_LT: Int = 1 shl 16
internal const val PAD_SYNTH_BIT_RT: Int = 1 shl 17

internal data class PadAssignableButtonOption(
    val bit: Int,
    val shortLabel: String
)

internal val AllPadAssignableButtons: List<PadAssignableButtonOption> = listOf(
    PadAssignableButtonOption(RemotePadButtons.A, "A"),
    PadAssignableButtonOption(RemotePadButtons.B, "B"),
    PadAssignableButtonOption(RemotePadButtons.X, "X"),
    PadAssignableButtonOption(RemotePadButtons.Y, "Y"),
    PadAssignableButtonOption(RemotePadButtons.LB, "LB"),
    PadAssignableButtonOption(RemotePadButtons.RB, "RB"),
    PadAssignableButtonOption(PAD_SYNTH_BIT_LT, "LT"),
    PadAssignableButtonOption(PAD_SYNTH_BIT_RT, "RT"),
    PadAssignableButtonOption(RemotePadButtons.L3, "L3"),
    PadAssignableButtonOption(RemotePadButtons.R3, "R3"),
    PadAssignableButtonOption(RemotePadButtons.DPAD_UP, "▲"),
    PadAssignableButtonOption(RemotePadButtons.DPAD_DOWN, "▼"),
    PadAssignableButtonOption(RemotePadButtons.DPAD_LEFT, "◀"),
    PadAssignableButtonOption(RemotePadButtons.DPAD_RIGHT, "▶"),
    PadAssignableButtonOption(RemotePadButtons.VIEW, "View"),
    PadAssignableButtonOption(RemotePadButtons.MENU, "Menu"),
    PadAssignableButtonOption(RemotePadButtons.XBOX, "Xbox")
)

internal fun primaryBitForNodeKey(nodeKey: String): Int = when (nodeKey) {
    "btn_a" -> RemotePadButtons.A
    "btn_b" -> RemotePadButtons.B
    "btn_x" -> RemotePadButtons.X
    "btn_y" -> RemotePadButtons.Y
    "btn_lb" -> RemotePadButtons.LB
    "btn_rb" -> RemotePadButtons.RB
    "btn_lt" -> PAD_SYNTH_BIT_LT
    "btn_rt" -> PAD_SYNTH_BIT_RT
    "btn_l3" -> RemotePadButtons.L3
    "btn_r3" -> RemotePadButtons.R3
    "dpad_up" -> RemotePadButtons.DPAD_UP
    "dpad_down" -> RemotePadButtons.DPAD_DOWN
    "dpad_left" -> RemotePadButtons.DPAD_LEFT
    "dpad_right" -> RemotePadButtons.DPAD_RIGHT
    "btn_view" -> RemotePadButtons.VIEW
    "btn_menu" -> RemotePadButtons.MENU
    "btn_xbox" -> RemotePadButtons.XBOX
    else -> 0
}

internal fun formatMaskBadge(mask: Int): String {
    if (mask == 0) return ""
    return AllPadAssignableButtons
        .filter { (mask and it.bit) != 0 }
        .joinToString("+") { it.shortLabel }
}

internal fun friendlyNodeTitle(nodeKey: String, lang: PadLanguage, customButtons: List<PadCustomButtonSpec>): String {
    if (nodeKey.startsWith("custom_")) {
        val custom = customButtons.firstOrNull { it.id == nodeKey }
        return if (custom != null) {
            PadStrings.tr(lang, "Nút trống [${custom.label}]", "Custom [${custom.label}]")
        } else {
            nodeKey
        }
    }
    return when (nodeKey) {
        "btn_lt" -> "Cò trái LT"
        "btn_rt" -> "Cò phải RT"
        "btn_lb" -> "Nút vai LB"
        "btn_rb" -> "Nút vai RB"
        "btn_l3" -> "Nút ấn cần L3"
        "btn_r3" -> "Nút ấn cần R3"
        "btn_view" -> "Nút ⧉ View"
        "btn_xbox" -> "Nút Xbox Guide"
        "btn_menu" -> "Nút ☰ Menu"
        "stick_left" -> "Cần Analog Trái (L)"
        "stick_right" -> "Cần Analog Phải (R)"
        "dpad" -> "Cụm D-Pad"
        "dpad_up" -> "D-Pad ▲ Lên"
        "dpad_down" -> "D-Pad ▼ Xuống"
        "dpad_left" -> "D-Pad ◀ Trái"
        "dpad_right" -> "D-Pad ▶ Phải"
        "abxy" -> "Cụm ABXY"
        "btn_a" -> "Nút A"
        "btn_b" -> "Nút B"
        "btn_x" -> "Nút X"
        "btn_y" -> "Nút Y"
        else -> nodeKey
    }
}
