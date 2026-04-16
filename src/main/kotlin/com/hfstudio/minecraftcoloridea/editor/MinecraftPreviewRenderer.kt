package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import java.awt.Graphics
import java.awt.Rectangle

class MinecraftPreviewRenderer(
    private val preview: MinecraftResolvedPreview
) : EditorCustomElementRenderer {
    private val visiblePreviewText = buildString {
        var index = 0
        while (index < preview.previewText.length) {
            val markerLength = when {
                preview.previewText.startsWith("\\u00a7", index) || preview.previewText.startsWith("\\u00A7", index) -> 6
                preview.previewText[index] == '\u00a7' -> 1
                else -> 0
            }

            if (markerLength > 0 && index + markerLength < preview.previewText.length) {
                index += markerLength + 1
                continue
            }

            append(preview.previewText[index])
            index += 1
        }
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.contentComponent.font
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth("  $visiblePreviewText")
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val color = preview.baseColorHex?.let(ColorUtil::fromHex)
            ?: textAttributes.foregroundColor
            ?: inlay.editor.colorsScheme.defaultForeground

        g.color = color
        g.font = inlay.editor.contentComponent.font
        g.drawString("  $visiblePreviewText", targetRegion.x, targetRegion.y + inlay.editor.ascent)
    }
}
