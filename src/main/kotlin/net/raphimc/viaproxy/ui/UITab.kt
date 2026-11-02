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
package net.raphimc.viaproxy.ui

import javax.swing.JPanel
import javax.swing.JTabbedPane

abstract class UITab(
    @JvmField protected val viaProxyWindow: ViaProxyWindow,
    tabName: String
) {

    val name: String = I18n.get("tab.$tabName.name")

    @JvmField
    protected val contentPane: JPanel = JPanel()

    init {
        this.contentPane.layout = null
        this.init(this.contentPane)
    }

    fun add(tabbedPane: JTabbedPane) {
        tabbedPane.addTab(this.name, this.contentPane)
    }

    protected abstract fun init(contentPane: JPanel)

    open fun onTabOpened() {
    }

}
