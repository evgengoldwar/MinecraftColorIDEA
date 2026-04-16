package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.SpecialFormatting
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import kotlin.math.max

class MinecraftSourceMarkerRenderer(
    val marker: MinecraftSourceMarker
) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int = 14

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val graphics = g as? Graphics2D ?: return
        val boxSize = 10
        val x = targetRegion.x + 2
        val y = targetRegion.y + max(0, (targetRegion.height - boxSize) / 2)

        when (marker.kind) {
            MinecraftSourceMarkerKind.HEX_COLOR,
            MinecraftSourceMarkerKind.MINECRAFT_COLOR -> paintColorMarker(
                graphics = graphics,
                x = x,
                y = y,
                size = boxSize
            )

            MinecraftSourceMarkerKind.MINECRAFT_FORMAT -> paintFormatMarker(
                graphics = graphics,
                x = x,
                y = y,
                size = boxSize
            )
        }
    }

    private fun paintColorMarker(
        graphics: Graphics2D,
        x: Int,
        y: Int,
        size: Int
    ) {
        val color = marker.colorHex?.let(MinecraftSourceMarkerEditSupport::parseColorHex)
            ?: return
        paintTransparencyBackground(graphics, x, y, size)
        graphics.color = color
        graphics.fillRoundRect(x, y, size, size, 4, 4)
        graphics.color = borderColor(color)
        graphics.drawRoundRect(x, y, size, size, 4, 4)
    }

    private fun paintFormatMarker(
        graphics: Graphics2D,
        x: Int,
        y: Int,
        size: Int
    ) {
        val background = when (marker.formatting) {
            SpecialFormatting.OBFUSCATED -> JBColor(Color(0x1F2937), Color(0xD1D5DB))
            SpecialFormatting.RESET -> JBColor(Color(0x6B7280), Color(0x9CA3AF))
            else -> JBColor(Color(0x475569), Color(0x94A3B8))
        }
        val foreground = when (marker.formatting) {
            SpecialFormatting.OBFUSCATED -> JBColor(Color(0xF9FAFB), Color(0x111827))
            else -> JBColor(Color.WHITE, Color.BLACK)
        }

        graphics.color = background
        graphics.fillRoundRect(x, y, size, size, 4, 4)
        graphics.color = foreground
        graphics.drawRoundRect(x, y, size, size, 4, 4)

        if (marker.formatting == SpecialFormatting.OBFUSCATED) {
            graphics.drawLine(x + 2, y + 3, x + size - 2, y + size - 3)
            graphics.drawLine(x + 2, y + size - 3, x + size - 2, y + 3)
        }

        val text = marker.code?.uppercaseChar()?.toString().orEmpty()
        val originalFont = graphics.font
        graphics.font = originalFont.deriveFont(Font.BOLD, 8f)
        val metrics = graphics.getFontMetrics(graphics.font)
        val textX = x + (size - metrics.stringWidth(text)) / 2
        val textY = y + (size + metrics.ascent - metrics.descent) / 2 - 1
        graphics.drawString(text, textX, textY)
        graphics.font = originalFont
    }

    private fun paintTransparencyBackground(
        graphics: Graphics2D,
        x: Int,
        y: Int,
        size: Int
    ) {
        val step = 4
        for (row in 0 until size step step) {
            for (col in 0 until size step step) {
                graphics.color = if (((row + col) / step) % 2 == 0) {
                    JBColor(Color(0xE5E7EB), Color(0x374151))
                } else {
                    JBColor(Color(0xF9FAFB), Color(0x4B5563))
                }
                graphics.fillRect(x + col, y + row, step, step)
            }
        }
    }

    private fun borderColor(color: Color): Color {
        return if (ColorUtil.isDark(color)) {
            JBColor(Color(0xF3F4F6), Color(0xE5E7EB))
        } else {
            JBColor(Color(0x111827), Color(0x111827))
        }
    }
}
