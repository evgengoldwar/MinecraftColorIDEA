package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.MinecraftColorBundle
import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftJavaVersion
import com.hfstudio.minecraftcoloridea.core.MinecraftMarker
import com.hfstudio.minecraftcoloridea.core.MinecraftVersion
import com.hfstudio.minecraftcoloridea.editor.MinecraftColorApplicationService
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MinecraftColorConfigurable : Configurable {
    private val settings = service<MinecraftColorSettingsState>()

    private val enableCheckBox = JBCheckBox(MinecraftColorBundle.message("settings.global.enable"))
    private val prefixesField = JBTextField()
    private val versionComboBox = ComboBox(MinecraftVersion.entries.toTypedArray())
    private val globalJavaVersionField = JBTextField()
    private val preferredLocaleField = JBTextField()
    private val secondaryLocaleField = JBTextField()
    private val markerComboBox = ComboBox(MinecraftMarker.entries.toTypedArray())
    private val fallbackCheckBox = JBCheckBox(MinecraftColorBundle.message("settings.global.fallback.enable"))
    private val fallbackRegexArea = JBTextArea(6, 40)
    private val extraMethodsArea = JBTextArea(6, 40)

    private var component: JComponent? = null

    override fun getDisplayName(): String = MinecraftColorBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        if (component == null) {
            versionComboBox.renderer = SimpleListCellRenderer.create("") { value ->
                value?.id ?: ""
            }
            markerComboBox.renderer = SimpleListCellRenderer.create("") { value ->
                value?.id ?: ""
            }
            fallbackRegexArea.lineWrap = false
            fallbackRegexArea.wrapStyleWord = false
            extraMethodsArea.lineWrap = false
            extraMethodsArea.wrapStyleWord = false

            val form = FormBuilder.createFormBuilder()
                .addComponent(enableCheckBox)
                .addLabeledComponent(MinecraftColorBundle.message("settings.global.prefixes"), prefixesField, 1, false)
                .addLabeledComponent(MinecraftColorBundle.message("settings.global.version"), versionComboBox, 1, false)
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.global.default.java.version"),
                    globalJavaVersionField,
                    1,
                    false
                )
                .addLabeledComponent(MinecraftColorBundle.message("settings.global.preferred.locale"), preferredLocaleField, 1, false)
                .addLabeledComponent(MinecraftColorBundle.message("settings.global.secondary.locale"), secondaryLocaleField, 1, false)
                .addLabeledComponent(MinecraftColorBundle.message("settings.global.marker"), markerComboBox, 1, false)
                .addComponent(fallbackCheckBox)
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.global.fallback.regex"),
                    JBScrollPane(fallbackRegexArea),
                    1,
                    false
                )
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.global.extra.methods"),
                    JBScrollPane(extraMethodsArea),
                    1,
                    false
                )
                .panel

            component = JPanel(BorderLayout()).apply {
                add(form, BorderLayout.NORTH)
            }
        }

        return component!!
    }

    override fun isModified(): Boolean {
        return parseConfigFromUi() != settings.toConfig()
    }

    override fun apply() {
        settings.update(parseConfigFromUi())
        globalJavaVersionField.text = settings.toConfig().effectiveJavaVersionId
        service<MinecraftColorApplicationService>().refreshAllEditors()
    }

    override fun reset() {
        val config = settings.toConfig()
        enableCheckBox.isSelected = config.enable
        prefixesField.text = config.prefixes.joinToString(", ")
        versionComboBox.selectedItem = config.version
        globalJavaVersionField.text = config.effectiveJavaVersionId
        preferredLocaleField.text = config.preferredLocale
        secondaryLocaleField.text = config.secondaryLocale
        markerComboBox.selectedItem = config.marker
        fallbackCheckBox.isSelected = config.fallback
        fallbackRegexArea.text = config.fallbackRegex.joinToString("\n")
        extraMethodsArea.text = config.extraLocalizationMethods.joinToString("\n")
    }

    private fun parseConfigFromUi(): MinecraftColorConfig {
        val prefixes = prefixesField.text
            .split(',', '\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .ifEmpty { MinecraftColorConfig().prefixes }
        val fallbackRegex = fallbackRegexArea.text
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
            .ifEmpty { MinecraftColorConfig.DEFAULT_FALLBACK_REGEX }
        val extraMethods = extraMethodsArea.text
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

        return MinecraftColorConfig(
            enable = enableCheckBox.isSelected,
            prefixes = prefixes,
            version = versionComboBox.item ?: MinecraftVersion.BEDROCK,
            marker = markerComboBox.item ?: MinecraftMarker.FOREGROUND,
            fallback = fallbackCheckBox.isSelected,
            fallbackRegex = fallbackRegex,
            effectiveJavaVersionId = MinecraftJavaVersion.fromId(globalJavaVersionField.text)?.id
                ?: MinecraftJavaVersion.LATEST_SUPPORTED_ID,
            preferredLocale = preferredLocaleField.text.trim().ifEmpty { "en_us" },
            secondaryLocale = secondaryLocaleField.text.trim().ifEmpty { "zh_cn" },
            extraLocalizationMethods = extraMethods
        )
    }
}
