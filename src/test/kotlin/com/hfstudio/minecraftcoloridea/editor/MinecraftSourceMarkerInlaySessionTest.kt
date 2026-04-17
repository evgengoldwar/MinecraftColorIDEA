package com.hfstudio.minecraftcoloridea.editor

import java.awt.Dimension
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftSourceMarkerInlaySessionTest {
    @Test
    fun hexColorPickerPopupMinSizeUsesExpandedMinimumWidth() {
        val size = MinecraftSourceMarkerInlaySession.hexColorPickerPopupMinSize(Dimension(320, 260))

        assertEquals(MinecraftSourceMarkerInlaySession.HEX_COLOR_PICKER_MIN_WIDTH, size.width)
        assertEquals(260, size.height)
    }

    @Test
    fun hexColorPickerPopupMinSizeKeepsLargerExistingWidth() {
        val size = MinecraftSourceMarkerInlaySession.hexColorPickerPopupMinSize(Dimension(520, 260))

        assertEquals(520, size.width)
        assertEquals(260, size.height)
    }
}
