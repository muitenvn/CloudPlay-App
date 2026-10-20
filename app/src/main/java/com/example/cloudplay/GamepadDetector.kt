package com.example.cloudplay

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GamepadStatus(
    val hasPhysicalController: Boolean = false,
    val controllerName: String = "Không có",
    val deviceCount: Int = 0
)

class GamepadDetector(context: Context) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val _status = MutableStateFlow(GamepadStatus())
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
        var name = "Không có"
        var count = 0

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad && !device.isVirtual) {
                foundGamepad = true
                name = device.name ?: "Tay cầm Gamepad"
                count++
            }
        }

        _status.value = GamepadStatus(
            hasPhysicalController = foundGamepad,
            controllerName = name,
            deviceCount = count
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
