package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftJavaVersion
import com.hfstudio.minecraftcoloridea.core.MinecraftMarker
import com.hfstudio.minecraftcoloridea.core.MinecraftVersion
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.APP)
@State(
    name = "MinecraftColorSettings",
    storages = [Storage("minecraft-color.xml")]
)
class MinecraftColorSettingsState : PersistentStateComponent<MinecraftColorSettingsState.StoredState> {
    class StoredState {
        var enable: Boolean = true
        var prefixes: MutableList<String> = mutableListOf("&", "\u00a7")
        var version: String = MinecraftVersion.BEDROCK.id
        var marker: String = MinecraftMarker.FOREGROUND.id
        var fallback: Boolean = true
        var fallbackRegex: MutableList<String> = MinecraftColorConfig.DEFAULT_FALLBACK_REGEX.toMutableList()
        var globalDefaultJavaVersion: String = MinecraftJavaVersion.LATEST_SUPPORTED_ID
        var preferredLocale: String = "en_us"
        var secondaryLocale: String = "zh_cn"
        var extraLocalizationMethods: MutableList<String> = mutableListOf()
    }

    private var state = StoredState()

    override fun getState(): StoredState = state

    override fun loadState(state: StoredState) {
        this.state = state
    }

    fun toConfig(): MinecraftColorConfig {
        val defaultConfig = MinecraftColorConfig()
        val prefixes = state.prefixes
            .map(String::trim)
            .filter(String::isNotEmpty)
            .ifEmpty { defaultConfig.prefixes }

        val version = MinecraftVersion.fromId(state.version) ?: defaultConfig.version
        val marker = MinecraftMarker.fromId(state.marker) ?: defaultConfig.marker
        val fallbackRegex = state.fallbackRegex
            .map(String::trim)
            .filter(String::isNotEmpty)
            .ifEmpty { defaultConfig.fallbackRegex }
        val effectiveJavaVersionId = MinecraftJavaVersion.fromId(state.globalDefaultJavaVersion)?.id
            ?: defaultConfig.effectiveJavaVersionId
        val extraMethods = state.extraLocalizationMethods
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

        return try {
            MinecraftColorConfig(
                enable = state.enable,
                prefixes = prefixes,
                version = version,
                marker = marker,
                fallback = state.fallback,
                fallbackRegex = fallbackRegex,
                effectiveJavaVersionId = effectiveJavaVersionId,
                preferredLocale = state.preferredLocale.ifBlank { defaultConfig.preferredLocale },
                secondaryLocale = state.secondaryLocale.ifBlank { defaultConfig.secondaryLocale },
                extraLocalizationMethods = extraMethods
            ).also { it.compiledFallbackRegex() }
        } catch (error: IllegalArgumentException) {
            thisLogger().warn("Invalid Minecraft Color fallback regex configuration. Falling back to defaults.", error)
            defaultConfig.copy(
                enable = state.enable,
                prefixes = prefixes,
                version = version,
                marker = marker,
                fallback = state.fallback,
                effectiveJavaVersionId = effectiveJavaVersionId,
                preferredLocale = state.preferredLocale.ifBlank { defaultConfig.preferredLocale },
                secondaryLocale = state.secondaryLocale.ifBlank { defaultConfig.secondaryLocale },
                extraLocalizationMethods = extraMethods
            )
        }
    }

    fun update(config: MinecraftColorConfig) {
        state.enable = config.enable
        state.prefixes = config.prefixes.toMutableList()
        state.version = config.version.id
        state.marker = config.marker.id
        state.fallback = config.fallback
        state.fallbackRegex = config.fallbackRegex.toMutableList()
        state.globalDefaultJavaVersion = config.effectiveJavaVersionId
        state.preferredLocale = config.preferredLocale
        state.secondaryLocale = config.secondaryLocale
        state.extraLocalizationMethods = config.extraLocalizationMethods.toMutableList()
    }
}
