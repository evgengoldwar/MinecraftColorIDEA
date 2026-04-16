package com.hfstudio.minecraftcoloridea.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "MinecraftColorProjectSettings", storages = [Storage("minecraft-color-project.xml")])
class MinecraftColorProjectSettingsState : PersistentStateComponent<MinecraftColorProjectSettingsState.StoredState> {
    class StoredState {
        var projectJavaVersionOverride: String? = null
    }

    private var storedState = StoredState()

    override fun getState(): StoredState = storedState

    override fun loadState(state: StoredState) {
        this.storedState = state
    }

    fun projectJavaVersionOverride(): String? {
        return storedState.projectJavaVersionOverride
            ?.trim()
            ?.ifEmpty { null }
    }

    fun setProjectJavaVersionOverride(value: String?) {
        storedState.projectJavaVersionOverride = value
            ?.trim()
            ?.ifEmpty { null }
    }

    fun resolveEffectiveJavaVersionId(
        detectedVersionId: String?,
        globalDefaultVersionId: String,
        builtInDefaultVersionId: String = "1.20.1"
    ): String {
        return projectJavaVersionOverride()
            ?: detectedVersionId?.trim()?.ifEmpty { null }
            ?: globalDefaultVersionId.trim().ifEmpty { builtInDefaultVersionId }
    }
}
