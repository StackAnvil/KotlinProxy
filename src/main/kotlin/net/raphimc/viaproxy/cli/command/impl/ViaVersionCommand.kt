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
package net.raphimc.viaproxy.cli.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.viaversion.viaversion.api.Via
import net.raphimc.viaproxy.cli.command.Command
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ConsoleCommandSender
import java.util.regex.Pattern

class ViaVersionCommand : Command("viaversion", "Execute a ViaVersion command", "viaversion <args>", "via", "vv") {

    override fun register(builder: LiteralArgumentBuilder<CommandExecutor>) {
        builder.then(argument("args", StringArgumentType.greedyString()).executes { context ->
            Via.getManager().commandHandler.onCommand(
                ConsoleCommandSender(),
                Pattern.compile(" ").split(StringArgumentType.getString(context, "args"))
            )
            1
        })
    }

}
