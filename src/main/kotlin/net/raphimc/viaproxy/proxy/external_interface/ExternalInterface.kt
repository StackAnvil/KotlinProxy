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
package net.raphimc.viaproxy.proxy.external_interface

import com.google.common.primitives.Longs
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.minecraft.ProfileKey
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_0
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_1
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.lenni0451.commons.httpclient.proxy.SingleProxyAuthenticator
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.viabedrock.protocol.storage.AuthData
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.FillPlayerDataEvent
import net.raphimc.viaproxy.plugins.events.JoinServerRequestEvent
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.CloseAndReturn
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount
import net.raphimc.viaproxy.util.Proxy
import net.raphimc.viaproxy.util.logging.Logger
import java.net.Authenticator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object ExternalInterface {

    @JvmStatic
    fun fillPlayerData(proxyConnection: ProxyConnection) {
        Logger.u_info("auth", proxyConnection, "Filling player data")
        try {
            if (proxyConnection.getUserOptions()!!.account() != null) {
                val account: Account = proxyConnection.getUserOptions()!!.account()!!

                proxyConnection.setGameProfile(account.getGameProfile())
                val user: UserConnection = proxyConnection.getUserConnection()!!

                if (ViaProxy.getConfig().shouldSignChat() && proxyConnection.getServerVersion()!!.newerThanOrEqualTo(ProtocolVersion.v1_19) && account is MicrosoftAccount) {
                    val playerCertificates: MinecraftPlayerCertificates = account.getAuthManager().getMinecraftPlayerCertificates().getUpToDate()
                    val expiresAt: Instant = Instant.ofEpochMilli(playerCertificates.getExpireTimeMs())
                    val expiresAtMillis: Long = playerCertificates.getExpireTimeMs()
                    val publicKey: PublicKey = playerCertificates.getKeyPair().getPublic()
                    val publicKeyBytes: ByteArray = publicKey.getEncoded()
                    val keySignature: ByteArray = playerCertificates.getPublicKeySignature()
                    val privateKey: PrivateKey = playerCertificates.getKeyPair().getPrivate()
                    val uuid: UUID = proxyConnection.getGameProfile()!!.getId()

                    var loginHelloKeySignature: ByteArray = keySignature
                    if (proxyConnection.getClientVersion()!!.equals(ProtocolVersion.v1_19)) {
                        loginHelloKeySignature = playerCertificates.getLegacyPublicKeySignature()
                    }
                    proxyConnection.setLoginHelloPacket(C2SLoginHelloPacket(proxyConnection.getGameProfile()!!.getName(), expiresAt, publicKey, loginHelloKeySignature, proxyConnection.getGameProfile()!!.getId()))

                    user.put(ChatSession1_19_0(uuid, privateKey, ProfileKey(expiresAtMillis, publicKeyBytes, playerCertificates.getLegacyPublicKeySignature())))
                    user.put(ChatSession1_19_1(uuid, privateKey, ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)))
                    user.put(ChatSession1_19_3(uuid, privateKey, ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)))
                } else if (proxyConnection.getServerVersion()!!.equals(BedrockProtocolVersion.BEDROCK_LATEST) && account is BedrockAccount) {
                    val multiplayerToken: MinecraftMultiplayerToken = account.getAuthManager().getMinecraftMultiplayerToken().getUpToDate()
                    user.put(AuthData(multiplayerToken.getToken(), account.getAuthManager().getSessionKeyPair(), account.getAuthManager().getDeviceId()))
                }
            }

            ViaProxy.EVENT_MANAGER.call(FillPlayerDataEvent(proxyConnection))
        } catch (e: CloseAndReturn) {
            throw e
        } catch (e: Throwable) {
            Logger.LOGGER.error("Failed to fill player data", e)
            proxyConnection.kickClient("§cFailed to fill player data. This might be caused by outdated account tokens or rate limits. Wait a couple of seconds and try again. If the problem persists, remove and re-add your account.")
        }

        proxyConnection.getLoginHelloPacket()!!.name = proxyConnection.getGameProfile()!!.getName()
        proxyConnection.getLoginHelloPacket()!!.uuid = proxyConnection.getGameProfile()!!.getId()
    }

    @JvmStatic
    fun joinServer(serverIdHash: String, proxyConnection: ProxyConnection) {
        Logger.u_info("auth", proxyConnection, "Trying to join online mode server")
        try {
            val account = proxyConnection.getUserOptions()!!.account()
            if (account is MicrosoftAccount) {
                try {
                    if (ViaProxy.getConfig().getBackendProxy() == null) {
                        AuthLibServices.SESSION_SERVICE.joinServer(account.getGameProfile().getId(), account.getAuthManager().getMinecraftToken().getUpToDate().getToken(), serverIdHash)
                    } else {
                        val proxy: Proxy = ViaProxy.getConfig().getBackendProxy()!!
                        val sessionService: MinecraftSessionService = YggdrasilAuthenticationService(proxy.toJavaProxy()).createMinecraftSessionService()
                        val prevAuthenticator: Authenticator = Authenticator.getDefault()
                        try {
                            if (proxy.username != null && proxy.password != null) {
                                Authenticator.setDefault(SingleProxyAuthenticator(proxy.username, proxy.password))
                            }
                            sessionService.joinServer(account.getGameProfile().getId(), account.getAuthManager().getMinecraftToken().getUpToDate().getToken(), serverIdHash)
                        } finally {
                            Authenticator.setDefault(prevAuthenticator)
                        }
                    }
                } catch (e: Throwable) {
                    proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again in a couple of seconds.")
                }
            } else if (!ViaProxy.EVENT_MANAGER.call(JoinServerRequestEvent(proxyConnection, serverIdHash)).isCancelled()) {
                proxyConnection.kickClient("§cThe configured target server is in online mode and requires a valid authentication mode.")
            }
        } catch (e: CloseAndReturn) {
            throw e
        } catch (e: Throwable) {
            Logger.LOGGER.error("Failed to join online mode server", e)
            proxyConnection.kickClient("§cFailed to join online mode server. See console for more information.")
        }
    }

    @JvmStatic
    @Throws(SignatureException::class)
    fun signNonce(nonce: ByteArray, packet: C2SLoginKeyPacket, proxyConnection: ProxyConnection) {
        Logger.u_info("auth", proxyConnection, "Requesting nonce signature")
        val user: UserConnection = proxyConnection.getUserConnection()!!

        if (user.has(ChatSession1_19_0::class.java)) {
            val salt: Long = ThreadLocalRandom.current().nextLong()
            packet.signature = user.get(ChatSession1_19_0::class.java)!!.sign { updater ->
                updater.accept(nonce)
                updater.accept(Longs.toByteArray(salt))
            }
            packet.salt = salt
        } else {
            proxyConnection.kickClient("§cThe configured target server requires a signed nonce. Please enable chat signing in the config and select a valid authentication mode.")
        }
    }

}
