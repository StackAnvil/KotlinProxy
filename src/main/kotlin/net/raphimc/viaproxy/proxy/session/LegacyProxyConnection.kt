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
package net.raphimc.viaproxy.proxy.session

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.util.AttributeKey
import net.raphimc.netminecraft.netty.connection.NetClient
import net.raphimc.netminecraft.util.TransportType
import net.raphimc.viaproxy.ViaProxy
import java.net.SocketAddress

class LegacyProxyConnection(
    channelInitializer: ChannelInitializer<Channel>,
    c2p: Channel
) : NetClient(channelInitializer) {

    companion object {
        @JvmField
        val LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY: AttributeKey<LegacyProxyConnection> = AttributeKey.valueOf("legacy_proxy_connection")

        @JvmStatic
        fun fromChannel(channel: Channel): LegacyProxyConnection {
            return channel.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY).get()!!
        }
    }

    private val c2pConnection: Channel = c2p
    private var serverAddress: SocketAddress? = null

    override fun initialize(transportType: TransportType, bootstrap: Bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
        bootstrap.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY, this)
        super.initialize(transportType, bootstrap)
    }

    override fun connect(serverAddress: SocketAddress): ChannelFuture {
        this.serverAddress = serverAddress
        return super.connect(serverAddress)
    }

    fun getC2P(): Channel {
        return this.c2pConnection
    }

    fun getServerAddress(): SocketAddress? {
        return this.serverAddress
    }

}
