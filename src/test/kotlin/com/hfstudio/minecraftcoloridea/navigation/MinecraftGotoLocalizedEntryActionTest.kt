package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.openapi.actionSystem.Presentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftGotoLocalizedEntryActionTest {
    @Test
    fun updatePresentationStaysVisibleForResolvedKeyEvenBeforeNavigationTargetLookup() {
        val presentation = Presentation()

        MinecraftGotoLocalizedEntryAction().updatePresentationForResolvedKey(
            presentation = presentation,
            locatedKey = MinecraftLocatedKey("Backpack title", 10..24)
        )

        assertTrue(presentation.isEnabledAndVisible)
    }

    @Test
    fun updatePresentationHidesActionWhenNoKeyIsResolved() {
        val presentation = Presentation()

        MinecraftGotoLocalizedEntryAction().updatePresentationForResolvedKey(
            presentation = presentation,
            locatedKey = null
        )

        assertFalse(presentation.isEnabledAndVisible)
    }

    @Test
    fun resolveNavigationRequestReturnsNotReadyWithoutLookupWhenIndexIsCold() {
        var lookupCalls = 0

        val resolved = MinecraftGotoLocalizedEntryAction().resolveNavigationRequest(
            key = "tooltip.backpack",
            sourceIndexStamp = 0L
        ) {
            lookupCalls += 1
            null
        }

        assertEquals(MinecraftGotoLocalizedEntryAction.NavigationRequestResult.IndexNotReady, resolved)
        assertEquals(0, lookupCalls)
    }
}
