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
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPacket
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPopPacket
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPushPacket
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigResourcePackPacket
import net.raphimc.netminecraft.packet.impl.play.C2SPlayResourcePackPacket
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ResourcePackAction
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import java.util.HashSet
import java.util.UUID

class ResourcePackSpooferPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    private val resourcePacks: MutableSet<UUID> = HashSet()

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CResourcePackPushPacket) {
            this.resourcePacks.add(packet.packId)
            this.sendResponse(packet.packId, null, ResourcePackAction.ACCEPTED.ordinal)
            for (resourcePack in this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal)
            }
            return false
        } else if (packet is S2CResourcePackPopPacket) {
            if (packet.packId != null) {
                this.resourcePacks.remove(packet.packId)
            } else {
                this.resourcePacks.clear()
            }
            for (resourcePack in this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal)
            }
            return false
        } else if (packet is S2CResourcePackPacket) {
            this.sendResponse(null, packet.hash, ResourcePackAction.ACCEPTED.ordinal)
            this.sendResponse(null, packet.hash, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal)
            return false
        }

        return true
    }

    private fun sendResponse(packId: UUID?, hash: String?, status: Int) {
        if (this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            this.proxyConnection.getChannel().writeAndFlush(C2SPlayResourcePackPacket(status, packId, hash)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        } else {
            this.proxyConnection.getChannel().writeAndFlush(C2SConfigResourcePackPacket(status, packId, hash)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        }
    }

}
