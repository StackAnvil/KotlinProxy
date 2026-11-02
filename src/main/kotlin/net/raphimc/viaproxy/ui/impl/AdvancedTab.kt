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

import com.viaversion.viaversion.util.DumpUtil
import gs.mclo.api.MclogsClient
import net.lenni0451.commons.swing.GBC
import net.lenni0451.lambdaevents.EventHandler
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.UITab
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.events.UICloseEvent
import net.raphimc.viaproxy.util.logging.Logger
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender
import java.awt.BorderLayout
import java.awt.Container
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ExecutionException
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING
private const val BODY_BLOCK_PADDING = ViaProxyWindow.BODY_BLOCK_PADDING

class AdvancedTab(frame: ViaProxyWindow) : UITab(frame, "advanced") {

    @JvmField
    var bindAddress: JTextField? = null

    @JvmField
    var proxy: JTextField? = null

    @JvmField
    var proxyOnlineMode: JCheckBox? = null

    @JvmField
    var legacySkinLoading: JCheckBox? = null

    @JvmField
    var chatSigning: JCheckBox? = null

    @JvmField
    var ignorePacketTranslationErrors: JCheckBox? = null

    @JvmField
    var allowBetaPinging: JCheckBox? = null

    @JvmField
    var simpleVoiceChatSupport: JCheckBox? = null

    @JvmField
    var fakeAcceptResourcePacks: JCheckBox? = null

    @JvmField
    var viaVersionDumpButton: JButton? = null

    @JvmField
    var uploadLogsButton: JButton? = null

    override fun init(contentPane: JPanel) {
        // The UI is built in the initializer below, after the component fields are initialized.
    }

    init {
        this.build(this.contentPane)
    }

    private fun build(contentPane: JPanel) {
        contentPane.layout = BorderLayout()

        this.addBody(contentPane)
        this.addFooter(contentPane)
    }

    private fun addBody(parent: Container) {
        val body = JPanel()
        body.layout = GridBagLayout()

        val checkboxes = JPanel()
        checkboxes.layout = GridLayout(0, 2, BORDER_PADDING, BORDER_PADDING)

        var gridy = 0
        val bindPortLabel = JLabel(I18n.get("tab.advanced.bind_address.label"))
        bindPortLabel.toolTipText = I18n.get("tab.advanced.bind_address.tooltip")
        GBC.create(body).grid(0, gridy).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(bindPortLabel)
        gridy++

        val bindAddress = JTextField()
        bindAddress.toolTipText = I18n.get("tab.advanced.bind_address.tooltip")
        bindAddress.text = "0.0.0.0:25568"
        ViaProxy.getSaveManager().uiSave.loadTextField("bind_address", bindAddress)
        this.bindAddress = bindAddress
        GBC.create(body).grid(0, gridy).weightx(1.0).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(bindAddress)
        gridy++

        val proxyLabel = JLabel(I18n.get("tab.advanced.proxy_url.label"))
        proxyLabel.toolTipText = I18n.get("tab.advanced.proxy_url.tooltip")
        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(proxyLabel)
        gridy++

        val proxy = JTextField()
        proxy.toolTipText = I18n.get("tab.advanced.proxy_url.tooltip")
        ViaProxy.getSaveManager().uiSave.loadTextField("proxy", proxy)
        this.proxy = proxy
        GBC.create(body).grid(0, gridy).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(proxy)
        gridy++

        val proxyOnlineMode = JCheckBox(I18n.get("tab.advanced.proxy_online_mode.label"))
        proxyOnlineMode.toolTipText = I18n.get("tab.advanced.proxy_online_mode.tooltip")
        proxyOnlineMode.isSelected = ViaProxy.getConfig().isProxyOnlineMode()
        this.proxyOnlineMode = proxyOnlineMode
        checkboxes.add(proxyOnlineMode)

        val legacySkinLoading = JCheckBox(I18n.get("tab.advanced.legacy_skin_loading.label"))
        legacySkinLoading.toolTipText = I18n.get("tab.advanced.legacy_skin_loading.tooltip")
        ViaProxy.getSaveManager().uiSave.loadCheckBox("legacy_skin_loading", legacySkinLoading)
        this.legacySkinLoading = legacySkinLoading
        checkboxes.add(legacySkinLoading)

        val chatSigning = JCheckBox(I18n.get("tab.advanced.chat_signing.label"))
        chatSigning.toolTipText = I18n.get("tab.advanced.chat_signing.tooltip")
        chatSigning.isSelected = ViaProxy.getConfig().shouldSignChat()
        this.chatSigning = chatSigning
        checkboxes.add(chatSigning)

        val ignorePacketTranslationErrors = JCheckBox(I18n.get("tab.advanced.ignore_packet_translation_errors.label"))
        ignorePacketTranslationErrors.toolTipText = I18n.get("tab.advanced.ignore_packet_translation_errors.tooltip")
        ignorePacketTranslationErrors.isSelected = false
        ignorePacketTranslationErrors.isSelected = ViaProxy.getConfig().shouldIgnoreProtocolTranslationErrors()
        this.ignorePacketTranslationErrors = ignorePacketTranslationErrors
        checkboxes.add(ignorePacketTranslationErrors)

        val allowBetaPinging = JCheckBox(I18n.get("tab.advanced.allow_beta_pinging.label"))
        allowBetaPinging.toolTipText = I18n.get("tab.advanced.allow_beta_pinging.tooltip")
        allowBetaPinging.isSelected = false
        allowBetaPinging.isSelected = ViaProxy.getConfig().shouldAllowBetaPinging()
        this.allowBetaPinging = allowBetaPinging
        checkboxes.add(allowBetaPinging)

        val simpleVoiceChatSupport = JCheckBox(I18n.get("tab.advanced.simple_voice_chat_support.label"))
        simpleVoiceChatSupport.toolTipText = I18n.get("tab.advanced.simple_voice_chat_support.tooltip")
        simpleVoiceChatSupport.isSelected = false
        simpleVoiceChatSupport.isSelected = ViaProxy.getConfig().shouldSupportSimpleVoiceChat()
        this.simpleVoiceChatSupport = simpleVoiceChatSupport
        checkboxes.add(simpleVoiceChatSupport)

        val fakeAcceptResourcePacks = JCheckBox(I18n.get("tab.advanced.fake_accept_resource_packs.label"))
        fakeAcceptResourcePacks.toolTipText = I18n.get("tab.advanced.fake_accept_resource_packs.tooltip")
        fakeAcceptResourcePacks.isSelected = false
        fakeAcceptResourcePacks.isSelected = ViaProxy.getConfig().shouldFakeAcceptResourcePacks()
        this.fakeAcceptResourcePacks = fakeAcceptResourcePacks
        checkboxes.add(fakeAcceptResourcePacks)

        GBC.create(body).grid(0, gridy).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1.0, 1.0).add(checkboxes)

