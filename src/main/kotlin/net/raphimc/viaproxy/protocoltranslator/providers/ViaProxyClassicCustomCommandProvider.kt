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
import net.raphimc.vialegacy.ViaLegacy
import net.raphimc.vialegacy.protocol.alpha.a1_0_16_2toa1_0_17_1_0_17_4.storage.TimeLockStorage
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicCustomCommandProvider
import java.util.logging.Level

class ViaProxyClassicCustomCommandProvider : ClassicCustomCommandProvider() {

    override fun handleChatMessage(user: UserConnection, message: String): Boolean {
        var msg = message
        try {
            if (msg.startsWith("/")) {
                msg = msg.substring(1)
                val args = msg.split(" ")
                if (args.isEmpty()) return super.handleChatMessage(user, msg)
                if (args[0] == "settime") {
                    try {
                        if (args.size > 1) {
                            val time = args[1].toLong() % 24_000L
                            user.get(TimeLockStorage::class.java)!!.setTime(time)
                            this.sendFeedback(user, "§aTime has been set to §6" + time)
                        } else {
                            throw RuntimeException("Invalid usage")
                        }
                    } catch (ignored: Throwable) {
                        this.sendFeedback(user, "§cUsage: /settime <Time (Long)>")
                    }
                    return true
                }
            }
        } catch (e: Throwable) {
            ViaLegacy.getPlatform().logger.log(Level.WARNING, "Error handling custom classic command", e)
        }

        return super.handleChatMessage(user, msg)
    }

}
