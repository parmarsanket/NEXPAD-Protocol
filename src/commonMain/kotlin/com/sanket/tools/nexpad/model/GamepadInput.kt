package com.sanket.tools.nexpad.model

data class GamepadInput(
    // Face Buttons
    @Volatile var btnA: Boolean = false,
    @Volatile var btnB: Boolean = false,
    @Volatile var btnX: Boolean = false,
    @Volatile var btnY: Boolean = false,

    // D-Pad
    @Volatile var dpadUp: Boolean = false,
    @Volatile var dpadDown: Boolean = false,
    @Volatile var dpadLeft: Boolean = false,
    @Volatile var dpadRight: Boolean = false,

    // Bumpers & Clicks
    @Volatile var btnL1: Boolean = false, // LB
    @Volatile var btnR1: Boolean = false, // RB
    @Volatile var btnL3: Boolean = false, // LS Click
    @Volatile var btnR3: Boolean = false, // RS Click

    // System Buttons
    @Volatile var btnStart: Boolean = false, // Menu
    @Volatile var btnSelect: Boolean = false, // View
    @Volatile var btnGuide: Boolean = false, // Xbox / Home
    @Volatile var btnShare: Boolean = false, // Share
    @Volatile var btnScreenshot: Boolean = false,

    // Advanced / Elite
    @Volatile var btnM1: Boolean = false,
    @Volatile var btnM2: Boolean = false,
    @Volatile var btnM3: Boolean = false,
    @Volatile var btnM4: Boolean = false,
    @Volatile var btnProfile: Boolean = false,
    @Volatile var btnTurbo: Boolean = false,

    // Triggers (0.0 to 1.0)
    @Volatile var triggerL2: Float = 0f,
    @Volatile var triggerR2: Float = 0f,

    // Left Joystick (-1.0 to 1.0)
    @Volatile var leftStickX: Float = 0f,
    @Volatile var leftStickY: Float = 0f,

    // Right Joystick (-1.0 to 1.0)
    @Volatile var rightStickX: Float = 0f,
    @Volatile var rightStickY: Float = 0f,

    // Gyroscope data (Angular Velocity)
    @Volatile var gyroX: Float = 0f,
    @Volatile var gyroY: Float = 0f,
    @Volatile var gyroZ: Float = 0f,

    // Accelerometer data (G-Force)
    @Volatile var accelX: Float = 0f,
    @Volatile var accelY: Float = 0f,
    @Volatile var accelZ: Float = 0f,

    // Gravity sensor data (filtered, Earth gravity only)
    @Volatile var gravityX: Float = 0f,
    @Volatile var gravityY: Float = 0f,
    @Volatile var gravityZ: Float = 0f,

    // Sensor flags (bitmask): bit 0 = accel fields carry gravity data
    @Volatile var sensorFlags: Byte = 0,
    
    // Networking
    @Volatile var sequenceNumber: Int = 0
)
