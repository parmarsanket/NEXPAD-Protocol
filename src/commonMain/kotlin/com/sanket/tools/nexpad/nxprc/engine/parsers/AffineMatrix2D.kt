package com.sanket.tools.nexpad.nxprc.engine.parsers

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 2D Affine Transformation Matrix:
 * [ a  c  e ]
 * [ b  d  f ]
 * [ 0  0  1 ]
 *
 * Transforms (x, y) to (a*x + c*y + e, b*x + d*y + f).
 * Follows W3C SVG transform specifications.
 */
data class AffineMatrix2D(
    val a: Float = 1f, val b: Float = 0f,
    val c: Float = 0f, val d: Float = 1f,
    val e: Float = 0f, val f: Float = 0f
) {
    val isIdentity: Boolean
        get() = a == 1f && b == 0f && c == 0f && d == 1f && e == 0f && f == 0f

    fun transformX(x: Float, y: Float): Float = a * x + c * y + e
    fun transformY(x: Float, y: Float): Float = b * x + d * y + f

    fun multiply(o: AffineMatrix2D): AffineMatrix2D {
        return AffineMatrix2D(
            a = a * o.a + c * o.b,
            b = b * o.a + d * o.b,
            c = a * o.c + c * o.d,
            d = b * o.c + d * o.d,
            e = a * o.e + c * o.f + e,
            f = b * o.e + d * o.f + f
        )
    }

    companion object {
        val IDENTITY = AffineMatrix2D()

        fun translate(tx: Float, ty: Float = 0f) = AffineMatrix2D(1f, 0f, 0f, 1f, tx, ty)

        fun scale(sx: Float, sy: Float = sx) = AffineMatrix2D(sx, 0f, 0f, sy, 0f, 0f)

        fun rotate(deg: Float, cx: Float = 0f, cy: Float = 0f): AffineMatrix2D {
            val rad = deg * (PI / 180.0)
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()
            return if (cx == 0f && cy == 0f) {
                AffineMatrix2D(cosVal, sinVal, -sinVal, cosVal, 0f, 0f)
            } else {
                val t1 = translate(cx, cy)
                val r = AffineMatrix2D(cosVal, sinVal, -sinVal, cosVal, 0f, 0f)
                val t2 = translate(-cx, -cy)
                t1.multiply(r).multiply(t2)
            }
        }

        fun skewX(deg: Float): AffineMatrix2D {
            val rad = deg * (PI / 180.0)
            val tanVal = tan(rad).toFloat()
            return AffineMatrix2D(1f, 0f, tanVal, 1f, 0f, 0f)
        }

        fun skewY(deg: Float): AffineMatrix2D {
            val rad = deg * (PI / 180.0)
            val tanVal = tan(rad).toFloat()
            return AffineMatrix2D(1f, tanVal, 0f, 1f, 0f, 0f)
        }

        private val TRANSFORM_OP_REGEX = Regex("""(\w+)\s*\(([^)]*)\)""")

        fun parseTransform(transformStr: String?): AffineMatrix2D {
            if (transformStr.isNullOrBlank()) return IDENTITY
            var result = IDENTITY
            val matches = TRANSFORM_OP_REGEX.findAll(transformStr)
            for (match in matches) {
                val op = match.groupValues[1].lowercase()
                val rawArgs = match.groupValues[2]
                val args = rawArgs.split(Regex("[,\\s]+"))
                    .map { it.replace(Regex("[a-zA-Z%]+"), "").trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toFloatOrNull() }

                val matrix = when (op) {
                    "translate" -> {
                        val tx = args.getOrNull(0) ?: 0f
                        val ty = args.getOrNull(1) ?: 0f
                        translate(tx, ty)
                    }
                    "rotate" -> {
                        val deg = args.getOrNull(0) ?: 0f
                        val cx = args.getOrNull(1) ?: 0f
                        val cy = args.getOrNull(2) ?: 0f
                        rotate(deg, cx, cy)
                    }
                    "scale" -> {
                        val sx = args.getOrNull(0) ?: 1f
                        val sy = args.getOrNull(1) ?: sx
                        scale(sx, sy)
                    }
                    "matrix" -> {
                        if (args.size >= 6) {
                            AffineMatrix2D(args[0], args[1], args[2], args[3], args[4], args[5])
                        } else IDENTITY
                    }
                    "skewx" -> {
                        val deg = args.getOrNull(0) ?: 0f
                        skewX(deg)
                    }
                    "skewy" -> {
                        val deg = args.getOrNull(0) ?: 0f
                        skewY(deg)
                    }
                    else -> IDENTITY
                }
                result = result.multiply(matrix)
            }
            return result
        }
    }
}
