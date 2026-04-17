package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceIndexService
import com.hfstudio.minecraftcoloridea.navigation.MinecraftCodeUsageFileScope
import com.hfstudio.minecraftcoloridea.navigation.MinecraftCodeUsageIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionSignalFiles
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
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
        val basePath = project.basePath ?: return
        val appService = ApplicationManager.getApplication().service<MinecraftColorApplicationService>()
        val langService = project.service<MinecraftLangIndexService>()
        val langSourceService = project.service<MinecraftLangSourceIndexService>()
        val codeUsageService = project.service<MinecraftCodeUsageIndexService>()
        val fileIndex = ProjectFileIndex.getInstance(project)
        val maxEnumeratedKeys = project.service<MinecraftColorProjectSettingsState>()
            .resolveMaxEnumeratedKeys(project.service<MinecraftColorSettingsState>().toConfig().maxEnumeratedKeys)

        val relevantEvents = events.filter { event ->
            val path = normalizePath(event.path)
            MinecraftEditorFileScope.isProjectOwned(basePath, path) ||
                eventOldPath(event)?.let { MinecraftEditorFileScope.isProjectOwned(basePath, it) } == true ||
                MinecraftProjectFileRules.staleProjectLangPath(basePath, event) != null ||
                MinecraftProjectFileRules.staleProjectCodeUsagePath(basePath, event) != null ||
                isDependencyLangFile(fileIndex, path, event.file)
        }
        if (relevantEvents.isEmpty()) {
            return
        }

        val staleProjectLangPaths = MinecraftProjectFileRules.staleProjectLangPaths(basePath, relevantEvents)
        val staleProjectCodePaths = MinecraftProjectFileRules.staleProjectCodeUsagePaths(basePath, relevantEvents)

        val projectLangEvents = relevantEvents.filter { event ->
            val path = normalizePath(event.path)
            MinecraftEditorFileScope.isProjectOwned(basePath, path) && MinecraftProjectFileRules.isLangFile(path)
        }
        val projectCodeEvents = relevantEvents.filter { event ->
            val path = normalizePath(event.path)
            MinecraftEditorFileScope.isProjectOwned(basePath, path) && MinecraftCodeUsageFileScope.isCandidate(path)
        }
        val dependencyLangChanged = relevantEvents.any { event ->
            val path = normalizePath(event.path)
            !MinecraftEditorFileScope.isProjectOwned(basePath, path) &&
                isDependencyLangFile(fileIndex, path, event.file)
        }
        val versionChanged = relevantEvents.any { event ->
            val path = normalizePath(event.path)
            (MinecraftEditorFileScope.isProjectOwned(basePath, path) &&
                MinecraftVersionSignalFiles.isVersionSignalFile(path)) ||
                eventOldPath(event)?.let { oldPath ->
                    MinecraftEditorFileScope.isProjectOwned(basePath, oldPath) &&
                        MinecraftVersionSignalFiles.isVersionSignalFile(oldPath)
                } == true
        }

        val changedKeys = linkedSetOf<String>()
        staleProjectLangPaths.forEach { path ->
            langSourceService.removeProjectFile(path)
            changedKeys += langService.removeProjectFile(path)
        }
        staleProjectCodePaths.forEach(codeUsageService::removeProjectFile)

        if (projectLangEvents.isNotEmpty()) {
            val changedFiles = projectLangEvents.mapNotNull { it.file }
            if (changedFiles.size == projectLangEvents.size) {
                langSourceService.refreshChangedFiles(changedFiles.asSequence())
                changedKeys += langService.refreshChangedFiles(changedFiles.asSequence())
            } else {
                langService.refreshProjectResources()
                langSourceService.refreshProjectResources()
                changedKeys.clear()
            }
        }

        if (projectCodeEvents.isNotEmpty()) {
            val changedFiles = projectCodeEvents.mapNotNull { it.file }
            if (changedFiles.size == projectCodeEvents.size) {
                codeUsageService.refreshChangedFiles(changedFiles.asSequence(), maxEnumeratedKeys)
            } else {
                codeUsageService.refreshProjectResources(maxEnumeratedKeys)
            }
        }

        if (projectLangEvents.isNotEmpty() || staleProjectLangPaths.isNotEmpty()) {
            val affectedDocuments = project.service<MinecraftProjectRefreshCoordinator>()
                .affectedDocuments(changedKeys)
            if (affectedDocuments.isEmpty()) {
                appService.refreshProject(project)
            } else {
                appService.refreshDocuments(project, affectedDocuments)
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

    private fun normalizePath(path: String): String = path.replace('\\', '/')

    private fun eventOldPath(event: VFileEvent): String? {
        return when (event) {
            is com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent -> normalizePath(event.oldPath)
            is com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent -> normalizePath(event.oldPath)
            else -> null
        }
    }

    private fun isDependencyLangFile(
        fileIndex: ProjectFileIndex,
        path: String,
        file: VirtualFile?
    ): Boolean {
        if (!MinecraftProjectFileRules.isLangFile(path)) {
            return false
        }

        if (file == null) {
            return true
        }

        return fileIndex.isInLibraryClasses(file) || fileIndex.isInLibrarySource(file)
    }
}
