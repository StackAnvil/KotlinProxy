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
package net.raphimc.viaproxy.proxy.proxy2server

import com.google.common.collect.Lists
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.raphimc.netminecraft.constants.ConnectionState
import net.raphimc.netminecraft.packet.Packet
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.packethandler.PacketHandler
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.proxy.util.ExceptionUtil
import net.raphimc.viaproxy.util.logging.Logger

class Proxy2ServerHandler : SimpleChannelInboundHandler<Packet>() {

    private var proxyConnection: ProxyConnection? = null

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        super.channelRegistered(ctx)

        this.proxyConnection = ProxyConnection.fromChannel(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)

        if (ViaProxy.getConfig().shouldLogClientStatusRequests() || this.proxyConnection!!.getP2sConnectionState() != ConnectionState.STATUS) {
            Logger.u_info("disconnect", this.proxyConnection!!, "Connection closed")
        }
        try {
            this.proxyConnection!!.getC2P().close()
        } catch (ignored: Throwable) {
        }
    }

    protected override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
        if (this.proxyConnection!!.isClosed()) return

        val listeners: MutableList<ChannelFutureListener> = Lists.newArrayList(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        for (packetHandler in this.proxyConnection!!.getPacketHandlers()) {
            if (!packetHandler.handleP2S(packet, listeners)) {
                return
            }
        }
        this.proxyConnection!!.getC2P().writeAndFlush(packet).addListeners(*listeners.toTypedArray())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, false)
    }

}
