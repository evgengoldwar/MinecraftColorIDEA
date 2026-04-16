package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.SpecialFormatting
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftSourceMarkerCollectorTest {
    private val collector = MinecraftSourceMarkerCollector()

    @Test
    fun collectsMinecraftAndHexMarkersFromSourceText() {
        val source = "\"${0xA7.toChar()}eHello #80FFAA00 0xC0C0C0 \\u00a7k\""

        val markers = collector.collect(
            text = source,
            languageId = "JAVA",
            config = MinecraftColorConfig()
        )

        assertEquals(4, markers.size)
        assertEquals(MinecraftSourceMarkerKind.MINECRAFT_COLOR, markers[0].kind)
        assertEquals("#ffff55", markers[0].colorHex)
        assertEquals("${0xA7.toChar()}e", source.substring(markers[0].start, markers[0].end))

        assertEquals(MinecraftSourceMarkerKind.HEX_COLOR, markers[1].kind)
        assertEquals("#80FFAA00", markers[1].rawText)
        assertEquals("#80ffaa00", markers[1].colorHex)

        assertEquals(MinecraftSourceMarkerKind.HEX_COLOR, markers[2].kind)
        assertEquals("0xC0C0C0", markers[2].rawText)
        assertEquals("#c0c0c0", markers[2].colorHex)

        assertEquals(MinecraftSourceMarkerKind.MINECRAFT_FORMAT, markers[3].kind)
        assertEquals(SpecialFormatting.OBFUSCATED, markers[3].formatting)
        assertEquals("\\u00a7k", markers[3].rawText)
    }
}
