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
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.viaproxy.proxy.session.ProxyConnection

class UnexpectedPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    override fun handleC2P(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        val connectionState = this.proxyConnection.getC2pConnectionState()
        if (connectionState == ConnectionState.HANDSHAKING) {
            throw IllegalStateException("Unexpected packet in " + connectionState + " state")
        }

        return true
    }

}
