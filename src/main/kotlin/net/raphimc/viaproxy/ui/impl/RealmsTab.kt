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

import com.google.common.collect.Iterables
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.api.protocol.version.VersionType
import net.lenni0451.commons.swing.GBC
import net.lenni0451.commons.swing.layouts.VerticalLayout
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.extra.realms.exception.RealmsRequestException
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer
import net.raphimc.minecraftauth.extra.realms.service.RealmsService
import net.raphimc.minecraftauth.extra.realms.service.impl.BedrockRealmsService
import net.raphimc.minecraftauth.extra.realms.service.impl.JavaRealmsService
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.viabedrock.protocol.data.ProtocolConstants
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.UITab
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.util.logging.Logger
import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.util.concurrent.CompletableFuture
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager

class RealmsTab(frame: ViaProxyWindow) : UITab(frame, "realms") {

    companion object {
        private val LATEST_JAVA_RELEASE: ProtocolVersion
        private val LATEST_JAVA_SNAPSHOT: ProtocolVersion?

        init {
            var latestVersion: ProtocolVersion? = null
            var latestSnapshotVersion: ProtocolVersion? = null
            val supportedVersions = ProtocolVersion.getProtocols()
            for (i in supportedVersions.size - 1 downTo 0) {
                val version = supportedVersions[i]
                if (version.getVersionType() != VersionType.RELEASE) continue
                if (version.isSnapshot() && latestSnapshotVersion == null) {
                    latestSnapshotVersion = version
                } else if (!version.isSnapshot()) {
                    latestVersion = version
                    break
                }
            }
            if (latestVersion == null) throw IllegalStateException("Could not find compatible version")
            LATEST_JAVA_RELEASE = latestVersion
            LATEST_JAVA_SNAPSHOT = latestSnapshotVersion
        }
    }

    private var currentAccount: Account? = null
    private var currentSelectedJavaVersion: ProtocolVersion = LATEST_JAVA_RELEASE

    override fun onTabOpened() {
        if (ViaProxy.getConfig().getAccount() !== this.currentAccount) {
            this.currentAccount = ViaProxy.getConfig().getAccount()
            this.reinit()
        }
    }

    private fun reinit() {
        this.contentPane.removeAll()
        this.init(this.contentPane)
        this.contentPane.revalidate()
        this.contentPane.repaint()
    }

    override fun init(contentPane: JPanel) {
        val body = JPanel()
        body.layout = GridBagLayout()

        val statusLabel = JLabel("")
        statusLabel.font = statusLabel.font.deriveFont(20F)
        statusLabel.horizontalAlignment = SwingConstants.CENTER
        body.add(statusLabel)

        if (this.currentAccount == null) {
            statusLabel.text = I18n.get("tab.realms.no_account")
        } else {
            statusLabel.text = I18n.get("tab.realms.refreshing_account")
            CompletableFuture.runAsync {
                try {
                    SwingUtilities.invokeLater {
                        val account = this.currentAccount
                        if (account is MicrosoftAccount) {
                            val realmsService = JavaRealmsService(MinecraftAuth.createHttpClient(), Iterables.getLast(this.currentSelectedJavaVersion.getIncludedVersions()), account.getAuthManager().getMinecraftToken(), account.getAuthManager().getMinecraftProfile())
                            this.loadRealms(realmsService, body, statusLabel)
                        } else if (account is BedrockAccount) {
                            val realmsService = BedrockRealmsService(MinecraftAuth.createHttpClient(), ProtocolConstants.BEDROCK_VERSION_NAME, account.getAuthManager().getRealmsXstsToken())
                            this.loadRealms(realmsService, body, statusLabel)
                        } else {
                            statusLabel.text = I18n.get("tab.realms.unsupported_account")
                        }
                    }
                } catch (e: Throwable) {
                    Logger.LOGGER.error("Failed to refresh account", e)
                    ViaProxyWindow.showError(I18n.get("tab.realms.error_account", e.message))
                    SwingUtilities.invokeLater { statusLabel.text = I18n.get("tab.realms.error_account_label") }
                }
            }
        }

        contentPane.layout = BorderLayout()
        contentPane.add(body, BorderLayout.NORTH)
    }

