package com.sanket.tools.nexpad.protocol

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Universal Binary Protocol for NEXPAD.
 *
 * Byte layout for Input Packet (44 bytes):
 * | Offset | Size | Field           | Type    | Range / Notes                                |
 * |--------|------|-----------------|---------|----------------------------------------------|
 * | 0      | 1    | Version         | Byte    | Always 1                                     |
 * | 1      | 4    | Buttons         | Int32   | Bitmask (see below)                          |
 * | 5      | 2    | Left Stick X    | Int16   | -32768 to 32767                              |
 * | 7      | 2    | Left Stick Y    | Int16   | -32768 to 32767                              |
 * | 9      | 2    | Right Stick X   | Int16   | -32768 to 32767                              |
 * | 11     | 2    | Right Stick Y   | Int16   | -32768 to 32767                              |
 * | 13     | 1    | Trigger L2      | UInt8   | 0 to 255                                     |
 * | 14     | 1    | Trigger R2      | UInt8   | 0 to 255                                     |
 * | 15     | 24   | IMU Sensors     | Float*6 | gyroX, gyroY, gyroZ, accelX, accelY, accelZ  |
 * | 39     | 1    | Sensor Flags    | Byte    | Bitmask: bit0 = accel carries gravity data   |
 * | 40     | 4    | Sequence Number | Int32   | Packet ID for ordering & RTT                 |
 *
 * Byte layout for Feedback Packet (10 bytes):
 * | Offset | Size | Field           | Type    | Range / Notes                                |
 * |--------|------|-----------------|---------|----------------------------------------------|
 * | 0      | 1    | Version         | Byte    | Always 1                                     |
 * | 1      | 1    | Left Motor      | UInt8   | 0 to 255                                     |
 * | 2      | 1    | Right Motor     | UInt8   | 0 to 255                                     |
 * | 3      | 2    | Lightbar RG     | UInt8*2 | R, G (0-255) unused                          |
 * | 5      | 1    | Packet Loss     | UInt8   | scaled 0-255 for dropped packets %           |
 * | 6      | 4    | Sequence Echo   | Int32   | Echo of last received sequence number        |
 */
object NexpadProtocol {
    const val PROTOCOL_VERSION: Byte = 1

    // Packet Types
    const val PACKET_TYPE_INPUT: Byte = 0x01
    const val PACKET_TYPE_DISCOVER: Byte = 0x02
    const val PACKET_TYPE_SERVER_INFO: Byte = 0x03
    const val PACKET_TYPE_CONNECT: Byte = 0x04
    const val PACKET_TYPE_CONNECTED: Byte = 0x05
    const val PACKET_TYPE_DISCONNECT: Byte = 0x06
    const val PACKET_TYPE_FILE_SYNC_START: Byte = 0xAF.toByte()

    const val SYNC_TCP_PORT = 9995
    const val FILE_SYNC_ACK: Byte = 0x06
    const val FILE_SYNC_NACK: Byte = 0x15

    const val INPUT_PACKET_SIZE = 44
    const val FEEDBACK_PACKET_SIZE = 10

    // Button bitmasks
    const val MASK_BTN_A = 1 shl 0
    const val MASK_BTN_B = 1 shl 1
    const val MASK_BTN_X = 1 shl 2
    const val MASK_BTN_Y = 1 shl 3
    const val MASK_DPAD_UP = 1 shl 4
    const val MASK_DPAD_DOWN = 1 shl 5
    const val MASK_DPAD_LEFT = 1 shl 6
    const val MASK_DPAD_RIGHT = 1 shl 7
    const val MASK_BTN_L1 = 1 shl 8
    const val MASK_BTN_R1 = 1 shl 9
    const val MASK_BTN_L3 = 1 shl 10
    const val MASK_BTN_R3 = 1 shl 11
    const val MASK_BTN_START = 1 shl 12
    const val MASK_BTN_SELECT = 1 shl 13
    const val MASK_BTN_GUIDE = 1 shl 14
    const val MASK_BTN_SHARE = 1 shl 15

