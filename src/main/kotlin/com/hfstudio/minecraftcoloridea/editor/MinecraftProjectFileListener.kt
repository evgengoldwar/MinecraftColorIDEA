package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class MinecraftProjectFileListener : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        if (events.isEmpty()) {
            return
        }

        ProjectManager.getInstance().openProjects.forEach { project ->
            handleProjectEvents(project, events)
        }
    }

    private fun handleProjectEvents(project: Project, events: List<VFileEvent>) {
        val basePath = project.basePath?.let(::normalizePath) ?: return
        val appService = ApplicationManager.getApplication().service<MinecraftColorApplicationService>()
        val langService = project.service<MinecraftLangIndexService>()

        val relevantEvents = events.filter { event ->
            val path = normalizePath(event.path)
            path.startsWith(basePath) || isLangFile(path)
        }
        if (relevantEvents.isEmpty()) {
            return
        }

        val projectLangEvents = relevantEvents.filter { event ->
            val path = normalizePath(event.path)
            path.startsWith(basePath) && isLangFile(path)
        }
        val dependencyLangChanged = relevantEvents.any { event ->
            val path = normalizePath(event.path)
            !path.startsWith(basePath) && isLangFile(path)
        }
        val versionChanged = relevantEvents.any { event ->
            val path = normalizePath(event.path)
            path.startsWith(basePath) && isVersionSignalFile(path)
        }

        if (projectLangEvents.isNotEmpty()) {
            val changedFiles = projectLangEvents.mapNotNull { it.file }
            if (changedFiles.size == projectLangEvents.size) {
                val changedKeys = langService.refreshChangedFiles(changedFiles.asSequence())
                val affectedDocuments = project.service<MinecraftProjectRefreshCoordinator>()
                    .affectedDocuments(changedKeys)
                if (affectedDocuments.isEmpty()) {
                    appService.refreshProject(project)
                } else {
                    appService.refreshDocuments(project, affectedDocuments)
                }
            } else {
                langService.refreshProjectResources()
                appService.refreshProject(project)
            }
        }

        if (dependencyLangChanged) {
            langService.invalidateDependencyResources()
            appService.refreshProject(project)
        }

        if (versionChanged) {
            project.service<MinecraftVersionDetectionCache>().invalidate()
            appService.refreshProject(project)
        }
    }

    private fun isLangFile(path: String): Boolean {
        return path.contains("/lang/") && (path.endsWith(".lang") || path.endsWith(".json"))
    }

    private fun isVersionSignalFile(path: String): Boolean {
        return path.endsWith("/mcmod.info") ||
            path.endsWith("/mods.toml") ||
            path.endsWith("/fabric.mod.json") ||
            path.endsWith("/quilt.mod.json") ||
            path.endsWith("/build.gradle") ||
            path.endsWith("/build.gradle.kts") ||
            path.endsWith("/gradle.properties")
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
}
