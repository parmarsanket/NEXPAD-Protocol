package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Centralized Enum of compiled regular expression patterns for CSS dimension, geometry, and filter parsing.
 * Eliminates duplicate, hardcoded inline pattern strings across GeometryParser, ShadowParser, FilterParser, etc.
 */
enum class CssSyntaxPattern(val pattern: Pattern) {
    /** Matches pixel font size: e.g. "22px" */
    FONT_SIZE(Pattern.compile("(\\d+(?:\\.\\d+)?)px")),

    /** Matches pixel or percentage dimension with optional negative sign: e.g. "10px", "-5", "50%" */
    LENGTH(Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?")),

    /** Matches border width: e.g. "2px", "1.5px" */
    BORDER_WIDTH(Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px)?")),

    /** Matches border radius (px or %): e.g. "14px", "50%" */
    RADIUS(Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px|%)?")),

    /** Matches CSS insets (px or %): e.g. "8px", "-10px", "20%" */
    INSET(Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px|%)?")),

    /** Matches pixel values with optional unit: e.g. "12px", "12" */
    PIXEL(Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?")),

    /** Matches one or more whitespace characters */
    WHITESPACE(Pattern.compile("\\s+")),

    /** Matches percentages: e.g. "50%" */
    PERCENT(Pattern.compile("(\\d+(?:\\.\\d+)?)%")),

    /** Matches degree angles: e.g. "45deg" */
    DEGREE(Pattern.compile("(-?\\d+(?:\\.\\d+)?)deg")),

    /** Matches turn angles: e.g. "0.25turn" */
    TURN(Pattern.compile("(-?\\d+(?:\\.\\d+)?)turn")),

    /** Matches radian angles: e.g. "1.57rad" */
    RADIAN(Pattern.compile("(-?\\d+(?:\\.\\d+)?)rad")),

    /** Matches arbitrary decimal numbers with optional negative sign and leading dot: e.g. "-5", "0.93", ".93" */
    NUMBER(Pattern.compile("(-?[0-9.]+)")),

    /** Matches generic CSS functions: e.g. "blur(4px)", "calc(100% - 10px)" */
    FUNCTION_CALL(Pattern.compile("([a-zA-Z0-9_-]+)\\s*\\(([^)]+)\\)")),

    /** Matches numbers with optional px suffix: e.g. "4px", "1.5" */
    NUMBER_PX(Pattern.compile("([0-9.]+)\\s*(px)?"));

    fun matcher(input: CharSequence): Matcher = pattern.matcher(input)
    fun split(input: CharSequence): List<String> = pattern.split(input).toList()
}
