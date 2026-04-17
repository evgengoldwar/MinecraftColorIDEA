package com.hfstudio.minecraftcoloridea

import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class MinecraftColorBundleTest {
    @Test
    fun loadsEnglishAndChineseMessages() {
        assertEquals("Minecraft Color Highlighter", MinecraftColorBundle.message("settings.display.name"))
        assertEquals(
            "Minecraft \u989c\u8272\u9ad8\u4eae",
            MinecraftColorBundle.messageForLocale("settings.display.name", Locale.SIMPLIFIED_CHINESE)
        )
        assertEquals(
            "Go to Localized Entry",
            MinecraftColorBundle.message("action.MinecraftColor.GotoLocalizedEntry.text")
        )
        assertEquals(
            "\u8df3\u8f6c\u5230\u672c\u5730\u5316\u6761\u76ee",
            MinecraftColorBundle.messageForLocale(
                "action.MinecraftColor.GotoLocalizedEntry.text",
                Locale.SIMPLIFIED_CHINESE
            )
        )
    }

    @Test
    fun keepsEnglishAndChineseBundleKeysInSync() {
        val englishKeys = loadBundleKeys("messages/MinecraftColorBundle.properties")
        val chineseKeys = loadBundleKeys("messages/MinecraftColorBundle_zh_CN.properties")

        assertEquals(englishKeys, chineseKeys)
    }

    @Test
    fun usesSupportedPluginDescriptorLocalizationConventions() {
        val pluginXml = loadResourceText("META-INF/plugin.xml")

        assertTrue(pluginXml.contains("<name>Minecraft Color Highlighter</name>"))
        assertFalse(pluginXml.contains("<name>%"))
        assertTrue(pluginXml.contains("""id="MinecraftColor.GotoLocalizedEntry""""))
        assertTrue(pluginXml.contains("""<applicationConfigurable"""))
        assertTrue(pluginXml.contains("""key="settings.display.name""""))
        assertTrue(pluginXml.contains("""bundle="messages.MinecraftColorBundle""""))
        assertFalse(pluginXml.contains("""displayName="%settings.display.name""""))
        assertFalse(pluginXml.contains("""text="%action.goto.localized.entry.text""""))
        assertFalse(pluginXml.contains("""description="%action.goto.localized.entry.description""""))
    }

    private fun loadBundleKeys(path: String): Set<String> {
        val stream = javaClass.classLoader.getResourceAsStream(path)
        assertNotNull(stream, "Missing test resource: $path")
        return stream.use {
            val properties = Properties()
            properties.load(it)
            properties.stringPropertyNames()
        }
    }

    private fun loadResourceText(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
        assertNotNull(stream, "Missing test resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
