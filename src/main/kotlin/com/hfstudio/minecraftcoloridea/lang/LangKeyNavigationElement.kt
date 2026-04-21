package com.hfstudio.minecraftcoloridea.lang

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import javax.swing.Icon

class LangKeyNavigationElement(
    private val psiFile: PsiFile,
    private val lineIndex: Int,
    private val matchedKey: String,
    val matchType: MatchType
) : PsiElement {

    override fun getProject(): Project = psiFile.project
    override fun getLanguage() = psiFile.language
    override fun getManager(): PsiManager = psiFile.manager ?: PsiManager.getInstance(project)

    override fun getText(): String {
        if (!isValid) return "<invalid>"
        return when (matchType) {
            MatchType.EXACT -> psiFile.name
            else -> "${psiFile.name} ($matchedKey - ${matchType.name.lowercase()})"
        }
    }

    override fun getTextRange(): TextRange? = null
    override fun getStartOffsetInParent(): Int = 0
    override fun getTextLength(): Int = text.length
    override fun getParent(): PsiElement? = psiFile
    override fun getChildren(): Array<PsiElement> = PsiElement.EMPTY_ARRAY
    override fun getFirstChild(): PsiElement? = null
    override fun getLastChild(): PsiElement? = null
    override fun getNextSibling(): PsiElement? = null
    override fun getPrevSibling(): PsiElement? = null
    override fun getContainingFile(): PsiFile = psiFile

    override fun getNavigationElement(): PsiElement {
        if (!isValid) return this
        val virtualFile = psiFile.virtualFile ?: return this
        val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
        if (document != null && lineIndex < document.lineCount) {
            val lineStartOffset = document.getLineStartOffset(lineIndex)
            ApplicationManager.getApplication().invokeLater {
                OpenFileDescriptor(psiFile.project, virtualFile, lineStartOffset).navigate(true)
            }
        }
        return this
    }

    override fun getOriginalElement(): PsiElement = this
    override fun toString(): String = if (isValid) "${psiFile.name}:${lineIndex + 1} ($matchedKey)" else "<invalid>"
    override fun copy(): PsiElement = this
    override fun accept(visitor: PsiElementVisitor) {}
    override fun acceptChildren(visitor: PsiElementVisitor) {}
    override fun replace(newElement: PsiElement): PsiElement = this
    override fun add(element: PsiElement): PsiElement = this
    override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement = this
    override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement = this
    override fun addRangeBefore(p0: PsiElement, p1: PsiElement, p2: PsiElement?): PsiElement = this
    override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement = this
    @Deprecated("Deprecated in Java")
    override fun checkAdd(element: PsiElement) {}
    override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement = this
    override fun delete() {}
    @Deprecated("Deprecated in Java")
    override fun checkDelete() {}
    override fun deleteChildRange(first: PsiElement, last: PsiElement) {}
    override fun isWritable(): Boolean = false
    override fun isPhysical(): Boolean = false

    override fun isValid(): Boolean {
        return psiFile.isValid && lineIndex >= 0
    }

    override fun findElementAt(offset: Int): PsiElement? = null
    override fun findReferenceAt(offset: Int): PsiReference? = null
    override fun getTextOffset(): Int = 0
    override fun textToCharArray(): CharArray = text.toCharArray()
    override fun textMatches(text: CharSequence): Boolean = this.text == text.toString()
    override fun textMatches(element: PsiElement): Boolean = this.text == element.text
    override fun textContains(c: Char): Boolean = text.contains(c)
    override fun getReference(): PsiReference? = null
    override fun getReferences(): Array<PsiReference> = PsiReference.EMPTY_ARRAY
    override fun <T : Any?> getCopyableUserData(key: Key<T>): T? = null
    override fun <T : Any?> putCopyableUserData(key: Key<T>, value: T?) {}
    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean = false
    override fun getContext(): PsiElement? = psiFile
    override fun getResolveScope(): GlobalSearchScope = psiFile.resolveScope
    override fun getUseScope(): SearchScope = psiFile.useScope
    override fun getNode(): ASTNode? = null
    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is LangKeyNavigationElement) {
            return psiFile == another.psiFile && lineIndex == another.lineIndex
        }
        return false
    }
    override fun <T : Any?> getUserData(key: Key<T>): T? = null
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {}
    override fun getIcon(flags: Int): Icon? = psiFile.getIcon(flags)
}