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
package net.raphimc.viaproxy

import net.raphimc.viaproxy.bootstrap.MixinClassLoader
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URL

/**
 * Starts the application in a child-first class loader so mixins can be applied
 * while classes are defined.
 */
object KotlinProxyLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val urls = ArrayList<URL>()
        val classPath = System.getProperty("java.class.path")
        if (classPath != null) {
            for (entry in classPath.split(File.pathSeparatorChar)) {
                if (entry.isEmpty()) continue
                urls.add(File(entry).toURI().toURL())
            }
        }
        val ownLocation = KotlinProxyLauncher::class.java.protectionDomain?.codeSource?.location
        if (ownLocation != null && urls.none { it == ownLocation }) {
            urls.add(ownLocation)
        }

        val parent = ClassLoader.getSystemClassLoader()
        val classLoader = MixinClassLoader(urls.toTypedArray(), parent)
        Thread.currentThread().contextClassLoader = classLoader

        try {
            val mainClass = classLoader.loadClass("net.raphimc.viaproxy.ViaProxy")
            val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

}
