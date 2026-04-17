package com.hfstudio.minecraftcoloridea.settings

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftColorSettingsStateTest {
    @Test
    fun persistsConfiguredGlobalInferenceLimit() {
        val state = MinecraftColorSettingsState()

        state.update(MinecraftColorConfig(maxEnumeratedKeys = 12))

        assertEquals(12, state.toConfig().maxEnumeratedKeys)
    }

    @Test
    fun clampsInvalidPersistedGlobalInferenceLimit() {
        val state = MinecraftColorSettingsState()
        val storedState = MinecraftColorSettingsState.StoredState().apply {
            maxEnumeratedKeys = 200
        }

        state.loadState(storedState)

        assertEquals(MinecraftColorConfig.DEFAULT_MAX_ENUMERATED_KEYS, state.toConfig().maxEnumeratedKeys)
    }
}
