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
package net.raphimc.viaproxy.plugins.events

import com.google.common.net.HostAndPort
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.channel.Channel
import net.raphimc.netminecraft.constants.IntendedState
import net.raphimc.viaproxy.plugins.events.types.EventCancellable
import java.net.SocketAddress

class PreConnectEvent(
    var serverAddress: SocketAddress,
    var serverVersion: ProtocolVersion,
    val clientVersion: ProtocolVersion,
    val clientHandshakeAddress: HostAndPort,
    val intendedState: IntendedState,
    val clientChannel: Channel
) : EventCancellable() {

    var cancelMessage: String = "§cCould not connect to the backend server! (Server is blacklisted)"
        set(value) {
            this.setCancelled(true)
            field = value
        }

}
