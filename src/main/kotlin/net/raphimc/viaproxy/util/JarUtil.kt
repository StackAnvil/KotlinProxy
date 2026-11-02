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

import net.raphimc.viaproxy.ViaProxy
import java.io.File
import java.io.IOException
import java.util.Optional

object JarUtil {

    @JvmStatic
    fun getJarFile(): Optional<File> {
        try {
            return Optional.of(File(ViaProxy::class.java.protectionDomain.codeSource.location.toURI()))
        } catch (ignored: Throwable) {
            return Optional.empty()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun launch(jarFile: File) {
        ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-jar", jarFile.absolutePath).directory(ViaProxy.getCwd()).start()
    }

}
