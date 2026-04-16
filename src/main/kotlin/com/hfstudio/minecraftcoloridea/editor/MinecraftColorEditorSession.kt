package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.ColorMarkerInfo
import com.hfstudio.minecraftcoloridea.core.FormattingState
import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftHighlightEngine
import com.hfstudio.minecraftcoloridea.core.MinecraftMarker
import com.hfstudio.minecraftcoloridea.core.ResolvedHighlightSpan
import com.hfstudio.minecraftcoloridea.lang.MinecraftCollectedPreview
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.lang.MinecraftPreviewCollector
import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.ColorUtil
import com.intellij.util.Alarm
import java.awt.Color
import java.awt.Font

class MinecraftColorEditorSession(
    private val editor: Editor,
    private val settings: MinecraftColorSettingsState,
    private val engine: MinecraftHighlightEngine
) : Disposable {
    private data class Snapshot(
        val text: String,
        val languageId: String?,
        val modificationStamp: Long,
        val filePath: String?
    )

    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val highlighters = mutableListOf<RangeHighlighter>()
    private val previewSession = MinecraftPreviewInlaySession(editor)
    private val sourceMarkerSession = MinecraftSourceMarkerInlaySession(editor)

    private var disposed = false
    private var lastModificationStamp = Long.MIN_VALUE
    private var lastFingerprint = ""

    fun scheduleRefresh() {
        if (disposed) {
            return
        }

        alarm.cancelAllRequests()
        alarm.addRequest({ refreshNow() }, 30)
    }

    private fun refreshNow() {
        if (disposed || editor.isDisposed) {
            return
        }

        val project = editor.project ?: return
        val langService = project.service<MinecraftLangIndexService>()
        val versionCache = project.service<MinecraftVersionDetectionCache>()
        if (langService.langIndexStamp() == 0L) {
            langService.refreshProjectResources()
        }

        val config = effectiveConfig(
            project = project,
            baseConfig = settings.toConfig(),
            versionCache = versionCache
        )
        val snapshot = createSnapshot(project) ?: return
        val fingerprint = listOf(
            config.hashCode(),
            snapshot.languageId.orEmpty(),
            langService.langIndexStamp(),
            versionCache.stamp()
        ).joinToString("|")

        if (!config.enable) {
            applyDecorations(
                spans = emptyList(),
                previews = emptyList(),
                sourceMarkers = emptyList(),
                referencedKeys = emptySet(),
                modificationStamp = snapshot.modificationStamp,
                fingerprint = fingerprint,
                config = config
            )
            return
        }

        if (snapshot.modificationStamp == lastModificationStamp && fingerprint == lastFingerprint) {
            return
        }

        val spans = engine.highlight(
            text = snapshot.text,
            languageId = snapshot.languageId,
            config = config
        )

        val previews = resolvePreviews(
            snapshot = snapshot,
            config = config,
            langService = langService
        )
        val sourceMarkers = MinecraftSourceMarkerCollector().collect(
            text = snapshot.text,
            languageId = snapshot.languageId,
            config = config
        )
        applyDecorations(
            spans = spans,
            previews = previews,
            sourceMarkers = sourceMarkers,
            referencedKeys = previews.flatMapTo(linkedSetOf()) { it.referencedKeys },
            modificationStamp = snapshot.modificationStamp,
            fingerprint = fingerprint,
            config = config
        )
    }

    private fun createSnapshot(project: Project): Snapshot? {
        return ReadAction.compute<Snapshot?, RuntimeException> {
            if (disposed || editor.isDisposed) {
                return@compute null
            }

            val document = editor.document
            val file = FileDocumentManager.getInstance().getFile(document)
            val languageId = file
                ?.let { PsiManager.getInstance(project).findFile(it)?.language?.id }
                ?: file?.fileType?.name

            Snapshot(
                text = document.immutableCharSequence.toString(),
                languageId = languageId,
                modificationStamp = document.modificationStamp,
                filePath = file?.path
            )
        }
    }

    private fun applyDecorations(
        spans: List<ResolvedHighlightSpan>,
        previews: List<MinecraftResolvedPreview>,
        sourceMarkers: List<MinecraftSourceMarker>,
        referencedKeys: Set<String>,
        modificationStamp: Long,
        fingerprint: String,
        config: MinecraftColorConfig
    ) {
        ApplicationManager.getApplication().invokeLater {
            if (disposed || editor.isDisposed) {
                return@invokeLater
            }

            if (editor.document.modificationStamp != modificationStamp) {
                return@invokeLater
            }

            clearHighlighters()
            val markupModel = editor.markupModel
            for (span in spans) {
                val highlighter = markupModel.addRangeHighlighter(
                    span.start,
                    span.end,
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    createTextAttributes(span),
                    HighlighterTargetArea.EXACT_RANGE
                )
                highlighters += highlighter
            }
            previewSession.replace(previews)
            sourceMarkerSession.replace(sourceMarkers, config)
            editor.project?.service<MinecraftProjectRefreshCoordinator>()
                ?.updateDependencies(editor.document, referencedKeys)

            lastModificationStamp = modificationStamp
            lastFingerprint = fingerprint
        }
    }

    private fun effectiveConfig(
        project: Project,
        baseConfig: MinecraftColorConfig,
        versionCache: MinecraftVersionDetectionCache
    ): MinecraftColorConfig {
        val detectedVersionId = versionCache.getOrDetect()?.versionId
        val projectSettings = project.service<MinecraftColorProjectSettingsState>()
        return projectSettings.resolveEffectiveConfig(
            baseConfig = baseConfig,
            detectedVersionId = detectedVersionId
        )
    }

    private fun resolvePreviews(
        snapshot: Snapshot,
        config: MinecraftColorConfig,
        langService: MinecraftLangIndexService
    ): List<MinecraftResolvedPreview> {
        val normalizedPath = snapshot.filePath?.replace('\\', '/').orEmpty()
        if (normalizedPath.contains("/lang/")) {
            return emptyList()
        }

        val localeOrder = buildLocaleOrder(config)
        // Triggers dependency locale materialization lazily once and reuses it afterwards.
        langService.lookup("__minecraft_color_probe__", localeOrder)

        return MinecraftPreviewCollector(
            index = langService.currentIndex(),
            extraMethodNames = config.extraLocalizationMethods
        ).collect(
            text = snapshot.text,
            localeOrder = localeOrder
        ).map(MinecraftCollectedPreview::preview)
    }

    private fun buildLocaleOrder(config: MinecraftColorConfig): List<String> {
        return linkedSetOf<String>().apply {
            config.preferredLocale.trim().takeIf(String::isNotEmpty)?.lowercase()?.let(::add)
            config.secondaryLocale.trim().takeIf(String::isNotEmpty)?.lowercase()?.let(::add)
            add("en_us")
        }.toList()
    }

    private fun createTextAttributes(span: ResolvedHighlightSpan): TextAttributes {
        val attributes = TextAttributes()
        val baseColor = defaultForeground()

        applyColorMarker(attributes, span.colorMarker)

        val effectiveForeground = attributes.foregroundColor ?: baseColor
        applyFormatting(attributes, span.formatting, effectiveForeground)

        if (span.formatting.obfuscated) {
            attributes.foregroundColor = withAlpha(attributes.foregroundColor ?: baseColor, 0.75f)
        }

        return attributes
    }

    private fun applyColorMarker(attributes: TextAttributes, marker: ColorMarkerInfo?) {
        if (marker == null) {
            return
        }

        val color = ColorUtil.fromHex(marker.colorHex)
        when (marker.marker) {
            MinecraftMarker.FOREGROUND -> attributes.foregroundColor = color
            MinecraftMarker.BACKGROUND -> {
                attributes.backgroundColor = color
                attributes.foregroundColor = ColorUtil.fromHex(marker.contrastHex ?: "#FFFFFF")
            }
            MinecraftMarker.OUTLINE -> attributes.withAdditionalEffect(EffectType.ROUNDED_BOX, color)
            MinecraftMarker.UNDERLINE -> attributes.withAdditionalEffect(EffectType.LINE_UNDERSCORE, color)
        }
    }

    private fun applyFormatting(
        attributes: TextAttributes,
        formatting: FormattingState,
        effectColor: Color
    ) {
        var fontType = Font.PLAIN
        if (formatting.bold) {
            fontType = fontType or Font.BOLD
        }
        if (formatting.italic) {
            fontType = fontType or Font.ITALIC
        }
        attributes.fontType = fontType

        if (formatting.underline) {
            attributes.withAdditionalEffect(EffectType.LINE_UNDERSCORE, effectColor)
        }
        if (formatting.strikethrough) {
            attributes.withAdditionalEffect(EffectType.STRIKEOUT, effectColor)
        }
    }

    private fun defaultForeground(): Color {
        return editor.colorsScheme.defaultForeground ?: EditorColorsManager.getInstance().globalScheme.defaultForeground
    }

    private fun withAlpha(color: Color, alpha: Float): Color {
        val normalizedAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color(color.red, color.green, color.blue, normalizedAlpha)
    }

    private fun clearHighlighters() {
        highlighters.forEach(RangeHighlighter::dispose)
        highlighters.clear()
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        disposed = true
        alarm.cancelAllRequests()
        previewSession.clear()
        sourceMarkerSession.dispose()
        editor.project?.service<MinecraftProjectRefreshCoordinator>()
            ?.updateDependencies(editor.document, emptySet())
        clearHighlighters()
    }
}
