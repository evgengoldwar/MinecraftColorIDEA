package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftLocaleTargetResolverTest {
    @Test
    fun projectLocalesComeBeforeGlobalLocalesAndEnglishFallback() {
        val order = MinecraftLocaleTargetResolver.resolve(
            baseConfig = MinecraftColorConfig(preferredLocale = "zh_cn", secondaryLocale = "ja_jp"),
            projectSettings = MinecraftColorProjectSettingsState().apply {
                setPreferredLocaleOverride("ru_ru")
                setSecondaryLocaleOverride("ko_kr")
            }
        )

        assertEquals(listOf("ru_ru", "ko_kr", "zh_cn", "ja_jp", "en_us"), order)
    }

    @Test
    fun normalizesAndDeduplicatesLocalesWhilePreservingPriorityOrder() {
        val order = MinecraftLocaleTargetResolver.resolve(
            baseConfig = MinecraftColorConfig(preferredLocale = " Zh_CN ", secondaryLocale = "EN_US"),
            projectSettings = MinecraftColorProjectSettingsState().apply {
                setPreferredLocaleOverride("ZH_CN")
                setSecondaryLocaleOverride(" en_us ")
            }
        )

        assertEquals(listOf("zh_cn", "en_us"), order)
    }
}
