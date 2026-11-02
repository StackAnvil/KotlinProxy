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

import com.google.common.cache.CacheBuilder
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.CookieStorage
import io.netty.channel.Channel
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.time.temporal.ChronoUnit

object TransferDataHolder {

    private val TEMP_REDIRECTS: MutableMap<InetAddress, InetSocketAddress> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.of(1, ChronoUnit.MINUTES))
        .build<InetAddress, InetSocketAddress>()
        .asMap()
    private val COOKIE_STORAGES: MutableMap<InetAddress, CookieStorage> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.of(1, ChronoUnit.MINUTES))
        .build<InetAddress, CookieStorage>()
        .asMap()

    @JvmStatic
    fun addTempRedirect(channel: Channel, redirect: InetSocketAddress) {
        TEMP_REDIRECTS.put(getChannelAddress(channel), redirect)
    }

    @JvmStatic
    fun addCookieStorage(channel: Channel, cookieStorage: CookieStorage) {
        COOKIE_STORAGES.put(getChannelAddress(channel), cookieStorage)
    }

    @JvmStatic
    fun removeTempRedirect(channel: Channel): InetSocketAddress? {
        return TEMP_REDIRECTS.remove(getChannelAddress(channel))
    }

    @JvmStatic
    fun removeCookieStorage(channel: Channel): CookieStorage? {
        return COOKIE_STORAGES.remove(getChannelAddress(channel))
    }

    @JvmStatic
    fun hasTempRedirect(channel: Channel): Boolean {
        return TEMP_REDIRECTS.containsKey(getChannelAddress(channel))
    }

    @JvmStatic
    fun hasCookieStorage(channel: Channel): Boolean {
        return COOKIE_STORAGES.containsKey(getChannelAddress(channel))
    }

    private fun getChannelAddress(channel: Channel): InetAddress {
        return (channel.remoteAddress() as InetSocketAddress).getAddress()
    }

}