    const val MASK_BTN_SCREENSHOT = 1 shl 16
    const val MASK_BTN_M1 = 1 shl 17
    const val MASK_BTN_M2 = 1 shl 18
    const val MASK_BTN_M3 = 1 shl 19
    const val MASK_BTN_M4 = 1 shl 20
    const val MASK_BTN_PROFILE = 1 shl 21
    const val MASK_BTN_TURBO = 1 shl 22

    // Sensor flags
    const val SENSOR_FLAG_GRAVITY: Byte = 0x01  // bit 0: accel fields carry gravity sensor data

    private var currentSequenceNumber = 0
    fun nextSequenceNumber(): Int {
        val next = currentSequenceNumber + 1
        currentSequenceNumber = if (next == Int.MAX_VALUE) 0 else next
        return currentSequenceNumber
    }

    /**
     * Converts a float value in the range [-1f, 1f] to a short [-32768, 32767].
     */
    fun floatToStick(value: Float): Short {
        val clamped = value.coerceIn(-1f, 1f)
        return if (clamped >= 0) {
            (clamped * 32767f).toInt().toShort()
        } else {
            (clamped * 32768f).toInt().toShort()
        }
    }

    /**
     * Converts a short value in the range [-32768, 32767] to a float [-1f, 1f].
     */
    fun stickToFloat(value: Short): Float {
        return if (value >= 0) {
            value.toFloat() / 32767f
        } else {
            value.toFloat() / 32768f
        }
    }

    /**
     * Converts a float value in the range [0f, 1f] to a byte [0, 255].
     */
    fun floatToTrigger(value: Float): Byte {
        val clamped = value.coerceIn(0f, 1f)
        return (clamped * 255f).toInt().toByte()
    }

    /**
     * Converts an unsigned byte value [0, 255] to a float [0f, 1f].
     */
    fun triggerToFloat(value: Byte): Float {
        val unsignedVal = value.toInt() and 0xFF
        return unsignedVal.toFloat() / 255f
    }

    private fun writeShort(out: ByteArray, p: Int, value: Short) {
        val v = value.toInt()
        out[p] = (v ushr 8).toByte()
        out[p + 1] = v.toByte()
    }

    private fun writeInt(out: ByteArray, p: Int, value: Int) {
        out[p] = (value ushr 24).toByte()
        out[p + 1] = (value ushr 16).toByte()
        out[p + 2] = (value ushr 8).toByte()
        out[p + 3] = value.toByte()
    }

    private fun writeFloat(out: ByteArray, p: Int, value: Float): Int {
        val bits = value.toRawBits()
        out[p] = (bits ushr 24).toByte()
        out[p + 1] = (bits ushr 16).toByte()
        out[p + 2] = (bits ushr 8).toByte()
        out[p + 3] = bits.toByte()
        return p + 4
    }

    private fun readShort(data: ByteArray, p: Int): Short {
        return (((data[p].toInt() and 0xFF) shl 8) or (data[p + 1].toInt() and 0xFF)).toShort()
    }

    private fun readInt(data: ByteArray, p: Int): Int {
        return ((data[p].toInt() and 0xFF) shl 24) or
               ((data[p + 1].toInt() and 0xFF) shl 16) or
               ((data[p + 2].toInt() and 0xFF) shl 8) or
               (data[p + 3].toInt() and 0xFF)
    }

    private fun readFloat(data: ByteArray, p: Int): Float {
        return Float.fromBits(readInt(data, p))
    }

