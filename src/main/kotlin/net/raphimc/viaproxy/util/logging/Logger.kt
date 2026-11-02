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
package net.raphimc.viaproxy.util.logging

import com.mojang.authlib.GameProfile
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.AddressUtil
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.plugins.util.PluginManager
import org.jline.jansi.AnsiConsole
import java.io.PrintStream
import java.net.SocketAddress
import java.util.Locale

class Logger {

    companion object {
        init {
            PluginManager.addPackage("net.raphimc.viaproxy.util.logging")
        }

        @JvmField
        val LOGGER: org.apache.logging.log4j.Logger = LogManager.getLogger("ViaProxy")

        @JvmField
        val SYSOUT: PrintStream = System.out

        @JvmField
        val SYSERR: PrintStream = System.err

        @JvmStatic
        fun setup() {
            if (System.console() != null) { // jANSI is the best lib. If there is no console it just segfaults the JVM process. Thanks!
                AnsiConsole.systemInstall()
            }
            System.setErr(LoggerPrintStream("STDERR", SYSERR))
            System.setOut(LoggerPrintStream("STDOUT", SYSOUT))
        }

        @JvmStatic
        fun u_info(title: String, proxyConnection: ProxyConnection, msg: String) {
            u_log(Level.INFO, title, proxyConnection, msg)
        }

        @JvmStatic
        fun u_warn(title: String, proxyConnection: ProxyConnection, msg: String) {
            u_log(Level.WARN, title, proxyConnection, msg)
        }

        @JvmStatic
        fun u_err(title: String, proxyConnection: ProxyConnection, msg: String) {
            u_log(Level.INFO, title, proxyConnection, msg)
        }

        @JvmStatic
        fun u_log(level: Level, title: String, proxyConnection: ProxyConnection, msg: String) {
            val address = proxyConnection.getC2P().remoteAddress()
            val gameProfile = proxyConnection.getGameProfile()
            u_log(level, title, address, gameProfile, msg)
        }

        @JvmStatic
        fun u_log(level: Level, title: String, address: SocketAddress, gameProfile: GameProfile?, msg: String) {
            LOGGER.log(level, "[" + title.uppercase(Locale.ROOT) + "] (" + AddressUtil.toString(address) + " | " + (if (gameProfile != null) gameProfile.getName() else "null") + ") " + msg)
        }
    }

}
