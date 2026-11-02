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
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.ViaProxyWindow
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.WindowConstants

private const val BORDER_PADDING = ViaProxyWindow.BORDER_PADDING

class DownloadPopup(
    private val parent: JFrame?,
    private val url: String,
    private val file: File,
    private val finishListener: Runnable,
    private val stopConsumer: Consumer<Throwable?>
) : JDialog(parent, true) {

    private lateinit var progressBar: JProgressBar
    private var downloadThread: Thread? = null

    init {
        this.initWindow()
        this.initComponents()
        this.isVisible = true
    }

    private fun initWindow() {
        this.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                this@DownloadPopup.close(false)
            }
        })
        this.title = I18n.get("popup.download.title")
        this.setSize(400, 110)
        this.isResizable = false
        this.setLocationRelativeTo(this.parent)
    }

    private fun initComponents() {
        val contentPane = JPanel()
        contentPane.layout = GridBagLayout()
        this.progressBar = JProgressBar()
        this.progressBar.isStringPainted = true
        GBC.create(contentPane).grid(0, 0).weightx(1.0).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.progressBar)

        val cancelButton = JButton(I18n.get("generic.cancel"))
        cancelButton.addActionListener { _ -> this.close(false) }
        GBC.create(contentPane).grid(0, 1).weightx(1.0).insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(cancelButton)
        this.contentPane = contentPane
        this.start()
    }

    private fun start() {
        val thread = Thread({
            try {
                val con = URL(this.url).openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                con.setRequestProperty("User-Agent", "Viaproxy/" + ViaProxy.VERSION)
                con.connectTimeout = 5000
                con.readTimeout = 5000

                val contentLength = con.contentLength
                var current = 0
                val tempFile = File.createTempFile("ViaProxy-download", "")
                val inputStream = con.inputStream
                val fos = FileOutputStream(tempFile)
                val buffer = ByteArray(1024 * 1024)
                var len = inputStream.read(buffer)
                while (len != -1) {
                    if (this.downloadThread!!.isInterrupted) throw InterruptedException()
                    fos.write(buffer, 0, len)

                    if (contentLength != -1) {
                        current += len
                        this.progressBar.value = (100F / contentLength * current).toInt()
                    }
                    len = inputStream.read(buffer)
                }
                fos.close()
                inputStream.close()
                con.disconnect()

                Files.move(tempFile.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING)

                this.close(true)
                this.finishListener.run()
            } catch (ignored: InterruptedException) {
            } catch (t: Throwable) {
                this.close(false)
                this.stopConsumer.accept(t)
            }
        }, "Download Popup Thread")
        this.downloadThread = thread
        thread.isDaemon = true
        thread.start()
    }

    private fun close(success: Boolean) {
        if (!success && this.downloadThread != null && this.downloadThread!!.isAlive) {
            this.downloadThread!!.interrupt()
            this.stopConsumer.accept(null)
        }

        this.isVisible = false
        this.dispose()
    }

}
