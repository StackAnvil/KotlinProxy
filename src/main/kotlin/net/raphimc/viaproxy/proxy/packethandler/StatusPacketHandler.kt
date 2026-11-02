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
package net.raphimc.viaproxy.proxy.packethandler

import com.google.gson.JsonParser
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.impl.status.S2CStatusPongResponsePacket
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.logging.Logger
import java.io.File
import java.nio.file.Files
import java.util.Base64

class StatusPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    companion object {
        private var FAVICON_BASE_64: String? = null
    }

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CStatusPongResponsePacket) {
            listeners.add(ChannelFutureListener.CLOSE)
        } else if (packet is S2CStatusResponsePacket && (!ViaProxy.getConfig().getCustomMotd().isBlank() || !ViaProxy.getConfig().getCustomFaviconPath().isBlank())) {
            try {
                val obj = JsonParser.parseString(packet.statusJson).getAsJsonObject()
                if (!ViaProxy.getConfig().getCustomMotd().isBlank()) {
                    obj.addProperty("description", ViaProxy.getConfig().getCustomMotd())
                }
                if (!ViaProxy.getConfig().getCustomFaviconPath().isBlank()) {
                    if (FAVICON_BASE_64 == null) {
                        try {
                            val faviconBytes = Files.readAllBytes(File(ViaProxy.getCwd(), ViaProxy.getConfig().getCustomFaviconPath()).toPath())
                            FAVICON_BASE_64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(faviconBytes)
                        } catch (e: Throwable) {
                            Logger.LOGGER.error("Failed to load custom favicon from path: " + ViaProxy.getConfig().getCustomFaviconPath(), e)
                            FAVICON_BASE_64 = ""
                        }
                    }
                    val favicon = FAVICON_BASE_64
                    if (favicon != null && !favicon.isBlank()) {
                        obj.addProperty("favicon", favicon)
                    }
                }
                packet.statusJson = obj.toString()
            } catch (ignored: Throwable) {
            }
        }

        return true
    }

}
