package com.example.cloudplay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.InputDevice
import android.view.Surface
import android.view.WindowManager
import com.example.data.GyroControlMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Bộ điều khiển Con quay hồi chuyển (Gyroscope) cho Ravno:
 * - Mặc định TẮT hoàn toàn để không can thiệp vào cảm biến gốc của game Xbox nếu người dùng không bật.
 * - Khi người dùng BẬT:
 *   + Tự động ưu tiên cảm biến con quay hồi chuyển tích hợp sẵn bên trong tay cầm thật (Android 12+ InputDevice.sensorManager,
 *     ví dụ DualSense / DualShock 4 / Switch Pro / GameSir) nếu có, hoặc dùng cảm biến của điện thoại.
 *   + Hỗ trợ 3 chế độ:
 *     1. FPS_VR_360 (Bắn súng / VR - Mọi góc -> Cần phải RS): nghiêng/xoay mọi góc ngang & dọc.
 *     2. RACING_STEER (Đua xe - Chỉ qua lại trái/phải -> Cần trái LS): chỉ nhận nghiêng vô-lăng trái/phải, khóa trục dọc = 0.
 *     3. LEFT_STICK_360 (Di chuyển / Lái máy bay 360° -> Cần trái LS): nghiêng mọi góc ngang & dọc.
 *   + Hỗ trợ chỉnh độ nhạy Ngang / Dọc (10% - 300%), Vùng chết chống rung tay (0% - 20%), Đảo chiều Ngang / Dọc,
 *     và Đặt lại tâm chuẩn (Recenter 0°) theo tư thế cầm máy hiện tại.
 */
