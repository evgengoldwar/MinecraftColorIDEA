package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
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
        this.storedState = state
    }

    fun projectJavaVersionOverride(): String? {
        return storedState.projectJavaVersionOverride
            ?.trim()
            ?.ifEmpty { null }
    }

    fun setProjectJavaVersionOverride(value: String?) {
        storedState.projectJavaVersionOverride = value
            ?.trim()
            ?.ifEmpty { null }
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
        builtInDefaultVersionId: String = "1.20.1"
    ): String {
        return projectJavaVersionOverride()
            ?: detectedVersionId?.trim()?.ifEmpty { null }
            ?: globalDefaultVersionId.trim().ifEmpty { builtInDefaultVersionId }
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
            ),
            preferredLocale = preferredLocaleOverride() ?: baseConfig.preferredLocale,
            secondaryLocale = secondaryLocaleOverride() ?: baseConfig.secondaryLocale
        )
    }
}
