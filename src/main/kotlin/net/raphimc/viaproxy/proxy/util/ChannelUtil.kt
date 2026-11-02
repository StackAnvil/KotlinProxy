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

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.util.Stack

object ChannelUtil {

    private val LAST_AUTO_READ: AttributeKey<Stack<Boolean>> = AttributeKey.valueOf("last-auto-read")

    @JvmStatic
    fun disableAutoRead(channel: Channel) {
        if (channel.attr(LAST_AUTO_READ).get() == null) {
            channel.attr(LAST_AUTO_READ).set(Stack())
        }

        channel.attr(LAST_AUTO_READ).get().push(channel.config().isAutoRead())
        channel.config().setAutoRead(false)
    }

    @JvmStatic
    fun restoreAutoRead(channel: Channel) {
        if (channel.attr(LAST_AUTO_READ).get() == null) {
            throw IllegalStateException("Tried to restore auto read, but it was never disabled")
        }
        if (channel.config().isAutoRead()) {
            throw IllegalStateException("Race condition detected: Auto read has been enabled somewhere else")
        }
        channel.config().setAutoRead(channel.attr(LAST_AUTO_READ).get().pop())
    }

}
