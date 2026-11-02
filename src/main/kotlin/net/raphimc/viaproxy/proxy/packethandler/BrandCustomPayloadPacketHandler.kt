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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.util.Key
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.PacketTypes
import net.raphimc.netminecraft.packet.impl.common.S2CCustomPayloadPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection

class BrandCustomPayloadPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    companion object {
        private const val BRAND_CHANNEL = "minecraft:brand"
        private const val LEGACY_BRAND_CHANNEL = "MC|Brand"
    }

    @Throws(Exception::class)
    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CCustomPayloadPacket) {
            if (Key.namespaced(packet.channel) == BRAND_CHANNEL || packet.channel == LEGACY_BRAND_CHANNEL) {
                val data = Unpooled.wrappedBuffer(packet.data)
                val brand = try {
                    PacketTypes.readString(data, Short.MAX_VALUE.toInt())
                } catch (e: Exception) {
                    if (this.proxyConnection.getServerVersion().newerThan(ProtocolVersion.v1_20)) {
                        throw e
                    } else { // <=1.20 clients ignore errors
                        "Unknown"
                    }
                }
                val newBrand = "ViaProxy (" + this.proxyConnection.getClientVersion().getName() + ") -> " + brand + " §r(" + this.proxyConnection.getServerVersion().getName() + ")"

                val newData = Unpooled.buffer()
                PacketTypes.writeString(newData, newBrand)
                packet.data = ByteBufUtil.getBytes(newData)
            }
        }

        return super.handleP2S(packet, listeners)
    }

}
