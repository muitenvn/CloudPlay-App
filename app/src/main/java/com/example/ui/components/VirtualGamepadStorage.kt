package com.example.ui.components

import com.example.data.GamepadButtonPressMode
import com.example.data.GamepadHapticLevel
import kotlin.math.abs
import kotlin.math.roundToInt

data class GamepadEditableNodeInfo(
    val id: String,
    val labelVi: String,
    val labelEn: String,
    val shortBadge: String,
    val primaryButtonIndex: Int? = null
)

data class XboxTargetButtonChoice(
    val index: Int,
    val badge: String,
    val labelVi: String,
    val labelEn: String
)

internal val XBOX_TARGET_BUTTON_CHOICES = listOf(
    XboxTargetButtonChoice(0, "A", "Nút A", "A"),
    XboxTargetButtonChoice(1, "B", "Nút B", "B"),
    XboxTargetButtonChoice(2, "X", "Nút X", "X"),
    XboxTargetButtonChoice(3, "Y", "Nút Y", "Y"),
    XboxTargetButtonChoice(4, "LB", "Nút LB", "LB"),
    XboxTargetButtonChoice(5, "RB", "Nút RB", "RB"),
    XboxTargetButtonChoice(6, "LT", "Cò LT", "LT"),
    XboxTargetButtonChoice(7, "RT", "Cò RT", "RT"),
    XboxTargetButtonChoice(10, "L3", "Nhấn cần L3", "L3"),
    XboxTargetButtonChoice(11, "R3", "Nhấn cần R3", "R3"),
    XboxTargetButtonChoice(12, "▲", "Phím Lên ▲", "Up ▲"),
    XboxTargetButtonChoice(13, "▼", "Phím Xuống ▼", "Down ▼"),
    XboxTargetButtonChoice(14, "◀", "Phím Trái ◀", "Left ◀"),
    XboxTargetButtonChoice(15, "▶", "Phím Phải ▶", "Right ▶"),
    XboxTargetButtonChoice(8, "View", "Nút View ⧉", "View ⧉"),
    XboxTargetButtonChoice(9, "Menu", "Nút Menu ☰", "Menu ☰"),
    XboxTargetButtonChoice(16, "Xbox", "Nút Xbox", "Xbox")
)

internal val ALL_GAMEPAD_NODES = listOf(
    GamepadEditableNodeInfo("btn_lt", "Cò trái LT", "Left Trigger LT", "LT", 6),
    GamepadEditableNodeInfo("btn_lb", "Nút vai LB", "Left Bumper LB", "LB", 4),
    GamepadEditableNodeInfo("btn_l3", "Nhấn cần trái L3", "Left Stick Click L3", "L3", 10),
    GamepadEditableNodeInfo("btn_r3", "Nhấn cần phải R3", "Right Stick Click R3", "R3", 11),
    GamepadEditableNodeInfo("btn_rb", "Nút vai RB", "Right Bumper RB", "RB", 5),
    GamepadEditableNodeInfo("btn_rt", "Cò phải RT", "Right Trigger RT", "RT", 7),
    GamepadEditableNodeInfo("btn_view", "Nút View (⧉)", "View Button (⧉)", "⧉ View", 8),
    GamepadEditableNodeInfo("btn_xbox", "Nút Xbox", "Xbox Guide Button", "Xbox", 16),
    GamepadEditableNodeInfo("btn_menu", "Nút Menu (☰)", "Menu Button (☰)", "☰ Menu", 9),
    GamepadEditableNodeInfo("left_stick", "Cần điều khiển trái (L)", "Left Analog Stick (L)", "Cần L", null),
    GamepadEditableNodeInfo("right_stick", "Cần ngắm phải (R)", "Right Analog Stick (R)", "Cần R", null),
    GamepadEditableNodeInfo("dpad", "Cụm phím D-Pad", "D-Pad Cross Cluster", "D-Pad", null),
    GamepadEditableNodeInfo("dpad_up", "Phím Lên (▲)", "D-Pad Up (▲)", "▲", 12),
    GamepadEditableNodeInfo("dpad_down", "Phím Xuống (▼)", "D-Pad Down (▼)", "▼", 13),
    GamepadEditableNodeInfo("dpad_left", "Phím Trái (◀)", "D-Pad Left (◀)", "◀", 14),
    GamepadEditableNodeInfo("dpad_right", "Phím Phải (▶)", "D-Pad Right (▶)", "▶", 15),
    GamepadEditableNodeInfo("abxy", "Chùm nút ABXY", "ABXY Action Cluster", "ABXY", null),
    GamepadEditableNodeInfo("btn_a", "Nút A (Xanh lá)", "A Button (Green)", "A", 0),
    GamepadEditableNodeInfo("btn_b", "Nút B (Đỏ)", "B Button (Red)", "B", 1),
    GamepadEditableNodeInfo("btn_x", "Nút X (Xanh dương)", "X Button (Blue)", "X", 2),
    GamepadEditableNodeInfo("btn_y", "Nút Y (Vàng)", "Y Button (Yellow)", "Y", 3)
)

