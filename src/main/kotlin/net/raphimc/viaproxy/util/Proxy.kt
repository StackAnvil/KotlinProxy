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

import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.ProxyHandler
import io.netty.handler.proxy.Socks4ProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import net.lenni0451.commons.httpclient.proxy.ProxyType
import net.lenni0451.commons.httpclient.utils.URLWrapper
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

class Proxy(
    protocol: String,
    val address: SocketAddress,
    val username: String?,
    val password: String?
) {

    val protocol: String = protocol.uppercase(Locale.ROOT)

    constructor(uri: URI) : this(uri.scheme, InetSocketAddress(uri.host, uri.port), uri.userInfo)

    constructor(protocol: String, address: SocketAddress) : this(protocol, address, null, null)

    constructor(protocol: String, address: SocketAddress, userInfo: String?) : this(
        protocol,
        address,
        if (userInfo != null) Pattern.compile(":").split(userInfo, 2)[0] else null,
        if (userInfo != null) Pattern.compile(":").split(userInfo, 2).let { if (it.size > 1) it[1] else null } else null
    )

    fun createNettyProxyHandler(): ProxyHandler {
        return when (this.protocol) {
            "HTTP", "HTTPS" -> {
                if (this.username != null && this.password != null) HttpProxyHandler(this.address, this.username, this.password)
                else HttpProxyHandler(this.address)
            }
            "SOCKS4" -> {
                if (this.username != null) Socks4ProxyHandler(this.address, this.username)
                else Socks4ProxyHandler(this.address)
            }
            "SOCKS5" -> {
                if (this.username != null && this.password != null) Socks5ProxyHandler(this.address, this.username, this.password)
                else Socks5ProxyHandler(this.address)
            }
            else -> throw IllegalArgumentException("Unsupported proxy protocol: " + this.protocol)
        }
    }

    fun toJavaProxy(): java.net.Proxy {
        val proxyType = when (this.protocol) {
            "HTTP", "HTTPS" -> ProxyType.HTTP
            "SOCKS4" -> ProxyType.SOCKS4
            "SOCKS5" -> ProxyType.SOCKS5
            else -> throw IllegalArgumentException("Unsupported proxy protocol: " + this.protocol)
        }
        return net.lenni0451.commons.httpclient.proxy.ProxyHandler(proxyType, this.address, this.username, this.password).toJavaProxy()
    }

    fun toURI(): URI {
        var userInfo: String? = null
        if (this.username != null) {
            userInfo = this.username
            if (this.password != null) {
                userInfo += ":" + this.password
            }
        }
        return URLWrapper.empty().setProtocol(this.protocol.lowercase(Locale.ROOT)).setHost(AddressUtil.toString(this.address)).setUserInfo(userInfo).toURI()
    }

}
