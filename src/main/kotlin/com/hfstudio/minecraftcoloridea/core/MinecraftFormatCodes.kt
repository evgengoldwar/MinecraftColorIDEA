package com.hfstudio.minecraftcoloridea.core

data class MinecraftFormatCodes(
    val colors: Map<Char, String>,
    val special: Map<Char, SpecialFormatting>
)

enum class SpecialFormatting {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    UNDERLINE_STRIKETHROUGH,
    RESET,
    OBFUSCATED
}

object MinecraftVersionRegistry {
    private val javaFormatCodes = MinecraftFormatCodes(
        colors = mapOf(
            '0' to "#000000",
            '1' to "#0000AA",
            '2' to "#00AA00",
            '3' to "#00AAAA",
            '4' to "#AA0000",
            '5' to "#AA00AA",
            '6' to "#FFAA00",
            '7' to "#C6C6C6",
            '8' to "#555555",
            '9' to "#5555FF",
            'a' to "#55FF55",
            'b' to "#55FFFF",
            'c' to "#ff5555",
            'd' to "#ff55ff",
            'e' to "#ffff55",
            'f' to "#ffffff"
        ),
        special = mapOf(
            'l' to SpecialFormatting.BOLD,
            'o' to SpecialFormatting.ITALIC,
            'r' to SpecialFormatting.RESET,
            'n' to SpecialFormatting.UNDERLINE,
            'm' to SpecialFormatting.STRIKETHROUGH,
            'k' to SpecialFormatting.OBFUSCATED
        )
    )

    val profiles: Map<MinecraftVersion, MinecraftFormatCodes> = mapOf(
        MinecraftVersion.BEDROCK to MinecraftFormatCodes(
            colors = mapOf(
                '0' to "#000000",
                '1' to "#0000AA",
                '2' to "#00AA00",
                '3' to "#00AAAA",
                '4' to "#AA0000",
                '5' to "#AA00AA",
                '6' to "#FFAA00",
                '7' to "#C6C6C6",
                '8' to "#555555",
                '9' to "#5555FF",
                'a' to "#55FF55",
                'b' to "#55FFFF",
                'c' to "#ff5555",
                'd' to "#ff55ff",
                'e' to "#ffff55",
                'f' to "#ffffff",
                'g' to "#ddd605",
                'h' to "#E3D4D1",
                'i' to "#CECACA",
                'j' to "#443A3B",
                'm' to "#971607",
                'n' to "#B4684D",
                'p' to "#DEB12D",
                'q' to "#47A036",
                's' to "#2CBAA8",
                't' to "#21497B",
                'u' to "#9A5CC6",
                'v' to "#eb7214"
            ),
            special = mapOf(
                'l' to SpecialFormatting.BOLD,
                'o' to SpecialFormatting.ITALIC,
                'r' to SpecialFormatting.RESET,
                'k' to SpecialFormatting.OBFUSCATED
            )
        ),
        MinecraftVersion.BEDROCK_PRE_1_21_50 to MinecraftFormatCodes(
            colors = mapOf(
                '0' to "#000000",
                '1' to "#0000AA",
                '2' to "#00AA00",
                '3' to "#00AAAA",
                '4' to "#AA0000",
                '5' to "#AA00AA",
                '6' to "#FFAA00",
                '7' to "#C6C6C6",
                '8' to "#555555",
                '9' to "#5555FF",
                'a' to "#55FF55",
                'b' to "#55FFFF",
                'c' to "#ff5555",
                'd' to "#ff55ff",
                'e' to "#ffff55",
                'f' to "#ffffff",
                'g' to "#ddd605",
                'h' to "#E3D4D1",
                'i' to "#CECACA",
                'j' to "#443A3B",
                'm' to "#971607",
                'n' to "#B4684D",
                'p' to "#DEB12D",
                'q' to "#47A036",
                's' to "#2CBAA8",
                't' to "#21497B",
                'u' to "#9A5CC6"
            ),
            special = mapOf(
                'l' to SpecialFormatting.BOLD,
                'o' to SpecialFormatting.ITALIC,
                'r' to SpecialFormatting.RESET,
                'k' to SpecialFormatting.OBFUSCATED
            )
        ),
        MinecraftVersion.BEDROCK_PRE_1_19_70 to MinecraftFormatCodes(
            colors = mapOf(
                '0' to "#000000",
                '1' to "#0000AA",
                '2' to "#00AA00",
                '3' to "#00AAAA",
                '4' to "#AA0000",
                '5' to "#AA00AA",
                '6' to "#FFAA00",
                '7' to "#C6C6C6",
                '8' to "#555555",
                '9' to "#5555FF",
                'a' to "#55FF55",
                'b' to "#55FFFF",
                'c' to "#ff5555",
                'd' to "#ff55ff",
                'e' to "#ffff55",
                'f' to "#ffffff",
                'g' to "#ddd605"
            ),
            special = mapOf(
                'l' to SpecialFormatting.BOLD,
                'o' to SpecialFormatting.ITALIC,
                'r' to SpecialFormatting.RESET,
                'n' to SpecialFormatting.UNDERLINE,
                'm' to SpecialFormatting.STRIKETHROUGH,
                'k' to SpecialFormatting.OBFUSCATED
            )
        ),
        MinecraftVersion.JAVA to javaFormatCodes
    )

    fun profile(version: MinecraftVersion): MinecraftFormatCodes = profiles.getValue(version)

    fun profile(config: MinecraftColorConfig): MinecraftFormatCodes {
        return if (config.version == MinecraftVersion.JAVA) {
            javaProfile(config.effectiveJavaVersionId)
        } else {
            profiles.getValue(config.version)
        }
    }

    fun javaProfile(versionId: String): MinecraftFormatCodes {
        val javaVersion = MinecraftJavaVersion.fromId(versionId) ?: return javaFormatCodes
        return when (javaVersion.toProfile()) {
            MinecraftVersionProfile.LEGACY_JAVA_1_7_TO_1_15 -> javaFormatCodes
            MinecraftVersionProfile.MODERN_JAVA_1_16_PLUS -> javaFormatCodes
        }
    }
}

fun isResetCode(code: SpecialFormatting): Boolean = code == SpecialFormatting.RESET
