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
package net.raphimc.viaproxy.ui.popups

import net.lenni0451.commons.swing.GBC
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.elements.LinkLabel
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.function.Consumer
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.WindowConstants

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING
private const val BODY_BLOCK_PADDING = ViaProxyWindow.BODY_BLOCK_PADDING

class AddAccountPopup(
    private val parent: ViaProxyWindow,
    private val msaDeviceCode: MsaDeviceCode,
    popupConsumer: Consumer<AddAccountPopup>,
    closeListener: Runnable
) : JDialog(parent, true) {

    private var externalClose = false

    init {
        popupConsumer.accept(this)

        this.initWindow(closeListener)
        this.initComponents()
        this.pack()
        this.isVisible = true
    }

    private fun initWindow(closeListener: Runnable) {
        this.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                if (!this@AddAccountPopup.externalClose) closeListener.run()
            }
        })
        this.title = I18n.get("popup.login_account.title")
        this.setSize(400, 140)
        this.isResizable = false
        this.setLocationRelativeTo(this.parent)
    }

    private fun initComponents() {
        val contentPane = JPanel()
        contentPane.layout = GridBagLayout()
        val browserLabel = JLabel("<html><p>" + I18n.get("popup.login_account.instructions.browser") + "</p></html>")
        GBC.create(contentPane).grid(0, 0).weightx(1.0).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(browserLabel)

        GBC.create(contentPane).grid(0, 1).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(LinkLabel(this.msaDeviceCode.getDirectVerificationUri(), this.msaDeviceCode.getDirectVerificationUri()))

        val closeInfo = JLabel("<html><p>" + I18n.get("popup.login_account.instructions.close") + "</p></html>")
        GBC.create(contentPane).grid(0, 2).weightx(1.0).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(closeInfo)
        this.contentPane = contentPane
    }

    fun markExternalClose() {
        this.externalClose = true
    }

}