    /**
     * Encodes a GamepadInput object into a 44-byte array using direct bit manipulation.
     * Zero allocations.
     */
    fun encodeInput(input: GamepadInput, byteArray: ByteArray, offset: Int = 0) {
        require(byteArray.size - offset >= INPUT_PACKET_SIZE) { "expected $INPUT_PACKET_SIZE bytes, got ${byteArray.size - offset}" }
        var p = offset
        byteArray[p++] = PROTOCOL_VERSION

        var buttons = 0
        if (input.btnA) buttons = buttons or MASK_BTN_A
        if (input.btnB) buttons = buttons or MASK_BTN_B
        if (input.btnX) buttons = buttons or MASK_BTN_X
        if (input.btnY) buttons = buttons or MASK_BTN_Y
        if (input.dpadUp) buttons = buttons or MASK_DPAD_UP
        if (input.dpadDown) buttons = buttons or MASK_DPAD_DOWN
        if (input.dpadLeft) buttons = buttons or MASK_DPAD_LEFT
        if (input.dpadRight) buttons = buttons or MASK_DPAD_RIGHT
        if (input.btnL1) buttons = buttons or MASK_BTN_L1
        if (input.btnR1) buttons = buttons or MASK_BTN_R1
        if (input.btnL3) buttons = buttons or MASK_BTN_L3
        if (input.btnR3) buttons = buttons or MASK_BTN_R3
        if (input.btnStart) buttons = buttons or MASK_BTN_START
        if (input.btnSelect) buttons = buttons or MASK_BTN_SELECT
        if (input.btnGuide) buttons = buttons or MASK_BTN_GUIDE
        if (input.btnShare) buttons = buttons or MASK_BTN_SHARE
        if (input.btnScreenshot) buttons = buttons or MASK_BTN_SCREENSHOT
        if (input.btnM1) buttons = buttons or MASK_BTN_M1
        if (input.btnM2) buttons = buttons or MASK_BTN_M2
        if (input.btnM3) buttons = buttons or MASK_BTN_M3
        if (input.btnM4) buttons = buttons or MASK_BTN_M4
        if (input.btnProfile) buttons = buttons or MASK_BTN_PROFILE
        if (input.btnTurbo) buttons = buttons or MASK_BTN_TURBO

        writeInt(byteArray, p, buttons); p += 4
        writeShort(byteArray, p, floatToStick(input.leftStickX)); p += 2
        writeShort(byteArray, p, floatToStick(input.leftStickY)); p += 2
        writeShort(byteArray, p, floatToStick(input.rightStickX)); p += 2
        writeShort(byteArray, p, floatToStick(input.rightStickY)); p += 2
        byteArray[p++] = floatToTrigger(input.triggerL2)
        byteArray[p++] = floatToTrigger(input.triggerR2)
        p = writeFloat(byteArray, p, input.gyroX)
        p = writeFloat(byteArray, p, input.gyroY)
        p = writeFloat(byteArray, p, input.gyroZ)
        p = writeFloat(byteArray, p, input.accelX)
        p = writeFloat(byteArray, p, input.accelY)
        p = writeFloat(byteArray, p, input.accelZ)
        byteArray[p++] = input.sensorFlags
        writeInt(byteArray, p, input.sequenceNumber)
    }

    /**
     * Allocating overload for convenience.
     */
    fun encodeInput(input: GamepadInput): ByteArray {
        val out = ByteArray(INPUT_PACKET_SIZE)
        encodeInput(input, out, 0)
        return out
    }

