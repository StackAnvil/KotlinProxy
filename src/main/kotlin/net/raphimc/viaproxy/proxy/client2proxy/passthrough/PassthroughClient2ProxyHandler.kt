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
package net.raphimc.viaproxy.proxy.client2proxy.passthrough

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent
import net.raphimc.viaproxy.plugins.events.ProxySessionCreationEvent
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerChannelInitializer
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerHandler
import net.raphimc.viaproxy.proxy.session.LegacyProxyConnection
import net.raphimc.viaproxy.proxy.util.ChannelUtil
import net.raphimc.viaproxy.proxy.util.ExceptionUtil
import net.raphimc.viaproxy.proxy.util.HAProxyUtil
import net.raphimc.viaproxy.proxy.util.ThrowingChannelFutureListener
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.logging.Logger
import org.apache.logging.log4j.Level
import java.net.SocketAddress
import java.util.function.Supplier

open class PassthroughClient2ProxyHandler : SimpleChannelInboundHandler<ByteBuf>() {

    private var proxyConnection: LegacyProxyConnection? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        this.connectToServer(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)

        try {
            if (this.proxyConnection != null) {
                this.proxyConnection!!.getChannel().close()
            }
        } catch (ignored: Throwable) {
        }
    }

    protected override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        if (!ctx.channel().isOpen) return
        if (!msg.isReadable) return

        this.proxyConnection!!.getChannel().writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ExceptionUtil.handleNettyException(ctx, cause, null, true)
    }

    protected open fun connectToServer(c2pChannel: Channel) {
        val handlerSupplier: Supplier<ChannelHandler> = Supplier {
            ViaProxy.EVENT_MANAGER.call(Proxy2ServerHandlerCreationEvent(PassthroughProxy2ServerHandler(), true)).handler
        }
        val proxyConnection = LegacyProxyConnection(PassthroughProxy2ServerChannelInitializer(handlerSupplier), c2pChannel)
        this.proxyConnection = ViaProxy.EVENT_MANAGER.call(ProxySessionCreationEvent(proxyConnection, true)).proxySession
        this.proxyConnection!!.getC2P().attr(LegacyProxyConnection.LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection)

        val serverAddress = this.getServerAddress()

        ChannelUtil.disableAutoRead(this.proxyConnection!!.getC2P())
        Logger.u_log(Level.INFO, "connect", this.proxyConnection!!.getC2P().remoteAddress(), null, "[Legacy <-> Legacy] Connecting to " + AddressUtil.toString(serverAddress))

        this.proxyConnection!!.connect(serverAddress).addListeners(
            ThrowingChannelFutureListener { f ->
                if (f.isSuccess()) {
                    f.channel().eventLoop().submit(Runnable {
                        if (ViaProxy.getConfig().useBackendHaProxy()) {
                            this.proxyConnection!!.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.proxyConnection!!.getC2P(), this.proxyConnection!!.getChannel(), null)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                        }

                        ChannelUtil.restoreAutoRead(this.proxyConnection!!.getC2P())
                    })
                }
            },
            ThrowingChannelFutureListener { f ->
                if (!f.isSuccess()) {
                    Logger.LOGGER.error("Failed to connect to target server", f.cause())
                    this.proxyConnection!!.getC2P().close()
                    this.proxyConnection = null
                }
            }
        )
    }

    protected open fun getServerAddress(): SocketAddress {
        return ViaProxy.getConfig().getTargetAddress()
    }

}
