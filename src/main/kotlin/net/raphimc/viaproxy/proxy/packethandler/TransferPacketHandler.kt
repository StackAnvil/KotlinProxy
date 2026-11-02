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

import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.impl.common.S2CTransferPacket
import net.raphimc.netminecraft.util.MinecraftServerAddress
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.TransferDataHolder
import net.raphimc.viaproxy.util.logging.Logger
import java.net.InetSocketAddress

class TransferPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CTransferPacket) {
            val newAddress = MinecraftServerAddress.ofResolved(packet.host, packet.port)
            TransferDataHolder.addTempRedirect(this.proxyConnection.getC2P(), newAddress)

            val handshakeAddress = this.proxyConnection.getClientHandshakeAddress()
            if (handshakeAddress != null) {
                packet.host = handshakeAddress.getHost()
                packet.port = handshakeAddress.getPort()
            } else {
                Logger.u_warn("transfer", this.proxyConnection, "Client handshake address is invalid, using ViaProxy bind address instead")
                val bindAddress = ViaProxy.getCurrentProxyServer()!!.getChannel().localAddress()
                if (bindAddress !is InetSocketAddress) {
                    throw IllegalArgumentException("ViaProxy bind address must be an InetSocketAddress")
                }
                val clientAddress = this.proxyConnection.getC2P().localAddress()
                if (clientAddress !is InetSocketAddress) {
                    throw IllegalArgumentException("Client address must be an InetSocketAddress")
                }
                packet.host = clientAddress.getHostString()
                packet.port = bindAddress.getPort()
            }
        }

        return true
    }

}
