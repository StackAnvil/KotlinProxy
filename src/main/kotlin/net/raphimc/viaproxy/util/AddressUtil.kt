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
package net.raphimc.viaproxy.util

import com.google.common.net.HostAndPort
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.channel.unix.DomainSocketAddress
import net.lenni0451.mcping.ServerAddress
import net.lenni0451.reflect.stream.RStream
import net.raphimc.netminecraft.util.MinecraftServerAddress
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.viaproxy.util.address.NetherNetLanAddress
import net.raphimc.viaproxy.util.address.NetherNetXboxAddress
import net.raphimc.viaproxy.util.address.NetherNetXboxJsonRpcAddress
import java.net.InetSocketAddress
import java.net.SocketAddress

object AddressUtil {

    @JvmStatic
    fun parse(serverAddress: String, version: ProtocolVersion?): SocketAddress {
        if (serverAddress.startsWith("file://") || serverAddress.startsWith("unix://")) { // Unix Socket
            return DomainSocketAddress(serverAddress.substring(serverAddress.indexOf("://") + 3))
        } else if (serverAddress.startsWith("nethernet-lan://")) { // NetherNet LAN Address
            val hostAndPort = HostAndPort.fromString(serverAddress.substring(serverAddress.indexOf("://") + 3))
            if (hostAndPort.host.isBlank()) {
                throw IllegalArgumentException("Server address cannot be blank")
            }
            return NetherNetLanAddress(hostAndPort.host, hostAndPort.getPortOrDefault(ServerAddress.DEFAULT_BEDROCK_NETHERNET_LAN_PORT))
        } else if (serverAddress.startsWith("nethernet-xbox://")) { // NetherNet Xbox Address
            return NetherNetXboxAddress(serverAddress.substring(serverAddress.indexOf("://") + 3))
        } else if (serverAddress.startsWith("nethernet-xbox-json-rpc://")) { // NetherNet XBOX JSON RPC Address
            return NetherNetXboxJsonRpcAddress(serverAddress.substring(serverAddress.indexOf("://") + 3))
        } else { // IP Address
            val hostAndPort = HostAndPort.fromString(serverAddress)
            if (hostAndPort.host.isBlank()) {
                throw IllegalArgumentException("Server address cannot be blank")
            }

            val port: Int
            if (version != null) {
                port = hostAndPort.getPortOrDefault(if (version == BedrockProtocolVersion.BEDROCK_LATEST) ServerAddress.DEFAULT_BEDROCK_PORT else ServerAddress.DEFAULT_JAVA_PORT)
            } else {
                port = hostAndPort.getPort()
            }

            return if (version == null || version.olderThan(LegacyProtocolVersion.r1_3_1tor1_3_2) || version == BedrockProtocolVersion.BEDROCK_LATEST) {
                InetSocketAddress(hostAndPort.host, port)
            } else {
                MinecraftServerAddress.ofResolved(hostAndPort.host, port)
            }
        }
    }

    @JvmStatic
    fun toString(address: SocketAddress): String {
        return when (address) {
            is DomainSocketAddress -> "unix://" + address.path()
            is NetherNetLanAddress -> "nethernet-lan://" + address.getHostString() + ":" + address.getPort()
            is NetherNetXboxAddress -> "nethernet-xbox://" + address
            is NetherNetXboxJsonRpcAddress -> "nethernet-xbox-rpc://" + address
            is InetSocketAddress -> address.getHostString() + ":" + address.getPort()
            else -> address.toString()
        }
    }

    @Deprecated("Deprecated for removal")
    @JvmStatic
    fun getDefaultPort(version: ProtocolVersion): Int {
        if (version == BedrockProtocolVersion.BEDROCK_LATEST) {
            return ServerAddress.DEFAULT_BEDROCK_PORT
        }

        return ServerAddress.DEFAULT_JAVA_PORT
    }

    /**
     * Converts the netty domain socket address to a Java 16 unix domain socket address if possible
     *
     * @param address The netty domain socket address
     * @return The Java 16 unix domain socket address or the original address if it is not a domain socket address
     */
    @JvmStatic
    fun toJ16UnixSocketAddress(address: SocketAddress): SocketAddress {
        try {
            if (address is DomainSocketAddress) {
                return RStream.of("java.net.UnixDomainSocketAddress").methods().by("of", String::class.java).invokeArgs<SocketAddress>(address.path())
            }
        } catch (ignored: Throwable) {
        }

        return address
    }

}
