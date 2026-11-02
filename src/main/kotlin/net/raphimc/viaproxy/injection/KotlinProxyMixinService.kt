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

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinAuditTrail
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory
import org.spongepowered.asm.service.IClassBytecodeProvider
import org.spongepowered.asm.service.IClassProvider
import org.spongepowered.asm.service.IClassTracker
import org.spongepowered.asm.service.IMixinInternal
import org.spongepowered.asm.service.IMixinServiceBootstrap
import org.spongepowered.asm.service.ITransformerProvider
import org.spongepowered.asm.service.MixinServiceAbstract
import org.spongepowered.asm.transformers.MixinClassReader
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader

/**
 * SpongePowered Mixin host service for the standalone proxy. Mixin normally
 * runs inside a game launcher which supplies a service implementation. This
 * service provides the small amount of environment glue needed to apply the
 * mixin configurations directly to the application class loader.
 */
class KotlinProxyMixinService : MixinServiceAbstract() {

    companion object {
        @JvmStatic
        val instance: KotlinProxyMixinService by lazy { KotlinProxyMixinService() }

        private val classLoader: ClassLoader = KotlinProxyMixinService::class.java.classLoader
    }

    private val classProvider = object : IClassProvider {
        @Deprecated("Mixin only uses this for legacy launchers")
        override fun getClassPath(): Array<URL> =
            (classLoader as? URLClassLoader)?.urLs ?: emptyArray()

        override fun findClass(name: String): Class<*> = Class.forName(name, false, classLoader)

        override fun findClass(name: String, initialize: Boolean): Class<*> =
            Class.forName(name, initialize, classLoader)

        override fun findAgentClass(name: String, initialize: Boolean): Class<*> =
            Class.forName(name, initialize, classLoader)
    }

    private val bytecodeProvider = object : IClassBytecodeProvider {
        override fun getClassNode(name: String): ClassNode = getClassNode(name, false, 0)

        override fun getClassNode(name: String, runTransformers: Boolean): ClassNode =
            getClassNode(name, runTransformers, 0)

        override fun getClassNode(name: String, runTransformers: Boolean, readerFlags: Int): ClassNode {
            val resource = name.replace('.', '/') + ".class"
            val bytes = classLoader.getResourceAsStream(resource)
                ?: throw ClassNotFoundException(name)
            val data = bytes.use { it.readBytes() }
            val node = ClassNode()
            MixinClassReader(data, name).accept(node, readerFlags)
            return node
        }
    }

    private val primaryContainer = object : IContainerHandle {
        override fun getId(): String = "kotlinproxy"

        override fun getDescription(): String = "KotlinProxy standalone mixin container"

        override fun getAttribute(name: String): String? = null

        override fun getNestedContainers(): Collection<IContainerHandle> = emptyList()
    }

    @Volatile
    var transformerFactory: IMixinTransformerFactory? = null
        private set

    override fun getName(): String = "KotlinProxy"

    override fun isValid(): Boolean = true

    override fun getClassProvider(): IClassProvider = classProvider

    override fun getBytecodeProvider(): IClassBytecodeProvider = bytecodeProvider

    override fun getTransformerProvider(): ITransformerProvider? = null

    override fun getClassTracker(): IClassTracker? = null

    override fun getAuditTrail(): IMixinAuditTrail? = null

    override fun getPlatformAgents(): Collection<String> = emptyList()

    override fun getPrimaryContainer(): IContainerHandle = primaryContainer

    override fun getResourceAsStream(name: String): InputStream? = classLoader.getResourceAsStream(name)

    override fun getMinCompatibilityLevel(): MixinEnvironment.CompatibilityLevel =
        MixinEnvironment.CompatibilityLevel.JAVA_8

    override fun getMaxCompatibilityLevel(): MixinEnvironment.CompatibilityLevel =
        MixinEnvironment.CompatibilityLevel.JAVA_21

    override fun offer(internal: IMixinInternal) {
        if (internal is IMixinTransformerFactory) {
            this.transformerFactory = internal
        }
        super.offer(internal)
    }

}

class KotlinProxyMixinBootstrap : IMixinServiceBootstrap {

    override fun getName(): String = "KotlinProxy"

    override fun getServiceClassName(): String = KotlinProxyMixinService::class.java.name

    override fun bootstrap() {
        // Nothing to do; the service has no launcher to prepare.
    }

}
