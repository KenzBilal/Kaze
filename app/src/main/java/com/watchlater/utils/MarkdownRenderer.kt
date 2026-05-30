package com.watchlater.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * Lightweight Markdown → AnnotatedString renderer.
 * Supports: **bold**, *italic*, # headings (H1/H2/H3), - bullet lists, `inline code`
 * Pass [codeBackground] from MaterialTheme at the call site for theme-aware code blocks.
 */
object MarkdownRenderer {

    fun render(
        markdown: String,
        codeBackground: Color = Color(0xFF1E1E2E)
    ): AnnotatedString = buildAnnotatedString {
        val lines = markdown.lines()
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)) {
                        appendInline(line.removePrefix("### "), codeBackground)
                    }
                }
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp)) {
                        appendInline(line.removePrefix("## "), codeBackground)
                    }
                }
                line.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)) {
                        appendInline(line.removePrefix("# "), codeBackground)
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("  • ")
                    appendInline(line.drop(2), codeBackground)
                }
                line.startsWith("  - ") || line.startsWith("  * ") -> {
                    append("    ◦ ")
                    appendInline(line.drop(4), codeBackground)
                }
                else -> appendInline(line, codeBackground)
            }
            if (index < lines.lastIndex) append("\n")
        }
    }

    private fun AnnotatedString.Builder.appendInline(text: String, codeBackground: Color) {
        val segments = parseInline(text)
        segments.forEach { seg ->
            when (seg.style) {
                InlineStyle.BOLD        -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(seg.text) }
                InlineStyle.ITALIC      -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(seg.text) }
                InlineStyle.BOLD_ITALIC -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(seg.text) }
                InlineStyle.CODE        -> withStyle(SpanStyle(fontWeight = FontWeight.Medium, background = codeBackground)) { append(seg.text) }
                InlineStyle.PLAIN       -> append(seg.text)
            }
        }
    }

    private enum class InlineStyle { PLAIN, BOLD, ITALIC, BOLD_ITALIC, CODE }
    private data class Segment(val text: String, val style: InlineStyle)

    private fun parseInline(text: String): List<Segment> {
        val segments = mutableListOf<Segment>()
        var i = 0
        val sb = StringBuilder()

        fun flush(style: InlineStyle = InlineStyle.PLAIN) {
            if (sb.isNotEmpty()) {
                segments.add(Segment(sb.toString(), style))
                sb.clear()
            }
        }

        while (i < text.length) {
            when {
                // Bold+Italic ***
                text.startsWith("***", i) -> {
                    flush()
                    i += 3
                    val end = text.indexOf("***", i)
                    if (end != -1) {
                        segments.add(Segment(text.substring(i, end), InlineStyle.BOLD_ITALIC))
                        i = end + 3
                    } else { sb.append("***") }
                }
                // Bold **
                text.startsWith("**", i) -> {
                    flush()
                    i += 2
                    val end = text.indexOf("**", i)
                    if (end != -1) {
                        segments.add(Segment(text.substring(i, end), InlineStyle.BOLD))
                        i = end + 2
                    } else { sb.append("**") }
                }
                // Italic *
                text[i] == '*' -> {
                    flush()
                    i += 1
                    val end = text.indexOf("*", i)
                    if (end != -1) {
                        segments.add(Segment(text.substring(i, end), InlineStyle.ITALIC))
                        i = end + 1
                    } else { sb.append("*") }
                }
                // Inline code `
                text[i] == '`' -> {
                    flush()
                    i += 1
                    val end = text.indexOf("`", i)
                    if (end != -1) {
                        segments.add(Segment(text.substring(i, end), InlineStyle.CODE))
                        i = end + 1
                    } else { sb.append("`") }
                }
                else -> {
                    sb.append(text[i])
                    i++
                }
            }
        }
        flush()
        return segments
    }
}