    /**
     * Decodes a binary packet into a GamepadInput object.
     * Reads directly from the given byte array offset with zero ByteBuffer allocations.
     * Returns null if the packet size is incorrect or the protocol version does not match.
     */
    @kotlin.jvm.JvmOverloads
    fun decodeInput(data: ByteArray, offset: Int = 0): GamepadInput? {
        if (data.size - offset < INPUT_PACKET_SIZE) return null
        var p = offset
        val version = data[p++]
        if (version != PROTOCOL_VERSION) return null

        val buttons = readInt(data, p); p += 4

        val leftStickX = readShort(data, p); p += 2
        val leftStickY = readShort(data, p); p += 2
        val rightStickX = readShort(data, p); p += 2
        val rightStickY = readShort(data, p); p += 2

        val triggerL2 = data[p++]
        val triggerR2 = data[p++]

        val gyroX = readFloat(data, p); p += 4
        val gyroY = readFloat(data, p); p += 4
        val gyroZ = readFloat(data, p); p += 4

        val accelX = readFloat(data, p); p += 4
        val accelY = readFloat(data, p); p += 4
        val accelZ = readFloat(data, p); p += 4

        val sensorFlags = data[p++]

        val sequenceNumber = readInt(data, p)

        // If the gravity flag is set, the accel fields carry gravity sensor data
        val hasGravity = (sensorFlags.toInt() and SENSOR_FLAG_GRAVITY.toInt()) != 0

        return GamepadInput(
            btnA = (buttons and MASK_BTN_A) != 0,
            btnB = (buttons and MASK_BTN_B) != 0,
            btnX = (buttons and MASK_BTN_X) != 0,
            btnY = (buttons and MASK_BTN_Y) != 0,
            dpadUp = (buttons and MASK_DPAD_UP) != 0,
            dpadDown = (buttons and MASK_DPAD_DOWN) != 0,
            dpadLeft = (buttons and MASK_DPAD_LEFT) != 0,
            dpadRight = (buttons and MASK_DPAD_RIGHT) != 0,
            btnL1 = (buttons and MASK_BTN_L1) != 0,
            btnR1 = (buttons and MASK_BTN_R1) != 0,
            btnL3 = (buttons and MASK_BTN_L3) != 0,
            btnR3 = (buttons and MASK_BTN_R3) != 0,
            btnStart = (buttons and MASK_BTN_START) != 0,
            btnSelect = (buttons and MASK_BTN_SELECT) != 0,
            btnGuide = (buttons and MASK_BTN_GUIDE) != 0,
            btnShare = (buttons and MASK_BTN_SHARE) != 0,
            btnScreenshot = (buttons and MASK_BTN_SCREENSHOT) != 0,
            btnM1 = (buttons and MASK_BTN_M1) != 0,
            btnM2 = (buttons and MASK_BTN_M2) != 0,
            btnM3 = (buttons and MASK_BTN_M3) != 0,
            btnM4 = (buttons and MASK_BTN_M4) != 0,
            btnProfile = (buttons and MASK_BTN_PROFILE) != 0,
            btnTurbo = (buttons and MASK_BTN_TURBO) != 0,
            triggerL2 = triggerToFloat(triggerL2),
            triggerR2 = triggerToFloat(triggerR2),
            leftStickX = stickToFloat(leftStickX),
            leftStickY = stickToFloat(leftStickY),
            rightStickX = stickToFloat(rightStickX),
            rightStickY = stickToFloat(rightStickY),
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,
            accelX = if (hasGravity) 0f else accelX,
            accelY = if (hasGravity) 0f else accelY,
            accelZ = if (hasGravity) 0f else accelZ,
            gravityX = if (hasGravity) accelX else 0f,
            gravityY = if (hasGravity) accelY else 0f,
            gravityZ = if (hasGravity) accelZ else 0f,
            sensorFlags = sensorFlags,
            sequenceNumber = sequenceNumber
        )
    }

    /**
     * In-place zero-allocation feedback encoder. Writes directly into the provided output array.
     */
    @kotlin.jvm.JvmOverloads
    fun encodeFeedback(feedback: GamepadFeedback, echoSequenceNumber: Int, packetLossByte: Byte = 0, out: ByteArray, offset: Int = 0) {
        require(out.size - offset >= FEEDBACK_PACKET_SIZE) { "expected at least $FEEDBACK_PACKET_SIZE bytes, got ${out.size - offset}" }
        var p = offset
        out[p++] = PROTOCOL_VERSION
        out[p++] = feedback.leftMotorSpeed.coerceIn(0, 255).toByte()
        out[p++] = feedback.rightMotorSpeed.coerceIn(0, 255).toByte()
        out[p++] = 0 // R
        out[p++] = 0 // G
        out[p++] = packetLossByte
        writeInt(out, p, echoSequenceNumber)
    }

