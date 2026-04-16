package com.hfstudio.minecraftcoloridea.settings

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
    private val detectedVersionLabel = JBLabel()
    private val effectiveVersionLabel = JBLabel()

    private var component: JComponent? = null

    override fun getDisplayName(): String = "Minecraft Color Highlighter"

    override fun createComponent(): JComponent {
        if (component == null) {
            val form = FormBuilder.createFormBuilder()
                .addLabeledComponent("Project Java version override:", overrideField, 1, false)
                .addLabeledComponent("Detected Java version:", detectedVersionLabel, 1, false)
                .addLabeledComponent("Effective Java version:", effectiveVersionLabel, 1, false)
                .panel

            component = JPanel(BorderLayout()).apply {
                add(form, BorderLayout.NORTH)
            }
        }

        return component!!
    }

    override fun isModified(): Boolean {
        val current = projectSettings.projectJavaVersionOverride().orEmpty()
        return overrideField.text.trim() != current
    }

    override fun apply() {
        projectSettings.setProjectJavaVersionOverride(overrideField.text.trim())
        refreshLabels()
    }

    override fun reset() {
        overrideField.text = projectSettings.projectJavaVersionOverride().orEmpty()
        refreshLabels()
    }

    private fun refreshLabels() {
        val detected = versionCache.getOrDetect()
        val effective = projectSettings.resolveEffectiveJavaVersionId(
            detectedVersionId = detected?.versionId,
            globalDefaultVersionId = globalSettings.toConfig().effectiveJavaVersionId
        )

        detectedVersionLabel.text = detected?.versionId ?: "Not detected"
        effectiveVersionLabel.text = effective
    }
}
