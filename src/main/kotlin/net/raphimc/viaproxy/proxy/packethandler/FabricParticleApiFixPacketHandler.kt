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

import com.google.common.collect.Lists
import com.viaversion.viaversion.util.Key
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.impl.common.C2SCustomPayloadPacket
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

// See https://github.com/ViaVersion/ViaFabric/issues/428
class FabricParticleApiFixPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    companion object {
        private const val REGISTER_CHANNEL = "minecraft:register"
        private const val UNREGISTER_CHANNEL = "minecraft:unregister"
    }

    @Throws(Exception::class)
    override fun handleC2P(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is C2SCustomPayloadPacket) {
            val channel = Key.namespaced(packet.channel)
            if (channel == REGISTER_CHANNEL || channel == UNREGISTER_CHANNEL) {
                val channels = Lists.newArrayList(*Pattern.compile("\u0000").split(String(packet.data, StandardCharsets.UTF_8)))
                if (channels.remove("fabric:extended_block_state_particle_effect_sync") or channels.remove("fabric:extended_block_particle_option_sync")) {
                    if (!channels.isEmpty()) {
                        packet.data = channels.joinToString("\u0000").toByteArray(StandardCharsets.UTF_8)
                    } else {
                        return false // Cancel packet
                    }
                }
            }
        }

        return super.handleC2P(packet, listeners)
    }

}