class GyroscopeController(
    context: Context,
    private val onOutputChanged: (targetRightStick: Boolean, outX: Float, outY: Float) -> Unit
) : SensorEventListener {

    companion object {
        @Volatile
        private var recenterRequestTick: Long = 0L

        fun requestRecenterNow() {
            recenterRequestTick = System.currentTimeMillis()
        }

        fun hasAvailableGyroSensor(context: Context): Boolean {
            if (findPhysicalControllerSensorManager()?.let { sm ->
                    sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
                        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                        sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
                        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                } == true
            ) {
                return true
            }
            val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                ?: return false
            return sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
                sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }

        private fun findPhysicalControllerSensorManager(): SensorManager? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            return try {
                for (id in InputDevice.getDeviceIds()) {
                    val dev = InputDevice.getDevice(id) ?: continue
                    val isGamepad = (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                        (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                    if (isGamepad && !dev.isVirtual) {
                        val sm = dev.sensorManager
                        val hasMotion = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
                            sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                            sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
                            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                        if (hasMotion) return sm
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    private val appContext = context.applicationContext ?: context
    private var activeSensorManager: SensorManager? = null
    private var isUsingControllerSensor: Boolean = false
    private var isListening: Boolean = false

    var enabled: Boolean = false
        private set
    var mode: GyroControlMode = GyroControlMode.FPS_VR_360
    var sensitivityXPercent: Int = 100
    var sensitivityYPercent: Int = 100
    var deadzonePercent: Int = 2
    var invertX: Boolean = false
    var invertY: Boolean = false

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasReferencePose = false
    private var refYawRad = 0f
    private var refPitchRad = 0f
    private var refRollRad = 0f
    private var lastHandledRecenterTick = 0L

    // Dùng khi thiết bị chỉ có GYROSCOPE + ACCELEROMETER rời
    private var integratedX = 0f
    private var integratedY = 0f
    private var lastGyroTimestampNs = 0L

    private var lastDispatchedX = 0f
    private var lastDispatchedY = 0f
    private var lastDispatchedRightStick = true

    fun updateConfiguration(
        enabled: Boolean,
        mode: GyroControlMode,
        sensitivityXPercent: Int,
        sensitivityYPercent: Int,
        deadzonePercent: Int,
        invertX: Boolean,
        invertY: Boolean
    ) {
        val prevMode = this.mode
        this.mode = mode
        this.sensitivityXPercent = sensitivityXPercent.coerceIn(10, 300)
        this.sensitivityYPercent = sensitivityYPercent.coerceIn(10, 300)
        this.deadzonePercent = deadzonePercent.coerceIn(0, 20)
        this.invertX = invertX
        this.invertY = invertY

        if (prevMode != mode) {
            recenter()
        }

        if (enabled != this.enabled) {
            this.enabled = enabled
            if (enabled) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    fun recenter() {
        hasReferencePose = false
        integratedX = 0f
        integratedY = 0f
        lastGyroTimestampNs = 0L
        dispatchOutput(mode == GyroControlMode.FPS_VR_360, 0f, 0f, force = true)
    }

    private fun startListening() {
        if (isListening) return
        val controllerSm = findPhysicalControllerSensorManager()
        val systemSm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val chosenSm = controllerSm ?: systemSm ?: return
        isUsingControllerSensor = (controllerSm != null)
        activeSensorManager = chosenSm

        hasReferencePose = false
        integratedX = 0f
        integratedY = 0f
        lastGyroTimestampNs = 0L

        val rotSensor = chosenSm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: chosenSm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        var registered = false
        if (rotSensor != null) {
            registered = chosenSm.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (!registered) {
            val gyro = chosenSm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            val accel = chosenSm.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: chosenSm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (gyro != null) {
                registered = chosenSm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            }
            if (accel != null) {
                registered = chosenSm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME) || registered
            }
        }
        isListening = registered
    }

    fun stopListening() {
        if (isListening) {
            try {
                activeSensorManager?.unregisterListener(this)
            } catch (_: Exception) {
            }
            isListening = false
        }
        activeSensorManager = null
        hasReferencePose = false
        integratedX = 0f
        integratedY = 0f
        lastGyroTimestampNs = 0L
        dispatchOutput(mode == GyroControlMode.FPS_VR_360, 0f, 0f, force = true)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (!enabled || event == null) return

        val reqTick = recenterRequestTick
        if (reqTick > lastHandledRecenterTick) {
            lastHandledRecenterTick = reqTick
            recenter()
        }

        val targetRightStick = (mode == GyroControlMode.FPS_VR_360)

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                remapForScreenRotation(rotationMatrix, remappedMatrix)
                SensorManager.getOrientation(remappedMatrix, orientationAngles)

                val yaw = orientationAngles[0]   // Xoay ngang trái/phải
                val pitch = orientationAngles[1] // Ngẩng lên / cúi xuống
                val roll = orientationAngles[2]  // Nghiêng vô-lăng trái/phải

                if (!hasReferencePose) {
                    refYawRad = yaw
                    refPitchRad = pitch
                    refRollRad = roll
                    hasReferencePose = true
                }

                val dYaw = normalizeAngleRad(yaw - refYawRad)
                val dPitch = normalizeAngleRad(pitch - refPitchRad)
                val dRoll = normalizeAngleRad(roll - refRollRad)

                // Quy đổi góc nghiêng (khoảng ±32 độ = ±0.56 rad tương ứng 100% cần ở độ nhạy 100%)
                val fullScaleRad = 0.56f
                val rawHorizontal = when (mode) {
                    GyroControlMode.RACING_STEER -> (dRoll / fullScaleRad)
                    GyroControlMode.FPS_VR_360,
                    GyroControlMode.LEFT_STICK_360 -> ((dYaw * 0.85f + dRoll * 0.65f) / fullScaleRad)
                }
                val rawVertical = when (mode) {
                    GyroControlMode.RACING_STEER -> 0f
                    GyroControlMode.FPS_VR_360,
                    GyroControlMode.LEFT_STICK_360 -> (dPitch / fullScaleRad)
                }

                val processedX = processAxis(
                    raw = rawHorizontal,
                    sensPercent = sensitivityXPercent,
                    deadzonePct = deadzonePercent,
                    invert = invertX
                )
                val processedY = if (mode == GyroControlMode.RACING_STEER) {
                    0f
                } else {
                    processAxis(
                        raw = rawVertical,
                        sensPercent = sensitivityYPercent,
                        deadzonePct = deadzonePercent,
                        invert = invertY
                    )
                }

                dispatchOutput(targetRightStick, processedX, processedY, force = false)
            }

            Sensor.TYPE_GYROSCOPE -> {
                val nowNs = event.timestamp
                if (lastGyroTimestampNs != 0L) {
                    val dt = ((nowNs - lastGyroTimestampNs) * 1e-9f).coerceIn(0f, 0.1f)
                    // Góc vận tốc (rad/s) tích phân có hồi tâm nhẹ cho FPS/VR hoặc góc nghiêng
                    val wx = event.values[0]
                    val wy = event.values[1]
                    val wz = event.values[2]
                    integratedX = (integratedX + (-wz * 0.7f + wy * 0.5f) * dt * 1.8f).coerceIn(-1.5f, 1.5f)
                    integratedY = (integratedY + (-wx) * dt * 1.8f).coerceIn(-1.5f, 1.5f)

                    val outX = processAxis(integratedX, sensitivityXPercent, deadzonePercent, invertX)
                    val outY = if (mode == GyroControlMode.RACING_STEER) {
                        0f
                    } else {
                        processAxis(integratedY, sensitivityYPercent, deadzonePercent, invertY)
                    }
                    dispatchOutput(targetRightStick, outX, outY, force = false)
                }
                lastGyroTimestampNs = nowNs
            }

            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ACCELEROMETER -> {
                if (mode == GyroControlMode.RACING_STEER) {
                    // Dùng trọng lực để đo góc nghiêng vô-lăng trái/phải chính xác nếu máy không có RotationVector
                    val gx = event.values[0]
                    val gy = event.values[1]
                    val gz = event.values[2]
                    val norm = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1f)
                    val steerAngle = atan2(gy / norm, gx / norm)
                    if (!hasReferencePose) {
                        refRollRad = steerAngle
                        hasReferencePose = true
                    }
                    val dRoll = normalizeAngleRad(steerAngle - refRollRad)
                    val rawSteer = dRoll / 0.56f
                    val outX = processAxis(rawSteer, sensitivityXPercent, deadzonePercent, invertX)
                    dispatchOutput(false, outX, 0f, force = false)
                }
            }
        }
    }

    private fun remapForScreenRotation(inR: FloatArray, outR: FloatArray) {
        if (isUsingControllerSensor) {
            System.arraycopy(inR, 0, outR, 0, 9)
            return
        }
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val rotation = wm?.defaultDisplay?.rotation ?: Surface.ROTATION_90
        when (rotation) {
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                inR,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                outR
            )
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                inR,
                SensorManager.AXIS_MINUS_Y,
                SensorManager.AXIS_X,
                outR
            )
            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                inR,
                SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_MINUS_Y,
                outR
            )
            else -> System.arraycopy(inR, 0, outR, 0, 9)
        }
    }

    private fun normalizeAngleRad(angle: Float): Float {
        var a = angle
        val pi = Math.PI.toFloat()
        while (a > pi) a -= 2f * pi
        while (a < -pi) a += 2f * pi
        return a
    }

    private fun processAxis(
        raw: Float,
        sensPercent: Int,
        deadzonePct: Int,
        invert: Boolean
    ): Float {
        val dz = (deadzonePct.coerceIn(0, 20) / 100f)
        val absRaw = abs(raw)
        if (absRaw <= dz) return 0f
        val rescaled = ((absRaw - dz) / (1f - dz).coerceAtLeast(0.1f)) * sign(raw)
        val sensScale = (sensPercent.coerceIn(10, 300) / 100f)
        val scaled = (rescaled * sensScale).coerceIn(-1f, 1f)
        return if (invert) -scaled else scaled
    }

    private fun dispatchOutput(
        targetRightStick: Boolean,
        outX: Float,
        outY: Float,
        force: Boolean
    ) {
        val dx = abs(outX - lastDispatchedX)
        val dy = abs(outY - lastDispatchedY)
        if (force || targetRightStick != lastDispatchedRightStick || dx >= 0.008f || dy >= 0.008f || (outX == 0f && lastDispatchedX != 0f) || (outY == 0f && lastDispatchedY != 0f)) {
            lastDispatchedRightStick = targetRightStick
            lastDispatchedX = outX
            lastDispatchedY = outY
            onOutputChanged(targetRightStick, outX, outY)
        }
    }
}
