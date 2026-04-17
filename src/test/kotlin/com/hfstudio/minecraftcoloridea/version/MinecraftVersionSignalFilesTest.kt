package com.hfstudio.minecraftcoloridea.version

import kotlin.test.Test
import kotlin.test.assertTrue

class MinecraftVersionSignalFilesTest {
    @Test
    fun recognizesNeoforgeModsTomlAsVersionSignalFile() {
        assertTrue(
            MinecraftVersionSignalFiles.isVersionSignalFile(
                "E:/Github/ExampleMod/src/main/resources/META-INF/neoforge.mods.toml"
            )
        )
    }

    @Test
    fun includesNeoforgeModsTomlInCandidatePaths() {
        assertTrue(
            "src/main/resources/META-INF/neoforge.mods.toml" in
                MinecraftVersionSignalFiles.candidateRelativePaths()
        )
    }
}
