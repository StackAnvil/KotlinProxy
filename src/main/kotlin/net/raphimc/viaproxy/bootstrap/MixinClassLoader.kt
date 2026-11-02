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
package net.raphimc.viaproxy.bootstrap

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.security.ProtectionDomain

/**
 * Child-first loader which applies mixins while classes are defined. It can also
 * define the synthetic classes Mixin generates for mixins.
 *
 * The hot path deliberately avoids Kotlin collection helpers and streams: this
 * loader defines the Kotlin runtime itself, so using those helpers here would
 * cause class circularity errors.
 */
class MixinClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

    companion object {
        private val PARENT_FIRST = arrayOf(
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.w3c.",
            "org.xml.",
            "org.ietf.",
            "net.raphimc.viaproxy.bootstrap.",
            "org.objectweb.asm."
        )

        private fun isParentFirst(name: String): Boolean {
            var i = 0
            while (i < PARENT_FIRST.size) {
                if (name.startsWith(PARENT_FIRST[i])) {
                    return true
                }
                i++
            }
            return false
        }

        private fun needsMixins(resource: String): Boolean {
            if (resource.startsWith("net/raphimc/viaproxy/injection/mixins/")) {
                return false
            }
            return resource.startsWith("net/raphimc/") || resource.startsWith("com/viaversion/")
        }

        private fun readAll(stream: InputStream): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            try {
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) {
                        break
                    }
                    output.write(buffer, 0, read)
                }
            } finally {
                stream.close()
            }
            return output.toByteArray()
        }
    }

    private val protectionDomain: ProtectionDomain =
        ProtectionDomain(CodeSource(urls.firstOrNull(), emptyArray<java.security.CodeSigner>()), null)

    @Suppress("DEPRECATION")
    private fun ensurePackage(name: String) {
        val separator = name.lastIndexOf('.')
        if (separator < 0) {
            return
        }
        val packageName = name.substring(0, separator)
        if (getPackage(packageName) == null) {
            try {
                definePackage(packageName, null, null, null, null, null, null, null)
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name)
            if (loaded != null) {
                if (resolve) {
                    resolveClass(loaded)
                }
                return loaded
            }

            if (isParentFirst(name)) {
                return super.loadClass(name, resolve)
            }

            try {
                val defined = findClass(name)
                if (resolve) {
                    resolveClass(defined)
                }
                return defined
            } catch (ignored: ClassNotFoundException) {
                val generator = MixinHook.generator
                val generated = generator?.generate(name)
                if (generated != null) {
                    ensurePackage(name)
                    val defined = defineClass(name, generated, 0, generated.size, protectionDomain)
                    if (resolve) {
                        resolveClass(defined)
                    }
                    return defined
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    override fun findClass(name: String): Class<*> {
        val resource = name.replace('.', '/') + ".class"
        val stream = getResourceAsStream(resource) ?: throw ClassNotFoundException(name)
        val original = readAll(stream)
        var bytes = original
        if (needsMixins(resource)) {
            val transformer = MixinHook.transformer
            if (transformer != null) {
                val transformed = transformer.transform(name, original)
                if (transformed != null) {
                    bytes = transformed
                }
            }
        }
        ensurePackage(name)
        return defineClass(name, bytes, 0, bytes.size, protectionDomain)
    }

}
