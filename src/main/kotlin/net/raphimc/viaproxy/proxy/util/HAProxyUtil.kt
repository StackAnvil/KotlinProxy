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
package net.raphimc.viaproxy.proxy.util

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.handler.codec.haproxy.HAProxyCommand
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion
import io.netty.handler.codec.haproxy.HAProxyTLV
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

object HAProxyUtil {

    @JvmStatic
    fun createMessage(sourceChannel: Channel, targetChannel: Channel, clientVersion: ProtocolVersion?): HAProxyMessage {
        val tlvs = ArrayList<HAProxyTLV>()
        if (clientVersion != null) {
            tlvs.add(HAProxyTLV(0xE0.toByte(), Unpooled.buffer().writeInt(clientVersion.getOriginalVersion())))
        }

        val sourceRemoteAddress = sourceChannel.remoteAddress()
        val targetRemoteAddress = targetChannel.remoteAddress()
        if (sourceRemoteAddress is InetSocketAddress && targetRemoteAddress is InetSocketAddress) {
            val sourceAddress = sourceRemoteAddress
            val targetAddress = targetRemoteAddress
            val protocol = if (sourceAddress.getAddress() is Inet4Address) HAProxyProxiedProtocol.TCP4 else HAProxyProxiedProtocol.TCP6
            val sourceAddressString = sourceAddress.getAddress().getHostAddress()
            val targetAddressString: String
            if (protocol.addressFamily() == HAProxyProxiedProtocol.AddressFamily.AF_IPv4) {
                targetAddressString = getInetAddress(targetAddress.getHostString(), Inet4Address::class.java)!!.getHostAddress()
            } else {
                targetAddressString = getInetAddress(targetAddress.getHostString(), Inet6Address::class.java)!!.getHostAddress()
            }

            return HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, protocol, sourceAddressString, targetAddressString, sourceAddress.getPort(), targetAddress.getPort(), tlvs)
        } else if (targetRemoteAddress is DomainSocketAddress) {
            return HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.UNIX_STREAM, "", targetRemoteAddress.path(), 0, 0, tlvs)
        } else {
            throw IllegalArgumentException("Unsupported address type: " + targetChannel.remoteAddress()!!.javaClass.name)
        }
    }

    private fun <T : InetAddress> getInetAddress(host: String, addressClass: Class<T>): T? {
        try {
            val addresses = InetAddress.getAllByName(host)
            for (addr in addresses) {
                if (addressClass.isInstance(addr)) {
                    return addressClass.cast(addr)
                }
            }
        } catch (e: UnknownHostException) {
            throw RuntimeException(e)
        }
        return null
    }

}
