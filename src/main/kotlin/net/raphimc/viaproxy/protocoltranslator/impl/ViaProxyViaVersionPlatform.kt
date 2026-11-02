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
package net.raphimc.viaproxy.protocoltranslator.impl

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.libs.gson.JsonArray
import com.viaversion.viaversion.libs.gson.JsonObject
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigCustomPayloadPacket
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigCustomPayloadPacket
import net.raphimc.netminecraft.packet.impl.play.C2SPlayCustomPayloadPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayCustomPayloadPacket
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.CloseAndReturn
import net.raphimc.viaproxy.util.logging.JLoggerToSLF4J
import org.slf4j.LoggerFactory
import java.util.logging.Logger as JULLogger

class ViaProxyViaVersionPlatform : UserConnectionViaVersionPlatform(ViaProxy.getCwd()) {

    override fun createLogger(name: String): JULLogger {
        return JLoggerToSLF4J(LoggerFactory.getLogger(name))
    }

    override fun getPlatformName(): String {
        return "ViaProxy"
    }

    override fun getPlatformVersion(): String {
        return ViaProxy.VERSION
    }

    override fun kickPlayer(connection: UserConnection, message: String): Boolean {
        try {
            ProxyConnection.fromUserConnection(connection)!!.kickClient(message)
        } catch (ignored: CloseAndReturn) {
        }
        return true
    }

    override fun sendCustomPayload(connection: UserConnection, channel: String, message: ByteArray) {
        val proxyConnection = ProxyConnection.fromUserConnection(connection)!!
        proxyConnection.channel.writeAndFlush(
            when (proxyConnection.getP2sConnectionState()) {
                ConnectionState.CONFIGURATION -> C2SConfigCustomPayloadPacket(channel, message)
                ConnectionState.PLAY -> C2SPlayCustomPayloadPacket(channel, message)
                else -> throw UnsupportedOperationException("Can't send custom payloads in state: " + proxyConnection.getP2sConnectionState())
            }
        ).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    override fun sendCustomPayloadToClient(connection: UserConnection, channel: String, message: ByteArray) {
        val proxyConnection = ProxyConnection.fromUserConnection(connection)!!
        proxyConnection.getC2P().writeAndFlush(
            when (proxyConnection.getC2pConnectionState()) {
                ConnectionState.CONFIGURATION -> S2CConfigCustomPayloadPacket(channel, message)
                ConnectionState.PLAY -> S2CPlayCustomPayloadPacket(channel, message)
                else -> throw UnsupportedOperationException("Can't send custom payloads in state: " + proxyConnection.getC2pConnectionState())
            }
        ).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    override fun getDump(): JsonObject {
        val root = JsonObject()

        root.addProperty("impl_version", ViaProxy.IMPL_VERSION)

        val plugins = JsonArray()
        for (plugin in ViaProxy.getPluginManager().plugins) {
            val pluginObj = JsonObject()
            pluginObj.addProperty("name", plugin.name)
            pluginObj.addProperty("version", plugin.version)
            pluginObj.addProperty("author", plugin.author)
            plugins.add(pluginObj)
        }
        root.add("plugins", plugins)

        return root
    }

}
