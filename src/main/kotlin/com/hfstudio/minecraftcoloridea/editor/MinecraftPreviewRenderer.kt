package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

class MinecraftPreviewRenderer(
    private val preview: MinecraftResolvedPreview
) : EditorCustomElementRenderer {
    private val segments = MinecraftPreviewStyleParser().parse(
        previewText = preview.previewText,
        baseColorHex = preview.baseColorHex,
        baseFormatting = preview.baseFormatting
    )

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.contentComponent.font
        val baseMetrics = inlay.editor.contentComponent.getFontMetrics(font)
        return baseMetrics.stringWidth("  ") + segments.sumOf { segment ->
            val metrics = inlay.editor.contentComponent.getFontMetrics(font.deriveFont(fontStyle(segment)))
            metrics.stringWidth(segment.text)
        }
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val baseFont = inlay.editor.contentComponent.font
        var cursorX = targetRegion.x
        val baseline = targetRegion.y + inlay.editor.ascent

        g.font = baseFont
        g.color = textAttributes.foregroundColor ?: inlay.editor.colorsScheme.defaultForeground
        g.drawString("  ", cursorX, baseline)
        cursorX += inlay.editor.contentComponent.getFontMetrics(baseFont).stringWidth("  ")

        segments.forEach { segment ->
            val font = baseFont.deriveFont(fontStyle(segment))
            val metrics = inlay.editor.contentComponent.getFontMetrics(font)
            val width = metrics.stringWidth(segment.text)
            val color = segment.colorHex?.let(MinecraftSourceMarkerEditSupport::parseRenderColorHex)
                ?: textAttributes.foregroundColor
                ?: inlay.editor.colorsScheme.defaultForeground

            g.font = font
            g.color = color
            g.drawString(segment.text, cursorX, baseline)

            if (segment.underline) {
                val underlineY = baseline + 1
                g.drawLine(cursorX, underlineY, cursorX + width, underlineY)
            }
            if (segment.strikethrough) {
                val strikeY = baseline - metrics.ascent / 3
                g.drawLine(cursorX, strikeY, cursorX + width, strikeY)
            }

            cursorX += width
        }
    }

    private fun fontStyle(segment: MinecraftPreviewStyleSegment): Int {
        var style = Font.PLAIN
        if (segment.bold) {
            style = style or Font.BOLD
        }
        if (segment.italic) {
            style = style or Font.ITALIC
        }
        return style
    }
}
