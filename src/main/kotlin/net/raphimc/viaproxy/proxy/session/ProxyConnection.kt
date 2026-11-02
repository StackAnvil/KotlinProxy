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

import com.google.common.net.HostAndPort
import com.google.gson.JsonPrimitive
import com.mojang.authlib.GameProfile
import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.util.AttributeKey
import net.lenni0451.mcstructs.text.components.StringComponent
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.MCPipeline
import net.raphimc.netminecraft.netty.connection.NetClient
import net.raphimc.netminecraft.netty.crypto.AESEncryption
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigDisconnectPacket
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket
import net.raphimc.netminecraft.packet.impl.play.S2CPlayDisconnectPacket
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry
import net.raphimc.netminecraft.util.TransportType
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.cli.ConsoleFormatter
import net.raphimc.viaproxy.proxy.packethandler.PacketHandler
import net.raphimc.viaproxy.proxy.util.CloseAndReturn
import net.raphimc.viaproxy.util.logging.Logger
import java.net.SocketAddress
import java.security.GeneralSecurityException
import java.security.Key

open class ProxyConnection(
    channelInitializer: ChannelInitializer<Channel>?,
    private val c2p: Channel
) : NetClient(channelInitializer) {

    companion object {
        @JvmField
        val PROXY_CONNECTION_ATTRIBUTE_KEY: AttributeKey<ProxyConnection> = AttributeKey.valueOf("proxy_connection")

        @JvmStatic
        fun fromChannel(channel: Channel): ProxyConnection {
            return channel.attr(PROXY_CONNECTION_ATTRIBUTE_KEY).get()!!
        }

        @JvmStatic
        fun fromUserConnection(userConnection: UserConnection): ProxyConnection {
            return fromChannel(userConnection.getChannel()!!)
        }
    }

    private val packetHandlers: MutableList<PacketHandler> = ArrayList()

    private var serverAddress: SocketAddress? = null

    private var serverVersion: ProtocolVersion? = null
    private var clientVersion: ProtocolVersion? = null

    private var clientHandshakeAddress: HostAndPort? = null
    private var gameProfile: GameProfile? = null
    private var loginHelloPacket: C2SLoginHelloPacket? = null
    private var storedSecretKey: Key? = null

    private var userConnection: UserConnection? = null
    private var userOptions: UserOptions? = null

    private var c2pConnectionState: ConnectionState = ConnectionState.HANDSHAKING
    private var p2sConnectionState: ConnectionState = ConnectionState.HANDSHAKING

    @Deprecated("Deprecated in Java")
    override fun connect(address: SocketAddress): ChannelFuture {
        throw UnsupportedOperationException()
    }

    override fun initialize(transportType: TransportType, bootstrap: Bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
        bootstrap.attr(PROXY_CONNECTION_ATTRIBUTE_KEY, this)
        super.initialize(transportType, bootstrap)
    }

    open fun connectToServer(serverAddress: SocketAddress?, targetVersion: ProtocolVersion?): ChannelFuture {
        this.serverAddress = serverAddress
        this.serverVersion = targetVersion
        return super.connect(serverAddress)
    }

    fun getC2P(): Channel {
        return this.c2p
    }

    fun getPacketHandlers(): MutableList<PacketHandler> {
        return this.packetHandlers
    }

    fun <T> getPacketHandler(packetHandlerType: Class<T>): T? {
        for (packetHandler in this.packetHandlers) {
            if (packetHandlerType.isInstance(packetHandler)) {
                return packetHandlerType.cast(packetHandler)
            }
        }
        return null
    }

    open fun getServerAddress(): SocketAddress {
        return this.serverAddress!!
    }

    open fun getServerVersion(): ProtocolVersion {
        return this.serverVersion!!
    }

    open fun getClientVersion(): ProtocolVersion {
        return this.clientVersion!!
    }

    open fun setClientVersion(clientVersion: ProtocolVersion?) {
        this.clientVersion = clientVersion
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(DefaultPacketRegistry(false, clientVersion!!.getVersion()))
    }

    open fun getClientHandshakeAddress(): HostAndPort {
        return this.clientHandshakeAddress!!
    }

    open fun setClientHandshakeAddress(clientHandshakeAddress: HostAndPort?) {
        this.clientHandshakeAddress = clientHandshakeAddress
    }

    open fun getGameProfile(): GameProfile? {
        return this.gameProfile
    }

    open fun setGameProfile(gameProfile: GameProfile?) {
        this.gameProfile = gameProfile
    }

    open fun getLoginHelloPacket(): C2SLoginHelloPacket {
        return this.loginHelloPacket!!
    }

    open fun setLoginHelloPacket(loginHelloPacket: C2SLoginHelloPacket?) {
        this.loginHelloPacket = loginHelloPacket
    }

    open fun setKeyForPreNettyEncryption(key: Key?) {
        this.storedSecretKey = key
    }

    @Throws(GeneralSecurityException::class)
    open fun enablePreNettyEncryption() {
        this.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(AESEncryption(this.storedSecretKey))
    }

    open fun getUserConnection(): UserConnection {
        return this.userConnection!!
    }

    open fun setUserConnection(userConnection: UserConnection?) {
        this.userConnection = userConnection
    }

    open fun getUserOptions(): UserOptions {
        return this.userOptions!!
    }

    open fun setUserOptions(userOptions: UserOptions?) {
        this.userOptions = userOptions
    }

    open fun getC2pConnectionState(): ConnectionState {
        return this.c2pConnectionState
    }

    open fun getP2sConnectionState(): ConnectionState {
        return this.p2sConnectionState
    }

    open fun setC2pConnectionState(connectionState: ConnectionState) {
        this.c2pConnectionState = connectionState
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState)
    }

    open fun setP2sConnectionState(connectionState: ConnectionState) {
        this.p2sConnectionState = connectionState
        if (this.getChannel() != null) {
            this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState)
        }
    }

    @Throws(CloseAndReturn::class)
    fun kickClient(message: String) {
        Logger.u_err("proxy kick", this, ConsoleFormatter.convert(message))

        val future: ChannelFuture
        if (this.c2pConnectionState == ConnectionState.STATUS) {
            future = this.c2p.writeAndFlush(S2CStatusResponsePacket("{\"players\":{\"max\":0,\"online\":0},\"description\":" + JsonPrimitive(message) + ",\"version\":{\"protocol\":-1,\"name\":\"ViaProxy\"}}"))
        } else if (this.c2pConnectionState == ConnectionState.LOGIN) {
            future = this.c2p.writeAndFlush(S2CLoginDisconnectPacket(StringComponent(message)))
        } else if (this.c2pConnectionState == ConnectionState.CONFIGURATION) {
            future = this.c2p.writeAndFlush(S2CConfigDisconnectPacket(StringComponent(message)))
        } else if (this.c2pConnectionState == ConnectionState.PLAY) {
            future = this.c2p.writeAndFlush(S2CPlayDisconnectPacket(StringComponent(message)))
        } else {
            future = this.c2p.newSucceededFuture()
        }

        future.addListener(ChannelFutureListener.CLOSE)
        throw CloseAndReturn.INSTANCE
    }

    open fun isClosed(): Boolean {
        return !this.c2p.isOpen() || (this.getChannel() != null && !this.getChannel().isOpen())
    }

}
