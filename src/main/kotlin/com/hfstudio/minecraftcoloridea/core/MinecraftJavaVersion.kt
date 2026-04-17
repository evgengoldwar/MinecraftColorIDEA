package com.hfstudio.minecraftcoloridea.core

data class MinecraftJavaVersion(
    val id: String,
    val major: Int,
    val minor: Int,
    val patch: Int?
) {
    fun isSupported(): Boolean {
        if (major != 1) {
            return false
        }

        if (minor == 7) {
            return patch == 10
        }

        if (minor !in 8..MAX_SUPPORTED_MINOR) {
            return false
        }

        if (minor < MAX_SUPPORTED_MINOR) {
            return true
        }

        return (patch ?: 0) <= MAX_SUPPORTED_PATCH
    }

    companion object {
        const val LATEST_SUPPORTED_ID = "1.21.10"
        private const val MAX_SUPPORTED_MINOR = 21
        private const val MAX_SUPPORTED_PATCH = 10
        private val pattern = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?$""")

        fun fromId(id: String?): MinecraftJavaVersion? {
            val normalizedId = id?.trim() ?: return null
            val match = pattern.matchEntire(normalizedId) ?: return null
            val version = MinecraftJavaVersion(
                id = normalizedId,
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].takeIf(String::isNotEmpty)?.toInt()
            )
            return version.takeIf(MinecraftJavaVersion::isSupported)
        }
    }
}
