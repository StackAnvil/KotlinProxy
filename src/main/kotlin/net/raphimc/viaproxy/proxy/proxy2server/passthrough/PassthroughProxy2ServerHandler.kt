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
package net.raphimc.viaproxy.proxy.proxy2server.passthrough

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.raphimc.viaproxy.proxy.session.LegacyProxyConnection
import net.raphimc.viaproxy.proxy.util.ExceptionUtil

class PassthroughProxy2ServerHandler : SimpleChannelInboundHandler<ByteBuf>() {

    private var proxyConnection: LegacyProxyConnection? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        this.proxyConnection = LegacyProxyConnection.fromChannel(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)

        try {
            this.proxyConnection!!.getC2P().close()
        } catch (ignored: Throwable) {
        }
    }

    protected override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        this.proxyConnection!!.getC2P().writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ExceptionUtil.handleNettyException(ctx, cause, null, false)
    }

}
