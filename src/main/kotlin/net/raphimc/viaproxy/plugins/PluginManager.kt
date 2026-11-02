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

import com.vdurmont.semver4j.Semver
import net.lenni0451.reflect.stream.RStream
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.util.logging.Logger
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URLClassLoader
import java.util.Collections

class PluginManager {

    companion object {
        @JvmField
        val PLUGINS_DIR = File(ViaProxy.getCwd(), "plugins")
    }

    private val yaml = Yaml()
    private val pluginList = ArrayList<ViaProxyPlugin>()

    init {
        this.loadPlugins()
        Runtime.getRuntime().addShutdownHook(Thread(this::unloadPlugins))
    }

    val plugins: List<ViaProxyPlugin>
        get() = Collections.unmodifiableList(this.pluginList)

    fun getPlugin(name: String): ViaProxyPlugin? =
        this.pluginList.firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun loadPlugins() {
        if (!PLUGINS_DIR.exists() || !PLUGINS_DIR.isDirectory) {
            if (!PLUGINS_DIR.mkdirs()) {
                return
            }
        }

        val files = PLUGINS_DIR.listFiles() ?: return
        for (file in files) {
            if (!file.name.lowercase().endsWith(".jar")) continue
            try {
                this.loadAndScanJar(file)
            } catch (throwable: Throwable) {
                Logger.LOGGER.error("Unable to load plugin '${file.name}'", throwable)
            }
        }

        for (plugin in this.pluginList) {
            if (!plugin.isEnabled) {
                this.enablePlugin(plugin)
            }
        }
    }

    private fun loadAndScanJar(file: File) {
        val url = file.toURI().toURL()
        val classLoader = URLClassLoader(arrayOf(url), PluginManager::class.java.classLoader)

        val viaproxyYml = classLoader.getResourceAsStream("viaproxy.yml")
            ?: throw IllegalStateException("Plugin '${file.name}' does not have a viaproxy.yml")
        val yaml: Map<String, Any> = viaproxyYml.use { this.yaml.load(it) }
        if (!yaml.containsKey("name")) throw IllegalStateException("Plugin '${file.name}' does not have a name attribute in the viaproxy.yml")
        if (!yaml.containsKey("author")) throw IllegalStateException("Plugin '${file.name}' does not have a author attribute in the viaproxy.yml")
        if (!yaml.containsKey("version")) throw IllegalStateException("Plugin '${file.name}' does not have a version attribute in the viaproxy.yml")
        if (!yaml.containsKey("main")) throw IllegalStateException("Plugin '${file.name}' does not have a main attribute in the viaproxy.yml")

        val minVersion = Semver(yaml.getOrDefault("min-version", "0.0.0").toString())
        if (!ViaProxy.VERSION.startsWith("\${") && minVersion.isGreaterThan(ViaProxy.VERSION.replace("-SNAPSHOT", ""))) {
            throw IllegalStateException("Plugin '${file.name}' requires a newer version of KotlinProxy (v$minVersion)")
        }

        val main = yaml["main"] as String
        val mainClass = classLoader.loadClass(main)
        if (!ViaProxyPlugin::class.java.isAssignableFrom(mainClass)) {
            throw IllegalStateException("Class '${mainClass.name}' from '${file.name}' does not extend ViaProxyPlugin")
        }
        val plugin = mainClass.getDeclaredConstructor().newInstance() as ViaProxyPlugin

        plugin.init(classLoader, yaml)

        if (plugin.depends.size > 1) {
            throw IllegalStateException("Plugin '${file.name}' has more than one dependency. This is not supported yet.")
        }

        Logger.LOGGER.info("Loaded plugin '${plugin.name}' by ${plugin.author} (v${plugin.version})")
        this.pluginList.add(plugin)
    }

    private fun enablePlugin(plugin: ViaProxyPlugin) {
        for (depend in plugin.depends) {
            val dependPlugin = this.getPlugin(depend)
            if (dependPlugin == null) {
                Logger.LOGGER.error("Plugin '${plugin.name}' depends on '$depend' which is not loaded")
                return
            }
            if (!dependPlugin.isEnabled) {
                this.enablePlugin(dependPlugin)
            }

            RStream.of(plugin.classLoader).withSuper().fields().by("parent").set(dependPlugin.classLoader)
        }

        try {
            plugin.enable()
            Logger.LOGGER.info("Enabled plugin '${plugin.name}'")
        } catch (throwable: Throwable) {
            Logger.LOGGER.error("Failed to enable plugin '${plugin.name}'", throwable)
        }
    }

    private fun unloadPlugins() {
        for (plugin in this.pluginList) {
            if (plugin.isEnabled) {
                this.disablePlugin(plugin)
            }
        }
    }

    private fun disablePlugin(plugin: ViaProxyPlugin) {
        for (depend in plugin.depends) {
            val dependPlugin = this.getPlugin(depend)
            if (dependPlugin == null) {
                Logger.LOGGER.error("Plugin '${plugin.name}' depends on '$depend' which is not loaded")
                return
            }
            if (dependPlugin.isEnabled) {
                this.disablePlugin(dependPlugin)
            }
        }

        try {
            plugin.disable()
            Logger.LOGGER.info("Disabled plugin '${plugin.name}'")
        } catch (throwable: Throwable) {
            Logger.LOGGER.error("Failed to disable plugin '${plugin.name}'", throwable)
        }
    }

}
