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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken
import net.raphimc.netminecraft.util.EventLoops
import net.raphimc.netminecraft.util.TransportType
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount
import net.raphimc.viaproxy.util.address.NetherNetLanAddress
import net.raphimc.viaproxy.util.address.NetherNetXboxAddress
import net.raphimc.viaproxy.util.address.NetherNetXboxJsonRpcAddress
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetClientSignaling
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetDiscoverySignaling
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPClientSignaling
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetXboxSignaling
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity
import java.net.SocketAddress

class BedrockProxyConnection(
    channelInitializer: ChannelInitializer<Channel>,
    c2p: Channel
) : ProxyConnection(channelInitializer, c2p) {

    private var serverAddress: SocketAddress? = null

    override fun initialize(transportType: TransportType, bootstrap: Bootstrap) {
        val serverAddress = this.serverAddress
        val netherNetSignaling: NetherNetClientSignaling
        if (serverAddress is NetherNetLanAddress) {
            netherNetSignaling = NetherNetDiscoverySignaling()
        } else if (serverAddress is NetherNetXboxAddress || serverAddress is NetherNetXboxJsonRpcAddress) {
            val account = this.getUserOptions()!!.account()
            if (account is BedrockAccount) {
                if (serverAddress is NetherNetXboxAddress) {
                    netherNetSignaling = NetherNetXboxSignaling(account.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader())
                } else if (serverAddress is NetherNetXboxJsonRpcAddress) {
                    netherNetSignaling = NetherNetXboxRpcSignaling(account.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader())
                } else {
                    throw IllegalStateException("Unsupported NetherNet address type: " + serverAddress.javaClass.name)
                }
            } else {
                this.kickClient("§cThe configured target server requires Xbox signaling, but no Minecraft: Bedrock Edition account is selected.")
                return
            }
        } else {
            netherNetSignaling = NetherNetHTTPClientSignaling()
        }

        val identity: OperatorIdentity?
        val account = this.getUserOptions()!!.account()
        if (account is BedrockAccount) {
            val authManager: BedrockAuthManager = account.getAuthManager()
            val multiplayerToken: MinecraftMultiplayerToken = authManager.getMinecraftMultiplayerToken().getUpToDateUnchecked()
            identity = OperatorIdentity.fromToken(authManager.getSessionKeyPair(), multiplayerToken.getToken(), "https://authorization.franchise.minecraft-services.net/")
        } else {
            identity = null
        }

        bootstrap
            .group(EventLoops.getClientEventLoop(TransportType.NIO))
            .channelFactory(NetherNetChannelFactory.client(netherNetSignaling))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
            .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, ViaProxy.getConfig().getConnectTimeout())
            .option(NetherChannelOption.NETHER_CLIENT_MAX_HANDSHAKE_ATTEMPTS, 1)
            .option(NetherChannelOption.NETHER_CLIENT_IDENTITY, identity)
            .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
            .handler(this.channelInitializer)
        this.channelFuture = bootstrap.register().syncUninterruptibly()
    }

    override fun connectToServer(serverAddress: SocketAddress?, targetVersion: ProtocolVersion?): ChannelFuture {
        this.serverAddress = serverAddress
        return super.connectToServer(serverAddress, targetVersion)
    }

}
