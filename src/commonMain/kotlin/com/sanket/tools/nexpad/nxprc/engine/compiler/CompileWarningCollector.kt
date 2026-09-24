package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.CompileWarning
import com.sanket.tools.nexpad.nxprc.WarningSeverity

/**
 * Mutable collector for compiler diagnostics passed through the compilation pipeline.
 * Parsers and sub-compilers emit warnings without changing their return types.
 */
internal class CompileWarningCollector {
    private val _warnings = mutableListOf<CompileWarning>()

    fun info(
        code: String,
        message: String,
        source: String = "",
        nodePath: String? = null,
        property: String? = null,
        originalValue: String? = null,
        suggestedFix: String? = null
    ) {
        _warnings += CompileWarning(WarningSeverity.INFO, code, message, source, nodePath, property, originalValue, suggestedFix)
    }

    fun warn(
        code: String,
        message: String,
        source: String = "",
        nodePath: String? = null,
        property: String? = null,
        originalValue: String? = null,
        suggestedFix: String? = null
    ) {
        _warnings += CompileWarning(WarningSeverity.WARNING, code, message, source, nodePath, property, originalValue, suggestedFix)
    }

    fun dropped(
        code: String,
        message: String,
        source: String = "",
        nodePath: String? = null,
        property: String? = null,
        originalValue: String? = null,
        suggestedFix: String? = null
    ) {
        _warnings += CompileWarning(WarningSeverity.DROPPED, code, message, source, nodePath, property, originalValue, suggestedFix)
    }

    fun build(): List<CompileWarning> = _warnings.toList()
    val isEmpty: Boolean get() = _warnings.isEmpty()
}
