package com.example.cloudplay

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import androidx.compose.runtime.Immutable
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class GamepadStatus(
    val hasPhysicalController: Boolean = false,
    val controllerName: String = "",
    val deviceCount: Int = 0,
    val controllerBrand: String = "Standard",
    val connectionTransport: String = "",
    val vendorProductHex: String = "",
    val supportsRumble: Boolean = false
) {
    fun summaryBadge(isEn: Boolean): String {
        if (!hasPhysicalController) {
            return if (isEn) "Virtual Touch Pad" else "Tay cầm cảm ứng"
        }
        val transportLabel = when (connectionTransport) {
            "USB" -> "USB"
            "BT" -> "Bluetooth"
            else -> if (isEn) "Connected" else "Đã kết nối"
        }
        return "$controllerBrand • $transportLabel"
    }
}

class GamepadDetector(context: Context) : InputManager.InputDeviceListener {

    private val appContext = context.applicationContext ?: context
    private val inputManager = appContext.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val _status = MutableStateFlow(
        GamepadStatus(controllerName = appContext.getString(R.string.gamepad_none))
    )
    val status: StateFlow<GamepadStatus> = _status.asStateFlow()

    init {
        try {
            inputManager?.registerInputDeviceListener(this, null)
            checkControllers()
        } catch (ignored: Exception) {
        }
    }

    fun checkControllers() {
        val deviceIds = InputDevice.getDeviceIds()
        var foundGamepad = false
        var name = appContext.getString(R.string.gamepad_none)
        var count = 0
        var brand = "Standard"
        var transport = ""
        var vidPid = ""
        var hasRumble = false

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad && !device.isVirtual) {
                if (!foundGamepad) {
                    foundGamepad = true
                    name = device.name ?: appContext.getString(R.string.gamepad_default_name)
                    val vid = device.vendorId
                    val pid = device.productId
                    if (vid > 0 || pid > 0) {
                        vidPid = String.format("%04X:%04X", vid, pid)
                    }
                    val lowerName = name.lowercase()
                    brand = when {
                        vid == 0x045E || "xbox" in lowerName || "x-box" in lowerName -> "Xbox"
                        vid == 0x054C || "dualsense" in lowerName || "dualshock" in lowerName || "playstation" in lowerName || "wireless controller" in lowerName -> "PlayStation"
                        vid == 0x057E || "nintendo" in lowerName || "pro controller" in lowerName || "joy-con" in lowerName -> "Nintendo"
                        vid == 0x1532 || "razer" in lowerName || "kishi" in lowerName -> "Razer"
                        vid == 0x358A || "backbone" in lowerName -> "Backbone"
                        "gamesir" in lowerName -> "GameSir"
                        "8bitdo" in lowerName -> "8BitDo"
                        else -> "Gamepad"
                    }
                    transport = when {
                        "bluetooth" in lowerName || "wireless" in lowerName || "bt" in lowerName -> "BT"
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && device.isExternal -> "USB"
                        else -> "BT"
                    }
                    hasRumble = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            device.vibratorManager.vibratorIds.isNotEmpty()
                        } else {
                            @Suppress("DEPRECATION")
                            device.vibrator.hasVibrator()
                        }
                    } catch (_: Exception) {
                        false
                    }
                }
                count++
            }
        }

        _status.value = GamepadStatus(
            hasPhysicalController = foundGamepad,
            controllerName = name,
            deviceCount = count,
            controllerBrand = brand,
            connectionTransport = transport,
            vendorProductHex = vidPid,
            supportsRumble = hasRumble
        )
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        checkControllers()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        checkControllers()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        checkControllers()
    }

    fun release() {
        try {
            inputManager?.unregisterInputDeviceListener(this)
        } catch (ignored: Exception) {
        }
    }
}

