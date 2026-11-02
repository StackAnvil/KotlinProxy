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

import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.api.type.Types
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.MCPackets
import net.raphimc.netminecraft.constants.MCPipeline
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.PacketTypes
import net.raphimc.netminecraft.packet.UnknownPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import java.util.BitSet

class ChatSignaturePacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    private val joinGameId = MCPackets.S2C_LOGIN.getId(proxyConnection.getClientVersion().getVersion())
    private val chatSessionUpdateId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(proxyConnection.getClientVersion().getVersion())
    private val chatMessageId = MCPackets.C2S_CHAT.getId(proxyConnection.getClientVersion().getVersion())

    @Throws(Exception::class)
    override fun handleC2P(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is UnknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            val user = this.proxyConnection.getUserConnection()

            if (packet.packetId == this.chatSessionUpdateId && (!this.isP2sEncrypted() || user.has(ChatSession1_19_3::class.java))) {
                return false
            } else if (packet.packetId == this.chatMessageId && user.has(ChatSession1_19_3::class.java)) {
                val chatSession = user.get(ChatSession1_19_3::class.java)!!

                val oldChatMessage = Unpooled.wrappedBuffer(packet.data)
                val message = PacketTypes.readString(oldChatMessage, 256) // message
                val timestamp = oldChatMessage.readLong() // timestamp
                val salt = oldChatMessage.readLong() // salt

                val metadata = MessageMetadata(null, timestamp, salt)
                val signature = chatSession.signChatMessage(metadata, message, arrayOf<PlayerMessageSignature>())

                val newChatMessageData = Unpooled.buffer()
                PacketTypes.writeString(newChatMessageData, message) // message
                newChatMessageData.writeLong(timestamp) // timestamp
                newChatMessageData.writeLong(salt) // salt
                Types.OPTIONAL_SIGNATURE_BYTES.write(newChatMessageData, signature) // signature
                PacketTypes.writeVarInt(newChatMessageData, 0) // offset
                Types.ACKNOWLEDGED_BIT_SET.write(newChatMessageData, BitSet(20)) // acknowledged
                if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_5)) {
                    newChatMessageData.writeByte(0) // checksum
                }
                packet.data = ByteBufUtil.getBytes(newChatMessageData)
            }
        }

        return true
    }

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is UnknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            val user = this.proxyConnection.getUserConnection()

            if (packet.packetId == this.joinGameId && this.isP2sEncrypted() && user.has(ChatSession1_19_3::class.java)) {
                val chatSession = user.get(ChatSession1_19_3::class.java)!!
                listeners.add(ChannelFutureListener { f ->
                    if (f.isSuccess()) {
                        val chatSessionUpdate = Unpooled.buffer()
                        PacketTypes.writeVarInt(chatSessionUpdate, this.chatSessionUpdateId)
                        PacketTypes.writeUuid(chatSessionUpdate, chatSession.getSessionId()) // session id
                        Types.PROFILE_KEY.write(chatSessionUpdate, chatSession.getProfileKey()) // profile key
                        this.proxyConnection.getChannel().writeAndFlush(chatSessionUpdate).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                    }
                })
            }
        }

        return true
    }

    private fun isP2sEncrypted(): Boolean {
        return this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).get() != null
    }

}
