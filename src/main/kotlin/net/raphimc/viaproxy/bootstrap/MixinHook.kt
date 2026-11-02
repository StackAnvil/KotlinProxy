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

/**
 * Bridge between the bootstrap class loader and the application class loader.
 * This class is intentionally loaded by the parent loader so both sides see the
 * same instance.
 */
object MixinHook {

    fun interface Transformer {
        fun transform(className: String, classBytes: ByteArray): ByteArray?
    }

    fun interface Generator {
        fun generate(className: String): ByteArray?
    }

    @JvmStatic
    var transformer: Transformer? = null

    @JvmStatic
    var generator: Generator? = null

}