    /**
     * Allocating overload for convenience. Prefer using the in-place overload on hot streaming paths.
     */
    @kotlin.jvm.JvmOverloads
    fun encodeFeedback(feedback: GamepadFeedback, echoSequenceNumber: Int, packetLossByte: Byte = 0): ByteArray {
        val out = ByteArray(FEEDBACK_PACKET_SIZE)
        encodeFeedback(feedback, echoSequenceNumber, packetLossByte, out, 0)
        return out
    }

    /**
     * Decodes a 10-byte feedback array into a Pair<GamepadFeedback, Int>.
     * Reads directly from data at the specified offset with zero ByteBuffer allocations.
     */
    @kotlin.jvm.JvmOverloads
    fun decodeFeedback(data: ByteArray, offset: Int = 0): Pair<GamepadFeedback, Int>? {
        if (data.size - offset < FEEDBACK_PACKET_SIZE) return null
        var p = offset
        val version = data[p++]
        if (version != PROTOCOL_VERSION) return null

        val leftMotorSpeed = data[p++].toInt() and 0xFF
        val rightMotorSpeed = data[p++].toInt() and 0xFF
        p++ // R
        p++ // G
        val packetLossPct = data[p++].toInt() and 0xFF
        val echoSequenceNumber = readInt(data, p)

        return Pair(
            GamepadFeedback(
                leftMotorSpeed = leftMotorSpeed,
                rightMotorSpeed = rightMotorSpeed,
                packetLossPct = packetLossPct
            ),
            echoSequenceNumber
        )
    }

    private val CRC32_TABLE = IntArray(256) { i ->
        var c = i
        for (j in 0 until 8) {
            c = if ((c and 1) != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1
        }
        c
    }

    /**
     * Standard IEEE 802.3 CRC32 checksum for payload integrity verification.
     */
    fun computeCrc32(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = -1
        for (i in offset until (offset + length)) {
            val byte = data[i].toInt() and 0xFF
            crc = CRC32_TABLE[(crc xor byte) and 0xFF] xor (crc ushr 8)
        }
        return crc.inv()
    }

    data class FileSyncHeader(
        val componentId: String,
        val fileSize: Int,
        val checksum: Int,
        val headerSize: Int
    )

    /**
     * Encodes a file sync header.
     * Format:
     * [0xAF (1B)] [fileSize (4B)] [idLength (1B)] [idBytes (NB)] [checksum (4B)]
     */
    fun encodeFileSyncHeader(componentId: String, payloadSize: Int, checksum: Int): ByteArray {
        val idBytes = componentId.encodeToByteArray()
        val idLen = idBytes.size.coerceAtMost(255)
        val headerSize = 1 + 4 + 1 + idLen + 4
        val out = ByteArray(headerSize)
        var p = 0
        out[p++] = PACKET_TYPE_FILE_SYNC_START
        writeInt(out, p, payloadSize); p += 4
        out[p++] = idLen.toByte()
        idBytes.copyInto(out, p, 0, idLen); p += idLen
        writeInt(out, p, checksum)
        return out
    }

    /**
     * Decodes a file sync header from the given byte array.
     * Returns null if incomplete or invalid magic.
     */
    fun decodeFileSyncHeader(data: ByteArray, offset: Int = 0): FileSyncHeader? {
        if (data.size - offset < 10) return null
        var p = offset
        val magic = data[p++]
        if (magic != PACKET_TYPE_FILE_SYNC_START) return null

        val fileSize = readInt(data, p); p += 4
        if (fileSize < 0) return null

        val idLen = data[p++].toInt() and 0xFF
        if (data.size - p < idLen + 4) return null

        val componentId = data.decodeToString(p, p + idLen); p += idLen
        val checksum = readInt(data, p); p += 4

        return FileSyncHeader(
            componentId = componentId,
            fileSize = fileSize,
            checksum = checksum,
            headerSize = p - offset
        )
    }
}
