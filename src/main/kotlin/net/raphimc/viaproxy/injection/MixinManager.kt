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
package net.raphimc.viaproxy.injection

import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.mixin.transformer.IMixinTransformer
import org.spongepowered.asm.service.MixinService
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * Boots the SpongePowered Mixin subsystem and installs its transformer.
 */
object MixinManager {

    const val MIXIN_CONFIG = "mixins.kotlinproxy.json"

    private val TRANSFORMED_PACKAGES = arrayOf(
        "net/raphimc/",
        "com/viaversion/",
        "com/github/",
        "org/cloudburstmc/"
    )

    @Volatile
    private var transformer: IMixinTransformer? = null

    @JvmStatic
    @Synchronized
    fun initialize() {
        if (this.transformer != null) {
            return
        }

        MixinBootstrap.init()
        val environment = MixinEnvironment.getDefaultEnvironment()

        var active = environment.activeTransformer as? IMixinTransformer
        if (active == null) {
            val service = MixinService.getService() as KotlinProxyMixinService
            val factory = service.transformerFactory
                ?: throw IllegalStateException("Mixin transformer factory was not offered by the service")
            active = factory.createTransformer()
        }

        Mixins.addConfiguration(MIXIN_CONFIG)
        this.transformer = active
    }

    @JvmStatic
    fun transform(className: String, classBytes: ByteArray): ByteArray? =
        this.transformer?.transformClassBytes(className, className, classBytes)

    @JvmStatic
    fun shouldTransform(className: String): Boolean =
        TRANSFORMED_PACKAGES.any { className.startsWith(it) }

    @JvmStatic
    fun installMixins(instrumentation: Instrumentation) {
        initialize()
        instrumentation.addTransformer(KotlinProxyClassTransformer(), true)
    }

}

class KotlinProxyClassTransformer : ClassFileTransformer {

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        if (className == null || classfileBuffer == null) {
            return null
        }
        if (!MixinManager.shouldTransform(className)) {
            return null
        }

        return try {
            MixinManager.transform(className, classfileBuffer)
        } catch (throwable: Throwable) {
            System.err.println("Failed to apply mixins to $className")
            throwable.printStackTrace()
            null
        }
    }

}
