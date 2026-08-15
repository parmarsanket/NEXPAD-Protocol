package com.sanket.tools.nexpad.model

data class GamepadFeedback(
    val leftMotorSpeed: Int, // 0-255 (Heavy rumble)
    val rightMotorSpeed: Int, // 0-255 (Light rumble)
    val packetLossPct: Int = 0 // 0-255 scaled percentage of lost packets
)
