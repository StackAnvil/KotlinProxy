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
package net.raphimc.viaproxy.util.config

import net.lenni0451.optconfig.serializer.ConfigTypeSerializer
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig
import net.raphimc.viaproxy.saves.impl.accounts.Account

class AccountTypeSerializer(private val configInstance: ViaProxyConfig) :
    ConfigTypeSerializer<ViaProxyConfig, Account>(configInstance) {

    override fun deserialize(typeClass: Class<Account>, serializedObject: Any?): Account? {
        val accounts = ViaProxy.getSaveManager().accountsSave.accounts
        val accountIndex = serializedObject as Int
        return if (this.configInstance.getAuthMethod() == ViaProxyConfig.AuthMethod.ACCOUNT && accountIndex >= 0 && accountIndex < accounts.size) {
            accounts[accountIndex]
        } else {
            null
        }
    }

    override fun serialize(obj: Account?): Any {
        return if (obj != null) {
            ViaProxy.getSaveManager().accountsSave.accounts.indexOf(obj)
        } else {
            0
        }
    }

}
