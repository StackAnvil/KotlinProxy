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
package net.raphimc.viaproxy.protocoltranslator.providers

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.api.protocol.version.VersionType
import com.viaversion.viaversion.protocol.SpecialProtocolVersion
import com.viaversion.viaversion.protocol.version.BaseVersionProvider
import net.raphimc.netminecraft.constants.MCVersion
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import java.util.Comparator

class ViaProxyVersionProvider : BaseVersionProvider() {

    override fun getClientProtocol(connection: UserConnection): ProtocolVersion? {
        val clientProtocol = connection.getProtocolInfo().protocolVersion()
        if (!clientProtocol.isKnown() && ProtocolVersion.isRegistered(VersionType.SPECIAL, clientProtocol.getOriginalVersion())) {
            return ProtocolVersion.getProtocol(VersionType.SPECIAL, clientProtocol.getOriginalVersion())
        } else {
            return super.getClientProtocol(connection)
        }
    }

    override fun getClosestServerProtocol(connection: UserConnection): ProtocolVersion? {
        val clientProtocol = connection.getProtocolInfo().protocolVersion()
        if (connection.isClientSide) {
            return ProxyConnection.fromUserConnection(connection)!!.getServerVersion()
        } else if (clientProtocol.getVersionType() == VersionType.RELEASE) {
            if (MCVersion.ALL_VERSIONS.containsKey(clientProtocol.getVersion())) {
                return clientProtocol
            } else { // Version not supported by NetMinecraft
                return MCVersion.ALL_VERSIONS.keys.stream().min(Comparator { o1: Int, o2: Int ->
                    val diff1 = Math.abs(o1 - clientProtocol.getVersion())
                    val diff2 = Math.abs(o2 - clientProtocol.getVersion())
                    Integer.compare(diff1, diff2)
                }).map { ProtocolVersion.getProtocol(it) }.orElse(ProtocolVersion.unknown)
            }
        } else if (clientProtocol is SpecialProtocolVersion) {
            return clientProtocol.getDelegate()
        } else {
            return ProtocolVersion.v1_7_2
        }
    }

}
