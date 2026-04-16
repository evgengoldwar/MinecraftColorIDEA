package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftVersionRegistry
import com.hfstudio.minecraftcoloridea.core.SpecialFormatting
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColorPicker
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

class MinecraftSourceMarkerInlaySession(private val editor: Editor) {
    private data class MinecraftCodeOption(
        val code: Char,
        val colorHex: String? = null,
        val formatting: SpecialFormatting? = null,
        val label: String
    ) {
        fun popupLabel(): String {
            val swatch = when {
                colorHex != null -> "<span style='color:${colorHex.uppercase()}'>&#9632;</span>"
                formatting == SpecialFormatting.OBFUSCATED -> "<span style='color:#94A3B8'>&#9645;</span>"
                else -> "<span style='color:#94A3B8'>&#9632;</span>"
            }
            return "<html>$swatch&nbsp;&nbsp;<code>&#167;$code</code>&nbsp;&nbsp;$label</html>"
        }
    }

    private val inlays = mutableListOf<Inlay<*>>()
    private val mouseListener = object : EditorMouseListener {
        override fun mouseClicked(event: EditorMouseEvent) {
            val inlay = event.inlay ?: return
            val renderer = inlay.renderer as? MinecraftSourceMarkerRenderer ?: return
            handleClick(inlay, renderer.marker)
            event.consume()
        }
    }

    private var config = MinecraftColorConfig()

    init {
        editor.addEditorMouseListener(mouseListener)
    }

    fun replace(markers: List<MinecraftSourceMarker>, config: MinecraftColorConfig) {
        this.config = config
        clear()
        markers.forEach { marker ->
            editor.inlayModel.addInlineElement(
                marker.start,
                false,
                MinecraftSourceMarkerRenderer(marker)
            )?.let(inlays::add)
        }
    }

    fun clear() {
        inlays.forEach(Inlay<*>::dispose)
        inlays.clear()
    }

    fun dispose() {
        clear()
        editor.removeEditorMouseListener(mouseListener)
    }

    private fun handleClick(inlay: Inlay<*>, marker: MinecraftSourceMarker) {
        val project = editor.project ?: return
        when (marker.kind) {
            MinecraftSourceMarkerKind.HEX_COLOR -> {
                val color = MinecraftSourceMarkerEditSupport.parseColor(marker) ?: return
                ColorPicker.showColorPickerPopup(
                    project,
                    color,
                    { nextColor, _ ->
                        updateMarkerRange(
                            marker = marker,
                            replacement = MinecraftSourceMarkerEditSupport.formatHexReplacement(marker, nextColor)
                        )
                    },
                    popupPoint(inlay),
                    true
                )
            }

            MinecraftSourceMarkerKind.MINECRAFT_COLOR,
            MinecraftSourceMarkerKind.MINECRAFT_FORMAT -> {
                val options = popupOptions()
                val popup = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(options)
                    .setTitle("Minecraft Format Code")
                    .setRenderer(SimpleListCellRenderer.create("") { value ->
                        value?.popupLabel().orEmpty()
                    })
                    .setItemChosenCallback { option ->
                        updateMarkerRange(
                            marker = marker,
                            replacement = marker.rawText.replaceRange(
                                marker.rawText.length - 1,
                                marker.rawText.length,
                                option.code.toString()
                            )
                        )
                    }
                    .setMovable(false)
                    .setResizable(false)
                    .setRequestFocus(true)
                    .setVisibleRowCount(12)
                    .createPopup()
                popup.show(popupPoint(inlay))
            }
        }
    }

    private fun popupOptions(): List<MinecraftCodeOption> {
        val profile = MinecraftVersionRegistry.profile(config)
        val colors = profile.colors.toSortedMap().map { (code, colorHex) ->
            MinecraftCodeOption(
                code = code,
                colorHex = colorHex.lowercase(),
                label = minecraftColorName(code)
            )
        }
        val formatting = profile.special.toList()
            .sortedBy { it.first }
            .map { (code, formatting) ->
                MinecraftCodeOption(
                    code = code,
                    formatting = formatting,
                    label = formattingName(formatting)
                )
            }
        return colors + formatting
    }

    private fun updateMarkerRange(
        marker: MinecraftSourceMarker,
        replacement: String
    ) {
        val project = editor.project ?: return
        val rangeMarker = createRangeMarker(marker)
        try {
            if (!rangeMarker.isValid) {
                return
            }

            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.replaceString(
                    rangeMarker.startOffset,
                    rangeMarker.endOffset,
                    replacement
                )
            }
        } finally {
            rangeMarker.dispose()
        }
    }

    private fun createRangeMarker(marker: MinecraftSourceMarker): RangeMarker {
        return editor.document.createRangeMarker(marker.start, marker.end).also {
            it.setGreedyToLeft(false)
            it.setGreedyToRight(false)
        }
    }

    private fun popupPoint(inlay: Inlay<*>): RelativePoint {
        val bounds = inlay.bounds
        return if (bounds == null) {
            ColorPicker.bestLocationForColorPickerPopup(editor)
                ?: RelativePoint(editor.contentComponent, Point(0, editor.lineHeight))
        } else {
            RelativePoint(
                editor.contentComponent,
                Point(bounds.x, bounds.y + bounds.height)
            )
        }
    }

    private fun formattingName(formatting: SpecialFormatting): String {
        return when (formatting) {
            SpecialFormatting.BOLD -> "Bold"
            SpecialFormatting.ITALIC -> "Italic"
            SpecialFormatting.UNDERLINE -> "Underline"
            SpecialFormatting.STRIKETHROUGH -> "Strikethrough"
            SpecialFormatting.UNDERLINE_STRIKETHROUGH -> "Underline + Strikethrough"
            SpecialFormatting.RESET -> "Reset"
            SpecialFormatting.OBFUSCATED -> "Obfuscated"
        }
    }

    private fun minecraftColorName(code: Char): String {
        return when (code) {
            '0' -> "Black"
            '1' -> "Dark Blue"
            '2' -> "Dark Green"
            '3' -> "Dark Aqua"
            '4' -> "Dark Red"
            '5' -> "Dark Purple"
            '6' -> "Gold"
            '7' -> "Gray"
            '8' -> "Dark Gray"
            '9' -> "Blue"
            'a' -> "Green"
            'b' -> "Aqua"
            'c' -> "Red"
            'd' -> "Light Purple"
            'e' -> "Yellow"
            'f' -> "White"
            'g' -> "Minecoin Gold"
            'h' -> "Material Quartz"
            'i' -> "Material Iron"
            'j' -> "Material Netherite"
            'm' -> "Material Redstone"
            'n' -> "Material Copper"
            'p' -> "Material Gold"
            'q' -> "Material Emerald"
            's' -> "Material Diamond"
            't' -> "Material Lapis"
            'u' -> "Material Amethyst"
            'v' -> "Material Resin"
            else -> "Code ${code.uppercaseChar()}"
        }
    }
}
