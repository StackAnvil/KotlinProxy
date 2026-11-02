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
package net.raphimc.viaproxy.proxy.client2proxy

import com.google.common.collect.Lists
import com.google.common.net.HostAndPort
import com.google.gson.Gson
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.CookieStorage
import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.lenni0451.mcping.ServerAddress
import net.lenni0451.mcping.responses.MCPingResponse
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.constants.IntendedState
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket
import net.raphimc.netminecraft.packet.impl.status.S2CStatusPongResponsePacket
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.ConnectEvent
import net.raphimc.viaproxy.plugins.events.PreConnectEvent
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent
import net.raphimc.viaproxy.plugins.events.ProxySessionCreationEvent
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig
import net.raphimc.viaproxy.proxy.packethandler.BrandCustomPayloadPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.ChatSignaturePacketHandler
import net.raphimc.viaproxy.proxy.packethandler.CompressionPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.ConfigurationPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.DisconnectPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.FabricParticleApiFixPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.LoginPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.PacketHandler
import net.raphimc.viaproxy.proxy.packethandler.ResourcePackPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.ResourcePackSpooferPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.SimpleVoiceChatPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.StatusPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.TransferPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.UnexpectedPacketHandler
import net.raphimc.viaproxy.proxy.packethandler.ViaVersionConnectionDetailsPacketHandler
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerChannelInitializer
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerHandler
import net.raphimc.viaproxy.proxy.session.BedrockProxyConnection
import net.raphimc.viaproxy.proxy.session.DummyProxyConnection
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.session.UserOptions
import net.raphimc.viaproxy.proxy.util.ChannelUtil
import net.raphimc.viaproxy.proxy.util.CloseAndReturn
import net.raphimc.viaproxy.proxy.util.ExceptionUtil
import net.raphimc.viaproxy.proxy.util.HAProxyUtil
import net.raphimc.viaproxy.proxy.util.ThrowingChannelFutureListener
import net.raphimc.viaproxy.proxy.util.TransferDataHolder
import net.raphimc.viaproxy.saves.impl.accounts.ClassicAccount
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.ArrayHelper
import net.raphimc.viaproxy.util.ProtocolVersionUtil
import net.raphimc.viaproxy.util.StatusPingUtil
import net.raphimc.viaproxy.util.WildcardDomainParser
import net.raphimc.viaproxy.util.logging.Logger
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.function.Supplier
import java.util.regex.Pattern

class Client2ProxyHandler : SimpleChannelInboundHandler<Packet>() {

    private var proxyConnection: ProxyConnection? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        this.proxyConnection = DummyProxyConnection(ctx.channel())
        ViaProxy.getConnectedClients().add(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        if (this.proxyConnection is DummyProxyConnection) return

        try {
            this.proxyConnection!!.getChannel().close()
        } catch (ignored: Throwable) {
        }
    }

    protected override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
        if (this.proxyConnection!!.isClosed()) return

        if (this.proxyConnection!!.getC2pConnectionState() == ConnectionState.HANDSHAKING) {
            if (packet is C2SHandshakingClientIntentionPacket) {
                this.handleHandshake(packet)
            } else {
                throw IllegalStateException("Unexpected packet in HANDSHAKING state")
            }
            return
        }

