package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinecraftColorProjectSettingsStateTest {
    @Test
    fun effectiveConfigPreservesGlobalLocalesForFallbackOrdering() {
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

        assertEquals("zh_cn", resolved.preferredLocale)
        assertEquals("en_us", resolved.secondaryLocale)
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

    @Test
    fun localeTargetOrderPutsProjectOverridesAheadOfGlobalFallbacks() {
        val state = MinecraftColorProjectSettingsState()
        state.setPreferredLocaleOverride("ru_ru")
        state.setSecondaryLocaleOverride("ko_kr")

        val order = state.resolveLocaleTargetOrder(
            MinecraftColorConfig(
                preferredLocale = "zh_cn",
                secondaryLocale = "ja_jp"
            )
        )

        assertEquals(listOf("ru_ru", "ko_kr", "zh_cn", "ja_jp", "en_us"), order)
    }

    @Test
    fun rejectsUnsupportedProjectJavaVersionOverrideOnWrite() {
        val state = MinecraftColorProjectSettingsState()

        state.setProjectJavaVersionOverride("1.22")

        assertNull(state.projectJavaVersionOverride())
    }

    @Test
    fun normalizesUnsupportedProjectJavaVersionOverrideWhenLoadedFromPersistedState() {
        val state = MinecraftColorProjectSettingsState()
        val storedState = MinecraftColorProjectSettingsState.StoredState().apply {
            projectJavaVersionOverride = "1.22"
        }

        state.loadState(storedState)

        assertNull(state.projectJavaVersionOverride())
        assertNull(state.getState().projectJavaVersionOverride)
    }

    @Test
    fun unsupportedProjectJavaVersionOverrideFallsBackToDetectedVersion() {
        val state = MinecraftColorProjectSettingsState()
        state.setProjectJavaVersionOverride("1.22")

        val resolved = state.resolveEffectiveJavaVersionId(
            detectedVersionId = "1.21.10",
            globalDefaultVersionId = "1.20.1"
        )

        assertEquals("1.21.10", resolved)
    }

    @Test
    fun projectInferenceLimitOverrideFallsBackToGlobalWhenUnset() {
        val state = MinecraftColorProjectSettingsState()

        assertEquals(8, state.resolveMaxEnumeratedKeys(globalDefault = 8))
    }

    @Test
    fun projectInferenceLimitOverrideReplacesGlobalWhenValid() {
        val state = MinecraftColorProjectSettingsState()
        state.setMaxEnumeratedKeysOverride("16")

        assertEquals(16, state.resolveMaxEnumeratedKeys(globalDefault = 8))
    }

    @Test
    fun invalidProjectInferenceLimitOverrideFallsBackToGlobal() {
        val state = MinecraftColorProjectSettingsState()
        state.setMaxEnumeratedKeysOverride("200")

        assertEquals(8, state.resolveMaxEnumeratedKeys(globalDefault = 8))
    }
}