    private fun loadRealms(realmsService: RealmsService, body: JPanel, statusLabel: JLabel) {
        statusLabel.text = I18n.get("tab.realms.availability_check")

        realmsService.isCompatibleAsync().thenAccept { state ->
            if (state) {
                SwingUtilities.invokeLater { statusLabel.text = I18n.get("tab.realms.loading_worlds") }
                realmsService.getWorldsAsync().thenAccept { servers ->
                    SwingUtilities.invokeLater {
                        body.remove(statusLabel)
                        this.addHeader(body, realmsService is JavaRealmsService)
                        val realmsPanel = JPanel()
                        realmsPanel.layout = VerticalLayout(5, 5)
                        if (servers.isEmpty()) {
                            val label = JLabel(I18n.get("tab.realms.no_worlds"))
                            label.horizontalAlignment = SwingConstants.CENTER
                            label.font = label.font.deriveFont(20F)
                            realmsPanel.add(label)
                        } else {
                            this.addRealms(realmsPanel, realmsService, servers)
                        }
                        val realmsScrollPane = JScrollPane(realmsPanel)
                        realmsScrollPane.verticalScrollBar.unitIncrement = 10
                        contentPane.add(realmsScrollPane, BorderLayout.CENTER)
                        contentPane.revalidate()
                        contentPane.repaint()
                    }
                }.exceptionally { e ->
                    val cause = e.cause
                    Logger.LOGGER.error("Failed to get realms worlds", cause)
                    ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause!!.message))
                    SwingUtilities.invokeLater { statusLabel.text = I18n.get("tab.realms.error_generic_label") }
                    null
                }
            } else {
                SwingUtilities.invokeLater { statusLabel.text = I18n.get("tab.realms.unavailable") }
            }
        }.exceptionally { e ->
            val cause = e.cause
            Logger.LOGGER.error("Failed to check realms availability", cause)
            ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause!!.message))
            SwingUtilities.invokeLater { statusLabel.text = I18n.get("tab.realms.error_generic_label") }
            null
        }
    }

    private fun addHeader(parent: JPanel, showType: Boolean) {
        GBC.create(parent).grid(0, 0).weightx(1.0).insets(5, 5, 5, 5).fill(GBC.HORIZONTAL).add(JLabel(I18n.get("tab.realms.account", this.currentAccount!!.getDisplayString())))
        if (showType && LATEST_JAVA_SNAPSHOT != null) {
            val type = JComboBox<String>()
            type.addItem(I18n.get("tab.realms.release"))
            type.addItem(I18n.get("tab.realms.snapshot"))
            type.selectedIndex = if (this.currentSelectedJavaVersion.isSnapshot()) 1 else 0
            type.addActionListener { _ ->
                val selected = if (type.selectedIndex == 0) LATEST_JAVA_RELEASE else LATEST_JAVA_SNAPSHOT!!
                if (selected !== this.currentSelectedJavaVersion) {
                    this.currentSelectedJavaVersion = selected
                    this.reinit()
                }
            }
            GBC.create(parent).grid(1, 0).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(type)
        }
    }

    private fun addRealms(parent: JPanel, realmsService: RealmsService, servers: MutableList<RealmsServer>) {
        servers.sortWith { o1, o2 ->
            val o1Compatible = o1.isCompatible() && !o1.isExpired()
            val o2Compatible = o2.isCompatible() && !o2.isExpired()
            if (o1Compatible && !o2Compatible) -1
            else if (!o1Compatible && o2Compatible) 1
            else 0
        }
        for (server in servers) {
            val panel = JPanel()
            panel.layout = GridBagLayout()
            panel.border = BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"))

            var nameString = ""
            if (server.getOwnerName() != null) nameString += server.getOwnerName() + " - "
            var versionString = ""
            if (server.getActiveVersion() != null) versionString += " - " + server.getActiveVersion()
            GBC.create(panel).grid(0, 0).weightx(1.0).insets(5, 5, 0, 5).fill(GBC.HORIZONTAL).add(JLabel(nameString + server.getNameOr("") + " (" + server.getState() + ")"))
            GBC.create(panel).grid(1, 0).insets(5, 5, 0, 5).anchor(GBC.LINE_END).add(JLabel(server.getWorldType() + versionString))
            GBC.create(panel).grid(0, 1).insets(5, 5, 0, 5).fill(GBC.HORIZONTAL).add(JLabel(server.getMotdOr("")))
            val join = JButton(I18n.get("tab.realms.join"))
            if (server.isExpired()) {
                join.isEnabled = false
                join.toolTipText = I18n.get("tab.realms.expired")
            } else if (!server.isCompatible()) {
                join.isEnabled = false
                join.toolTipText = I18n.get("tab.realms.incompatible")
            }
            GBC.create(panel).grid(1, 1).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(join)
            join.addActionListener { _ ->
                join.isEnabled = false
                join.text = I18n.get("tab.realms.joining")
                realmsService.joinWorldAsync(server).thenAccept { joinInformation ->
                    SwingUtilities.invokeLater {
                        join.isEnabled = true
                        join.text = I18n.get("tab.realms.join")
                        this.setServerAddressAndStartViaProxy(joinInformation, if (realmsService is JavaRealmsService) this.currentSelectedJavaVersion else BedrockProtocolVersion.BEDROCK_LATEST)
                    }
                }.exceptionally { e ->
                    val cause = e.cause
                    SwingUtilities.invokeLater {
                        join.isEnabled = true
                        join.text = I18n.get("tab.realms.join")
                        if (realmsService is JavaRealmsService && cause is RealmsRequestException && cause.getErrorCode() == RealmsRequestException.ERROR_TOS_NOT_ACCEPTED) {
                            val chosen = JOptionPane.showConfirmDialog(this.viaProxyWindow, I18n.get("tab.realms.accept_tos", "https://aka.ms/MinecraftRealmsTerms"), "ViaProxy", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                            if (chosen == JOptionPane.YES_OPTION) {
                                realmsService.acceptTosUnchecked()
                                join.doClick(0)
                            }
                        } else if (realmsService is BedrockRealmsService && cause is RealmsRequestException && cause.getErrorCode() == RealmsRequestException.ERROR_TIMELINE_OPT_IN_REQUIRED) {
                            realmsService.updateWorldStorySettingsUnchecked(server, null, true)
                            join.doClick(0)
                        } else {
                            Logger.LOGGER.error("Failed to join realm", cause)
                            ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause!!.message))
                        }
                    }
                    null
                }
            }

            parent.add(panel)
        }
    }

    private fun setServerAddressAndStartViaProxy(joinInformation: RealmsJoinInformation, version: ProtocolVersion) {
        val generalTab = this.viaProxyWindow.generalTab
        if (generalTab.stateButton!!.isEnabled) {
            if (generalTab.stateButton!!.text != I18n.get("tab.general.state.start")) {
                generalTab.stateButton!!.doClick(0) // Stop the running proxy
            }

            when (joinInformation.getNetworkProtocol()) {
                RealmsJoinInformation.PROTOCOL_DEFAULT -> generalTab.serverAddress!!.text = joinInformation.getAddress()
                RealmsJoinInformation.PROTOCOL_NETHERNET -> generalTab.serverAddress!!.text = "nethernet-xbox://" + joinInformation.getAddress()
                RealmsJoinInformation.PROTOCOL_NETHERNET_JSONRPC -> generalTab.serverAddress!!.text = "nethernet-xbox-json-rpc://" + joinInformation.getAddress()
                else -> throw IllegalArgumentException("Unknown realms network protocol: " + joinInformation.getNetworkProtocol())
            }

            generalTab.serverVersion!!.selectedItem = version
            generalTab.authMethod!!.selectedIndex = 0
            generalTab.stateButton!!.doClick(0)
            this.viaProxyWindow.contentPane.selectedIndex = 0
        }
    }

}
