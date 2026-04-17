package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.MinecraftColorBundle
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MinecraftColorProjectConfigurable(private val project: Project) : Configurable {
    private val projectSettings = project.service<MinecraftColorProjectSettingsState>()
    private val globalSettings = service<MinecraftColorSettingsState>()
    private val versionCache = project.service<MinecraftVersionDetectionCache>()

    private val overrideField = JBTextField()
    private val preferredLocaleField = JBTextField()
    private val secondaryLocaleField = JBTextField()
    private val maxEnumeratedKeysOverrideField = JBTextField()
    private val detectedVersionLabel = JBLabel()
    private val effectiveVersionLabel = JBLabel()
    private val effectiveMaxEnumeratedKeysLabel = JBLabel()

    private var component: JComponent? = null

    override fun getDisplayName(): String = MinecraftColorBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        if (component == null) {
            val form = FormBuilder.createFormBuilder()
                .addLabeledComponent(MinecraftColorBundle.message("settings.project.java.override"), overrideField, 1, false)
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.project.preferred.locale.override"),
                    preferredLocaleField,
                    1,
                    false
                )
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.project.secondary.locale.override"),
                    secondaryLocaleField,
                    1,
                    false
                )
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.project.max.enumerated.keys.override"),
                    maxEnumeratedKeysOverrideField,
                    1,
                    false
                )
                .addLabeledComponent(MinecraftColorBundle.message("settings.project.detected.java.version"), detectedVersionLabel, 1, false)
                .addLabeledComponent(MinecraftColorBundle.message("settings.project.effective.java.version"), effectiveVersionLabel, 1, false)
                .addLabeledComponent(
                    MinecraftColorBundle.message("settings.project.effective.max.enumerated.keys"),
                    effectiveMaxEnumeratedKeysLabel,
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
        return overrideField.text.trim() != projectSettings.projectJavaVersionOverride().orEmpty() ||
            preferredLocaleField.text.trim() != projectSettings.preferredLocaleOverride().orEmpty() ||
            secondaryLocaleField.text.trim() != projectSettings.secondaryLocaleOverride().orEmpty() ||
            maxEnumeratedKeysOverrideField.text.trim() != projectSettings.maxEnumeratedKeysOverride().orEmpty()
    }

    override fun apply() {
        projectSettings.setProjectJavaVersionOverride(overrideField.text.trim())
        projectSettings.setPreferredLocaleOverride(preferredLocaleField.text.trim())
        projectSettings.setSecondaryLocaleOverride(secondaryLocaleField.text.trim())
        projectSettings.setMaxEnumeratedKeysOverride(maxEnumeratedKeysOverrideField.text.trim())
        overrideField.text = projectSettings.projectJavaVersionOverride().orEmpty()
        maxEnumeratedKeysOverrideField.text = projectSettings.maxEnumeratedKeysOverride().orEmpty()
        refreshLabels()
    }

    override fun reset() {
        overrideField.text = projectSettings.projectJavaVersionOverride().orEmpty()
        preferredLocaleField.text = projectSettings.preferredLocaleOverride().orEmpty()
        secondaryLocaleField.text = projectSettings.secondaryLocaleOverride().orEmpty()
        maxEnumeratedKeysOverrideField.text = projectSettings.maxEnumeratedKeysOverride().orEmpty()
        refreshLabels()
    }

    private fun refreshLabels() {
        val detected = versionCache.getOrDetect()
        val effective = projectSettings.resolveEffectiveJavaVersionId(
            detectedVersionId = detected?.versionId,
            globalDefaultVersionId = globalSettings.toConfig().effectiveJavaVersionId
        )
        val effectiveMaxEnumeratedKeys = projectSettings.resolveMaxEnumeratedKeys(
            globalSettings.toConfig().maxEnumeratedKeys
        )

        detectedVersionLabel.text = detected?.versionId ?: MinecraftColorBundle.message("settings.project.version.not.detected")
        effectiveVersionLabel.text = effective
        effectiveMaxEnumeratedKeysLabel.text = effectiveMaxEnumeratedKeys.toString()
    }
}
