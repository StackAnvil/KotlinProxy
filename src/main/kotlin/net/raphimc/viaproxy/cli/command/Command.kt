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
package net.raphimc.viaproxy.cli.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.lenni0451.commons.brigadier.CommandBuilder
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor

abstract class Command(
    name: String,
    val description: String,
    val help: String,
    vararg aliases: String
) : CommandBuilder<CommandExecutor> {

    private val names: Array<String> = arrayOf(name, *aliases)

    abstract fun register(builder: LiteralArgumentBuilder<CommandExecutor>)

    fun getName(): String {
        return this.names[0]
    }

    fun getNames(): Array<String> {
        return this.names
    }

}
