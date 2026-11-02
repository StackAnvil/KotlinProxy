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
package net.raphimc.viaproxy.proxy.packethandler

import com.mojang.authlib.GameProfile
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.channel.ChannelFutureListener
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.MCPipeline
import net.raphimc.netminecraft.netty.crypto.AESEncryption
import net.raphimc.netminecraft.netty.crypto.CryptUtil
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket
import net.raphimc.netminecraft.packet.impl.login.S2CLoginHelloPacket
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.vialegacy.api.util.GameProfileUtil
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.storage.ProtocolMetadataStorage
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.ClientLoggedInEvent
import net.raphimc.viaproxy.plugins.events.ShouldVerifyOnlineModeEvent
import net.raphimc.viaproxy.proxy.LoginState
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.ChannelUtil
import net.raphimc.viaproxy.proxy.util.CloseAndReturn
import net.raphimc.viaproxy.util.logging.Logger
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.time.Instant
import java.util.Arrays
import java.util.Random
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginPacketHandler(proxyConnection: ProxyConnection) : PacketHandler(proxyConnection) {

    companion object {
        private val KEY_PAIR: KeyPair = CryptUtil.generateKeyPair()
        private val RANDOM = Random()
        private val HTTP_EXECUTOR: ExecutorService = Executors.newWorkStealingPool(4)
    }

    private val verifyToken = ByteArray(4)
    private var loginState = LoginState.FIRST_PACKET

    init {
        RANDOM.nextBytes(this.verifyToken)
    }

    @Throws(GeneralSecurityException::class)
    override fun handleC2P(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is C2SLoginHelloPacket) {
            if (this.loginState != LoginState.FIRST_PACKET) throw CloseAndReturn.INSTANCE
            this.loginState = LoginState.SENT_HELLO

            if (packet.expiresAt != null && packet.expiresAt.isBefore(Instant.now())) {
                throw IllegalStateException("Expired public key")
            }

            proxyConnection.setLoginHelloPacket(packet)
            if (packet.uuid != null) {
                proxyConnection.setGameProfile(GameProfile(packet.uuid, packet.name))
            } else {
                proxyConnection.setGameProfile(GameProfile(GameProfileUtil.getOfflinePlayerUuid(packet.name), packet.name))
            }

            if (ViaProxy.getConfig().isProxyOnlineMode() && !ViaProxy.EVENT_MANAGER.call(ShouldVerifyOnlineModeEvent(this.proxyConnection)).isCancelled()) {
                this.proxyConnection.getC2P().writeAndFlush(S2CLoginHelloPacket("", KEY_PAIR.getPublic().getEncoded(), this.verifyToken, true)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
            } else {
                ViaProxy.EVENT_MANAGER.call(ClientLoggedInEvent(this.proxyConnection))
                ExternalInterface.fillPlayerData(this.proxyConnection)
                this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
            }

            return false
        } else if (packet is C2SLoginKeyPacket) {
            if (this.loginState != LoginState.SENT_HELLO) throw CloseAndReturn.INSTANCE
            this.loginState = LoginState.SENT_KEY

            if (packet.encryptedNonce != null) {
                if (!Arrays.equals(this.verifyToken, CryptUtil.decryptData(KEY_PAIR.getPrivate(), packet.encryptedNonce))) {
                    Logger.u_err("auth", this.proxyConnection, "Invalid verify token")
                    this.proxyConnection.kickClient("§cInvalid verify token!")
                }
            } else {
                val loginHelloPacket = this.proxyConnection.getLoginHelloPacket()
                if (loginHelloPacket.key == null || !CryptUtil.verifySignedNonce(loginHelloPacket.key, this.verifyToken, packet.salt, packet.signature)) {
                    Logger.u_err("auth", this.proxyConnection, "Invalid verify token")
                    this.proxyConnection.kickClient("§cInvalid verify token!")
                }
            }

            val secretKey = CryptUtil.decryptSecretKey(KEY_PAIR.getPrivate(), packet.encryptedSecretKey)
            this.proxyConnection.getC2P().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(AESEncryption(secretKey))

            HTTP_EXECUTOR.submit(Runnable {
                val userName = this.proxyConnection.getGameProfile()!!.getName()
                try {
                    val serverHash = BigInteger(CryptUtil.computeServerIdHash("", KEY_PAIR.getPublic(), secretKey)).toString(16)
                    val profileResult = AuthLibServices.SESSION_SERVICE.hasJoinedServer(userName, serverHash, null)
                    if (profileResult == null) {
                        Logger.u_err("auth", this.proxyConnection, "Invalid session")
                        this.proxyConnection.kickClient("§cInvalid session! Please restart minecraft (and the launcher) and try again.")
                    } else {
                        this.proxyConnection.setGameProfile(profileResult.profile())
                    }
                    Logger.u_info("auth", this.proxyConnection, "Authenticated as " + this.proxyConnection.getGameProfile()!!.getId().toString())
                } catch (ignored: CloseAndReturn) {
                } catch (e: Throwable) {
                    Logger.LOGGER.error("Failed to make session request for user '" + userName + "'!", e)
                    try {
                        this.proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again later.")
                    } catch (ignored: Throwable) {
                    }
                }

                this.proxyConnection.getC2P().eventLoop().execute(Runnable {
                    ViaProxy.EVENT_MANAGER.call(ClientLoggedInEvent(proxyConnection))
                    ExternalInterface.fillPlayerData(this.proxyConnection)
                    this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                })
            })
            return false
        }

        return true
    }

    @Throws(GeneralSecurityException::class, ExecutionException::class, InterruptedException::class)
    override fun handleP2S(packet: Packet, listeners: MutableList<ChannelFutureListener>): Boolean {
        if (packet is S2CLoginHelloPacket) {
            val publicKey = CryptUtil.decodeRsaPublicKey(packet.publicKey)
            val secretKey = CryptUtil.generateSecretKey()
            val serverHash = BigInteger(CryptUtil.computeServerIdHash(packet.serverId, publicKey, secretKey)).toString(16)

            var auth = this.proxyConnection.getClientVersion().olderThan(ProtocolVersion.v1_20_5) || packet.authenticate
            if (auth && this.proxyConnection.getServerVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
                auth = this.proxyConnection.getUserConnection().get(ProtocolMetadataStorage::class.java)!!.isAuthenticate()
            }
            if (auth) {
                ExternalInterface.joinServer(serverHash, this.proxyConnection)
            }

            val encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded())
            val encryptedNonce = CryptUtil.encryptData(publicKey, packet.nonce)

            val loginKey = C2SLoginKeyPacket(encryptedSecretKey, encryptedNonce)
            if (this.proxyConnection.getServerVersion().betweenInclusive(ProtocolVersion.v1_19, ProtocolVersion.v1_19_1) && this.proxyConnection.getLoginHelloPacket().key != null) {
                ExternalInterface.signNonce(packet.nonce, loginKey, this.proxyConnection)
            }
            this.proxyConnection.getChannel().writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)

            if (this.proxyConnection.getServerVersion().newerThanOrEqualTo(ProtocolVersion.v1_7_2)) {
                this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(AESEncryption(secretKey))
            } else {
                this.proxyConnection.setKeyForPreNettyEncryption(secretKey)
            }

            return false
        } else if (packet is S2CLoginGameProfilePacket) {
            val nextState = if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_2)) ConnectionState.CONFIGURATION else ConnectionState.PLAY

            this.proxyConnection.setGameProfile(GameProfile(packet.uuid, packet.name))
            Logger.u_info("session", this.proxyConnection, "Connected successfully! Switching to " + nextState + " state")

            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel())
            listeners.add(ChannelFutureListener { f ->
                if (f.isSuccess() && nextState != ConnectionState.CONFIGURATION) {
                    this.proxyConnection.setC2pConnectionState(nextState)
                    this.proxyConnection.setP2sConnectionState(nextState)
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel())
                }
            })
        }

        return true
    }

}