data class GamepadButtonBehavior(
    val pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    val turboHz: Int = 10,
    val analogSwipe: Boolean? = null,
    val extraButtons: Set<Int> = emptySet(),
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f
) {
    fun hasCustomDimensions(): Boolean {
        return abs(widthScale - 1.0f) >= 0.02f || abs(heightScale - 1.0f) >= 0.02f
    }

    fun isDefault(): Boolean {
        return pressMode == GamepadButtonPressMode.NORMAL &&
            turboHz == 10 &&
            analogSwipe == null &&
            extraButtons.isEmpty() &&
            !hasCustomDimensions()
    }
}

data class CustomGamepadButtonDef(
    val id: String,
    val isRound: Boolean = true,
    val targetButtons: Set<Int> = emptySet()
) {
    fun numberSuffix(): Int {
        return id.removePrefix("custom_").toIntOrNull() ?: 1
    }

    fun resolvedBadge(isEn: Boolean = false): String {
        if (targetButtons.isEmpty()) {
            val n = numberSuffix()
            return if (isEn) "M$n" else "Nút $n"
        }
        val badges = targetButtons.sorted().mapNotNull { idx ->
            XBOX_TARGET_BUTTON_CHOICES.find { it.index == idx }?.badge
        }
        return when {
            badges.isEmpty() -> "M${numberSuffix()}"
            badges.size <= 2 -> badges.joinToString("+")
            else -> "${badges.take(2).joinToString("+")}+${badges.size - 2}"
        }
    }
}

internal fun isCustomNodeId(nodeId: String): Boolean = nodeId.startsWith("custom_")

internal fun parseControlOffsets(raw: String): Map<String, Pair<Float, Float>> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, Pair<Float, Float>>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val coords = parts[1].split(",")
            if (coords.size == 2) {
                val x = coords[0].toFloatOrNull()
                val y = coords[1].toFloatOrNull()
                if (x != null && y != null) {
                    result[parts[0]] = x to y
                }
            }
        }
    }
    return result
}

internal fun serializeControlOffsets(map: Map<String, Pair<Float, Float>>): String {
    return map.entries.joinToString(";") { (k, v) ->
        "${k}:${v.first.roundToInt()},${v.second.roundToInt()}"
    }
}

internal fun parseButtonScales(raw: String): Map<String, Float> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, Float>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val scale = parts[1].toFloatOrNull()
            if (scale != null) {
                result[parts[0]] = scale.coerceIn(0.50f, 2.00f)
            }
        }
    }
    return result
}

internal fun serializeButtonScales(map: Map<String, Float>): String {
    return map.entries
        .filter { abs(it.value - 1.0f) >= 0.02f }
        .joinToString(";") { (k, v) ->
            val rounded = (v.coerceIn(0.50f, 2.00f) * 100f).roundToInt() / 100f
            "$k:$rounded"
        }
}

internal fun parseHiddenButtons(raw: String): Set<String> {
    if (raw.isBlank()) return emptySet()
    return raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

internal fun serializeHiddenButtons(set: Set<String>): String {
    return set.filter { it.isNotBlank() }.sorted().joinToString(",")
}

internal fun parseButtonOpacities(raw: String): Map<String, Float> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, Float>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val op = parts[1].toFloatOrNull()
            if (op != null) {
                result[parts[0]] = op.coerceIn(0.20f, 1.00f)
            }
        }
    }
    return result
}

