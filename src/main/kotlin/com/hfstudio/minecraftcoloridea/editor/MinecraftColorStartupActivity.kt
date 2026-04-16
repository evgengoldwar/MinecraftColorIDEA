package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MinecraftColorStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<MinecraftVersionDetectionCache>().getOrDetect()
        project.service<MinecraftLangIndexService>().refreshProjectResources()
        service<MinecraftColorApplicationService>().refreshProject(project)
    }
}
