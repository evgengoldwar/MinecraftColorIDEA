package com.hfstudio.minecraftcoloridea.version

import com.hfstudio.minecraftcoloridea.core.MinecraftJavaVersion

class MinecraftVersionDetector {
    private val supportedVersion = Regex("""\b1\.\d+(?:\.\d+)?\b""")
    private val mainClassHintPattern = Regex(
        """(?:acceptedMinecraftVersions|mcversion|MC_VERSION|minecraftVersion)[^0-9]*(1\.\d+(?:\.\d+)?)"""
    )
    private val dependencyBlockPattern = Regex(
        """(?ms)^\s*\[\[dependencies\.[^\]]+]]\s*(.*?)(?=^\s*\[\[|\z)"""
    )
    private val minecraftModIdPattern = Regex("""(?m)^\s*modId\s*=\s*["']minecraft["'](?:\s+#.*)?\s*$""")
    private val dependencyVersionPattern = Regex(
        """(?m)^\s*(versionRange|version)\s*=\s*["']([^"']+)["'](?:\s+#.*)?\s*$"""
    )
    private val metadataSources = listOf(
        MinecraftVersionSignalFiles.MCMOD_INFO to MinecraftVersionSource.MCMOD_INFO,
        MinecraftVersionSignalFiles.NEOFORGE_MODS_TOML to MinecraftVersionSource.NEOFORGE_MODS_TOML,
        MinecraftVersionSignalFiles.MODS_TOML to MinecraftVersionSource.MODS_TOML,
        MinecraftVersionSignalFiles.FABRIC_MOD_JSON to MinecraftVersionSource.FABRIC_MOD_JSON,
        MinecraftVersionSignalFiles.QUILT_MOD_JSON to MinecraftVersionSource.QUILT_MOD_JSON
    )
    private data class DependencyDetectionResult(
        val detectedVersion: MinecraftDetectedVersion?,
        val minecraftDependencyBlockFound: Boolean
    )

    fun detect(files: Map<String, String>): MinecraftDetectedVersion? {
        return metadataSources.firstNotNullOfOrNull { (relativePath, source) ->
            detectByPath(files, relativePath, source)
        }
            ?: detectMainClassHints(files)
            ?: detectGradle(files)
    }

    private fun detectByPath(
        files: Map<String, String>,
        relativePath: String,
        source: MinecraftVersionSource
    ): MinecraftDetectedVersion? {
        return files.entries.firstNotNullOfOrNull { (path, content) ->
            if (!MinecraftVersionSignalFiles.matchesRelativePath(path, relativePath)) {
                return@firstNotNullOfOrNull null
            }

            detectFromContent(path, content, source)
        }
    }

    private fun detectMainClassHints(files: Map<String, String>): MinecraftDetectedVersion? {
        return files.entries.firstNotNullOfOrNull { (path, content) ->
            if (!path.endsWith(".java") && !path.endsWith(".kt")) {
                return@firstNotNullOfOrNull null
            }

            val match = mainClassHintPattern.find(content)
                ?: return@firstNotNullOfOrNull null
            val version = MinecraftJavaVersion.fromId(match.groupValues[1])
                ?: return@firstNotNullOfOrNull null

            MinecraftDetectedVersion(
                versionId = version.id,
                source = MinecraftVersionSource.MAIN_CLASS_HINT,
                sourcePath = path,
                evidence = match.value
            )
        }
    }

    private fun detectGradle(files: Map<String, String>): MinecraftDetectedVersion? {
        return files.entries.firstNotNullOfOrNull { (path, content) ->
            if (!MinecraftVersionSignalFiles.gradleFallbackRelativePaths().any { relativePath ->
                    MinecraftVersionSignalFiles.matchesRelativePath(path, relativePath)
                }
            ) {
                return@firstNotNullOfOrNull null
            }

            detectFromContent(path, content, MinecraftVersionSource.GRADLE)
        }
    }

    private fun detectFromContent(
        path: String,
        content: String,
        source: MinecraftVersionSource
    ): MinecraftDetectedVersion? {
        val dependencyDetection = detectMinecraftDependencyVersion(path, content, source)
        dependencyDetection?.detectedVersion?.let { return it }
        if (dependencyDetection?.minecraftDependencyBlockFound == true) {
            return null
        }

        val version = supportedVersion.findAll(content)
            .mapNotNull { MinecraftJavaVersion.fromId(it.value) }
            .firstOrNull()
            ?: return null

        return MinecraftDetectedVersion(
            versionId = version.id,
            source = source,
            sourcePath = path,
            evidence = version.id
        )
    }

    private fun detectMinecraftDependencyVersion(
        path: String,
        content: String,
        source: MinecraftVersionSource
    ): DependencyDetectionResult? {
        if (source != MinecraftVersionSource.NEOFORGE_MODS_TOML && source != MinecraftVersionSource.MODS_TOML) {
            return null
        }

        val dependencyMatch = dependencyBlockPattern.findAll(content)
            .map { it.value }
            .firstOrNull { block -> minecraftModIdPattern.containsMatchIn(block) }
            ?: return DependencyDetectionResult(
                detectedVersion = null,
                minecraftDependencyBlockFound = false
            )
        val versionValue = dependencyVersionPattern.find(dependencyMatch)?.groupValues?.get(2)
            ?: return DependencyDetectionResult(
                detectedVersion = null,
                minecraftDependencyBlockFound = true
            )
        val version = supportedVersion.findAll(versionValue)
            .mapNotNull { MinecraftJavaVersion.fromId(it.value) }
            .firstOrNull()
            ?: return DependencyDetectionResult(
                detectedVersion = null,
                minecraftDependencyBlockFound = true
            )

        return DependencyDetectionResult(
            detectedVersion = MinecraftDetectedVersion(
                versionId = version.id,
                source = source,
                sourcePath = path,
                evidence = versionValue
            ),
            minecraftDependencyBlockFound = true
        )
    }
}
