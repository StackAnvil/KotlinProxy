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
package net.raphimc.viaproxy.util.logging

import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import java.util.regex.Pattern

@Plugin(name = "ip_redactor", category = PatternConverter.CATEGORY)
@ConverterKeys("ip_redactor")
class IpRedactor private constructor() : LogEventPatternConverter("IpRedactor", null) {

    private val IPV4_REGEX = Pattern.compile("(\\b25[0-5]|\\b2[0-4][0-9]|\\b[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}")
    private val IPV6_REGEX = Pattern.compile("(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))")

    companion object {
        @JvmStatic
        fun newInstance(options: Array<String>): IpRedactor {
            return IpRedactor()
        }
    }

    override fun format(event: LogEvent, toAppendTo: StringBuilder) {
        // The config is null until KotlinProxy has loaded it, but getConfig() is declared non-null.
        val config: ViaProxyConfig? = try {
            ViaProxy.getConfig()
        } catch (ignored: NullPointerException) {
            null
        }
        if (config != null && !config.shouldLogIps()) {
            var message = toAppendTo.toString()
            message = IPV4_REGEX.matcher(message).replaceAll("REDACTED_IP")
            message = IPV6_REGEX.matcher(message).replaceAll("REDACTED_IP")
            toAppendTo.setLength(0)
            toAppendTo.append(message)
        }
    }

}
