package com.example.cloudplay

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.runtime.Immutable

@Immutable
data class GamepadStatus(
    val hasPhysicalController: Boolean = false,
    val controllerName: String = "",
    val deviceCount: Int = 0
)

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

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad && !device.isVirtual) {
                foundGamepad = true
                name = device.name ?: appContext.getString(R.string.gamepad_default_name)
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

