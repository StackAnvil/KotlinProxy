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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.lenni0451.commons.brigadier.CommandBuilder
import net.lenni0451.commons.brigadier.HelpAppender
import net.lenni0451.reflect.stream.RStream
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor
import net.raphimc.viaproxy.cli.command.executor.ConsoleCommandExecutor
import net.raphimc.viaproxy.cli.command.impl.AccountCommand
import net.raphimc.viaproxy.cli.command.impl.GcCommand
import net.raphimc.viaproxy.cli.command.impl.HelpCommand
import net.raphimc.viaproxy.cli.command.impl.StopCommand
import net.raphimc.viaproxy.cli.command.impl.ThreadDumpCommand
import net.raphimc.viaproxy.cli.command.impl.UploadLogCommand
import net.raphimc.viaproxy.cli.command.impl.ViaVersionCommand
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent
import net.raphimc.viaproxy.util.logging.Logger
import java.util.regex.Pattern

class CommandManager : CommandBuilder<CommandExecutor> {

    private val dispatcher: CommandDispatcher<CommandExecutor> =
        HelpAppender.newAutoHelpAppendingCommandDispatcher { sender, _, usages ->
            sender.sendMessage("Incomplete command. Possible usages:")
            for (usage in usages) {
                sender.sendMessage(" - " + usage)
            }
        }
    private val commands = ArrayList<Command>()

    // Core commands
    private val helpCommand = HelpCommand(this)
    private val stopCommand = StopCommand()
    private val viaVersionCommand = ViaVersionCommand()
    private val uploadLogCommand = UploadLogCommand()
    private val accountCommand = AccountCommand()

    // Debugging commands
    private val gcCommand = GcCommand()
    private val threadDumpCommand = ThreadDumpCommand()

    init {
        RStream
            .of(this)
            .fields()
            .filter { Command::class.java.isAssignableFrom(it.type()) }
            .forEach { field ->
                val command: Command = field.get()
                try {
                    for (name in command.getNames()) {
                        val builder = literal(name)
                        command.register(builder)
                        this.dispatcher.register(builder)
                    }
                    this.commands.add(command)
                } catch (e: Throwable) {
                    Logger.LOGGER.error("Failed to register command " + command.getNames()[0], e)
                }
            }

        this.dispatcher.findAmbiguities { _, child, sibling, inputs ->
            Logger.LOGGER.warn(
                "Ambiguity between arguments {} and {} with inputs: {}",
                this.dispatcher.getPath(child),
                this.dispatcher.getPath(sibling),
                inputs
            )
        }
    }

    fun execute(message: String) {
        try {
            this.dispatcher.execute(message, ConsoleCommandExecutor)
        } catch (e: CommandSyntaxException) {
            val errorMessage = e.message!!
            if (errorMessage.startsWith("Unknown command at position 0")) { // Compat for the old command API
                val parts = Pattern.compile(" ").split(message)
                if (parts.isNotEmpty()) {
                    val command = parts[0]
                    val args = parts.copyOfRange(1, parts.size)
                    if (ViaProxy.EVENT_MANAGER.call(ConsoleCommandEvent(command, args)).isCancelled) {
                        return
                    }
                }
                Logger.LOGGER.error(errorMessage.replace("Unknown command", "Unknown or incomplete command"))
                Logger.LOGGER.error("Type 'help' for help and a list of available commands.")
            } else if (errorMessage.startsWith("Unknown command at position")) {
                Logger.LOGGER.error(errorMessage.replace("Unknown command", "Unknown or incomplete command"))
                Logger.LOGGER.error("Type 'help' for help and a list of available commands.")
            } else {
                Logger.LOGGER.error(errorMessage)
            }
        }
    }

    fun getCommands(): List<Command> {
        return this.commands
    }

}
