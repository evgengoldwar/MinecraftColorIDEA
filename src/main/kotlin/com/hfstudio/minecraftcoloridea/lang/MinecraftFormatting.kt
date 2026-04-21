package com.hfstudio.minecraftcoloridea.lang

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

enum class MinecraftFormatting(
    val code: String,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val displayName: String
) {
    BLACK("0", JBColor(Color(0, 0, 0, 150), Color(40, 40, 40, 150)), JBColor.WHITE, "Black"),
    DARK_BLUE("1", JBColor(Color(0, 0, 170, 150), Color(0, 0, 130, 150)), JBColor.WHITE, "Dark Blue"),
    DARK_GREEN("2", JBColor(Color(0, 170, 0, 150), Color(0, 130, 0, 150)), JBColor.WHITE, "Dark Green"),
    DARK_AQUA("3", JBColor(Color(0, 170, 170, 150), Color(0, 130, 130, 150)), JBColor.WHITE, "Dark Aqua"),
    DARK_RED("4", JBColor(Color(170, 0, 0, 150), Color(130, 0, 0, 150)), JBColor.WHITE, "Dark Red"),
    DARK_PURPLE("5", JBColor(Color(170, 0, 170, 150), Color(130, 0, 130, 150)), JBColor.WHITE, "Dark Purple"),
    GOLD("6", JBColor(Color(255, 170, 0, 150), Color(200, 130, 0, 150)), JBColor.BLACK, "Gold"),
    GRAY("7", JBColor(Color(170, 170, 170, 150), Color(130, 130, 130, 150)), JBColor.BLACK, "Gray"),
    DARK_GRAY("8", JBColor(Color(85, 85, 85, 150), Color(65, 65, 65, 150)), JBColor.WHITE, "Dark Gray"),
    BLUE("9", JBColor(Color(85, 85, 255, 150), Color(65, 65, 200, 150)), JBColor.WHITE, "Blue"),
    GREEN("a", JBColor(Color(85, 255, 85, 150), Color(65, 200, 65, 150)), JBColor.BLACK, "Green"),
    AQUA("b", JBColor(Color(85, 255, 255, 150), Color(65, 200, 200, 150)), JBColor.BLACK, "Aqua"),
    RED("c", JBColor(Color(255, 85, 85, 150), Color(200, 65, 65, 150)), JBColor.BLACK, "Red"),
    LIGHT_PURPLE("d", JBColor(Color(255, 85, 255, 150), Color(200, 65, 200, 150)), JBColor.BLACK, "Light Purple"),
    YELLOW("e", JBColor(Color(255, 255, 85, 150), Color(200, 200, 65, 150)), JBColor.BLACK, "Yellow"),
    WHITE("f", JBColor(Color(255, 255, 255, 150), Color(230, 230, 230, 150)), JBColor.BLACK, "White"),

    OBFUSCATED("k", JBColor(Color(50, 50, 50, 150), Color(70, 70, 70, 150)),
        JBColor(Gray._200, Gray._180), "Obfuscated"),
    BOLD("l", JBColor(Color(240, 240, 240, 150), Color(220, 220, 220, 150)),
        JBColor(Gray._30, Gray._50), "Bold"),
    STRIKETHROUGH("m", JBColor(Color(255, 200, 200, 150), Color(220, 170, 170, 150)),
        JBColor(Color(100, 0, 0), Color(130, 30, 30)), "Strikethrough"),
    UNDERLINE("n", JBColor(Color(200, 220, 255, 150), Color(170, 190, 220, 150)),
        JBColor(Color(0, 0, 150), Color(30, 30, 180)), "Underline"),
    ITALIC("o", JBColor(Color(255, 255, 220, 150), Color(220, 220, 190, 150)),
        JBColor(Color(100, 100, 0), Color(130, 130, 30)), "Italic"),
    RESET("r", JBColor(Color(200, 200, 200, 150), Color(170, 170, 170, 150)), JBColor.BLACK, "Reset");

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        fun fromCode(code: String): MinecraftFormatting? {
            return BY_CODE[code.lowercase()]
        }

        fun getAllForAutoComplete(): Map<String, String> {
            return entries.associate { format ->
                format.displayName.lowercase().replace(" ", "_") to "§${format.code}"
            }
        }
    }
}