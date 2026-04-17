package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.openapi.actionSystem.Presentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftGotoCodeUsageActionTest {
    @Test
    fun updatePresentationEnablesForLocatedLangKey() {
        val presentation = Presentation()

        MinecraftGotoCodeUsageAction().updatePresentationForResolvedKey(
            presentation = presentation,
            locatedKey = MinecraftLocatedLangKey("tooltip.backpack", 0..15)
        )

        assertTrue(presentation.isEnabledAndVisible)
    }

    @Test
    fun updatePresentationHidesActionWhenNoLangKeyIsResolved() {
        val presentation = Presentation()

        MinecraftGotoCodeUsageAction().updatePresentationForResolvedKey(
            presentation = presentation,
            locatedKey = null
        )

        assertFalse(presentation.isEnabledAndVisible)
    }

    @Test
    fun resolveNavigationRequestReturnsNotReadyWithoutLookupWhenIndexIsCold() {
        var lookupCalls = 0

        val resolved = MinecraftGotoCodeUsageAction().resolveNavigationRequest(
            key = "tooltip.backpack",
            usageIndexStamp = 0L
        ) {
            lookupCalls += 1
            MinecraftCodeUsageNavigationResolver.NavigationRequestResult.NotFound
        }

        assertEquals(MinecraftCodeUsageNavigationResolver.NavigationRequestResult.IndexNotReady, resolved)
        assertEquals(0, lookupCalls)
    }
}