        val listeners: MutableList<ChannelFutureListener> = Lists.newArrayList(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        for (packetHandler in this.proxyConnection!!.getPacketHandlers()) {
            if (!packetHandler.handleC2P(packet, listeners)) {
                return
            }
        }
        this.proxyConnection!!.getChannel().writeAndFlush(packet).addListeners(*listeners.toTypedArray())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, true)
    }

    private fun handleHandshake(packet: C2SHandshakingClientIntentionPacket) {
        val clientVersion = ProtocolVersion.getProtocol(packet.protocolVersion)

        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE
        }
        var intendedState: IntendedState = packet.intendedState

        this.proxyConnection!!.setClientVersion(clientVersion)
        this.proxyConnection!!.setC2pConnectionState(intendedState.getConnectionState())

        if (!ProtocolVersion.isRegistered(clientVersion.getVersionType(), clientVersion.getOriginalVersion())) {
            this.proxyConnection!!.kickClient("§cYour client version (Protocol " + clientVersion.getOriginalVersion() + ") is not supported by ViaProxy!")
        }

        val handshakeParts: Array<String>
        if (ViaProxy.getConfig().shouldPassthroughBungeecordPlayerInfo()) {
            handshakeParts = Pattern.compile("\u0000").split(packet.address)
        } else {
            handshakeParts = arrayOf(packet.address)
        }

        var serverAddress: SocketAddress = ViaProxy.getConfig().getTargetAddress()
        var serverVersion: ProtocolVersion = ViaProxy.getConfig().getTargetVersion()
        var classicMpPass: String? = (ViaProxy.getConfig().getAccount() as? ClassicAccount)?.getMppass()

        if (ViaProxy.getConfig().getWildcardDomainHandling() == ViaProxyConfig.WildcardDomainHandling.PUBLIC) {
            if (handshakeParts[0].lowercase().contains("f2.viaproxy.")) { // Format 2: address.<address>.port.<port>.version.<version>.f2.viaproxy.hostname
                val parsedDomain = WildcardDomainParser.parseFormat2(handshakeParts[0])
                if (parsedDomain == null) {
                    this.proxyConnection!!.kickClient("§cWrong domain syntax! §6Please use:\n§7address.<address>.port.<port>.version.<version>.f2.viaproxy.hostname")
                } else if (parsedDomain.version() == null) {
                    this.proxyConnection!!.kickClient("§cWrong domain syntax!\n§cUnknown server version.")
                } else {
                    serverAddress = parsedDomain.address()
                    serverVersion = parsedDomain.version()!!
                }
            } else if (handshakeParts[0].lowercase().contains("viaproxy.")) { // Format 1: address_port_version.viaproxy.hostname
                val parsedDomain = WildcardDomainParser.parseFormat1(handshakeParts[0])
                if (parsedDomain == null) {
                    this.proxyConnection!!.kickClient("§cWrong domain syntax! §6Please use:\n§7address_port_version.viaproxy.hostname")
                } else if (parsedDomain.version() == null) {
                    this.proxyConnection!!.kickClient("§cWrong domain syntax!\n§cUnknown server version.")
                } else {
                    serverAddress = parsedDomain.address()
                    serverVersion = parsedDomain.version()!!
                }
            } else {
                this.proxyConnection!!.kickClient("§cWrong domain syntax! §6Please use:\n§7address_port_version.viaproxy.hostname")
            }
        } else if (ViaProxy.getConfig().getWildcardDomainHandling() == ViaProxyConfig.WildcardDomainHandling.INTERNAL) {
            val arrayHelper = ArrayHelper.instanceOf(*Pattern.compile("\u0007").split(handshakeParts[0]))
            handshakeParts[0] = arrayHelper.get(0)!! // Restore the original address
            val versionString = arrayHelper.get(2)!!
            serverVersion = ProtocolVersionUtil.fromNameLenient(versionString) ?: throw CloseAndReturn.INSTANCE
            serverAddress = AddressUtil.parse(arrayHelper.get(1)!!, serverVersion)
            if (arrayHelper.isIndexValid(3)) {
                classicMpPass = arrayHelper.getString(3)
            }
        }

        if (intendedState.getConnectionState() == ConnectionState.LOGIN && TransferDataHolder.hasTempRedirect(this.proxyConnection!!.getC2P())) {
            serverAddress = TransferDataHolder.removeTempRedirect(this.proxyConnection!!.getC2P())!!
            if (clientVersion.olderThan(ProtocolVersion.v1_20_5)) {
                intendedState = IntendedState.TRANSFER
                packet.intendedState = intendedState
            }
        }

        val clientHandshakeAddress: HostAndPort? = try {
            HostAndPort.fromParts(handshakeParts[0], packet.port)
        } catch (e: Throwable) {
            null
        }

        val preConnectEvent = PreConnectEvent(serverAddress, serverVersion, clientVersion, clientHandshakeAddress!!, intendedState, this.proxyConnection!!.getC2P())
        if (ViaProxy.EVENT_MANAGER.call(preConnectEvent).isCancelled()) {
            this.proxyConnection!!.kickClient(preConnectEvent.cancelMessage)
        }
        serverAddress = preConnectEvent.serverAddress
        serverVersion = preConnectEvent.serverVersion

        ChannelUtil.disableAutoRead(this.proxyConnection!!.getC2P())

        val isJavaBetaPing = intendedState.getConnectionState() == ConnectionState.STATUS && serverVersion.olderThanOrEqualTo(LegacyProtocolVersion.b1_7tob1_7_3) && !ViaProxy.getConfig().shouldAllowBetaPinging()
        val isBedrockPing = intendedState.getConnectionState() == ConnectionState.STATUS && serverVersion.equals(BedrockProtocolVersion.BEDROCK_LATEST)
        if (isJavaBetaPing || isBedrockPing) {
            if (!ViaProxy.getConfig().getCustomMotd().isBlank()) {
                this.proxyConnection!!.kickClient(ViaProxy.getConfig().getCustomMotd())
            } else if (isBedrockPing) {
                StatusPingUtil.pingBedrockNetherNetHttp(serverAddress).thenAccept { response ->
                    val javaResponse: MCPingResponse = StatusPingUtil.convertToJavaPingResponse(response)
                    javaResponse.version.protocol = clientVersion.getOriginalVersion()
                    this.proxyConnection!!.getC2P().writeAndFlush(S2CStatusResponsePacket(Gson().toJson(javaResponse))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                    this.proxyConnection!!.getC2P().writeAndFlush(S2CStatusPongResponsePacket(0)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                }.exceptionally { t ->
                    this.proxyConnection!!.kickClient("§cPing failed: " + t.javaClass.simpleName + "\n§c" + t.message)
                    null
                }
            } else {
                this.proxyConnection!!.kickClient("§7ViaProxy is running!\n§7Connect to join the configured server")
            }
            return
        }

        val userOptions = UserOptions(classicMpPass, ViaProxy.getConfig().getAccount())
        if (intendedState.getConnectionState() == ConnectionState.LOGIN && serverVersion.equals(ProtocolTranslator.AUTO_DETECT_PROTOCOL)) {
            val finalServerAddress = serverAddress
            val finalClientHandshakeAddress = clientHandshakeAddress
            StatusPingUtil.detectProtocolVersion(serverAddress, clientVersion).thenAccept { detectedVersion ->
                this.connect(finalServerAddress, detectedVersion, clientVersion, intendedState, finalClientHandshakeAddress, userOptions, handshakeParts)
            }.exceptionally { t ->
                this.proxyConnection!!.kickClient("§cProtocol detection failed: " + t.javaClass.simpleName + "\n§c" + t.message)
                null
            }
        } else {
            this.connect(serverAddress, serverVersion, clientVersion, intendedState, clientHandshakeAddress, userOptions, handshakeParts)
        }
    }

    private fun connect(serverAddress: SocketAddress, serverVersion: ProtocolVersion, clientVersion: ProtocolVersion, intendedState: IntendedState, clientHandshakeAddress: HostAndPort?, userOptions: UserOptions, handshakeParts: Array<String>) {
        val handlerSupplier: Supplier<ChannelHandler> = Supplier {
            ViaProxy.EVENT_MANAGER.call(Proxy2ServerHandlerCreationEvent(Proxy2ServerHandler(), false)).handler
        }
        val proxyConnection: ProxyConnection
        if (serverVersion.equals(BedrockProtocolVersion.BEDROCK_LATEST)) {
            proxyConnection = BedrockProxyConnection(Proxy2ServerChannelInitializer(handlerSupplier), this.proxyConnection!!.getC2P())
        } else {
            proxyConnection = ProxyConnection(Proxy2ServerChannelInitializer(handlerSupplier), this.proxyConnection!!.getC2P())
        }
        val connection = ViaProxy.EVENT_MANAGER.call(ProxySessionCreationEvent(proxyConnection, false)).proxySession
        this.proxyConnection = connection
        connection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(connection)
        connection.setClientVersion(clientVersion)
        connection.setClientHandshakeAddress(clientHandshakeAddress)
        connection.setUserOptions(userOptions)
        connection.setC2pConnectionState(intendedState.getConnectionState())
        connection.getPacketHandlers().add(StatusPacketHandler(connection))
        if (ViaProxy.getConfig().shouldSupportSimpleVoiceChat() && serverVersion.newerThan(ProtocolVersion.v1_14) && clientVersion.newerThan(ProtocolVersion.v1_14)) {
            connection.getPacketHandlers().add(SimpleVoiceChatPacketHandler(connection))
        }
        if (ViaProxy.getConfig().shouldFixFabricParticleApi() && serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_21_5) && clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_21_5) && !serverVersion.equals(clientVersion)) {
            connection.getPacketHandlers().add(FabricParticleApiFixPacketHandler(connection))
        }
        if (ViaProxy.getConfig().shouldFakeAcceptResourcePacks() && serverVersion.newerThanOrEqualTo(LegacyProtocolVersion.r1_3_1tor1_3_2)) {
            connection.getPacketHandlers().add(ResourcePackSpooferPacketHandler(connection))
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_8)) {
            connection.getPacketHandlers().add(BrandCustomPayloadPacketHandler(connection))
        }
        connection.getPacketHandlers().add(CompressionPacketHandler(connection))
        connection.getPacketHandlers().add(LoginPacketHandler(connection))
        connection.getPacketHandlers().add(DisconnectPacketHandler(connection))
        if (ViaProxy.getConfig().shouldRewriteTransferPackets() && clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            connection.getPacketHandlers().add(TransferPacketHandler(connection))
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2) || serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            connection.getPacketHandlers().add(ConfigurationPacketHandler(connection))
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_19_3) && serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            connection.getPacketHandlers().add(ChatSignaturePacketHandler(connection))
        }
        if (!ViaProxy.getConfig().getResourcePackUrl().isBlank()) {
            connection.getPacketHandlers().add(ResourcePackPacketHandler(connection))
        }
        if (Via.getConfig().sendPlayerDetails() || Via.getConfig().sendServerDetails()) {
            connection.getPacketHandlers().add(ViaVersionConnectionDetailsPacketHandler(connection))
        }
        connection.getPacketHandlers().add(UnexpectedPacketHandler(connection))

        if (ViaProxy.getConfig().shouldLogClientStatusRequests() || intendedState != IntendedState.STATUS) {
            Logger.u_info("connect", connection, "[" + clientVersion.getName() + " <-> " + serverVersion.getName() + "] Connecting to " + AddressUtil.toString(serverAddress))
        }
        ViaProxy.EVENT_MANAGER.call(ConnectEvent(connection))

        val handshakePort: Int
        if (ViaProxy.getConfig().shouldRewriteHandshakePacket()) {
            if (serverAddress is InetSocketAddress) {
                handshakeParts[0] = serverAddress.getHostString()
                handshakePort = serverAddress.getPort()
            } else {
                handshakeParts[0] = AddressUtil.toString(serverAddress)
                handshakePort = ServerAddress.DEFAULT_JAVA_PORT
            }
        } else {
            handshakePort = clientHandshakeAddress!!.getPort()
        }

        connection.connectToServer(serverAddress, serverVersion).addListeners(
            ThrowingChannelFutureListener { f ->
                if (f.isSuccess()) {
                    f.channel().eventLoop().submit(Runnable { // Reschedule so the packets get sent after the channel is fully initialized and active
                        if (ViaProxy.getConfig().useBackendHaProxy()) {
                            this.proxyConnection!!.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.proxyConnection!!.getC2P(), this.proxyConnection!!.getChannel(), clientVersion)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                        }

                        val newHandshakePacket = C2SHandshakingClientIntentionPacket(clientVersion.getOriginalVersion(), handshakeParts.joinToString("\u0000"), handshakePort, intendedState)
                        this.proxyConnection!!.getChannel().writeAndFlush(newHandshakePacket).addListeners(
                            ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE,
                            ThrowingChannelFutureListener { f2 ->
                                if (f2.isSuccess()) {
                                    val userConnection: UserConnection = this.proxyConnection!!.getUserConnection()!!
                                    if (userConnection.has(CookieStorage::class.java) && TransferDataHolder.hasCookieStorage(this.proxyConnection!!.getC2P())) {
                                        userConnection.get(CookieStorage::class.java)!!.cookies().putAll(TransferDataHolder.removeCookieStorage(this.proxyConnection!!.getC2P())!!.cookies())
                                    }
                                    this.proxyConnection!!.setP2sConnectionState(intendedState.getConnectionState())
                                    ChannelUtil.restoreAutoRead(this.proxyConnection!!.getC2P())
                                }
                            }
                        )
                    })
                }
            },
            ThrowingChannelFutureListener { f ->
                if (!f.isSuccess()) {
                    Logger.LOGGER.error("Error while connecting to the backend server", f.cause())
                    val cause = f.cause()!!
                    this.proxyConnection!!.kickClient("§cConnect failed: " + cause.javaClass.simpleName + "\n§c" + cause.message)
                }
            }
        )
    }

}
