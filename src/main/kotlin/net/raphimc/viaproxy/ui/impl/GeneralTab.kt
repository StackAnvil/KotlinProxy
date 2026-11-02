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

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.lenni0451.commons.swing.GBC
import net.lenni0451.lambdaevents.EventHandler
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig
import net.raphimc.viaproxy.saves.impl.accounts.ClassicAccount
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.UITab
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.elements.LinkLabel
import net.raphimc.viaproxy.ui.events.UICloseEvent
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.Proxy
import net.raphimc.viaproxy.util.logging.Logger
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.Timer

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING
private const val BODY_BLOCK_PADDING = ViaProxyWindow.BODY_BLOCK_PADDING

class GeneralTab(frame: ViaProxyWindow) : UITab(frame, "general") {

    @JvmField
    var serverAddress: JTextField? = null

    @JvmField
    var serverVersion: JComboBox<ProtocolVersion>? = null

    @JvmField
    var authMethod: JComboBox<ViaProxyConfig.AuthMethod>? = null

    @JvmField
    var betaCraftAuth: JCheckBox? = null

    @JvmField
    var stateLabel: JLabel? = null

    @JvmField
    var stateButton: JButton? = null

    override fun init(contentPane: JPanel) {
        // The UI is built in the initializer below, after the component fields are initialized.
    }

    init {
        this.build(this.contentPane)
    }

    private fun build(contentPane: JPanel) {
        val top = JPanel()
        top.layout = BorderLayout()

        contentPane.layout = BorderLayout()
        contentPane.add(top, BorderLayout.NORTH)

        this.addHeader(top)
        this.addBody(top)
        this.addFooter(contentPane)
    }

    private fun addHeader(parent: Container) {
        val header = JPanel()
        header.layout = GridBagLayout()

        val discord = LinkLabel("Discord", "https://discord.gg/viaversion")
        GBC.create(header).grid(0, 0).width(0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(discord)

        val title = JLabel("ViaProxy")
        title.font = title.font.deriveFont(30F)
        GBC.create(header).grid(1, 0).weightx(1.0).width(0).insets(BORDER_PADDING, 0, 0, 0).anchor(GBC.CENTER).add(title)

        val copyright = JLabel("© RK_01 & Lenni0451")
        GBC.create(header).grid(2, 0).width(0).insets(BORDER_PADDING, 0, 0, BORDER_PADDING).anchor(GBC.NORTHEAST).add(copyright)

        parent.add(header, BorderLayout.NORTH)
    }

    private fun addBody(parent: Container) {
        val body = JPanel()
        body.layout = GridBagLayout()

        var gridy = 0
        val serverAddressLabel = JLabel(I18n.get("tab.general.server_address.label"))
        serverAddressLabel.toolTipText = I18n.get("tab.general.server_address.tooltip")
        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverAddressLabel)
        gridy++

        val serverAddress = JTextField()
        serverAddress.toolTipText = I18n.get("tab.general.server_address.tooltip")
        ViaProxy.getSaveManager().uiSave.loadTextField("server_address", serverAddress)
        this.serverAddress = serverAddress
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(serverAddress)
        gridy++

        val serverVersionLabel = JLabel(I18n.get("tab.general.server_version.label"))
        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverVersionLabel)
        gridy++

