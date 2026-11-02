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

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.channel.ChannelFutureListener
import net.lenni0451.mcstructs.text.TextComponent
import net.lenni0451.mcstructs.text.serializer.TextComponentSerializer
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.MCPackets
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.UnknownPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayCustomPayloadPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayResourcePackPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayResourcePackPushPacket
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.logging.Logger
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

class ResourcePackPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    private val joinGameId = MCPackets.S2C_LOGIN.getId(this.proxyConnection.getClientVersion().getVersion())

    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is UnknownPacket && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            if (packet.packetId == this.joinGameId) {
                listeners.add(ChannelFutureListener { f ->
                    if (f.isSuccess()) {
                        this.sendResourcePack()
                    }
                })
            }
        }

        return true
    }

    private fun sendResourcePack() {
        this.proxyConnection.getChannel().eventLoop().schedule(Runnable {
            try {
                val url = ViaProxy.getConfig().getResourcePackUrl()
                val required = Via.getConfig().isForcedUse1_17ResourcePack()
                val message: TextComponent?
                if (Via.getConfig().get1_17ResourcePackPrompt() != null) {
                    message = TextComponentSerializer.LATEST.deserialize(Via.getConfig().get1_17ResourcePackPrompt().toString())
                } else {
                    message = null
                }

                if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_3)) {
                    this.proxyConnection.getC2P().writeAndFlush(S2CPlayResourcePackPushPacket(UUID.randomUUID(), url, "", required, message)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                } else if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_8)) {
                    this.proxyConnection.getC2P().writeAndFlush(S2CPlayResourcePackPacket(url, "", required, message)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                } else if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_7_2)) {
                    val data = url!!.toByteArray(StandardCharsets.UTF_8)
                    this.proxyConnection.getC2P().writeAndFlush(S2CPlayCustomPayloadPacket("MC|RPack", data)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                }
            } catch (e: Throwable) {
                Logger.LOGGER.warn("Failed to send resource pack", e)
            }
        }, 250, TimeUnit.MILLISECONDS)
    }

}
