package com.hfstudio.minecraftcoloridea.lang

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class MinecraftLangIndexService(private val project: Project) {
    private var stamp: Long = 0
    private var projectLocales: Map<String, Map<String, String>> = emptyMap()
    private var dependencyLocales: Map<String, Map<String, String>>? = null

    fun currentIndex(): MinecraftLangIndex = MinecraftLangIndex(
        projectLocales = projectLocales,
        dependencyLocales = dependencyLocales.orEmpty()
    )

    fun langIndexStamp(): Long = stamp

    fun refreshProjectResources() {
        projectLocales = buildLocales(MinecraftResourceScanner.projectResources(project))
        dependencyLocales = null
        stamp += 1
    }

    fun refreshChangedFiles(files: Sequence<VirtualFile>): Set<String> {
        val changedKeys = files.flatMap { file ->
            when (file.extension?.lowercase()) {
                "lang" -> MinecraftLangFileParser.parseLang(String(file.contentsToByteArray())).keys.asSequence()
                "json" -> MinecraftLangFileParser.parseJson(String(file.contentsToByteArray())).keys.asSequence()
                else -> emptySequence()
            }
        }.toSet()

        projectLocales = buildLocales(MinecraftResourceScanner.projectResources(project))
        dependencyLocales = null
        stamp += 1
        return changedKeys
    }

    fun lookup(key: String, localeOrder: List<String>): String? {
        currentIndex().lookup(key, localeOrder)?.let { return it }

        val builtDependencyLocales = dependencyLocales ?: buildLocales(
            MinecraftResourceScanner.localDependencyResources(project)
        ).also { dependencyLocales = it }

        return MinecraftLangIndex(projectLocales, builtDependencyLocales).lookup(key, localeOrder)
    }

    fun invalidateDependencyResources() {
        dependencyLocales = null
        stamp += 1
    }

    private fun buildLocales(files: Sequence<VirtualFile>): Map<String, Map<String, String>> {
        return files.groupBy { it.nameWithoutExtension.lowercase() }
            .mapValues { (_, localeFiles) ->
                localeFiles.fold(emptyMap()) { acc, file ->
                    acc + when (file.extension?.lowercase()) {
                        "lang" -> MinecraftLangFileParser.parseLang(String(file.contentsToByteArray()))
                        "json" -> MinecraftLangFileParser.parseJson(String(file.contentsToByteArray()))
                        else -> emptyMap()
                    }
                }
            }
    }
}
