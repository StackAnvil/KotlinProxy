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

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import gs.mclo.api.MclogsClient
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.cli.command.Command
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor
import net.raphimc.viaproxy.util.logging.Logger
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ExecutionException

class UploadLogCommand : Command("uploadlog", "Upload the ViaProxy log", "uploadlog") {

    override fun register(builder: LiteralArgumentBuilder<CommandExecutor>) {
        builder.executes { context ->
            val logger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
            val fileAppender = logger.appenders["LatestFile"] as RollingRandomAccessFileAppender
            fileAppender.manager.flush()
            val logFile = File(fileAppender.fileName)

            try {
                val mclogsClient = MclogsClient("ViaProxy", ViaProxy.VERSION)
                val apiResponse = mclogsClient.uploadLog(logFile.toPath()).get()
                context.source.sendMessage("Uploaded log file to " + apiResponse.url)
            } catch (e: ExecutionException) {
                if (e.cause is FileNotFoundException) {
                    context.source.sendMessage("The log file could not be found")
                } else {
                    Logger.LOGGER.error("Failed to upload log file", e.cause)
                }
            } catch (e: Throwable) {
                Logger.LOGGER.error("Failed to upload log file", e)
            }
            1
        }
    }

}
