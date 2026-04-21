package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import java.awt.Color
import java.util.regex.Pattern

class MinecraftLangAnnotator : Annotator {

    private val langEntryPattern = Pattern.compile("^([^=]+)=(.*)$")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        if (!file.name.endsWith(".lang")) return
        if (element != file) return

        val settings = service<MinecraftColorSettingsState>()
        if (!settings.toConfig().enable) return

        val document = file.viewProvider.document ?: return
        val text = document.text
        var currentOffset = 0

        text.lineSequence().forEach { line ->
            if (line.isNotBlank() && !line.trimStart().startsWith("#")) {
                val matcher = langEntryPattern.matcher(line)

                if (matcher.find()) {
                    val keyStart = currentOffset + matcher.start(1)
                    val keyEnd = currentOffset + matcher.end(1)
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(keyStart, keyEnd))
                        .textAttributes(getKeyAttributes())
                        .create()

                    val equalStart = currentOffset + matcher.end(1)
                    val equalEnd = currentOffset + matcher.start(2)
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(equalStart, equalEnd))
                        .textAttributes(getEqualAttributes())
                        .create()
                }
            }
            currentOffset += line.length + 1
        }
    }

    private fun getKeyAttributes(): TextAttributesKey {
        val settings = service<MinecraftColorSettingsState>()
        val colorKey = "MINECRAFT_LANG_KEY_${settings.langKeyColor}"

        return attributesCache.getOrPut(colorKey) {
            val attributes = TextAttributes()
            attributes.foregroundColor = JBColor(Color(settings.langKeyColor), Color(settings.langKeyColor))
            TextAttributesKey.createTextAttributesKey(colorKey, attributes)
        }
    }

    private fun getEqualAttributes(): TextAttributesKey {
        val settings = service<MinecraftColorSettingsState>()
        val colorKey = "MINECRAFT_LANG_EQUAL_${settings.langEqualColor}"

        return attributesCache.getOrPut(colorKey) {
            val attributes = TextAttributes()
            attributes.foregroundColor = JBColor(Color(settings.langEqualColor), Color(settings.langEqualColor))
            TextAttributesKey.createTextAttributesKey(colorKey, attributes)
        }
    }

    companion object {
        private val attributesCache = mutableMapOf<String, TextAttributesKey>()

        @JvmStatic
        fun clearCache() {
            attributesCache.clear()
        }
    }
}