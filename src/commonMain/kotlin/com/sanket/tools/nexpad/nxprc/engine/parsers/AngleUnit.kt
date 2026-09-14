package com.sanket.tools.nexpad.nxprc.engine.parsers

import kotlin.math.PI

/**
 * Strongly-typed CSS Angle Units with conversion to standard degrees.
 */
enum class AngleUnit(val suffix: String, val degreesMultiplier: Float) {
    DEG("deg", 1.0f),
    TURN("turn", 360.0f),
    RAD("rad", (180.0 / PI).toFloat());

    companion object {
        fun parseToDegrees(value: String): Float? {
            val clean = value.trim().lowercase()
            for (unit in entries) {
                if (clean.endsWith(unit.suffix)) {
                    val num = clean.removeSuffix(unit.suffix).trim().toFloatOrNull() ?: return null
                    return num * unit.degreesMultiplier
                }
            }
            return clean.toFloatOrNull()
        }
    }
}