        parent.add(body, BorderLayout.NORTH)
    }

    private fun addFooter(container: Container) {
        val footer = JPanel()
        footer.layout = GridLayout(1, 2, BORDER_PADDING, 0)

        val viaVersionDumpButton = JButton(I18n.get("tab.advanced.create_viaversion_dump.label"))
        viaVersionDumpButton.addActionListener { _ ->
            viaVersionDumpButton.isEnabled = false
            DumpUtil.postDump(null).whenComplete { url, e ->
                if (e != null) {
                    Logger.LOGGER.error("Failed to create ViaVersion dump", e)
                    SwingUtilities.invokeLater { ViaProxyWindow.showError(e.message) }
                } else {
                    ViaProxyWindow.openURL(url)
                    val stringSelection = StringSelection(url)
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection)
                    SwingUtilities.invokeLater { ViaProxyWindow.showInfo(I18n.get("tab.advanced.create_viaversion_dump.success")) }
                }
                SwingUtilities.invokeLater { viaVersionDumpButton.isEnabled = true }
            }
        }
        this.viaVersionDumpButton = viaVersionDumpButton
        footer.add(viaVersionDumpButton)

        val uploadLogsButton = JButton(I18n.get("tab.advanced.upload_latest_log.label"))
        uploadLogsButton.addActionListener { _ ->
            val logger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
            val fileAppender = logger.appenders["LatestFile"] as RollingRandomAccessFileAppender
            fileAppender.manager.flush()
            val logFile = File(fileAppender.fileName)

            try {
                uploadLogsButton.isEnabled = false
                val mclogsClient = MclogsClient("ViaProxy", ViaProxy.VERSION)
                val apiResponse = mclogsClient.uploadLog(logFile.toPath()).get()

                ViaProxyWindow.openURL(apiResponse.url)
                val selection = StringSelection(apiResponse.url)
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
                ViaProxyWindow.showInfo("<html>" + I18n.get("tab.advanced.upload_latest_log.success", "<a href=\"\">" + apiResponse.getUrl() + "</a>") + "</html>")
            } catch (e: ExecutionException) {
                if (e.cause is FileNotFoundException) {
                    ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_not_found"))
                } else {
                    Logger.LOGGER.error("Failed to upload log file", e.cause)
                    ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_generic", e.cause!!.message))
                }
            } catch (e: Throwable) {
                Logger.LOGGER.error("Failed to upload log file", e)
                ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_generic", e.message))
            } finally {
                uploadLogsButton.isEnabled = true
            }
        }
        this.uploadLogsButton = uploadLogsButton
        footer.add(uploadLogsButton)

        val padding = JPanel()
        padding.layout = GridBagLayout()
        GBC.create(padding).grid(0, 0).weightx(1.0).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(footer)

        container.add(padding, BorderLayout.SOUTH)
    }

    @EventHandler(events = [UICloseEvent::class])
    fun applyGuiState() {
        ViaProxy.getSaveManager().uiSave.put("bind_address", this.bindAddress!!.text)
        ViaProxy.getSaveManager().uiSave.put("proxy", this.proxy!!.text)
        ViaProxy.getConfig().setProxyOnlineMode(this.proxyOnlineMode!!.isSelected)
        ViaProxy.getSaveManager().uiSave.put("legacy_skin_loading", this.legacySkinLoading!!.isSelected.toString())
        ViaProxy.getConfig().setChatSigning(this.chatSigning!!.isSelected)
        ViaProxy.getConfig().setIgnoreProtocolTranslationErrors(this.ignorePacketTranslationErrors!!.isSelected)
        ViaProxy.getConfig().setAllowBetaPinging(this.allowBetaPinging!!.isSelected)
        ViaProxy.getConfig().setSimpleVoiceChatSupport(this.simpleVoiceChatSupport!!.isSelected)
        ViaProxy.getConfig().setFakeAcceptResourcePacks(this.fakeAcceptResourcePacks!!.isSelected)
    }

}
