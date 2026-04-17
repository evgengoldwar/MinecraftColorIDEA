package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftTranslationKeyFamilyExpanderTest {
    @Test
    fun derivesLegacyNameSuffixForItemAndTileFamilies() {
        assertEquals(
            listOf("item.tank_upgrade", "item.tank_upgrade.name"),
            MinecraftTranslationKeyFamilyExpander.expandLegacyFamilies("item.tank_upgrade")
        )
        assertEquals(
            listOf("tile.leather_backpack", "tile.leather_backpack.name"),
            MinecraftTranslationKeyFamilyExpander.expandLegacyFamilies("tile.leather_backpack")
        )
    }
}
