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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel

class SplashScreen @Throws(IOException::class) constructor() : JFrame() {

    private val progressPanel: ProgressPanel = ProgressPanel()

    init {
        this.isAlwaysOnTop = true
        this.isUndecorated = true
        try {
            this.background = Color(0, 0, 0, 0)
        } catch (ignored: UnsupportedOperationException) { // https://github.com/ViaVersion/ViaProxy/issues/562
        }
        this.setType(Window.Type.UTILITY)
        this.setSize(300, 235)
        this.setLocationRelativeTo(null)
        this.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        this.init()
        this.isVisible = true
    }

    @Throws(IOException::class)
    private fun init() {
        val contentPane = JPanel()
        contentPane.isOpaque = false
        contentPane.background = Color(0, 0, 0, 0)
        contentPane.layout = BorderLayout()
        contentPane.add(SplashPanel(ImageIO.read(SplashScreen::class.java.getResourceAsStream("/assets/viaproxy/icons/icon.png"))), BorderLayout.CENTER)
        contentPane.add(this.progressPanel, BorderLayout.SOUTH)
        this.contentPane = contentPane
    }

    var progress: Float
        get() = this.progressPanel.progress
        set(progress) {
            this.progressPanel.progress = Math.max(0f, Math.min(1f, progress))
            this.progressPanel.repaint()
        }

    fun setText(text: String) {
        this.progressPanel.text = text
        this.progressPanel.repaint()
    }


    private class SplashPanel(private val image: BufferedImage) : JPanel() {

        init {
            this.isOpaque = false
            this.background = Color(0, 0, 0, 0)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.drawImage(this.image, 0, 0, this.width, this.height, this)
        }

    }

    private class ProgressPanel : JPanel() {

        var progress: Float = 0f
        var text: String = ""

        init {
            this.isOpaque = false
            this.background = Color(0, 0, 0, 0)
            this.preferredSize = Dimension(this.width, 30)
        }

        override fun paintComponent(g: Graphics) {
            if (g is Graphics2D) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            }

            g.color = Color.WHITE
            g.fillRect(0, 5, this.width, this.height - 5)
            g.color = Color(0, 69, 104)
            g.fillRect(2, 7, this.width - 4, this.height - 9)

            g.color = Color(2, 188, 216)
            var progressWidth = this.width - 4
            progressWidth = (progressWidth * this.progress).toInt()
            g.fillRect(2, 7, progressWidth, this.height - 9)

            g.color = Color.WHITE
            g.font = g.font.deriveFont(15F)
            val metrics: FontMetrics = g.fontMetrics
            val textWidth = metrics.stringWidth(this.text)
            g.drawString(this.text, (this.width - textWidth) / 2, (this.height - metrics.height) / 2 + metrics.ascent + metrics.descent / 2)
        }

    }

}
