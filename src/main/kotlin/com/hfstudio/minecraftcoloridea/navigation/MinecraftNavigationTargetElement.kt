package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import javax.swing.Icon

internal class MinecraftNavigationTargetElement(
    private val project: Project,
    private val filePath: String,
    private val targetOffset: Int,
    private val presentationData: MinecraftNavigationPresentation
) : FakePsiElement() {
    override fun getName(): String = presentationData.locationText

    override fun getParent(): PsiElement? = null

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String = presentationData.locationText

            override fun getLocationString(): String = presentationData.fileNameText

            override fun getIcon(unused: Boolean): Icon? = null
        }
    }

    override fun canNavigate(): Boolean {
        return LocalFileSystem.getInstance().findFileByPath(filePath.replace('\\', '/')) != null
    }

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun navigate(requestFocus: Boolean) {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath.replace('\\', '/')) ?: return
        OpenFileDescriptor(project, file, targetOffset.coerceAtLeast(0)).navigate(requestFocus)
    }

    override fun toString(): String = presentationData.locationText
}
