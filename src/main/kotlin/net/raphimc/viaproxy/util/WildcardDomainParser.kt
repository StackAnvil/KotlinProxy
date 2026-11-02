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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator
import java.net.SocketAddress
import java.util.Locale
import java.util.regex.Pattern

object WildcardDomainParser {

    private val PUBLIC_WILDCARD_FORMAT2_PATTERN = Pattern.compile("^address\\.(.+?)\\.port\\.(\\d+?)(?:\\.version\\.(.+?))?$")

    @JvmStatic
    fun parseFormat1(address: String): ParsedDomain? {
        try {
            val addressData = address.substring(0, address.lowercase(Locale.getDefault()).lastIndexOf(".viaproxy."))
            val arrayHelper = ArrayHelper.instanceOf(*Pattern.compile("_").split(addressData))
            if (arrayHelper.getLength() < 3) {
                return null
            }
            val versionString = arrayHelper.get(arrayHelper.getLength() - 1)!!
            val serverVersion = ProtocolVersionUtil.fromNameLenient(versionString)
            val connectAddress = arrayHelper.getAsString(0, arrayHelper.getLength() - 3, "_")
            val connectPort = arrayHelper.getInteger(arrayHelper.getLength() - 2)
            return ParsedDomain(AddressUtil.parse(connectAddress + ":" + connectPort, serverVersion), serverVersion)
        } catch (e: IllegalArgumentException) {
            return null
        } catch (e: StringIndexOutOfBoundsException) {
            return null
        }
    }

    @JvmStatic
    fun parseFormat2(address: String): ParsedDomain? {
        try {
            val addressData = address.substring(0, address.lowercase(Locale.getDefault()).lastIndexOf(".f2.viaproxy."))
            val matcher = PUBLIC_WILDCARD_FORMAT2_PATTERN.matcher(addressData)
            if (!matcher.matches()) {
                return null
            }
            val connectAddress = matcher.group(1)
            val connectPort = matcher.group(2).toInt()
            val versionString = matcher.group(3)
            val serverVersion = if (versionString != null) { // Version part is optional
                ProtocolVersionUtil.fromNameLenient(versionString)
            } else { // Default to auto-detect
                ProtocolTranslator.AUTO_DETECT_PROTOCOL
            }
            return ParsedDomain(AddressUtil.parse(connectAddress + ":" + connectPort, serverVersion), serverVersion)
        } catch (e: IllegalArgumentException) {
            return null
        } catch (e: StringIndexOutOfBoundsException) {
            return null
        }
    }


    data class ParsedDomain(val address: SocketAddress, val version: ProtocolVersion?) {

        fun address(): SocketAddress = this.address

        fun version(): ProtocolVersion? = this.version

    }

}
