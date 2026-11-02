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
package net.raphimc.viaproxy.ui.impl

import net.lenni0451.commons.swing.GBC
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.UITab
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.elements.LinkLabel
import net.raphimc.viaproxy.util.JarUtil
import net.raphimc.viaproxy.util.logging.Logger
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING
private const val BODY_BLOCK_PADDING = ViaProxyWindow.BODY_BLOCK_PADDING

class UISettingsTab(frame: ViaProxyWindow) : UITab(frame, "ui_settings") {

    override fun init(contentPane: JPanel) {
        val body = JPanel()
        body.layout = GridBagLayout()

        var gridy = 0
        val languageLabel = JLabel(I18n.get("tab.ui_settings.language.label"))
        GBC.create(body).grid(0, gridy).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).anchor(GBC.NORTHWEST).add(languageLabel)
        gridy++

        val language = JComboBox<String>(I18n.getAvailableLocales().toTypedArray())
        language.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                var renderValue = value
                if (renderValue is String) {
                    renderValue = "<html><b>" + I18n.getSpecific(renderValue, "language.name") + "</b> (" + I18n.get("tab.ui_settings.language.completion", I18n.getSpecific(renderValue, "language.completion")) + ")</html>"
                }
                return super.getListCellRendererComponent(list, renderValue, index, isSelected, cellHasFocus)
            }
        }
        language.selectedItem = I18n.getCurrentLocale()
        language.addActionListener { _ ->
            val locale = language.selectedItem
            if (locale !is String) return@addActionListener
            if (locale == I18n.getCurrentLocale()) return@addActionListener
            I18n.setLocale(locale)
            ViaProxyWindow.showInfo(I18n.get("tab.ui_settings.language.success", I18n.get("language.name"), locale))
            try {
                JarUtil.launch(JarUtil.getJarFile().orElseThrow())
                System.exit(0)
            } catch (e: Throwable) {
                Logger.LOGGER.error("Could not start the ViaProxy jar", e)
                ViaProxyWindow.showException(e)
                System.exit(1)
            }
        }
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(language)
        gridy++
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(JLabel("<html>" + I18n.get("tab.ui_settings.crowdin.info") + "</html>"))
        gridy++
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(LinkLabel(I18n.get("tab.ui_settings.crowdin.link"), "https://crowdin.com/project/viaproxy"))

        contentPane.layout = BorderLayout()
        contentPane.add(body, BorderLayout.NORTH)
    }

}
