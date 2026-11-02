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

import com.viaversion.viaversion.connection.ConnectionDetails
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.MCPackets
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.UnknownPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection

class ViaVersionConnectionDetailsPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    private val joinGameId = MCPackets.S2C_LOGIN.getId(this.proxyConnection.getClientVersion().getVersion())

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is UnknownPacket && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            if (packet.packetId == this.joinGameId) {
                ConnectionDetails.sendConnectionDetails(this.proxyConnection.getUserConnection(), ConnectionDetails.APP_CHANNEL)
            }
        }

        return true
    }

}
