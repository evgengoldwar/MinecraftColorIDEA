package com.hfstudio.minecraftcoloridea.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinecraftJavaVersionTest {
    @Test
    fun acceptsSupportedUpperBoundVersion() {
        val version = MinecraftJavaVersion.fromId("1.21.10")

        assertEquals("1.21.10", version?.id)
        assertEquals(21, version?.minor)
        assertEquals(10, version?.patch)
    }

    @Test
    fun rejectsFutureVersionsBeyondUpperBound() {
        assertNull(MinecraftJavaVersion.fromId("1.21.11"))
        assertNull(MinecraftJavaVersion.fromId("1.22"))
    }
}
