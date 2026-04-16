package com.hfstudio.minecraftcoloridea.version

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftVersionDetectorTest {
    @Test
    fun prefersMcmodInfoBeforeGradleHints() {
        val files = mapOf(
            "src/main/resources/mcmod.info" to """[{"mcversion":"1.7.10"}]""",
            "gradle.properties" to "minecraft_version=1.20.1"
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.7.10", detected?.versionId)
        assertEquals(MinecraftVersionSource.MCMOD_INFO, detected?.source)
    }

    @Test
    fun prefersMainClassHintBeforeGradleHints() {
        val files = mapOf(
            "src/main/java/example/ExampleMod.java" to """
                @Mod(modid = "example", acceptedMinecraftVersions = "[1.12.2]")
                public class ExampleMod {}
            """.trimIndent(),
            "gradle.properties" to "minecraft_version=1.20.1"
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.12.2", detected?.versionId)
        assertEquals(MinecraftVersionSource.MAIN_CLASS_HINT, detected?.source)
    }

    @Test
    fun fallsBackToGradleWhenMetadataAndMainClassCluesAreMissing() {
        val files = mapOf(
            "gradle.properties" to "minecraft_version=1.20.1"
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.20.1", detected?.versionId)
        assertEquals(MinecraftVersionSource.GRADLE, detected?.source)
    }
}
