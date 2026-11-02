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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.util.logging.Logger
import java.nio.channels.ClosedChannelException

object ExceptionUtil {

    @JvmStatic
    fun handleNettyException(ctx: ChannelHandlerContext, cause: Throwable, proxyConnection: ProxyConnection?, client2Proxy: Boolean) {
        if (!ctx.channel().isOpen()) return
        if (cause is ClosedChannelException) return
        if (cause is CloseAndReturn) {
            ctx.channel().close()
            return
        }
        if (!client2Proxy || !ViaProxy.getConfig().shouldSuppressClientProtocolErrors()) {
            Logger.LOGGER.error("Caught unhandled netty exception", cause)
            try {
                if (proxyConnection != null) {
                    proxyConnection.kickClient("§cAn unhandled error occurred in your connection and it has been closed.\n§aError details for report:§f" + ExceptionUtil.prettyPrint(cause))
                }
            } catch (ignored: Throwable) {
            }
        }
        ctx.channel().close()
    }

    @JvmStatic
    fun prettyPrint(t: Throwable): String {
        var throwable: Throwable? = t
        val msg = StringBuilder()
        if (throwable is EncoderException && throwable.cause != null) throwable = throwable.cause
        if (throwable is DecoderException && throwable.cause != null) throwable = throwable.cause
        while (throwable != null) {
            msg.append("\n")
            msg.append("§c").append(throwable.javaClass.simpleName).append("§7: §f").append(throwable.message)
            throwable = throwable.cause
            if (throwable != null) {
                msg.append(" §9Caused by")
            }
        }
        return msg.toString()
    }

}
