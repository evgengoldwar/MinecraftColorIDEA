package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceIndexService
import com.hfstudio.minecraftcoloridea.navigation.MinecraftCodeUsageIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MinecraftColorStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val baseConfig = service<MinecraftColorSettingsState>().toConfig()
        val maxEnumeratedKeys = project.service<MinecraftColorProjectSettingsState>()
            .resolveMaxEnumeratedKeys(baseConfig.maxEnumeratedKeys)
        project.service<MinecraftVersionDetectionCache>().getOrDetect()
        project.service<MinecraftLangIndexService>().refreshProjectResources()
        project.service<MinecraftLangSourceIndexService>().refreshProjectResources()
        project.service<MinecraftCodeUsageIndexService>().refreshProjectResources(maxEnumeratedKeys)
        service<MinecraftColorApplicationService>().refreshProject(project)
    }
}
