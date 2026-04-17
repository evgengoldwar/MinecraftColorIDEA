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
    fun prefersNeoforgeMetadataBeforeGradleHints() {
        val files = mapOf(
            "src/main/resources/META-INF/neoforge.mods.toml" to """
                loaderVersion="[1,)"
                [[mods]]
                modId="example"
                version="1.0.0"
                [[dependencies.example]]
                modId="minecraft"
                versionRange="[1.21.10,1.21.11)"
            """.trimIndent(),
            "gradle.properties" to "minecraft_version=1.20.1"
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.21.10", detected?.versionId)
        assertEquals(MinecraftVersionSource.NEOFORGE_MODS_TOML, detected?.source)
    }

    @Test
    fun prefersMinecraftDependencyVersionOverModVersionInNeoforgeMetadata() {
        val files = mapOf(
            "src/main/resources/META-INF/neoforge.mods.toml" to """
                loaderVersion="[1,)"
                [[mods]]
                modId="example"
                version="1.20.1"

                [[dependencies.example]]
                modId="minecraft"
                versionRange="[1.21.10,1.21.11)"
            """.trimIndent()
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.21.10", detected?.versionId)
        assertEquals(MinecraftVersionSource.NEOFORGE_MODS_TOML, detected?.source)
    }

    @Test
    fun acceptsTrailingCommentsOnTomlDependencyVersionLines() {
        val files = mapOf(
            "src/main/resources/META-INF/neoforge.mods.toml" to """
                loaderVersion="[1,)"
                [[mods]]
                modId="example"
                version="1.20.1"

                [[dependencies.example]]
                modId="minecraft"
                versionRange="[1.21.10,)" # comment
            """.trimIndent()
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.21.10", detected?.versionId)
        assertEquals(MinecraftVersionSource.NEOFORGE_MODS_TOML, detected?.source)
    }

    @Test
    fun acceptsTrailingCommentsOnTomlMinecraftModIdLines() {
        val files = mapOf(
            "src/main/resources/META-INF/mods.toml" to """
                modLoader="javafml"
                loaderVersion="[47,)"
                license="MIT"

                [[mods]]
                modId="example"
                version="1.20.1"

                [[dependencies.example]]
                modId = "minecraft" # comment
                versionRange="[1.21.10,)"
            """.trimIndent()
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals("1.21.10", detected?.versionId)
        assertEquals(MinecraftVersionSource.MODS_TOML, detected?.source)
    }

    @Test
    fun doesNotFallBackToWholeFileTomlScanWhenMinecraftDependencyBlockIsUnsupported() {
        val files = mapOf(
            "src/main/resources/META-INF/neoforge.mods.toml" to """
                loaderVersion="[1,)"
                [[mods]]
                modId="example"
                version="1.20.1"

                [[dependencies.example]]
                modId="minecraft"
                versionRange="[1.22,)"
            """.trimIndent()
        )

        val detected = MinecraftVersionDetector().detect(files)

        assertEquals(null, detected)
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
