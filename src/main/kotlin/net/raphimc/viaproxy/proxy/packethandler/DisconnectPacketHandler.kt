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
import net.raphimc.netminecraft.packet.impl.common.S2CDisconnectPacket
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket
import net.raphimc.viaproxy.cli.ConsoleFormatter
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.logging.Logger

class DisconnectPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    @Throws(Exception::class)
    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CLoginDisconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, ConsoleFormatter.convert(packet.reason.asLegacyFormatString()))
        } else if (packet is S2CDisconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, ConsoleFormatter.convert(packet.reason.asLegacyFormatString()))
        }
        return super.handleP2S(packet, listeners)
    }

}
