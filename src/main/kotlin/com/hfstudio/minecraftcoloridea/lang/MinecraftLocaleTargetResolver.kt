package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState

object MinecraftLocaleTargetResolver {
    fun resolve(
        baseConfig: MinecraftColorConfig,
        projectSettings: MinecraftColorProjectSettingsState
    ): List<String> {
        return linkedSetOf<String>().apply {
            projectSettings.preferredLocaleOverride()
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotEmpty)
                ?.let(::add)
            projectSettings.secondaryLocaleOverride()
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotEmpty)
                ?.let(::add)
            baseConfig.preferredLocale
                .trim()
                .lowercase()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
            baseConfig.secondaryLocale
                .trim()
                .lowercase()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
            add("en_us")
        }.toList()
    }
}
