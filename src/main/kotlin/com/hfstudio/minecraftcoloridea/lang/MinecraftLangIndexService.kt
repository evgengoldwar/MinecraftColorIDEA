package com.hfstudio.minecraftcoloridea.lang

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class MinecraftLangIndexService(private val project: Project) {
    private var stamp: Long = 0
    private val projectLocales = MinecraftLangFileStore()
    private var dependencyLocales: Map<String, Map<String, String>>? = null

    fun currentIndex(): MinecraftLangIndex = MinecraftLangIndex(
        projectLocales = projectLocales.snapshot(),
        dependencyLocales = dependencyLocales.orEmpty()
    )

    fun langIndexStamp(): Long = stamp

    fun refreshProjectResources() {
        projectLocales.clear()
        MinecraftResourceScanner.projectResources(project).forEach { file ->
            parseLocaleFile(file)?.let { (locale, entries) ->
                projectLocales.replaceFile(file.path, locale, entries)
            }
        }
        dependencyLocales = null
        stamp += 1
    }

    fun refreshChangedFiles(files: Sequence<VirtualFile>): Set<String> {
        val changedKeys = linkedSetOf<String>()
        files.forEach { file ->
            parseLocaleFile(file)?.let { (locale, entries) ->
                changedKeys += projectLocales.replaceFile(file.path, locale, entries)
            }
        }

        if (changedKeys.isNotEmpty()) {
            dependencyLocales = null
            stamp += 1
        }
        return changedKeys
    }

    fun refreshDocument(file: VirtualFile, text: String): Set<String> {
        val entries = parseEntries(file.extension, text) ?: return emptySet()
        val changedKeys = projectLocales.replaceFile(file.path, file.nameWithoutExtension, entries)
        if (changedKeys.isNotEmpty()) {
            stamp += 1
        }
        return changedKeys
    }

    fun removeProjectFile(path: String): Set<String> {
        val changedKeys = projectLocales.removeFile(path)
        if (changedKeys.isNotEmpty()) {
            dependencyLocales = null
            stamp += 1
        }
        return changedKeys
    }

    fun lookup(key: String, localeOrder: List<String>): String? {
        currentIndex().lookup(key, localeOrder)?.let { return it }

        val builtDependencyLocales = dependencyLocales ?: buildLocales(
            MinecraftResourceScanner.localDependencyResources(project)
        ).also { dependencyLocales = it }

        return MinecraftLangIndex(projectLocales.snapshot(), builtDependencyLocales).lookup(key, localeOrder)
    }

    fun invalidateDependencyResources() {
        dependencyLocales = null
        stamp += 1
    }

    private fun buildLocales(files: Sequence<VirtualFile>): Map<String, Map<String, String>> {
        return files.groupBy { it.nameWithoutExtension.lowercase() }
            .mapValues { (_, localeFiles) ->
                localeFiles.fold(emptyMap()) { acc, file ->
                    acc + (parseEntries(file.extension, String(file.contentsToByteArray())) ?: emptyMap())
                }
            }
    }

    private fun parseLocaleFile(file: VirtualFile): Pair<String, Map<String, String>>? {
        val entries = parseEntries(file.extension, String(file.contentsToByteArray())) ?: return null
        return file.nameWithoutExtension to entries
    }

    private fun parseEntries(extension: String?, text: String): Map<String, String>? {
        return when (extension?.lowercase()) {
            "lang" -> MinecraftLangFileParser.parseLang(text)
            "json" -> MinecraftLangFileParser.parseJson(text)
            else -> null
        }
    }
}
