/*
 * This file is part of KotlinProxy - https://github.com/StackAnvil/KotlinProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.cli

object ConsoleFormatter {

    private const val PREFIX = "\u001B["
    private const val SUFFIX = "m"

    @JvmStatic
    fun convert(s: String): String {
        val out = StringBuilder()
        val chars = s.toCharArray()
        var i = 0
        while (i < chars.size) {
            val c = chars[i]
            val next = if (i + 1 < chars.size) chars[i + 1] else '\u0000'
            if (c == '§') {
                if (next != '\u0000') {
                    if (isColor(next)) out.append(convertToAnsi('r'))
                    out.append(convertToAnsi(next))
                    i++
                }
            } else {
                out.append(c)
            }
            i++
        }
        return out.toString() + convertToAnsi('r')
    }

    //The ANSI codes are an approximation. True color is used to get the exact color.
    private fun convertToAnsi(color: Char): String {
        return when (color.lowercaseChar()) {
            '0' -> PREFIX + getColor("30", 0x00_00_00) + SUFFIX //Black
            '1' -> PREFIX + getColor("34", 0x00_00_AA) + SUFFIX //Dark Blue
            '2' -> PREFIX + getColor("32", 0x00_AA_00) + SUFFIX //Dark Green
            '3' -> PREFIX + getColor("36", 0x00_AA_AA) + SUFFIX //Dark Aqua
            '4' -> PREFIX + getColor("31", 0xAA_00_00) + SUFFIX //Dark Red
            '5' -> PREFIX + getColor("35", 0xAA_00_AA) + SUFFIX //Dark Purple
            '6' -> PREFIX + getColor("33", 0xFF_AA_00) + SUFFIX //Gold
            '7' -> PREFIX + getColor("37", 0xAA_AA_AA) + SUFFIX //Gray
            '8' -> PREFIX + getColor("90", 0x55_55_55) + SUFFIX //Dark Gray
            '9' -> PREFIX + getColor("94", 0x55_55_FF) + SUFFIX //Blue
            'a' -> PREFIX + getColor("92", 0x55_FF_55) + SUFFIX //Green
            'b' -> PREFIX + getColor("96", 0x55_FF_FF) + SUFFIX //Aqua
            'c' -> PREFIX + getColor("91", 0xFF_55_55) + SUFFIX //Red
            'd' -> PREFIX + getColor("95", 0xFF_55_FF) + SUFFIX //Light Purple
            'e' -> PREFIX + getColor("93", 0xFF_FF_55) + SUFFIX //Yellow
            'f' -> PREFIX + getColor("97", 0xFF_FF_FF) + SUFFIX //White
            'k' -> "" //Obfuscated, not supported in terminal
            'l' -> PREFIX + "1" + SUFFIX //Bold
            'm' -> PREFIX + "9" + SUFFIX //Strikethrough
            'n' -> PREFIX + "4" + SUFFIX //Underline
            'o' -> PREFIX + "3" + SUFFIX //Italic
            'r' -> PREFIX + 0 + SUFFIX //Reset
            else -> PREFIX + 0 + SUFFIX
        }
    }

    private fun isColor(color: Char): Boolean {
        val lower = color.lowercaseChar()
        return lower in '0'..'9' || lower in 'a'..'f'
    }

    private fun getColor(ansi: String, rgb: Int): String {
        return String.format("38;2;%d;%d;%d", (rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255)
    }

}
