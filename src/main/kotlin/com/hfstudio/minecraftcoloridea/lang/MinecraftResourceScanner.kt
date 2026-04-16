package com.hfstudio.minecraftcoloridea.lang

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

object MinecraftResourceScanner {
    fun projectResources(project: Project): Sequence<VirtualFile> =
        scanRoots(
            listOfNotNull(
                project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it.replace('\\', '/')) }
            ).asSequence()
        )

    fun localDependencyResources(project: Project): Sequence<VirtualFile> =
        scanRoots(OrderEnumerator.orderEntries(project).recursively().classesRoots.asSequence())

    private fun scanRoots(roots: Sequence<VirtualFile>): Sequence<VirtualFile> = sequence {
        roots.forEach { root ->
            val matches = mutableListOf<VirtualFile>()
            VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                if (!file.isDirectory && file.path.contains("/assets/") && file.path.contains("/lang/")) {
                    matches += file
                }
                true
            }
            yieldAll(matches)
        }
    }
}
