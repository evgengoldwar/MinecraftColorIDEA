package com.hfstudio.minecraftcoloridea.core

enum class MinecraftVersion(val id: String) {
    BEDROCK("bedrock"),
    BEDROCK_PRE_1_21_50("bedrock-pre-1.21.50"),
    BEDROCK_PRE_1_19_70("bedrock-pre-1.19.70"),
    JAVA("java");

    companion object {
        fun fromId(id: String?): MinecraftVersion? = entries.firstOrNull { it.id == id }
    }
}

enum class MinecraftMarker(val id: String) {
    FOREGROUND("foreground"),
    BACKGROUND("background"),
    OUTLINE("outline"),
    UNDERLINE("underline");

    companion object {
        fun fromId(id: String?): MinecraftMarker? = entries.firstOrNull { it.id == id }
    }
}

data class MinecraftColorConfig(
    val enable: Boolean = true,
    val prefixes: List<String> = listOf("&", "\u00a7"),
    val version: MinecraftVersion = MinecraftVersion.BEDROCK,
    val marker: MinecraftMarker = MinecraftMarker.FOREGROUND,
    val fallback: Boolean = true,
    val fallbackRegex: List<String> = DEFAULT_FALLBACK_REGEX,
    val effectiveJavaVersionId: String = MinecraftJavaVersion.LATEST_SUPPORTED_ID,
    val preferredLocale: String = "en_us",
    val secondaryLocale: String = "zh_cn",
    val extraLocalizationMethods: Set<String> = emptySet(),
    val maxEnumeratedKeys: Int = DEFAULT_MAX_ENUMERATED_KEYS
) {
    fun compiledFallbackRegex(): List<Regex> = fallbackRegex.map(::Regex)

    companion object {
        const val DEFAULT_MAX_ENUMERATED_KEYS = 8
        const val MIN_MAX_ENUMERATED_KEYS = 1
        const val MAX_MAX_ENUMERATED_KEYS = 64

        val DEFAULT_FALLBACK_REGEX = listOf(
            "(?<!\\\\)\"",
            "(?<!\\\\)'",
            "(?<!\\\\)`",
            "\\r?\\n"
        )
    }
}

fun markerRespectsScope(marker: MinecraftMarker): Boolean {
    return marker == MinecraftMarker.FOREGROUND || marker == MinecraftMarker.BACKGROUND
}