        val serverVersion = JComboBox<ProtocolVersion>(ProtocolVersion.getReversedProtocols().toTypedArray())
        serverVersion.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                var renderValue = value
                if (renderValue is ProtocolVersion) {
                    renderValue = renderValue.getName()
                }
                return super.getListCellRendererComponent(list, renderValue, index, isSelected, cellHasFocus)
            }
        }
        this.serverVersion = serverVersion
        serverVersion.addActionListener { _ ->
            if (this.betaCraftAuth == null) return@addActionListener // This is called when the JComboBox is created (before betaCraftAuth is set)
            val selectedVersion = this.serverVersion!!.selectedItem
            if (selectedVersion !is ProtocolVersion) return@addActionListener
            val betaCraftAuth = this.betaCraftAuth!!
            if (selectedVersion.olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                betaCraftAuth.isEnabled = true
            } else {
                betaCraftAuth.isEnabled = false
                betaCraftAuth.isSelected = false
            }
        }
        serverVersion.selectedItem = ViaProxy.getConfig().getTargetVersion()
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(serverVersion)
        gridy++

        val minecraftAccountLabel = JLabel(I18n.get("tab.general.minecraft_account.label"))
        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(minecraftAccountLabel)
        gridy++

        val authMethod = JComboBox<ViaProxyConfig.AuthMethod>(ViaProxyConfig.AuthMethod.values())
        authMethod.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                var renderValue = value
                if (renderValue is ViaProxyConfig.AuthMethod) {
                    renderValue = I18n.get(renderValue.getGuiTranslationKey())
                }
                return super.getListCellRendererComponent(list, renderValue, index, isSelected, cellHasFocus)
            }
        }
        this.authMethod = authMethod
        authMethod.selectedItem = ViaProxy.getConfig().getAuthMethod()
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(authMethod)
        gridy++

        val betaCraftAuth = JCheckBox(I18n.get("tab.general.betacraft_auth.label"))
        betaCraftAuth.toolTipText = I18n.get("tab.general.betacraft_auth.tooltip")
        betaCraftAuth.isSelected = ViaProxy.getConfig().useBetacraftAuth()
        this.betaCraftAuth = betaCraftAuth
        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(betaCraftAuth)
        // Simulate user action on serverVersion to update betaCraftAuth
        val fakeAction = ActionEvent(serverVersion, ActionEvent.ACTION_PERFORMED, "")
        for (listener in serverVersion.actionListeners) {
            listener.actionPerformed(fakeAction)
        }

        parent.add(body, BorderLayout.CENTER)
    }

    private fun addFooter(parent: Container) {
        val footer = JPanel()
        footer.layout = GridBagLayout()

        val stateLabel = JLabel("")
        stateLabel.isVisible = false
        this.stateLabel = stateLabel
        GBC.create(footer).grid(0, 0).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(stateLabel)

        val stateButton = JButton(I18n.get("tab.general.state.loading"))
        stateButton.addActionListener { _ ->
            if (stateButton.text.equals(I18n.get("tab.general.state.start"), ignoreCase = true)) this.start()
            else if (stateButton.text.equals(I18n.get("tab.general.state.stop"), ignoreCase = true)) this.stop()
        }
        stateButton.isEnabled = false
        this.stateButton = stateButton
        GBC.create(footer).grid(0, 1).weightx(1.0).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(stateButton)

        parent.add(footer, BorderLayout.SOUTH)

        val timer = Timer(100, null)
        timer.addActionListener { _ ->
            if (Via.getManager().getProtocolManager().hasLoadedMappings()) {
                stateButton.text = I18n.get("tab.general.state.start")
                stateButton.isEnabled = true
                timer.stop()

                if (System.getProperty("viaproxy.gui.autoStart") != null) {
                    stateButton.doClick(0)
                }
            }
        }
        timer.start()
    }

    private fun setComponentsEnabled(state: Boolean) {
        this.serverAddress!!.isEnabled = state
        this.serverVersion!!.isEnabled = state
        this.viaProxyWindow.advancedTab.bindAddress!!.isEnabled = state
        this.authMethod!!.isEnabled = state
        this.betaCraftAuth!!.isEnabled = state
        this.viaProxyWindow.advancedTab.proxyOnlineMode!!.isEnabled = state
        this.viaProxyWindow.advancedTab.proxy!!.isEnabled = state
        this.viaProxyWindow.advancedTab.legacySkinLoading!!.isEnabled = state
        this.viaProxyWindow.advancedTab.chatSigning!!.isEnabled = state
        this.viaProxyWindow.advancedTab.ignorePacketTranslationErrors!!.isEnabled = state
        this.viaProxyWindow.advancedTab.allowBetaPinging!!.isEnabled = state
        this.viaProxyWindow.advancedTab.simpleVoiceChatSupport!!.isEnabled = state
        this.viaProxyWindow.advancedTab.fakeAcceptResourcePacks!!.isEnabled = state
        if (state) this.serverVersion!!.actionListeners[0].actionPerformed(null)
    }

    private fun updateStateLabel() {
        val bindAddress = ViaProxy.getConfig().getBindAddress()
        if (bindAddress is InetSocketAddress) {
            this.stateLabel!!.text = I18n.get("tab.general.state.running", "1.7+", "127.0.0.1:" + bindAddress.port)
        } else {
            this.stateLabel!!.text = I18n.get("tab.general.state.running", "1.7+", AddressUtil.toString(bindAddress))
        }
        this.stateLabel!!.foreground = Color.GREEN
        this.stateLabel!!.isVisible = true
    }

    private fun start() {
        val selectedVersion = this.serverVersion!!.selectedItem
        if (selectedVersion !is ProtocolVersion) {
            ViaProxyWindow.showError(I18n.get("tab.general.error.no_server_version_selected"))
            return
        }
        if (ViaProxy.getSaveManager().uiSave.get("notice.ban_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.ban_warning", "true")
            ViaProxy.getSaveManager().save()

            ViaProxyWindow.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.ban_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>")
        }
        if (selectedVersion == BedrockProtocolVersion.BEDROCK_LATEST && ViaProxy.getSaveManager().uiSave.get("notice.bedrock_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.bedrock_warning", "true")
            ViaProxy.getSaveManager().save()

            ViaProxyWindow.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.bedrock_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>")
        }

        this.setComponentsEnabled(false)
        this.stateButton!!.isEnabled = false
        this.stateButton!!.text = I18n.get("tab.general.state.starting")

        Thread {
            val serverAddress = this.serverAddress!!.text.trim()
            val serverVersion = this.serverVersion!!.selectedItem as ProtocolVersion?
            val bindAddress = this.viaProxyWindow.advancedTab.bindAddress!!.text.trim()
            val authMethod = this.authMethod!!.selectedItem as ViaProxyConfig.AuthMethod?
            val proxyUrl = this.viaProxyWindow.advancedTab.proxy!!.text.trim()

            try {
                try {
                    if (serverAddress.startsWith("mc://")) { // ClassiCube Direct URL
                        val uri = URI(serverAddress)

                        val path = Pattern.compile("/").split(uri.getPath().substring(1))
                        if (path.size < 2) {
                            throw IllegalArgumentException(I18n.get("tab.general.error.invalid_classicube_url"))
                        }

                        ViaProxy.getConfig().setTargetAddress(InetSocketAddress(uri.getHost(), uri.getPort()))
                        ViaProxy.getConfig().setAccount(ClassicAccount(path[0], path[1]))
                    } else {
                        try {
                            ViaProxy.getConfig().setTargetAddress(AddressUtil.parse(serverAddress, serverVersion))
                        } catch (t: Throwable) {
                            throw IllegalArgumentException(I18n.get("tab.general.error.invalid_server_address"))
                        }

                        if (authMethod == ViaProxyConfig.AuthMethod.ACCOUNT) {
                            if (ViaProxy.getConfig().getAccount() == null) {
                                this.viaProxyWindow.accountsTab.markSelected(0)
                            }
                        } else {
                            ViaProxy.getConfig().setAccount(null)
                        }
                    }
                    try {
                        ViaProxy.getConfig().setBindAddress(AddressUtil.parse(bindAddress, null))
                    } catch (t: Throwable) {
                        throw IllegalArgumentException(I18n.get("tab.general.error.invalid_bind_address"))
                    }
                    if (!proxyUrl.isBlank()) {
                        try {
                            ViaProxy.getConfig().setBackendProxy(Proxy(URI(proxyUrl)))
                        } catch (e: URISyntaxException) {
                            throw IllegalArgumentException(I18n.get("tab.general.error.invalid_proxy_url"))
                        }
                    } else {
                        ViaProxy.getConfig().setBackendProxy(null)
                    }
                    this.applyGuiState()
                    this.viaProxyWindow.advancedTab.applyGuiState()
                    ViaProxy.getConfig().save()
                    ViaProxy.getSaveManager().save()
                } catch (t: Throwable) {
                    SwingUtilities.invokeLater { ViaProxyWindow.showError(t.message) }
                    throw t
                }

                try {
                    ViaProxy.startProxy()
                } catch (e: Throwable) {
                    SwingUtilities.invokeLater { ViaProxyWindow.showError(I18n.get("tab.general.error.failed_to_start")) }
                    throw e
                }

                SwingUtilities.invokeLater {
                    this.updateStateLabel()
                    this.stateButton!!.isEnabled = true
                    this.stateButton!!.text = I18n.get("tab.general.state.stop")
                }
            } catch (e: Throwable) {
                Logger.LOGGER.error("Error while starting ViaProxy", e)
                SwingUtilities.invokeLater {
                    this.setComponentsEnabled(true)
                    this.stateButton!!.isEnabled = true
                    this.stateButton!!.text = I18n.get("tab.general.state.start")
                    this.stateLabel!!.isVisible = false
                }
            }
        }.start()
    }

    private fun stop() {
        ViaProxy.stopProxy()

        this.stateLabel!!.isVisible = false
        this.stateButton!!.text = I18n.get("tab.general.state.start")
        this.setComponentsEnabled(true)
    }

    @EventHandler(events = [UICloseEvent::class])
    fun applyGuiState() {
        ViaProxy.getSaveManager().uiSave.put("server_address", this.serverAddress!!.text)
        val selectedVersion = this.serverVersion!!.selectedItem
        if (selectedVersion is ProtocolVersion) {
            ViaProxy.getConfig().setTargetVersion(selectedVersion)
        }
        val selectedAuthMethod = this.authMethod!!.selectedItem
        if (selectedAuthMethod is ViaProxyConfig.AuthMethod) {
            ViaProxy.getConfig().setAuthMethod(selectedAuthMethod)
        }
        ViaProxy.getConfig().setBetacraftAuth(this.betaCraftAuth!!.isSelected)
    }

}
