package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinecraftLangEntryKeyLocatorTest {
    @Test
    fun resolvesLangKeyUnderCaret() {
        val line = "item.tank_upgrade.name=Tank Upgrade"
        val resolved = MinecraftLangEntryKeyLocator.locateLang(line, line.indexOf("tank_upgrade"))

        assertEquals("item.tank_upgrade.name", resolved?.key)
    }

    @Test
    fun doesNotResolveLangValueText() {
        val line = "item.tank_upgrade.name=Tank Upgrade"
        val resolved = MinecraftLangEntryKeyLocator.locateLang(line, line.indexOf("Upgrade"))

        assertNull(resolved)
    }

    @Test
    fun resolvesJsonKeyUnderCaret() {
        val line = """  "tooltip.backpack": "Backpack""""
        val resolved = MinecraftLangEntryKeyLocator.locateJson(line, line.indexOf("backpack"))

        assertEquals("tooltip.backpack", resolved?.key)
    }
}
