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
package net.raphimc.viaproxy.util

import net.lenni0451.reflect.ClassLoaders
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.util.logging.Logger
import java.io.File

object ClassLoaderPriorityUtil {

    @JvmStatic
    fun loadOverridingJars() {
        val jarsFolder = File(ViaProxy.getCwd(), "jars")
        if (!jarsFolder.exists()) {
            jarsFolder.mkdir()
            return
        }

        if (jarsFolder.isDirectory) {
            for (file in jarsFolder.listFiles()) {
                try {
                    if (file.name.endsWith(".jar")) {
                        ClassLoaders.loadToFront(file.toURI().toURL())
                        Logger.LOGGER.warn("Loaded overriding jar " + file.name)
                    }
                } catch (e: Throwable) {
                    Logger.LOGGER.error("Failed to load overriding jar " + file.name, e)
                }
            }
        }
    }

}
