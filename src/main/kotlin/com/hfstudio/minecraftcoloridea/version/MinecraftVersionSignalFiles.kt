package com.hfstudio.minecraftcoloridea.version

object MinecraftVersionSignalFiles {
    const val MCMOD_INFO = "src/main/resources/mcmod.info"
    const val NEOFORGE_MODS_TOML = "src/main/resources/META-INF/neoforge.mods.toml"
    const val MODS_TOML = "src/main/resources/META-INF/mods.toml"
    const val FABRIC_MOD_JSON = "src/main/resources/fabric.mod.json"
    const val QUILT_MOD_JSON = "src/main/resources/quilt.mod.json"
    private val METADATA_RELATIVE_PATHS = listOf(
        MCMOD_INFO,
        NEOFORGE_MODS_TOML,
        MODS_TOML,
        FABRIC_MOD_JSON,
        QUILT_MOD_JSON
    )
    private val GRADLE_FALLBACK_RELATIVE_PATHS = listOf(
        "build.gradle",
        "build.gradle.kts",
        "gradle.properties"
    )
    private val CANDIDATE_RELATIVE_PATHS = METADATA_RELATIVE_PATHS + GRADLE_FALLBACK_RELATIVE_PATHS

    fun candidateRelativePaths(): List<String> = CANDIDATE_RELATIVE_PATHS

    fun isVersionSignalFile(path: String): Boolean {
        return CANDIDATE_RELATIVE_PATHS.any { relativePath ->
            matchesRelativePath(path, relativePath)
        }
    }

    internal fun metadataRelativePaths(): List<String> = METADATA_RELATIVE_PATHS

    internal fun gradleFallbackRelativePaths(): List<String> = GRADLE_FALLBACK_RELATIVE_PATHS

    internal fun matchesRelativePath(path: String, relativePath: String): Boolean {
        val normalizedPath = path.replace('\\', '/')
        return normalizedPath == relativePath || normalizedPath.endsWith("/$relativePath")
    }
}
