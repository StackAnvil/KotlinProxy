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
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier
import net.raphimc.viabedrock.protocol.data.ProtocolConstants
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.UITab
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.popups.AddAccountPopup
import net.raphimc.viaproxy.util.TFunction
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING
private const val BODY_BLOCK_PADDING = ViaProxyWindow.BODY_BLOCK_PADDING

class AccountsTab(frame: ViaProxyWindow) : UITab(frame, "accounts") {

    private lateinit var accountsList: JList<Account>
    private lateinit var addMicrosoftAccountButton: JButton
    private lateinit var addBedrockAccountButton: JButton

    private var addAccountPopup: AddAccountPopup? = null
    private var addThread: Thread? = null

    override fun init(contentPane: JPanel) {
        val body = JPanel()
        body.layout = GridBagLayout()

        var gridy = 0
        val infoLabel = JLabel("<html><p>" + I18n.get("tab.accounts.description.line1") + "</p></html>")
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(infoLabel)
        gridy++

        val scrollPane = JScrollPane()
        val model = DefaultListModel<Account>()
        this.accountsList = JList(model)
        this.accountsList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = this@AccountsTab.accountsList.locationToIndex(e.point)
                    this@AccountsTab.accountsList.selectedIndex = row
                } else if (e.clickCount == 2) {
                    val index = this@AccountsTab.accountsList.selectedIndex
                    if (index != -1) this@AccountsTab.markSelected(index)
                }
            }
        })
        this.accountsList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val index = this@AccountsTab.accountsList.selectedIndex
                if (index == -1) return
                if (e.keyCode == KeyEvent.VK_UP) {
                    this@AccountsTab.moveUp(index)
                    e.consume()
                } else if (e.keyCode == KeyEvent.VK_DOWN) {
                    this@AccountsTab.moveDown(index)
                    e.consume()
                }
            }
        })
        this.accountsList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as DefaultListCellRenderer
                val account = value as Account
                if (ViaProxy.getConfig().getAccount() === account) {
                    component.text = "<html><span style=\"color:rgb(0, 180, 0)\"><b>" + account.getDisplayString() + "</b></span></html>"
                } else {
                    component.text = account.getDisplayString()
                }
                return component
            }
        }
        scrollPane.setViewportView(this.accountsList)
        val contextMenu = JPopupMenu()
        val selectItem = JMenuItem(I18n.get("tab.accounts.list.context_menu.select"))
        selectItem.addActionListener { _ ->
            val index = this.accountsList.selectedIndex
            if (index != -1) this.markSelected(index)
        }
        contextMenu.add(selectItem)
        val removeItem = JMenuItem(I18n.get("tab.accounts.list.context_menu.remove"))
        removeItem.addActionListener { _ ->
            val index = this.accountsList.selectedIndex
            if (index != -1) {
                val removed = model.remove(index)
                ViaProxy.getSaveManager().accountsSave.removeAccount(removed)
                ViaProxy.getSaveManager().save()
                if (ViaProxy.getConfig().getAccount() === removed) {
                    if (model.isEmpty()) this.markSelected(-1)
                    else this.markSelected(0)
                }
            }
            if (index < model.size) this.accountsList.selectedIndex = index
            else if (index > 0) this.accountsList.selectedIndex = index - 1
        }
        contextMenu.add(removeItem)
        val moveUp = JMenuItem(I18n.get("tab.accounts.list.context_menu.move_up"))
        moveUp.addActionListener { _ ->
            val index = this.accountsList.selectedIndex
            if (index != -1) this.moveUp(index)
        }
        contextMenu.add(moveUp)
        val moveDown = JMenuItem(I18n.get("tab.accounts.list.context_menu.move_down"))
        moveDown.addActionListener { _ ->
            val index = this.accountsList.selectedIndex
            if (index != -1) this.moveDown(index)
        }
        contextMenu.add(moveDown)
        this.accountsList.componentPopupMenu = contextMenu
        GBC.create(body).grid(0, gridy).weight(1.0, 1.0).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.BOTH).add(scrollPane)
        gridy++

        val addButtons = JPanel()
        addButtons.layout = GridLayout(1, 3, BORDER_PADDING, 0)
        val addOfflineAccountButton = JButton(I18n.get("tab.accounts.add_offline.label"))
        addOfflineAccountButton.addActionListener { _ ->
            val username = JOptionPane.showInputDialog(this.viaProxyWindow, I18n.get("tab.accounts.add_offline.enter_username"), I18n.get("tab.accounts.add.title"), JOptionPane.PLAIN_MESSAGE)
            if (username != null && !username.trim().isEmpty()) {
                val account = ViaProxy.getSaveManager().accountsSave.addAccount(username)
                ViaProxy.getSaveManager().save()
                this.addAccount(account)
            }
        }
        addButtons.add(addOfflineAccountButton)

        val addMicrosoftAccountButton = JButton(I18n.get("tab.accounts.add_microsoft.label"))
        addMicrosoftAccountButton.addActionListener { _ ->
            addMicrosoftAccountButton.isEnabled = false
            this.handleLogin { msaDeviceCodeConsumer ->
                MicrosoftAccount(JavaAuthManager.create(MinecraftAuth.createHttpClient()).login(ParamMsaAuthServiceSupplier<Consumer<MsaDeviceCode>> { httpClient, applicationConfig, consumer -> DeviceCodeMsaAuthService(httpClient, applicationConfig, consumer) }, msaDeviceCodeConsumer))
            }
        }
        this.addMicrosoftAccountButton = addMicrosoftAccountButton
        addButtons.add(addMicrosoftAccountButton)

        val addBedrockAccountButton = JButton(I18n.get("tab.accounts.add_bedrock.label"))
        addBedrockAccountButton.addActionListener { _ ->
            addBedrockAccountButton.isEnabled = false
            this.handleLogin { msaDeviceCodeConsumer ->
                BedrockAccount(BedrockAuthManager.create(MinecraftAuth.createHttpClient(), ProtocolConstants.BEDROCK_VERSION_NAME).login(ParamMsaAuthServiceSupplier<Consumer<MsaDeviceCode>> { httpClient, applicationConfig, consumer -> DeviceCodeMsaAuthService(httpClient, applicationConfig, consumer) }, msaDeviceCodeConsumer))
            }
        }
        this.addBedrockAccountButton = addBedrockAccountButton
        addButtons.add(addBedrockAccountButton)

        val border = JPanel()
        border.layout = GridBagLayout()
        border.border = BorderFactory.createTitledBorder(I18n.get("tab.accounts.add.title"))
        GBC.create(border).grid(0, 0).weightx(1.0).insets(2, 4, 4, 4).fill(GBC.HORIZONTAL).add(addButtons)

        GBC.create(body).grid(0, gridy).weightx(1.0).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(border)

        contentPane.layout = BorderLayout()
        contentPane.add(body, BorderLayout.CENTER)

        ViaProxy.getSaveManager().accountsSave.accounts.forEach { account -> this.addAccount(account) }
        val listModel = this.accountsList.getModel() as DefaultListModel<Account>
        if (!listModel.isEmpty()) this.markSelected(0)
    }

    private fun closePopup() { // Might be getting called multiple times
        val popup = this.addAccountPopup
        if (popup != null) {
            popup.markExternalClose()
            popup.isVisible = false
            popup.dispose()
            this.addAccountPopup = null
        }
        this.addMicrosoftAccountButton.isEnabled = true
        this.addBedrockAccountButton.isEnabled = true
    }

    private fun addAccount(account: Account) {
        val model = this.accountsList.getModel() as DefaultListModel<Account>
        model.addElement(account)
    }

    fun markSelected(index: Int) {
        if (index < 0 || index >= this.accountsList.getModel().getSize()) {
            ViaProxy.getConfig().setAccount(null)
            return
        }

        ViaProxy.getConfig().setAccount(ViaProxy.getSaveManager().accountsSave.accounts.get(index))
        this.accountsList.repaint()
    }

    private fun moveUp(index: Int) {
        val model = this.accountsList.getModel() as DefaultListModel<Account>
        if (model.getSize() == 0) return
        if (index == 0) return

        val account = model.remove(index)
        model.add(index - 1, account)
        this.accountsList.selectedIndex = index - 1

        ViaProxy.getSaveManager().accountsSave.removeAccount(account)
        ViaProxy.getSaveManager().accountsSave.addAccount(index - 1, account)
        ViaProxy.getSaveManager().save()
    }

    private fun moveDown(index: Int) {
        val model = this.accountsList.getModel() as DefaultListModel<Account>
        if (model.getSize() == 0) return
        if (index == model.getSize() - 1) return

        val account = model.remove(index)
        model.add(index + 1, account)
        this.accountsList.selectedIndex = index + 1

        ViaProxy.getSaveManager().accountsSave.removeAccount(account)
        ViaProxy.getSaveManager().accountsSave.addAccount(index + 1, account)
        ViaProxy.getSaveManager().save()
    }

    private fun handleLogin(requestHandler: TFunction<Consumer<MsaDeviceCode>, Account>) {
        this.addThread = Thread({
            try {
                val account = requestHandler.apply(Consumer<MsaDeviceCode> { msaDeviceCode ->
                    SwingUtilities.invokeLater {
                        AddAccountPopup(this.viaProxyWindow, msaDeviceCode, Consumer<AddAccountPopup> { popup ->
                            this.addAccountPopup = popup
                        }, Runnable {
                            this.closePopup()
                            this.addThread!!.interrupt()
                        })
                    }
                })
                SwingUtilities.invokeLater {
                    this.closePopup()
                    ViaProxy.getSaveManager().accountsSave.addAccount(account)
                    ViaProxy.getSaveManager().save()
                    this.addAccount(account)
                    ViaProxyWindow.showInfo(I18n.get("tab.accounts.add.success", account.getName()))
                }
            } catch (ignored: InterruptedException) {
            } catch (e: TimeoutException) {
                SwingUtilities.invokeLater {
                    this.closePopup()
                    ViaProxyWindow.showError(I18n.get("tab.accounts.add.timeout", "300"))
                }
            } catch (t: Throwable) {
                if (t.cause !is InterruptedException) {
                    SwingUtilities.invokeLater {
                        this.closePopup()
                        ViaProxyWindow.showException(t)
                    }
                }
            }
        }, "Add Account Thread")
        this.addThread!!.isDaemon = true
        this.addThread!!.start()
    }

}
