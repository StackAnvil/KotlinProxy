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
package net.raphimc.viaproxy.injection.mixins

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4
import com.viaversion.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2
import net.raphimc.viaproxy.ViaProxy
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [Protocol1_20To1_20_2::class], remap = false)
abstract class MixinProtocol1_20To1_20_2 {

    private companion object {
        @Inject(method = ["lambda\$queueServerboundPacket\$12"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun dontQueueConfigPackets(packetType: ServerboundPackets1_20_2, wrapper: PacketWrapper, ci: CallbackInfo) {
            if (ViaProxy.getConfig().shouldSkipConfigStatePacketQueue()) {
                ci.cancel()
                if (packetType == ServerboundPackets1_20_2.CUSTOM_PAYLOAD) {
                    wrapper.setPacketType(ServerboundPackets1_19_4.CUSTOM_PAYLOAD)
                } else if (packetType == ServerboundPackets1_20_2.KEEP_ALIVE) {
                    wrapper.setPacketType(ServerboundPackets1_19_4.KEEP_ALIVE)
                } else if (packetType == ServerboundPackets1_20_2.PONG) {
                    wrapper.setPacketType(ServerboundPackets1_19_4.PONG)
                } else {
                    throw IllegalStateException("Unexpected packet type: " + packetType)
                }
            }
        }
    }

}
