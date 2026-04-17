package com.hfstudio.minecraftcoloridea.version

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore

@Service(Service.Level.PROJECT)
class MinecraftVersionDetectionCache(private val project: Project) {
    private var stamp: Long = 0
    private var cached: MinecraftDetectedVersion? = null

    fun getOrDetect(): MinecraftDetectedVersion? {
        if (cached != null) {
            return cached
        }

        cached = MinecraftVersionDetector().detect(collectCandidateFiles())
        return cached
    }

    fun invalidate() {
        stamp += 1
        cached = null
    }

    fun stamp(): Long = stamp

    private fun collectCandidateFiles(): Map<String, String> {
        val baseDir = project.basePath
            ?.let { LocalFileSystem.getInstance().findFileByPath(it.replace('\\', '/')) }
            ?: return emptyMap()
        val staticCandidates = MinecraftVersionSignalFiles.candidateRelativePaths().asSequence().mapNotNull { relativePath ->
            val file = baseDir.findFileByRelativePath(relativePath) ?: return@mapNotNull null
            relativePath to String(file.contentsToByteArray())
        }

        val sourceCandidates = sequenceOf("src/main/java", "src/main/kotlin")
            .mapNotNull { baseDir.findFileByRelativePath(it) }
            .flatMap { root ->
                val matches = mutableListOf<Pair<String, String>>()
                VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                    if (!file.isDirectory && (file.extension == "java" || file.extension == "kt")) {
                        val relativePath = file.path
                            .removePrefix(project.basePath.orEmpty() + "/")
                            .removePrefix(project.basePath.orEmpty() + "\\")
                        matches += relativePath to String(file.contentsToByteArray())
                    }
                    true
                }
                matches.asSequence()
            }

        return (staticCandidates + sourceCandidates).toMap()
    }
}
