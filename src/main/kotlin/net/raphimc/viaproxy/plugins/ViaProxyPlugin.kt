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
package net.raphimc.viaproxy.plugins

import java.io.File
import java.net.URLClassLoader
import java.util.Collections

abstract class ViaProxyPlugin {

    lateinit var classLoader: URLClassLoader
        private set

    private lateinit var viaProxyYaml: Map<String, Any?>

    private var enabled = false

    @JvmName("init")
    internal fun init(classLoader: URLClassLoader, viaProxyYaml: Map<String, Any?>) {
        this.classLoader = classLoader
        this.viaProxyYaml = viaProxyYaml
    }

    @JvmName("enable")
    internal fun enable() {
        this.onEnable()
        this.enabled = true
    }

    abstract fun onEnable()

    @JvmName("disable")
    internal fun disable() {
        try {
            this.onDisable()
        } finally {
            this.enabled = false
        }
    }

    open fun onDisable() {
    }

    val dataFolder: File
        get() {
            val folder = File(PluginManager.PLUGINS_DIR, this.viaProxyYaml["name"] as String)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            return folder
        }

    val name: String?
        get() = this.viaProxyYaml["name"] as String?

    val author: String?
        get() = this.viaProxyYaml["author"] as String?

    val version: String
        get() = this.viaProxyYaml["version"].toString()

    val depends: List<String>
        get() {
            @Suppress("UNCHECKED_CAST")
            val depends = this.viaProxyYaml.getOrDefault("depends", emptyList<String>()) as MutableList<String>
            return Collections.unmodifiableList(depends)
        }

    val isEnabled: Boolean
        get() = this.enabled

}
