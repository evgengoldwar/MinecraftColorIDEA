package com.hfstudio.minecraftcoloridea.core

data class MinecraftJavaVersion(
    val id: String,
    val major: Int,
    val minor: Int,
    val patch: Int?
) {
    fun isSupported(): Boolean {
        return major == 1 && (
            minor in 8..21 ||
                (minor == 7 && patch == 10)
        )
    }

    companion object {
        private val pattern = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?$""")

        fun fromId(id: String?): MinecraftJavaVersion? {
            val match = id?.trim()?.let(pattern::matchEntire) ?: return null
            val version = MinecraftJavaVersion(
                id = id,
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].takeIf(String::isNotEmpty)?.toInt()
            )
            return version.takeIf(MinecraftJavaVersion::isSupported)
        }
    }
}
