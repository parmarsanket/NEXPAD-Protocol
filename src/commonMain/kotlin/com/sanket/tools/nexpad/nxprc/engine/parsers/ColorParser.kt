package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Dedicated parser for CSS colors:
 * Supports:
 * - #rgb, #rgba, #rrggbb, #rrggbbaa
 * - rgb(r, g, b), rgba(r, g, b, a), rgb(r g b), rgb(r g b / a), rgba(r g b / a)
 * - Channel percentages e.g. rgb(100%, 0%, 50% / 0.5)
 * - hsl(h, s%, l%), hsla(h, s%, l%, a), hsl(h s% l% / a), hsla(h s% l% / a)
 * - Angle units in hue (deg, rad, turn)
 * - 148 standard W3C named colors, transparent, currentColor
 */
object ColorParser {

    val NAMED_COLORS = mapOf(
        "transparent" to 0x00000000L,
        "currentcolor" to 0xFFFFFFFFL,
        "aliceblue" to 0xFFF0F8FFL,
        "antiquewhite" to 0xFFFAEBD7L,
        "aqua" to 0xFF00FFFFL,
        "aquamarine" to 0xFF7FFFD4L,
        "azure" to 0xFFF0FFFFL,
        "beige" to 0xFFF5F5DCL,
        "bisque" to 0xFFFFE4C4L,
        "black" to 0xFF000000L,
        "blanchedalmond" to 0xFFFFEBCDL,
        "blue" to 0xFF0000FFL,
        "blueviolet" to 0xFF8A2BE2L,
        "brown" to 0xFFA52A2AL,
        "burlywood" to 0xFFDEB887L,
        "cadetblue" to 0xFF5F9EA0L,
        "chartreuse" to 0xFF7FFF00L,
        "chocolate" to 0xFFD2691EL,
        "coral" to 0xFFFF7F50L,
        "cornflowerblue" to 0xFF6495EDL,
        "cornsilk" to 0xFFFFF8DCL,
        "crimson" to 0xFFDC143CL,
        "cyan" to 0xFF00FFFFL,
        "darkblue" to 0xFF00008BL,
        "darkcyan" to 0xFF008B8BL,
        "darkgoldenrod" to 0xFFB8860BL,
        "darkgray" to 0xFFA9A9A9L,
        "darkgreen" to 0xFF006400L,
        "darkgrey" to 0xFFA9A9A9L,
        "darkkhaki" to 0xFFBDB76BL,
        "darkmagenta" to 0xFF8B008BL,
        "darkolivegreen" to 0xFF556B2FL,
        "darkorange" to 0xFFFF8C00L,
        "darkorchid" to 0xFF9932CCL,
        "darkred" to 0xFF8B0000L,
        "darksalmon" to 0xFFE9967AL,
        "darkseagreen" to 0xFF8FBC8FL,
        "darkslateblue" to 0xFF483D8BL,
        "darkslategray" to 0xFF2F4F4FL,
        "darkslategrey" to 0xFF2F4F4FL,
        "darkturquoise" to 0xFF00CED1L,
        "darkviolet" to 0xFF9400D3L,
        "deeppink" to 0xFFFF1493L,
        "deepskyblue" to 0xFF00BFFFL,
        "dimgray" to 0xFF696969L,
        "dimgrey" to 0xFF696969L,
        "dodgerblue" to 0xFF1E90FFL,
        "firebrick" to 0xFFB22222L,
        "floralwhite" to 0xFFFFFAF0L,
        "forestgreen" to 0xFF228B22L,
        "fuchsia" to 0xFFFF00FFL,
        "gainsboro" to 0xFFDCDCDCL,
        "ghostwhite" to 0xFFF8F8FFL,
        "gold" to 0xFFFFD700L,
        "goldenrod" to 0xFFDAA520L,
        "gray" to 0xFF808080L,
        "green" to 0xFF008000L,
        "greenyellow" to 0xFFADFF2FL,
        "grey" to 0xFF808080L,
        "honeydew" to 0xFFF0FFF0L,
        "hotpink" to 0xFFFF69B4L,
        "indianred" to 0xFFCD5C5CL,
        "indigo" to 0xFF4B0082L,
        "ivory" to 0xFFFFFFF0L,
        "khaki" to 0xFFF0E68CL,
        "lavender" to 0xFFE6E6FAL,
        "lavenderblush" to 0xFFFFF0F5L,
        "lawngreen" to 0xFF7CFC00L,
        "lemonchiffon" to 0xFFFFFACDL,
        "lightblue" to 0xFFADD8E6L,
        "lightcoral" to 0xFFF08080L,
        "lightcyan" to 0xFFE0FFFFL,
        "lightgoldenrodyellow" to 0xFFFAFAD2L,
        "lightgray" to 0xFFD3D3D3L,
        "lightgreen" to 0xFF90EE90L,
        "lightgrey" to 0xFFD3D3D3L,
        "lightpink" to 0xFFFFB6C1L,
        "lightsalmon" to 0xFFFFA07AL,
        "lightseagreen" to 0xFF20B2AAL,
        "lightskyblue" to 0xFF87CEFAL,
        "lightslategray" to 0xFF778899L,
        "lightslategrey" to 0xFF778899L,
        "lightsteelblue" to 0xFFB0C4DEL,
        "lightyellow" to 0xFFFFFFE0L,
        "lime" to 0xFF00FF00L,
        "limegreen" to 0xFF32CD32L,
        "linen" to 0xFFFAF0E6L,
        "magenta" to 0xFFFF00FFL,
        "maroon" to 0xFF800000L,
        "mediumaquamarine" to 0xFF66CDAAL,
        "mediumblue" to 0xFF0000CDL,
        "mediumorchid" to 0xFFBA55D3L,
        "mediumpurple" to 0xFF9370DBL,
        "mediumseagreen" to 0xFF3CB371L,
        "mediumslateblue" to 0xFF7B68EEL,
        "mediumspringgreen" to 0xFF00FA9AL,
        "mediumturquoise" to 0xFF48D1CCL,
        "mediumvioletred" to 0xFFC71585L,
        "midnightblue" to 0xFF191970L,
        "mintcream" to 0xFFF5FFFAL,
        "mistyrose" to 0xFFFFE4E1L,
        "moccasin" to 0xFFFFE4B5L,
        "navajowhite" to 0xFFFFDEADL,
        "navy" to 0xFF000080L,
        "oldlace" to 0xFFFDF5E6L,
        "olive" to 0xFF808000L,
        "olivedrab" to 0xFF6B8E23L,
        "orange" to 0xFFFFA500L,
        "orangered" to 0xFFFF4500L,
        "orchid" to 0xFFDA70D6L,
        "palegoldenrod" to 0xFFEEE8AAL,
        "palegreen" to 0xFF98FB98L,
        "paleturquoise" to 0xFFAFEEEEL,
        "palevioletred" to 0xFFDB7093L,
        "papayawhip" to 0xFFFFEFD5L,
        "peachpuff" to 0xFFFFDAB9L,
        "peru" to 0xFFCD853FL,
        "pink" to 0xFFFFC0CBL,
        "plum" to 0xFFDDA0DDL,
        "powderblue" to 0xFFB0E0E6L,
        "purple" to 0xFF800080L,
        "rebeccapurple" to 0xFF663399L,
        "red" to 0xFFFF0000L,
        "rosybrown" to 0xFFBC8F8FL,
        "royalblue" to 0xFF4169E1L,
        "saddlebrown" to 0xFF8B4513L,
        "salmon" to 0xFFFA8072L,
        "sandybrown" to 0xFFF4A460L,
        "seagreen" to 0xFF2E8B57L,
        "seashell" to 0xFFFFF5EEL,
        "sienna" to 0xFFA0522DL,
        "silver" to 0xFFC0C0C0L,
        "skyblue" to 0xFF87CEEBL,
        "slateblue" to 0xFF6A5ACDL,
        "slategray" to 0xFF708090L,
        "slategrey" to 0xFF708090L,
        "snow" to 0xFFFFFAFAL,
        "springgreen" to 0xFF00FF7FL,
        "steelblue" to 0xFF4682B4L,
        "tan" to 0xFFD2B48CL,
        "teal" to 0xFF008080L,
        "thistle" to 0xFFD8BFD8L,
        "tomato" to 0xFFFF6347L,
        "turquoise" to 0xFF40E0D0L,
        "violet" to 0xFFEE82EEL,
        "wheat" to 0xFFF5DEB3L,
        "white" to 0xFFFFFFFFL,
        "whitesmoke" to 0xFFF5F5F5L,
        "yellow" to 0xFFFFFF00L,
        "yellowgreen" to 0xFF9ACD32L
    )

