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
package net.raphimc.viaproxy.ui.elements

import net.raphimc.viaproxy.ui.ViaProxyWindow
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel

class LinkLabel(text: String, url: String) : JLabel("<html><a href=\"\">" + text + "</a></html>") {

    init {
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                ViaProxyWindow.openURL(url)
            }
        })
        this.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

}
