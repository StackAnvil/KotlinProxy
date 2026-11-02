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
import com.mojang.authlib.GameProfile
import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket
import net.raphimc.netminecraft.util.TransportType
import java.net.SocketAddress
import java.security.Key

class DummyProxyConnection(c2p: Channel) : ProxyConnection(null, c2p) {

    override fun initialize(transportType: TransportType, bootstrap: Bootstrap) {
        throw UnsupportedOperationException()
    }

    override fun connectToServer(serverAddress: SocketAddress?, targetVersion: ProtocolVersion?): ChannelFuture {
        throw UnsupportedOperationException()
    }

    override fun getServerAddress(): SocketAddress {
        throw UnsupportedOperationException()
    }

    override fun getServerVersion(): ProtocolVersion {
        throw UnsupportedOperationException()
    }

    override fun getClientHandshakeAddress(): HostAndPort {
        throw UnsupportedOperationException()
    }

    override fun setClientHandshakeAddress(clientHandshakeAddress: HostAndPort?) {
        throw UnsupportedOperationException()
    }

    override fun getGameProfile(): GameProfile? {
        return null
    }

    override fun setGameProfile(gameProfile: GameProfile?) {
        throw UnsupportedOperationException()
    }

    override fun getLoginHelloPacket(): C2SLoginHelloPacket {
        throw UnsupportedOperationException()
    }

    override fun setLoginHelloPacket(loginHelloPacket: C2SLoginHelloPacket?) {
        throw UnsupportedOperationException()
    }

    override fun setKeyForPreNettyEncryption(key: Key?) {
        throw UnsupportedOperationException()
    }

    override fun enablePreNettyEncryption() {
        throw UnsupportedOperationException()
    }

    override fun getUserConnection(): UserConnection {
        throw UnsupportedOperationException()
    }

    override fun setUserConnection(userConnection: UserConnection?) {
        throw UnsupportedOperationException()
    }

    override fun getUserOptions(): UserOptions {
        throw UnsupportedOperationException()
    }

    override fun setUserOptions(userOptions: UserOptions?) {
        throw UnsupportedOperationException()
    }

    override fun getC2pConnectionState(): ConnectionState {
        return ConnectionState.HANDSHAKING
    }

    override fun getP2sConnectionState(): ConnectionState {
        throw UnsupportedOperationException()
    }

    override fun isClosed(): Boolean {
        return false
    }

}
