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
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigFinishConfigurationPacket
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigFinishConfigurationPacket
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket
import net.raphimc.netminecraft.packet.impl.play.C2SPlayConfigurationAcknowledgedPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayStartConfigurationPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.ChannelUtil
import net.raphimc.viaproxy.util.logging.Logger

class ConfigurationPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    override fun handleC2P(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is C2SLoginAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION)
            listeners.add(ChannelFutureListener { f ->
                if (f.isSuccess()) {
                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION)
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel())
                }
            })
        } else if (packet is C2SConfigFinishConfigurationPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.PLAY)
            listeners.add(ChannelFutureListener { f ->
                if (f.isSuccess()) {
                    Logger.u_info("session", this.proxyConnection, "Configuration finished! Switching to PLAY state")
                    this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY)
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel())
                }
            })
        } else if (packet is C2SPlayConfigurationAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION)
            listeners.add(ChannelFutureListener { f ->
                if (f.isSuccess()) {
                    Logger.u_info("session", this.proxyConnection, "Switching to CONFIGURATION state")
                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION)
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel())
                }
            })
        }

        return true
    }

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CConfigFinishConfigurationPacket) {
            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel())
        } else if (packet is S2CPlayStartConfigurationPacket) {
            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel())
        }

        return true
    }

}