    private val PAREN_COLOR_PATTERN = Pattern.compile("(?:rgba?|hsla?)\\([^)]+\\)", Pattern.CASE_INSENSITIVE)
    private val HEX_COLOR_PATTERN = Pattern.compile("#([0-9a-fA-F]{3,8})\\b")
    private val DELIMITER_PATTERN = Pattern.compile("[,\\s]+")
    private val WORD_SPLIT_PATTERN = Pattern.compile("[^a-z0-9_-]+")

    fun parse(str: String?): Long? {
        if (str == null) return null
        val clean = str.trim().lowercase()
        if (clean.isBlank()) return null

        NAMED_COLORS[clean]?.let { return it }

        // Hex #RGB, #RGBA, #RRGGBB, #RRGGBBAA
        if (clean.startsWith("#")) {
            val hex = clean.removePrefix("#")
            return when (hex.length) {
                3 -> {
                    val full = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    ("FF$full").toLong(16)
                }
                4 -> {
                    val full = "${hex[3]}${hex[3]}${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    full.toLong(16)
                }
                6 -> ("FF$hex").toLong(16)
                8 -> {
                    // CSS #RRGGBBAA -> Compose AARRGGBB
                    val r = hex.substring(0, 2)
                    val g = hex.substring(2, 4)
                    val b = hex.substring(4, 6)
                    val a = hex.substring(6, 8)
                    "$a$r$g$b".toLong(16)
                }
                else -> null
            }
        }

        // RGB / RGBA (handles commas, spaces, and slash alpha)
        if (clean.startsWith("rgb")) {
            val inner = extractInner(clean, "rgb") ?: return null
            return parseRgbChannels(inner)
        }

        // HSL / HSLA (handles commas, spaces, and slash alpha)
        if (clean.startsWith("hsl")) {
            val inner = extractInner(clean, "hsl") ?: return null
            return parseHslChannels(inner)
        }

        return null
    }

