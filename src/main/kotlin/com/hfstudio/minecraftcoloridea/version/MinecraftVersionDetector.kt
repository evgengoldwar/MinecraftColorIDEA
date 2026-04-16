package com.hfstudio.minecraftcoloridea.version

import com.hfstudio.minecraftcoloridea.core.MinecraftJavaVersion

class MinecraftVersionDetector {
    private val supportedVersion = Regex("""\b1\.\d+(?:\.\d+)?\b""")
    private val mainClassHintPattern = Regex(
        """(?:acceptedMinecraftVersions|mcversion|MC_VERSION|minecraftVersion)[^0-9]*(1\.\d+(?:\.\d+)?)"""
    )

    fun detect(files: Map<String, String>): MinecraftDetectedVersion? {
        return detectByPath(files, "mcmod.info", MinecraftVersionSource.MCMOD_INFO)
            ?: detectByPath(files, "mods.toml", MinecraftVersionSource.MODS_TOML)
            ?: detectByPath(files, "fabric.mod.json", MinecraftVersionSource.FABRIC_MOD_JSON)
            ?: detectByPath(files, "quilt.mod.json", MinecraftVersionSource.QUILT_MOD_JSON)
            ?: detectMainClassHints(files)
            ?: detectGradle(files)
    }

    private fun detectByPath(
        files: Map<String, String>,
        suffix: String,
        source: MinecraftVersionSource
    ): MinecraftDetectedVersion? {
        return files.entries.firstNotNullOfOrNull { (path, content) ->
            if (!path.endsWith(suffix)) {
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
            if (!path.endsWith("gradle.properties") &&
                !path.endsWith("build.gradle") &&
                !path.endsWith("build.gradle.kts")
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
}
