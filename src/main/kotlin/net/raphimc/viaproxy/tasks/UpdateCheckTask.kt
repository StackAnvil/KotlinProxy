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
package net.raphimc.viaproxy.tasks

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.vdurmont.semver4j.Semver
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.ui.I18n
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.ui.popups.DownloadPopup
import net.raphimc.viaproxy.util.JarUtil
import net.raphimc.viaproxy.util.logging.Logger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Consumer
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class UpdateCheckTask(private val hasUI: Boolean) : Runnable {

    override fun run() {
        if (ViaProxy.VERSION.startsWith("$")) return // Dev env check
        try {
            val url = URL("https://api.github.com/repos/RaphiMC/ViaProxy/releases/latest")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("User-Agent", "ViaProxy/" + ViaProxy.VERSION)
            con.connectTimeout = 5000
            con.readTimeout = 5000

            val input = con.inputStream
            val bytes = ByteArray(1024)
            val builder = StringBuilder()
            var read = input.read(bytes)
            while (read != -1) {
                builder.append(String(bytes, 0, read))
                read = input.read(bytes)
            }
            con.disconnect()

            val obj = JsonParser.parseString(builder.toString()).asJsonObject
            val latestVersion = obj.get("tag_name").asString.substring(1)
            var updateAvailable: Boolean
            try {
                val versionSemver = Semver(ViaProxy.VERSION)
                val latestVersionSemver = Semver(latestVersion)
                updateAvailable = latestVersionSemver.isGreaterThan(versionSemver)
                if (versionSemver.isGreaterThan(latestVersionSemver)) Logger.LOGGER.warn("You are running a dev version of ViaProxy")
            } catch (t: Throwable) {
                updateAvailable = ViaProxy.VERSION != latestVersion
            }
            if (updateAvailable) {
                Logger.LOGGER.warn("You are running an outdated version of ViaProxy! Latest version: " + latestVersion)
                if (this.hasUI && JarUtil.getJarFile().isPresent) {
                    val runsJava8 = System.getProperty("java.version").startsWith("1.8")
                    val assets = obj.getAsJsonArray("assets")
                    var found = false
                    for (asset in assets) {
                        val assetObject = asset.asJsonObject
                        if ((this.isMainViaProxyJar(obj, assetObject) && !runsJava8) || this.isJava8ViaProxyJar(obj, assetObject) && runsJava8) {
                            found = true
                            SwingUtilities.invokeLater {
                                this.showUpdateQuestion(
                                    assetObject.get("name").asString,
                                    assetObject.get("browser_download_url").asString,
                                    latestVersion
                                )
                            }
                            break
                        }
                    }
                    if (!found) SwingUtilities.invokeLater { this.showUpdateWarning(latestVersion) }
                }
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun showUpdateWarning(latestVersion: String) {
        JOptionPane.showMessageDialog(
            ViaProxy.getForegroundWindow(),
            I18n.get("popup.update.info", ViaProxy.VERSION, latestVersion),
            "ViaProxy",
            JOptionPane.WARNING_MESSAGE
        )
    }

    private fun showUpdateQuestion(name: String, downloadUrl: String, latestVersion: String) {
        val chosen = JOptionPane.showConfirmDialog(
            ViaProxy.getForegroundWindow(),
            I18n.get("popup.update.info", ViaProxy.VERSION, latestVersion) + "\n\n" + I18n.get("popup.update.question"),
            "ViaProxy",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )
        if (chosen == JOptionPane.YES_OPTION) {
            val f = File(JarUtil.getJarFile().map(File::getParentFile).orElseThrow(), name)
            DownloadPopup(ViaProxy.getForegroundWindow()!!, downloadUrl, f, Runnable {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        ViaProxy.getForegroundWindow(),
                        I18n.get("popup.update.success"),
                        "ViaProxy",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    try {
                        JarUtil.launch(f)
                        System.exit(0)
                    } catch (e: Throwable) {
                        Logger.LOGGER.error("Could not start the new ViaProxy jar", e)
                        ViaProxyWindow.showException(e)
                    }
                }
            }, Consumer { t ->
                if (t != null) {
                    Logger.LOGGER.error("Could not download the latest version of ViaProxy", t)
                    ViaProxyWindow.showException(t)
                }
            })
        }
    }

    private fun isMainViaProxyJar(root: JsonObject, assetObject: JsonObject): Boolean {
        return assetObject.get("name").asString == root.get("name").asString + ".jar"
    }

    private fun isJava8ViaProxyJar(root: JsonObject, assetObject: JsonObject): Boolean {
        return assetObject.get("name").asString == root.get("name").asString + "+java8.jar"
    }

}
