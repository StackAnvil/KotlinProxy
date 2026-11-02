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
import net.raphimc.viaproxy.bootstrap.MixinHook
import org.spongepowered.asm.service.MixinService

/**
 * Boots the SpongePowered Mixin subsystem and installs its transformer.
 */
object MixinManager {

    const val MIXIN_CONFIG = "mixins.kotlinproxy.json"

    @Volatile
    private var transformer: IMixinTransformer? = null

    @JvmStatic
    @Synchronized
    fun initialize() {
        if (this.transformer != null) {
            return
        }

        MixinBootstrap.init()
        val service = MixinService.getService() as KotlinProxyMixinService
        Mixins.addConfiguration(MIXIN_CONFIG, service.containerHandle)
        gotoDefaultPhase()

        val environment = MixinEnvironment.getDefaultEnvironment()
        var active = environment.activeTransformer as? IMixinTransformer
        if (active == null) {
            val factory = service.transformerFactory
                ?: throw IllegalStateException("Mixin transformer factory was not offered by the service")
            active = factory.createTransformer()
        }
        this.transformer = active
        MixinHook.transformer = MixinHook.Transformer { className, classBytes -> transform(className, classBytes) }
        MixinHook.generator = MixinHook.Generator { className -> generateClass(className) }
    }

    private fun gotoDefaultPhase() {
        val method = MixinEnvironment::class.java.getDeclaredMethod(
            "gotoPhase",
            MixinEnvironment.Phase::class.java
        )
        method.isAccessible = true
        method.invoke(null, MixinEnvironment.Phase.DEFAULT)
    }

    @JvmStatic
    fun transform(className: String, classBytes: ByteArray): ByteArray? {
        val dottedName = className.replace('/', '.')
        return this.transformer?.transformClassBytes(dottedName, dottedName, classBytes)
    }

    @JvmStatic
    fun generateClass(className: String): ByteArray? {
        val dottedName = className.replace('/', '.')
        return this.transformer?.transformClassBytes(dottedName, dottedName, null)
    }

}
