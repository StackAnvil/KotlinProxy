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

import com.google.common.hash.Hashing
import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.connection.UserConnection
import net.lenni0451.commons.httpclient.HttpClient
import net.lenni0451.commons.httpclient.handler.ThrowingResponseHandler
import net.lenni0451.commons.httpclient.requests.impl.GetRequest
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicMpPassProvider
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import java.nio.charset.StandardCharsets
import java.util.HexFormat
import java.util.logging.Level

class ViaProxyClassicMPPassProvider : ClassicMpPassProvider() {

    override fun getMpPass(user: UserConnection): String {
        val mppass = ProxyConnection.fromUserConnection(user)!!.getUserOptions()!!.classicMpPass()
        if (mppass != null && !mppass.isBlank()) {
            return mppass
        } else if (ViaProxy.getConfig().useBetacraftAuth()) {
            try {
                val httpClient = HttpClient()
                val externalIp: String
                try {
                    externalIp = httpClient.execute(GetRequest("https://checkip.amazonaws.com"), ThrowingResponseHandler()).content.asString
                } catch (e: Throwable) {
                    throw RuntimeException("Failed to get external IP address!", e)
                }
                val hash = Hashing.sha1().hashString(externalIp.trim(), StandardCharsets.UTF_8).asBytes()
                Via.getManager().providers.get(OldAuthProvider::class.java)!!.sendAuthRequest(user, HexFormat.of().formatHex(hash))
            } catch (e: Throwable) {
                Via.getPlatform().logger.log(Level.WARNING, "An unknown error occurred while authenticating with BetaCraft", e)
            }
        }

        return super.getMpPass(user)
    }

}