    private fun extractInner(str: String, prefix: String): String? {
        val start = str.indexOf('(')
        val end = str.lastIndexOf(')')
        if (start != -1 && end > start) {
            return str.substring(start + 1, end).trim()
        }
        return null
    }

    private fun parseRgbChannels(inner: String): Long? {
        // Split on slash for alpha if present: "r g b / a"
        val slashParts = inner.split("/")
        val colorPart = slashParts[0].trim()
        val alphaPart = if (slashParts.size > 1) slashParts[1].trim() else null

        // Tokens in color part can be comma or whitespace delimited
        val tokens = colorPart.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        fun parseChannel(tok: String): Int {
            return if (tok.endsWith("%")) {
                val pct = tok.removeSuffix("%").toFloatOrNull() ?: 0f
                ((pct / 100f) * 255f).roundToInt().coerceIn(0, 255)
            } else {
                tok.toFloatOrNull()?.roundToInt()?.coerceIn(0, 255) ?: 0
            }
        }

        val r = parseChannel(tokens[0])
        val g = parseChannel(tokens[1])
        val b = parseChannel(tokens[2])

        var a = 255
        if (alphaPart != null) {
            a = parseAlpha(alphaPart)
        } else if (tokens.size >= 4) {
            a = parseAlpha(tokens[3])
        }

        return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun parseHslChannels(inner: String): Long? {
        val slashParts = inner.split("/")
        val colorPart = slashParts[0].trim()
        val alphaPart = if (slashParts.size > 1) slashParts[1].trim() else null

        val tokens = colorPart.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        // Hue
        val hStr = tokens[0].lowercase()
        val h = when {
            hStr.endsWith("deg") -> (hStr.removeSuffix("deg").toFloatOrNull() ?: 0f) % 360f
            hStr.endsWith("turn") -> ((hStr.removeSuffix("turn").toFloatOrNull() ?: 0f) * 360f) % 360f
            hStr.endsWith("rad") -> Math.toDegrees((hStr.removeSuffix("rad").toDoubleOrNull() ?: 0.0)).toFloat() % 360f
            else -> (hStr.toFloatOrNull() ?: 0f) % 360f
        }
        val normH = if (h < 0f) h + 360f else h

        // Saturation & Lightness
        val s = (tokens[1].removeSuffix("%").toFloatOrNull() ?: 0f) / 100f
        val l = (tokens[2].removeSuffix("%").toFloatOrNull() ?: 0f) / 100f

        var a = 255
        if (alphaPart != null) {
            a = parseAlpha(alphaPart)
        } else if (tokens.size >= 4) {
            a = parseAlpha(tokens[3])
        }

        val rgb = hslToRgb(normH, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
        return (a.toLong() shl 24) or (rgb[0].toLong() shl 16) or (rgb[1].toLong() shl 8) or rgb[2].toLong()
    }

    private fun parseAlpha(tok: String): Int {
        val clean = tok.trim()
        return if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: 100f
            ((pct / 100f) * 255f).roundToInt().coerceIn(0, 255)
        } else {
            val f = clean.toFloatOrNull() ?: 1.0f
            (f * 255f).roundToInt().coerceIn(0, 255)
        }
    }

    fun extractColorAnywhere(text: String): Long? {
        val parenColor = PAREN_COLOR_PATTERN.matcher(text)
        if (parenColor.find()) {
            parse(parenColor.group(0))?.let { return it }
        }
        val hexMatcher = HEX_COLOR_PATTERN.matcher(text)
        if (hexMatcher.find()) {
            parse(hexMatcher.group(0))?.let { return it }
        }
        // Check named colors (match whole words or tokens)
        val tokens = text.lowercase().split(WORD_SPLIT_PATTERN)
        for (tok in tokens) {
            NAMED_COLORS[tok]?.let { return it }
        }
        return null
    }

    fun isDark(color: Long): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return brightness < 50
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        var r1 = 0f
        var g1 = 0f
        var b1 = 0f
        when {
            h < 60f -> { r1 = c; g1 = x; b1 = 0f }
            h < 120f -> { r1 = x; g1 = c; b1 = 0f }
            h < 180f -> { r1 = 0f; g1 = c; b1 = x }
            h < 240f -> { r1 = 0f; g1 = x; b1 = c }
            h < 300f -> { r1 = x; g1 = 0f; b1 = c }
            else -> { r1 = c; g1 = 0f; b1 = x }
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return intArrayOf(r, g, b)
    }
}
