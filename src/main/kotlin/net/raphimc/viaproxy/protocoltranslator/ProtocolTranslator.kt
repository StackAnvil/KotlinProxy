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
package net.raphimc.viaproxy.protocoltranslator

import com.viaversion.viaaprilfools.ViaAprilFoolsPlatformImpl
import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion
import com.viaversion.viabackwards.ViaBackwardsPlatformImpl
import com.viaversion.viarewind.ViaRewindPlatformImpl
import com.viaversion.viaversion.ViaManagerImpl
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.api.protocol.version.VersionType
import com.viaversion.viaversion.commands.ViaCommandHandler
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5
import net.lenni0451.reflect.Classes
import net.raphimc.viabedrock.ViaBedrockPlatformImpl
import net.raphimc.viabedrock.api.BedrockProtocolVersion
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.ProtocolTranslatorInitEvent
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyInjector
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyPlatformLoader
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaLegacyPlatform
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaVersionPlatform
import net.raphimc.viaproxy.util.YamlConfigPatcher
import net.raphimc.viaproxy.util.logging.Logger
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Comparator
import java.util.LinkedHashMap

class ProtocolTranslator {

    companion object {

        @JvmField
        val AUTO_DETECT_PROTOCOL: ProtocolVersion =
            object : ProtocolVersion(VersionType.SPECIAL, -2, -1, "Auto Detect (1.7+ servers)", null) {

                protected override fun customComparator(): Comparator<ProtocolVersion> {
                    return Comparator { o1, o2 ->
                        if (o1 === AUTO_DETECT_PROTOCOL) {
                            1
                        } else if (o2 === AUTO_DETECT_PROTOCOL) {
                            -1
                        } else {
                            0
                        }
                    }
                }

                override fun isKnown(): Boolean {
                    return false
                }

            }

        @JvmStatic
        fun init() {
            moveConfigs()
            patchConfigs()
            val additionalPlatformSuppliers = ViaProxy.EVENT_MANAGER.call(ProtocolTranslatorInitEvent()).getPlatformSuppliers().toTypedArray()
            ViaManagerImpl.initAndLoad(ViaProxyViaVersionPlatform(), ViaProxyInjector(), ViaCommandHandler(false), ViaProxyPlatformLoader(), Runnable {
                if (java.lang.Boolean.parseBoolean(System.getProperty("viaproxy.enableViaBackwards", "true"))) {
                    ViaBackwardsPlatformImpl()
                }
                if (java.lang.Boolean.parseBoolean(System.getProperty("viaproxy.enableViaRewind", "true"))) {
                    ViaRewindPlatformImpl()
                }
                if (java.lang.Boolean.parseBoolean(System.getProperty("viaproxy.enableViaLegacy", "true"))) {
                    ViaProxyViaLegacyPlatform()
                } else {
                    Classes.ensureInitialized(LegacyProtocolVersion::class.java) // Register the versions to avoid crashes when using the API
                }
                if (java.lang.Boolean.parseBoolean(System.getProperty("viaproxy.enableViaAprilFools", "true"))) {
                    ViaAprilFoolsPlatformImpl()
                } else {
                    Classes.ensureInitialized(AprilFoolsProtocolVersion::class.java) // Register the versions to avoid crashes when using the API
                }
                if (java.lang.Boolean.parseBoolean(System.getProperty("viaproxy.enableViaBedrock", "true"))) {
                    ViaBedrockPlatformImpl()
                } else {
                    Classes.ensureInitialized(BedrockProtocolVersion::class.java) // Register the versions to avoid crashes when using the API
                }
                for (platformSupplier in additionalPlatformSuppliers) {
                    platformSupplier.get()
                }
            })
            Protocol1_20_3To1_20_5.strictErrorHandling = false
            ProtocolVersion.register(AUTO_DETECT_PROTOCOL)
        }

        // Migrate from old config location (ViaLoader folder) to new one
        private fun moveConfigs() {
            try {
                val oldConfigDir = File(ViaProxy.getCwd(), "ViaLoader")
                if (!oldConfigDir.exists() || !oldConfigDir.isDirectory) {
                    return
                }
                for (file in oldConfigDir.listFiles()!!) {
                    FileUtils.moveToDirectory(file, ViaProxy.getCwd(), true)
                }
                FileUtils.deleteDirectory(oldConfigDir)
            } catch (e: Throwable) {
                Logger.LOGGER.error("Failed to migrate old ViaLoader configs", e)
            }
        }

        private fun patchConfigs() {
            try {
                val configPatcher = YamlConfigPatcher(File(ViaProxy.getCwd(), "viaversion.yml"))
                val config = configPatcher.getConfig() as MutableMap<String, Any>
                config.putIfAbsent("1_13-tab-complete-delay", 5)
                config.putIfAbsent("no-delay-shield-blocking", true)
                config.putIfAbsent("handle-invalid-item-count", true)
                config.putIfAbsent("send-player-details", false)
                config.putIfAbsent("backup-nested-items", true)
                val packetLimiter = config.computeIfAbsent("packet-limiter") { LinkedHashMap<String, Any>() } as MutableMap<String, Any>
                packetLimiter.putIfAbsent("max-per-second", 1400)
                packetLimiter.putIfAbsent("sustained-max-per-second", 400)
                val logging = config.computeIfAbsent("logging") { LinkedHashMap<String, Any>() } as MutableMap<String, Any>
                logging.putIfAbsent("log-text-component-conversion-errors", true)
                logging.putIfAbsent("log-other-conversion-warnings", true)
                configPatcher.write()
            } catch (e: Throwable) {
                throw RuntimeException("Failed to patch ViaVersion config", e)
            }

            try {
                val configPatcher = YamlConfigPatcher(File(ViaProxy.getCwd(), "viabackwards.yml"))
                val config = configPatcher.getConfig() as MutableMap<String, Any>
                config.putIfAbsent("fix-1_13-face-player", true)
                config.putIfAbsent("handle-pings-as-inv-acknowledgements", true)
                configPatcher.write()
            } catch (e: Throwable) {
                throw RuntimeException("Failed to patch ViaBackwards config", e)
            }

            try {
                val configPatcher = YamlConfigPatcher(File(ViaProxy.getCwd(), "viarewind.yml"))
                val config = configPatcher.getConfig() as MutableMap<String, Any>
                config.putIfAbsent("replace-adventure", true)
                config.putIfAbsent("replace-particles", true)
                configPatcher.write()
            } catch (e: Throwable) {
                throw RuntimeException("Failed to patch ViaRewind config", e)
            }
        }

    }

}
