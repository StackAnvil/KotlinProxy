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
package net.raphimc.viaproxy.cli

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import joptsimple.BuiltinHelpFormatter
import joptsimple.OptionDescriptor
import joptsimple.internal.Classes
import joptsimple.internal.Strings

class BetterHelpFormatter : BuiltinHelpFormatter(120, 2) {

    override fun extractTypeIndicator(descriptor: OptionDescriptor): String {
        var indicator: String? = descriptor.argumentTypeIndicator()
        if (indicator != null) {
            indicator = indicator.substring(indicator.indexOf('$') + 1)
            if (indicator.startsWith("[")) {
                return indicator.substring(1, indicator.length - 1)
            }
        }
        return if (!Strings.isNullOrEmpty(indicator) && String::class.java.name != indicator) {
            Classes.shortNameOf(indicator)
        } else {
            "String"
        }
    }

    override fun createDefaultValuesDisplay(defaultValues: List<*>): String {
        if (defaultValues.size == 1) {
            val value = defaultValues[0]
            if (value is ProtocolVersion) {
                return value.name
            } else if (value is String) {
                return "\"" + value + "\""
            }

            return value.toString()
        }

        return defaultValues.toString()
    }

}
