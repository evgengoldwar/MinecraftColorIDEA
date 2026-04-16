package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftColorProjectSettingsStateTest {
    @Test
    fun projectScopedLocalesOverrideGlobalLocales() {
        val state = MinecraftColorProjectSettingsState()
        state.setPreferredLocaleOverride("ja_jp")
        state.setSecondaryLocaleOverride("ko_kr")

        val resolved = state.resolveEffectiveConfig(
            baseConfig = MinecraftColorConfig(
                preferredLocale = "zh_cn",
                secondaryLocale = "en_us",
                effectiveJavaVersionId = "1.20.1"
            ),
            detectedVersionId = null
        )

        assertEquals("ja_jp", resolved.preferredLocale)
        assertEquals("ko_kr", resolved.secondaryLocale)
    }

    @Test
    fun blankProjectLocalesFallBackToGlobalLocales() {
        val state = MinecraftColorProjectSettingsState()

        val resolved = state.resolveEffectiveConfig(
            baseConfig = MinecraftColorConfig(
                preferredLocale = "zh_cn",
                secondaryLocale = "en_us",
                effectiveJavaVersionId = "1.20.1"
            ),
            detectedVersionId = "1.7.10"
        )

        assertEquals("zh_cn", resolved.preferredLocale)
        assertEquals("en_us", resolved.secondaryLocale)
        assertEquals("1.7.10", resolved.effectiveJavaVersionId)
    }
}
