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

import com.viaversion.viaversion.util.Key
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.PacketTypes
import net.raphimc.netminecraft.packet.impl.common.S2CCustomPayloadPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.logging.Logger
import java.net.InetSocketAddress

class SimpleVoiceChatPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    companion object {
        private const val SECRET_CHANNEL = "voicechat:secret"
    }

    @Throws(Exception::class)
    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CCustomPayloadPacket) {
            if (Key.namespaced(packet.channel) == SECRET_CHANNEL) {
                val data = Unpooled.wrappedBuffer(packet.data)
                try {
                    val newData = Unpooled.buffer()
                    PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)) // secret
                    val port = data.readInt() // port
                    newData.writeInt(port)
                    PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)) // player uuid
                    newData.writeByte(data.readByte().toInt()) // codec
                    newData.writeInt(data.readInt()) // mtu size
                    newData.writeDouble(data.readDouble()) // voice chat distance
                    newData.writeInt(data.readInt()) // keep alive
                    newData.writeBoolean(data.readBoolean()) // groups enabled
                    val voiceHost = PacketTypes.readString(data, Short.MAX_VALUE.toInt()) // voice host
                    if (voiceHost.isEmpty()) {
                        val serverAddress = this.proxyConnection.getServerAddress()
                        if (serverAddress is InetSocketAddress) {
                            val newVoiceHost = AddressUtil.toString(InetSocketAddress(serverAddress.getAddress(), port))
                            PacketTypes.writeString(newData, newVoiceHost)
                            Logger.u_info("session", this.proxyConnection, "Redirecting Simple Voice Chat to " + newVoiceHost)
                        } else {
                            throw IllegalArgumentException("Server address must be an InetSocketAddress")
                        }
                    } else {
                        PacketTypes.writeString(newData, voiceHost)
                    }
                    newData.writeBytes(data)
                    packet.data = ByteBufUtil.getBytes(newData)
                } catch (e: Throwable) {
                    Logger.LOGGER.error("Failed to handle Simple Voice Chat packet", e)
                }
            }
        }

        return super.handleP2S(packet, listeners)
    }

}
