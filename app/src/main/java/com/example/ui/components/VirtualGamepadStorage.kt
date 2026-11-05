package com.example.ui.components

import kotlin.math.abs
import kotlin.math.roundToInt

data class GamepadEditableNodeInfo(
    val id: String,
    val labelVi: String,
    val labelEn: String,
    val shortBadge: String
)

internal val ALL_GAMEPAD_NODES = listOf(
    GamepadEditableNodeInfo("btn_lt", "Cò trái LT", "Left Trigger LT", "LT"),
    GamepadEditableNodeInfo("btn_lb", "Nút vai LB", "Left Bumper LB", "LB"),
    GamepadEditableNodeInfo("btn_l3", "Nhấn cần trái L3", "Left Stick Click L3", "L3"),
    GamepadEditableNodeInfo("btn_r3", "Nhấn cần phải R3", "Right Stick Click R3", "R3"),
    GamepadEditableNodeInfo("btn_rb", "Nút vai RB", "Right Bumper RB", "RB"),
    GamepadEditableNodeInfo("btn_rt", "Cò phải RT", "Right Trigger RT", "RT"),
    GamepadEditableNodeInfo("btn_view", "Nút View (⧉)", "View Button (⧉)", "⧉ View"),
    GamepadEditableNodeInfo("btn_xbox", "Nút Xbox", "Xbox Guide Button", "Xbox"),
    GamepadEditableNodeInfo("btn_menu", "Nút Menu (☰)", "Menu Button (☰)", "☰ Menu"),
    GamepadEditableNodeInfo("left_stick", "Cần điều khiển trái (L)", "Left Analog Stick (L)", "Cần L"),
    GamepadEditableNodeInfo("right_stick", "Cần ngắm phải (R)", "Right Analog Stick (R)", "Cần R"),
    GamepadEditableNodeInfo("dpad", "Cụm phím D-Pad", "D-Pad Cross Cluster", "D-Pad"),
    GamepadEditableNodeInfo("dpad_up", "Phím Lên (▲)", "D-Pad Up (▲)", "▲"),
    GamepadEditableNodeInfo("dpad_down", "Phím Xuống (▼)", "D-Pad Down (▼)", "▼"),
    GamepadEditableNodeInfo("dpad_left", "Phím Trái (◀)", "D-Pad Left (◀)", "◀"),
    GamepadEditableNodeInfo("dpad_right", "Phím Phải (▶)", "D-Pad Right (▶)", "▶"),
    GamepadEditableNodeInfo("abxy", "Chùm nút ABXY", "ABXY Action Cluster", "ABXY"),
    GamepadEditableNodeInfo("btn_a", "Nút A (Xanh lá)", "A Button (Green)", "A"),
    GamepadEditableNodeInfo("btn_b", "Nút B (Đỏ)", "B Button (Red)", "B"),
    GamepadEditableNodeInfo("btn_x", "Nút X (Xanh dương)", "X Button (Blue)", "X"),
    GamepadEditableNodeInfo("btn_y", "Nút Y (Vàng)", "Y Button (Yellow)", "Y")
)

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
