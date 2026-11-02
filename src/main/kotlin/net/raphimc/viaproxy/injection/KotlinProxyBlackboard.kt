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

import org.spongepowered.asm.service.IGlobalPropertyService
import org.spongepowered.asm.service.IPropertyKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Global property store used by Mixin during bootstrap.
 */
class KotlinProxyBlackboard : IGlobalPropertyService {

    private class Key(private val key: String) : IPropertyKey {
        override fun toString(): String = this.key
    }

    private val properties = ConcurrentHashMap<String, Any>()

    override fun resolveKey(name: String): IPropertyKey = Key(name)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getProperty(key: IPropertyKey): T? = this.properties[key.toString()] as T?

    override fun setProperty(key: IPropertyKey, value: Any) {
        this.properties[key.toString()] = value
    }

    override fun <T> getProperty(key: IPropertyKey, defaultValue: T): T =
        this.properties[key.toString()] as? T ?: defaultValue

    override fun getPropertyString(key: IPropertyKey, defaultValue: String): String =
        this.properties[key.toString()]?.toString() ?: defaultValue

}
