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

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector
import net.lenni0451.lambdaevents.LambdaManager
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator
import net.lenni0451.reflect.JavaBypass
import net.lenni0451.reflect.stream.RStream
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.ui.events.UICloseEvent
import net.raphimc.viaproxy.ui.impl.AccountsTab
import net.raphimc.viaproxy.ui.impl.AdvancedTab
import net.raphimc.viaproxy.ui.impl.GeneralTab
import net.raphimc.viaproxy.ui.impl.RealmsTab
import net.raphimc.viaproxy.ui.impl.UISettingsTab
import net.raphimc.viaproxy.util.logging.Logger
import java.awt.Desktop
import java.awt.Image
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIManager

class ViaProxyWindow : JFrame() {

    @JvmField
    val eventManager: LambdaManager = LambdaManager.threadSafe(LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP))

    @JvmField
    val contentPane: JTabbedPane = JTabbedPane()
    private val tabs = ArrayList<UITab>()

    @JvmField
    val generalTab: GeneralTab = GeneralTab(this)

    @JvmField
    val advancedTab: AdvancedTab = AdvancedTab(this)

    @JvmField
    val accountsTab: AccountsTab = AccountsTab(this)

    @JvmField
    val realmsTab: RealmsTab = RealmsTab(this)

    @JvmField
    val uiSettingsTab: UISettingsTab = UISettingsTab(this)

    private lateinit var icon: ImageIcon

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e -> showException(e) }
        this.eventManager.register(this)

        this.setLookAndFeel()
        this.loadIcons()
        this.initWindow()
        this.initTabs()

        FlatInspector.install("ctrl shift I")
        FlatUIDefaultsInspector.install("ctrl shift O")
        ToolTipManager.sharedInstance().initialDelay = 100
        ToolTipManager.sharedInstance().dismissDelay = 10_000
        SwingUtilities.updateComponentTreeUI(this)
        this.isVisible = true
    }

    private fun setLookAndFeel() {
        try {
            FlatDarkLaf.setup()

            UIManager.getLookAndFeelDefaults().put("TextComponent.arc", 5)
            UIManager.getLookAndFeelDefaults().put("Button.arc", 5)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun loadIcons() {
        this.icon = ImageIcon(this.javaClass.classLoader.getResource("assets/viaproxy/icons/icon.png"))
    }

    private fun initWindow() {
        this.title = "ViaProxy v" + ViaProxy.VERSION
        this.iconImage = this.icon.getImage()
        this.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                this@ViaProxyWindow.eventManager.call(UICloseEvent())
                ViaProxy.getConfig().save()
                ViaProxy.getSaveManager().save()
            }
        })
        this.setSize(500, 380)
        this.minimumSize = this.size
        this.setLocationRelativeTo(null)
        this.setContentPane(this.contentPane)
    }

    private fun initTabs() {
        RStream
            .of(this)
            .fields()
            .filter { field -> UITab::class.java.isAssignableFrom(field.type()) }
            .forEach { field ->
                val tab = field.get<UITab>()
                this.tabs.add(tab)
                tab.add(this.contentPane)
                this.eventManager.register(tab)
            }
        this.contentPane.addChangeListener { _ ->
            val selectedIndex = this.contentPane.selectedIndex
            if (selectedIndex >= 0 && selectedIndex < this.tabs.size) this.tabs[selectedIndex].onTabOpened()
        }
    }

    companion object {
        const val BORDER_PADDING: Int = 10
        const val BODY_BLOCK_PADDING: Int = 10

        @JvmStatic
        fun openURL(url: String) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(url))
                } else {
                    ProcessBuilder("xdg-open", url).start()
                }
            } catch (t: Throwable) {
                showInfo(I18n.get("generic.could_not_open_url", url))
            }
        }

        @JvmStatic
        fun showException(t: Throwable) {
            Logger.LOGGER.error("Caught exception in thread " + Thread.currentThread().name, t)
            val builder = StringBuilder("An error occurred:\n")
            builder.append("[").append(t.javaClass.simpleName).append("] ").append(t.message).append("\n")
            for (element in t.stackTrace) builder.append(element.toString()).append("\n")
            showError(builder.toString())
        }

        @JvmStatic
        fun showInfo(message: String?) {
            showNotification(message, JOptionPane.INFORMATION_MESSAGE)
        }

        @JvmStatic
        fun showWarning(message: String?) {
            showNotification(message, JOptionPane.WARNING_MESSAGE)
        }

        @JvmStatic
        fun showError(message: String?) {
            showNotification(message, JOptionPane.ERROR_MESSAGE)
        }

        @JvmStatic
        fun showNotification(message: String?, type: Int) {
            JOptionPane.showMessageDialog(ViaProxy.getForegroundWindow(), message, "ViaProxy", type)
        }
    }

}
