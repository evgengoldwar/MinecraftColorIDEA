package com.hfstudio.minecraftcoloridea.core

enum class MinecraftVersionProfile {
    LEGACY_JAVA_1_7_TO_1_15,
    MODERN_JAVA_1_16_PLUS
}

fun MinecraftJavaVersion.toProfile(): MinecraftVersionProfile {
    return if (minor >= 16) {
        MinecraftVersionProfile.MODERN_JAVA_1_16_PLUS
    } else {
        MinecraftVersionProfile.LEGACY_JAVA_1_7_TO_1_15
    }
}
