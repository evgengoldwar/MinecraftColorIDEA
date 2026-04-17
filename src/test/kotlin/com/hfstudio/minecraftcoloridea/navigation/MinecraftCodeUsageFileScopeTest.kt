package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftCodeUsageFileScopeTest {
    @Test
    fun recognizesProjectCodeFilesButSkipsBuildOutputAndLangResources() {
        assertTrue(MinecraftCodeUsageFileScope.isCandidate("E:/Github/ExampleMod/src/main/java/example/BackpackItem.java"))
        assertTrue(MinecraftCodeUsageFileScope.isCandidate("E:/Github/ExampleMod/docs/dev-notes.md"))
        assertFalse(MinecraftCodeUsageFileScope.isCandidate("E:/Github/ExampleMod/build/generated/example/BackpackItem.java"))
        assertFalse(MinecraftCodeUsageFileScope.isCandidate("E:/Github/ExampleMod/src/main/resources/assets/example/lang/en_us.json"))
    }
}