internal fun serializeButtonOpacities(map: Map<String, Float>): String {
    return map.entries
        .joinToString(";") { (k, v) ->
            val rounded = (v.coerceIn(0.20f, 2.00f).coerceIn(0.20f, 1.00f) * 100f).roundToInt() / 100f
            "$k:$rounded"
        }
}

internal fun parseButtonHaptics(raw: String): Map<String, GamepadHapticLevel> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, GamepadHapticLevel>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val lvl = runCatching { GamepadHapticLevel.valueOf(parts[1]) }.getOrNull()
            if (lvl != null) {
                result[parts[0]] = lvl
            }
        }
    }
    return result
}

internal fun serializeButtonHaptics(map: Map<String, GamepadHapticLevel>): String {
    return map.entries.joinToString(";") { (k, v) -> "$k:${v.name}" }
}

internal fun parseButtonBehaviors(raw: String): Map<String, GamepadButtonBehavior> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, GamepadButtonBehavior>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val nodeId = parts[0].trim()
            val fields = parts[1].split("|")
            if (nodeId.isNotEmpty() && fields.isNotEmpty()) {
                val mode = runCatching { GamepadButtonPressMode.valueOf(fields.getOrNull(0) ?: "") }
                    .getOrDefault(GamepadButtonPressMode.NORMAL)
                val hz = (fields.getOrNull(1)?.toIntOrNull() ?: 10).coerceIn(5, 20)
                val analog = when (fields.getOrNull(2)) {
                    "1" -> true
                    "0" -> false
                    else -> null
                }
                val extras = fields.getOrNull(3)
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.filter { it in 0..16 }
                    ?.toSet()
                    ?: emptySet()
                val wScale = (fields.getOrNull(4)?.toFloatOrNull() ?: 1.0f).coerceIn(0.50f, 2.50f)
                val hScale = (fields.getOrNull(5)?.toFloatOrNull() ?: 1.0f).coerceIn(0.50f, 2.50f)
                val b = GamepadButtonBehavior(mode, hz, analog, extras, wScale, hScale)
                if (!b.isDefault()) {
                    result[nodeId] = b
                }
            }
        }
    }
    return result
}

internal fun serializeButtonBehaviors(map: Map<String, GamepadButtonBehavior>): String {
    return map.entries
        .filter { !it.value.isDefault() }
        .joinToString(";") { (k, v) ->
            val analogCode = when (v.analogSwipe) {
                true -> "1"
                false -> "0"
                null -> "_"
            }
            val extrasCode = v.extraButtons.sorted().joinToString(",")
            val wCode = ((v.widthScale.coerceIn(0.50f, 2.50f) * 100f).roundToInt() / 100f).toString()
            val hCode = ((v.heightScale.coerceIn(0.50f, 2.50f) * 100f).roundToInt() / 100f).toString()
            "$k:${v.pressMode.name}|${v.turboHz.coerceIn(5, 20)}|$analogCode|$extrasCode|$wCode|$hCode"
        }
}

internal fun parseCustomButtons(raw: String): List<CustomGamepadButtonDef> {
    if (raw.isBlank()) return emptyList()
    val result = mutableListOf<CustomGamepadButtonDef>()
    val seen = mutableSetOf<String>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val id = parts[0].trim()
            if (id.startsWith("custom_") && seen.add(id)) {
                val fields = parts[1].split("|")
                val isRound = fields.getOrNull(0) != "pill"
                val targets = fields.getOrNull(1)
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.filter { it in 0..16 }
                    ?.toSet()
                    ?: emptySet()
                result.add(CustomGamepadButtonDef(id = id, isRound = isRound, targetButtons = targets))
            }
        }
    }
    return result
}

internal fun serializeCustomButtons(list: List<CustomGamepadButtonDef>): String {
    return list.joinToString(";") { def ->
        val shapeCode = if (def.isRound) "round" else "pill"
        val targetsCode = def.targetButtons.sorted().joinToString(",")
        "${def.id}:$shapeCode|$targetsCode"
    }
}
