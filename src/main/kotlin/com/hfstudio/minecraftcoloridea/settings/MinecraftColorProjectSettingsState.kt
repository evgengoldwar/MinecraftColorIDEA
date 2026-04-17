package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftJavaVersion
import com.hfstudio.minecraftcoloridea.lang.MinecraftLocaleTargetResolver
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "MinecraftColorProjectSettings", storages = [Storage("minecraft-color-project.xml")])
class MinecraftColorProjectSettingsState : PersistentStateComponent<MinecraftColorProjectSettingsState.StoredState> {
    class StoredState {
        var projectJavaVersionOverride: String? = null
        var preferredLocaleOverride: String? = null
        var secondaryLocaleOverride: String? = null
    }

    private var storedState = StoredState()

    override fun getState(): StoredState = storedState

    override fun loadState(state: StoredState) {
        this.storedState = state.apply {
            projectJavaVersionOverride = normalizeProjectJavaVersionOverride(projectJavaVersionOverride)
        }
    }

    fun projectJavaVersionOverride(): String? {
        return normalizeProjectJavaVersionOverride(storedState.projectJavaVersionOverride)
    }

    fun setProjectJavaVersionOverride(value: String?) {
        storedState.projectJavaVersionOverride = normalizeProjectJavaVersionOverride(value)
    }

    fun preferredLocaleOverride(): String? {
        return storedState.preferredLocaleOverride
            ?.trim()
            ?.ifEmpty { null }
    }

    fun setPreferredLocaleOverride(value: String?) {
        storedState.preferredLocaleOverride = value
            ?.trim()
            ?.ifEmpty { null }
    }

    fun secondaryLocaleOverride(): String? {
        return storedState.secondaryLocaleOverride
            ?.trim()
            ?.ifEmpty { null }
    }

    fun setSecondaryLocaleOverride(value: String?) {
        storedState.secondaryLocaleOverride = value
            ?.trim()
            ?.ifEmpty { null }
    }

    fun resolveEffectiveJavaVersionId(
        detectedVersionId: String?,
        globalDefaultVersionId: String,
        builtInDefaultVersionId: String = MinecraftJavaVersion.LATEST_SUPPORTED_ID
    ): String {
        return MinecraftJavaVersion.fromId(projectJavaVersionOverride())?.id
            ?: MinecraftJavaVersion.fromId(detectedVersionId)?.id
            ?: MinecraftJavaVersion.fromId(globalDefaultVersionId)?.id
            ?: builtInDefaultVersionId
    }

    fun resolveEffectiveConfig(
        baseConfig: MinecraftColorConfig,
        detectedVersionId: String?
    ): MinecraftColorConfig {
        return baseConfig.copy(
            effectiveJavaVersionId = resolveEffectiveJavaVersionId(
                detectedVersionId = detectedVersionId,
                globalDefaultVersionId = baseConfig.effectiveJavaVersionId,
                builtInDefaultVersionId = MinecraftColorConfig().effectiveJavaVersionId
            )
        )
    }

    fun resolveLocaleTargetOrder(baseConfig: MinecraftColorConfig): List<String> {
        return MinecraftLocaleTargetResolver.resolve(baseConfig, this)
    }

    private fun normalizeProjectJavaVersionOverride(value: String?): String? {
        val normalizedValue = value
            ?.trim()
            ?.ifEmpty { null }
        return MinecraftJavaVersion.fromId(normalizedValue)?.id
    }
}
