package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftVirtualFileTextLoader
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class MinecraftLangSourceIndexService(private val project: Project) {
    private var stamp: Long = 0
    private val projectLocales = MinecraftLangSourceStore()

    fun sourceIndexStamp(): Long = stamp

    fun lookup(key: String, localeOrder: List<String>): List<MinecraftLangSourceEntry>? {
        return projectLocales.lookup(key, localeOrder)
    }

    fun lookupAll(key: String): List<MinecraftLangSourceEntry>? {
        return projectLocales.lookupAll(key)
    }

    fun refreshProjectResources() {
        projectLocales.clear()
        MinecraftResourceScanner.projectResources(project).forEach { file ->
            parseLocaleFile(file)?.let { (locale, entries) ->
                projectLocales.replaceFile(file.path, locale, entries)
            }
        }
        stamp += 1
    }

    fun refreshChangedFiles(files: Sequence<VirtualFile>) {
        files.forEach { file ->
            parseLocaleFile(file)?.let { (locale, entries) ->
                projectLocales.replaceFile(file.path, locale, entries)
            }
        }
        stamp += 1
    }

    fun refreshDocument(file: VirtualFile, text: String) {
        parseEntries(file.extension, file.nameWithoutExtension, file.path, text)?.let { entries ->
            projectLocales.replaceFile(file.path, file.nameWithoutExtension, entries)
            stamp += 1
        }
    }

    fun removeProjectFile(path: String) {
        projectLocales.removeFile(path)
        stamp += 1
    }

    private fun parseLocaleFile(file: VirtualFile): Pair<String, List<MinecraftLangSourceEntry>>? {
        val entries = parseEntries(
            extension = file.extension,
            locale = file.nameWithoutExtension,
            filePath = file.path,
            text = MinecraftVirtualFileTextLoader.load(file)
        ) ?: return null
        return file.nameWithoutExtension to entries
    }

    private fun parseEntries(
        extension: String?,
        locale: String,
        filePath: String,
        text: String
    ): List<MinecraftLangSourceEntry>? {
        return when (extension?.lowercase()) {
            "lang" -> MinecraftLangFileParser.parseLangSources(
                content = text,
                locale = locale,
                filePath = filePath
            )

            "json" -> MinecraftLangFileParser.parseJsonSources(
                content = text,
                locale = locale,
                filePath = filePath
            )

            else -> null
        }
    }
}
